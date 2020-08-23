/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boubei.tss.dm.record.RecordField;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.entity.InvSoi;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OrderItem;

/**
 *  库存分配规则执行引擎。
 *  
 *  常见筛选规则：
		批次：货物状态、批号及自定义批次属性，(严格、优先、忽略)
		包装：严格按订单、往上取整、往下取零
		时间：生产（过期）日期、入库时间
		库位用途：退货暂存区、异常暂存区、发货暂存区、收货暂存区、拣选区（高/中/低俗）、存储区
			只分配拣选库位、先拣选库位，后存储库位、严格（零拣选库位，整存储库位）、优先（零拣选库位，整存储库位）
		库存量：库存量大（小）库位优先、最适量优先（inv.qty > oi.qty order by inv.qty asc）
 *  
 *  示例：某鞋服电商仓：
		B2C（严格批次、先拣选后存储、库存量小库位优先）
		B2B（严格批次、零拣选整存储、库存量大库位优先）
 */
@Component
public class PkRuleEngine {
	
	static final Logger log = Logger.getLogger(PkRuleEngine.class);
	
	@Autowired protected InventoryDao invDao;
	
	// 在库存表里查询
    static final String queryInInv = "select soi.id*1 , inv.id, p.text, (inv.qty - inv.qty_locked), soi.qty, " +
    		" inv.createdate, inv.expiredate, inv.invStatus, inv.lotatt01, inv.lotatt02, inv.lotatt03, inv.lotatt04, " +
    		" soi.invStatus, soi.lotatt01, soi.lotatt02, soi.lotatt03, soi.lotatt04" +
        " from wms_order_item soi, wms_order so, wms_inv inv, wms_location l, component_param p " +
        " where soi.order_id = so.id " +
        "	and so.warehouse_id = inv.wh_id " +
        "	and so.owner_id = inv.owner_id " +
        "	and inv.location_id = l.id and l.type_id = p.id " +
        "	and inv.sku_id = soi.sku_id " +
        "	and inv.qty > inv.qty_locked " + /* 从库存表取时去掉库存可用量为零的库存 */
        "	and IFNULL(l.holding, 0) + IFNULL(l.checking, 0) < 1  " + /* 剔除掉冻结 或 盘点中库位上的库存 */
        "	and soi.id in (select id from TBL_TEMP_ where thread = ? ) ";
    
    // 在候选集里查询
    static final String queryInCandidateInv = "select t.soi_id, t.inv_id, t.locType, t.invQty, t.soiQty, " +
        "     t.invCreatedate, t.invExpiredate, t.invStatus, t.invLotAtt01, t.invLotAtt02, t.invLotAtt03, t.invLotAtt04, " +
        "     t.soiCreatedate, t.soiExpiredate, t.soiStatus, t.soiLotAtt01, t.soiLotAtt02, t.soiLotAtt03, t.soiLotAtt04, " +
        "     t.candidateIndex, t.thread, t.id " +                           
        " from wms_inv_soi t" +
        " where 1=1 and thread = ? ";
    
    /**
     * 规则池的运算符集
     */
    static final List<String> operatorList = Arrays.asList(">", "<", "=", "<=", ">=", "<>", "in");

    /**
     * 使用规则池的拣货推荐逻辑实现批量取出满足各soi要求的库存列表。
     *
     * @param ruleContent
	 * 	    10 from 所有库存 where 生产日期 > {sysdate-30} 
	                      and 货物状态 严格 
	                      and 批次属性02 严格 
	            order by 生产时间 升序 into 候选集1;
		    20 from (copy)候选集1 where 库位类型 in ( 拣选库位 ) order by 库存量 升序, 入库时间 降序 into 最终结果集;
		    30 from (move)候选集1 where 库位类型 in ( 存储库位 ) order by 库存量 降序 into 最终结果集;
		    40 from 候选集1 order by 库存量 降序 into 最终结果集;
     *
     */
    public Map<Long, List<Inventory>> excuteRulesPool(List<OrderItem> oItems, String ruleContent) {
        
        invDao.deleteAll( invDao.getEntities("from InvSoi where thread=?", Environment.threadID()) );
        invDao.deleteAll( invDao.getEntities("from Temp where thread=?", Environment.threadID()) );
        
        invDao.insertEntityIds2TempTable(oItems);
        
        Map<String, String> myLotatts = new HashMap<String, String>();
        myLotatts.put("createdate", "生产日期");
        myLotatts.put("expiredate", "过期日期");
        myLotatts.put("invStatus", "货物状态");
        myLotatts.put("lotatt01", "批号");
        myLotatts.put("lotatt02", "装箱量");
        myLotatts.put("lotatt03", "批次属性3");
        myLotatts.put("lotatt04", "批次属性4");
        List<?> list = invDao.getEntities(" from RecordField where table is null and domain = ? ", Environment.getDomain());
        for( Object o : list ) {
        	RecordField rf = (RecordField) o;
        	myLotatts.put(rf.getLabel(), myLotatts.get(rf.getCode())); // 域批次别名 映射 默认别名 （eg：颜色 --> lotatt03 --> 批次属性3 ）
        }
        
        // 1、解析规则池内容
        String[] rules = ruleContent.split(";");
        for (String rule : rules) {
            if( EasyUtils.isNullOrEmpty(rule) ) continue;
            
            String sql = null;
            boolean isCopy = false; // copy or move
            boolean isQueryInInv = false;
            
            int fromIndex    = rule.indexOf(" from ");
            int whereIndex   = rule.indexOf(" where ");
            int orderByIndex = rule.indexOf(" order by ");
            int intoIndex    = rule.indexOf(" into ");
            
            String from = rule.substring(fromIndex + 5, whereIndex < 0 ? (orderByIndex < 0 ? intoIndex : orderByIndex) : whereIndex).trim();
            if(from.startsWith("(copy)")) {
                isCopy = true;
                from = from.substring(6).trim();
            } 
            else if(from.startsWith("(move)")) {
                isCopy = false;
                from = from.substring(6).trim();
            }
            
            if(from.equals("所有库存") || from.equals("库存表")) {
                isQueryInInv = true;
                sql = queryInInv;
            } 
            else if(from.startsWith("候选集")) { // 可以有多个候选集
                sql = queryInCandidateInv;
                String fromCandidateIndex = from.substring(3).trim(); // 截去前三个字符，得到候选集序号。
                sql += " and t.candidateIndex = " + fromCandidateIndex + " ";
            } 
            else {
            	continue;
            }
            
            if(whereIndex > 0) {
                List<Object> conditions = new ArrayList<Object>();
                
                String where = rule.substring(whereIndex + 6, orderByIndex < 0 ? intoIndex : orderByIndex).trim();
                String[] whereConditions = where.split("and"); 
                for (String condition : whereConditions) {
                    
                    conditions.add(" and ");
                    
                    StringTokenizer stIn = new StringTokenizer(condition); // 规则条件符号之间需以“空格”分隔
                    L: while (stIn.hasMoreElements()) {
                        String nextToken = stIn.nextToken().trim(); 
                        
                        // 运算符、括号等
                        if(operatorList.contains(nextToken) || nextToken.equals("(") || nextToken.equals(")")) {
                            conditions.add(nextToken);
                            continue;
                        }
                        
                        // 原生的条件，不需要解析（不能有空格）
                        if(nextToken.startsWith("{") && nextToken.endsWith("}")) {
                            conditions.add(nextToken.subSequence(1, nextToken.length() - 1));
                            continue;
                        }
                        
                        // 库位类型: 库位用途 in ( 拣选区 )
                        if( nextToken.equals("库位用途") ) {
                            conditions.add(isQueryInInv ? "p.text" : "t.locType");
                            continue;
                        }
                        for(String locType : WMS._LOC_TYPES) {
                            if( nextToken.equals( locType ) ) {
                                conditions.add( "'" + locType + "'" );
                                continue L;
                            }
                        }
                        
                        // 库存数量： 库存量 > 订单量
                        if(nextToken.equals("库存量")) {
                            conditions.add(isQueryInInv ? "inv.qty - inv.qty_locked" : "t.invQty");
                            continue;
                        }
                        if(nextToken.equals("订单量")) {
                            conditions.add(isQueryInInv ? "soi.qty" : "t.soiQty");
                            continue;
                        }
                        
                        // 批次属性:  inv.货物状态 严格 and 批次属性02 严格
                        nextToken = (String) EasyUtils.checkNull( myLotatts.get(nextToken), nextToken );
                        
                        if( nextToken.equals("生产日期") ) {
                            conditions.add(isQueryInInv ? "inv.createdate" : "t.invCreatedate");
                            continue;
                        }
                        if( nextToken.equals("过期日期") ) {
                            conditions.add(isQueryInInv ? "inv.expiredate" : "t.invExpiredate");
                            continue;
                        }
                        if(nextToken.equals("货物状态")) {
                            conditions.add(isQueryInInv ? "IFNULL(inv.invStatus, '1') = IFNULL(soi.invStatus, '1')" : "IFNULL(t.invStatus, '1') = IFNULL(t.soiStatus, '1')");
                            continue;
                        }
                        
                        if(nextToken.equals("批号")) {
                            conditions.add(isQueryInInv ? "IFNULL(inv.lotatt01, '1') = IFNULL(soi.lotatt01, '1')" : "IFNULL(t.invLotAtt01, '1') = IFNULL(t.soiLotAtt01, '1')");
                            continue;
                        }
                        if(nextToken.equals("装箱量")) {
                        	conditions.add(isQueryInInv ? "IFNULL(inv.lotatt02, '1') = IFNULL(soi.lotatt02, '1')" : "IFNULL(t.invLotAtt02, '1') = IFNULL(t.soiLotAtt02, '1')");
                            continue;
                        }
                        if( nextToken.startsWith("批次属性") ) {
                            String lotattIndex = nextToken.substring(4).trim(); // lotatt序号：02
                            conditions.add(isQueryInInv ? 
                                    "IFNULL(inv.lotatt" +lotattIndex+ ", '1')  = IFNULL(soi.lotatt" +lotattIndex+ ", '1')" : 
                                    "IFNULL(t.invLotAtt" +lotattIndex+ ", '1') = IFNULL(t.soiLotAtt" +lotattIndex+ ", '1')");
                            continue;
                        }
                    }
                }
                
                for(Object temp : conditions) {
                    sql += " " + temp;
                }
            }
            
            // 排序
            sql += " order by ";
            if(orderByIndex > 0) {
                String orderby = rule.substring(orderByIndex + 9, intoIndex).trim();
                String[] orderbyFileds = orderby.split(",");
                for(String orderbyFiled : orderbyFileds) {
                    StringTokenizer stIn = new StringTokenizer(orderbyFiled); 
                    while (stIn.hasMoreElements()) {
                        String nextToken = stIn.nextToken().trim(); 
                        
                        nextToken = (String) EasyUtils.checkNull( myLotatts.get(nextToken), nextToken );
                        if(nextToken.equals("生产日期")) {
                            sql += (isQueryInInv ? "inv.createdate" : "t.invCreatedate");
                            continue;
                        }
                        if(nextToken.equals("过期日期")) {
                            sql += (isQueryInInv ? "inv.expiredate" : "t.invExpiredate");
                            continue;
                        }
                        if(nextToken.equals("库存量")) {
                            sql += (isQueryInInv ? "inv.qty - inv.qty_locked" : "t.invQty");
                            continue;
                        }
                        
                        if(nextToken.equals("升序") || nextToken.equals("asc")) {
                            sql += " asc ";
                        }
                        if(nextToken.equals("降序") || nextToken.equals("desc")) {
                            sql += " desc ";
                        }
                    }
                    
                    sql += ", ";
                }
            }
            sql += isQueryInInv ? " inv.id " : " t.inv_id "; // 最后一级排序，没有其他排序的时候这个作为默认排序
            
            String into = rule.substring(intoIndex + 5).trim();
            int toCandidateIndex = -1;
            if(into.equals("最终结果集")) {
                toCandidateIndex = 0;
            } 
            else { // 候选集X
                toCandidateIndex = Integer.parseInt(into.substring(3).trim());
            }
            
            // 逐条执行规则
            log.info("excute rule: " + rule + " ----> " + sql);
            try {
                if(isQueryInInv) {
                    excuteRuleStep(sql, toCandidateIndex);
                } else {
                    excuteRuleStep2(sql, toCandidateIndex, isCopy);
                }
            } catch (Exception e) {
                throw new BusinessException(e.getMessage());
            }
        }

        log.info("excuteRuleSqls end!");
        
        // 3、返回库存结果集
        return queryFinalResult();
    }
    
    void excuteRuleStep(String sql, int toCandidateIndex) throws SQLException {
        List<?> list = invDao.getEntitiesByNativeSql(sql, Environment.threadID());
        for (Object row : list) {
        	Object[] objs = (Object[]) row;
        	
            InvSoi t = new InvSoi();
            t.setSoi_id( EasyUtils.obj2Long(objs[0]) );
            t.setInv_id( EasyUtils.obj2Long(objs[1]) );
            t.setLocType((String) objs[2]);
            t.setInvQty( EasyUtils.obj2Double(objs[3]) );
            t.setSoiQty( EasyUtils.obj2Double(objs[4]) );
            
            t.setInvCreatedate((Date) objs[5]);
            t.setInvExpiredate((Date) objs[6]);
            t.setInvStatus(  (String) objs[7]);
            t.setInvLotAtt01((String) objs[8]);
            t.setInvLotAtt02((String) objs[9]);
            t.setInvLotAtt03((String) objs[10]);
            t.setInvLotAtt04((String) objs[11]);
            
            t.setSoiStatus(  (String) objs[12]);
            t.setSoiLotAtt01((String) objs[13]);
            t.setSoiLotAtt02((String) objs[14]);
            t.setSoiLotAtt03((String) objs[15]);
            t.setSoiLotAtt04((String) objs[16]);
            
            t.setCandidateIndex(toCandidateIndex);
            t.setThread(Environment.threadID());
            
            invDao.createObject(t);
        }
    }
    
	void excuteRuleStep2(String sql, int inCandidateIndex, boolean isCopy) {
    	List<?> list = invDao.getEntitiesByNativeSql(sql, InvSoi.class, Environment.threadID()); 
    	for(Object t : list) {
    		if( !isCopy ) { // if move, 删除从原结果集里删除
    			invDao.delete(t); // InvSoi
            } else {
            	invDao.evict(t);
            }
    		
    		InvSoi _t = new InvSoi();
    		BeanUtil.copy(_t, t, "id".split(","));
    		_t.setCandidateIndex(inCandidateIndex);
        	invDao.createObject(_t);
    	}
    }
    
    Map<Long, List<Inventory>> queryFinalResult() {
        log.info("queryFinalResult start!");
        
        Map<Long, List<Inventory>> soi_invs = new HashMap<Long, List<Inventory>>();
        long threadID = Environment.threadID();
        
        // 先批量取出库存，放入一级缓存
        invDao.getEntities(" select o from Inventory o, InvSoi t where o.id=t.inv_id and t.candidateIndex=0 and t.thread=? ", threadID); // 取到二级缓存里
       
		List<?> list = invDao.getEntities("from InvSoi where candidateIndex=0 and thread=? order by id", threadID);
        for(Object obj : list) {
        	InvSoi is = (InvSoi) obj;
			Long soiId = is.getSoi_id();

			List<Inventory> invList = soi_invs.get(soiId);
			if (invList == null) {
				soi_invs.put(soiId, invList = new ArrayList<Inventory>());
			}

			// 上面已经批量取出库存了，这里能直接从一级缓存中取到
			Inventory inv = invDao.getEntity(is.getInv_id());
			if (!invList.contains(inv)) {
				invList.add(inv);
			}
        }
        
        log.info("queryFinalResult end!");
        return soi_invs;
    }

}
