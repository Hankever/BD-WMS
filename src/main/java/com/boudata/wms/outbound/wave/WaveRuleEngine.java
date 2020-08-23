/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MacrocodeCompiler;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Warehouse;
import com.boudata.wms.inventory.PkRuleEngine;
import com.boudata.wms.outbound.wave.WaveRule.Step;
import com.boudata.wms.outbound.wave.WaveRule.Subwave;

/**
 * 波次规则执行引擎
 * 
 * 1、波次规则可以指定”边摘边播“ 或 ”先摘后播“ 两种模式，后者的话需要对【库位、货品、批次】一致的拣货明细进行合并 （在前台打印拣货单时合并）
 * 2、波次作业 拣货明细支持 按 sku + 批次 + loc 等维度拆分工单   
 * 3、波次分组指定 ”集货位“（生成拣货工单时，为每个 订单 定好集货位， 作为拣货目的库位）
 * 
 * TODO 拣货单的目的库位怎么设定 ？如果只有一个出货区，则默认绑定；多个出货区（集货区、验货台、交接区）时，需通过域信息指定（或波次分组时人为指定）。
 * 
 */
@Component
public class WaveRuleEngine {
    
    static Log log = LogFactory.getLog(WaveRuleEngine.class);
    
    @Autowired private OrderHDao orderDao;
    @Autowired private OperationDao operationDao;
    
    @Autowired private InvOperation4Order invOperation;
    @Autowired private PkRuleEngine pkRuleEngine;
    
	public OrderWave excuteRule(OrderWave wave, List<Long> orderIds) {
    	
    	List<OrderItem> oi_list = orderDao.getOrderItems(orderIds);
    	
    	 // 获取波次作业规则
        _Warehouse wh = wave.getWarehouse();
        _Owner owner = oi_list.get(0).getOrder().getOwner();
        String ruleContent = _Util.ruleDef(wave.rules, _Rule.DEFAULT_WV_RULE, owner.getWaver(), wh.getWaver());
    	
        WaveRule rule = WaveRuleFactory.init(ruleContent);
        rule.step_ois.put(WaveRule.ALL_OIS, _Util.getIDs(oi_list));
        
        Map<Long, Double> lastInv_qty = new HashMap<Long, Double>(); // 最新库存余量
        Map<Long, Double> lastOi_qty  = new HashMap<Long, Double>(); // 最新的待分配量
        Map<Long, List<OperationItem>> oh_pkds = new HashMap<Long, List<OperationItem>>(); 
        
        for(Step step : rule.stepsMap.values()) {
            log.info(wave.getCode() + "." + step.getName() + " start ---------- ");

            List<OrderItem> stepOIs = queryOIsByStep(step, rule, orderIds);
            if( stepOIs.isEmpty() )  continue;
 
            // 批量取出符合条件的库存列表，按 oiId / invs 的格式存放于invsMap中
            owner = stepOIs.get(0).getOrder().getOwner();
            String pkRule = operationDao.getPKRule(wh, owner, step.rule, wave.rules);
            Map<Long, List<Inventory>> oi_invs = pkRuleEngine.excuteRulesPool(stepOIs, pkRule);
            for (List<Inventory> invs : oi_invs.values()) {
                /* 结合上一轮库存余量映射（用于多个step分配操作时）。如果该库存已经在上一轮参与过了预分配，则直接使用上一轮剩下的可用量。 */
                for (Inventory inv : invs) {
                    Long invId = inv.getId();
					if( !lastInv_qty.containsKey(invId) ) { 
                        lastInv_qty.put(invId, inv.getQty_avaiable()); 
                    }
                }
            }
            
            Object[] pre_result = invOperation.prePickup(stepOIs, oi_invs, lastInv_qty, lastOi_qty);
            @SuppressWarnings("unchecked")
			List<OperationItem> pre_pkds  = (List<OperationItem>) pre_result[0]; 
            @SuppressWarnings("unchecked")
            List<OrderItem> unFinishedOIs = (List<OrderItem>) pre_result[1]; // 未完成分配的OIs，库存不足或者连坐而被剔除PKD的
            
            step.pkds.addAll(pre_pkds);
            
            log.debug("【" + step.getName() + "】.pkd size = " + pre_pkds.size());
            for(OperationItem pkd : pre_pkds) {
                Long ohId = pkd.getOrderitem().getOrder().getId();
                List<OperationItem> tempList = oh_pkds.get(ohId);
                if(tempList == null) {
                    oh_pkds.put(ohId, tempList = new ArrayList<OperationItem>());
                }
                tempList.add(pkd);
            }
            
            /* 
             * 如果本步不允许部分分配，则如果某条OI不满足，需释放其订单其他OI的PKD。
             * 比如多品项订单优先在同一库区拣货，在单一库区无法满足才考虑混合拣货，这样在单一库区拣货的时候不允许部分分配。 
             */
            log.info(wave.getCode() + " dealwith part allocated OHs!");
            if( !step.canPartAllocate() && !EasyUtils.isNullOrEmpty(unFinishedOIs) ) {
                log.debug("波次策略【不允许部分分配】，以下PKD无效: ");
                
                Set<OrderItem> relateOIs = new HashSet<OrderItem>(); // 连坐（PartAllocate=N）而被剔除PKD的OIs.
                for(OrderItem unOI : unFinishedOIs) {
                    Long ohId = unOI.getOrder().getId();
                    List<OperationItem> pkds = oh_pkds.remove(ohId);  //  移除已经生成的PKD，整个订单的。
                    if( pkds == null ) continue;
                    
                    for(OperationItem pkd : pkds) { // PKD maybe belong to other orderItem of the same Order.
                        log.debug("-----" + pkd);
                        
                        OrderItem relateOI = pkd.getOrderitem();
                        Long oiID = relateOI.getId();
                        Long invID = pkd.getOpinv().getId();
                        
                        Double qtyAllocated = pkd.getQty();
                        lastInv_qty.put(invID, MathUtil.addDoubles(lastInv_qty.get(invID), qtyAllocated)); // 归还可用量
                        lastOi_qty.put(oiID,   MathUtil.addDoubles(lastOi_qty.get(oiID), qtyAllocated) ); // 增加待拣货量
                        
                        if( !unFinishedOIs.contains(relateOI) ) {
                            relateOIs.add(relateOI);
                        }
                    }
                }
                
                /* 
                 * 如果是连坐（PartAllocate=N）而被剔除的OI（本来已经生成PKDs且完全分配了），则从lastOi_qty中移除掉。
                 * 因lastOi_qty将被用作打库存不足标记的依据，而这些OI的库存其实是满足的，不应被打标记。
                 * 如后续step再对此OI进行分配，则step自己重新计算其待拣货量（下单量 - 已分配量）。
                 */
                for( OrderItem oi : relateOIs ) {
                    lastOi_qty.remove( oi.getId() );
                }
                
                unFinishedOIs.addAll(relateOIs); // 将连坐的oi也加入unFinishedOIs
            }
            
            // 记录下本step执行后未完成分配的OIs 和 OHs，以便后续step读取
            rule.recordStepResult(step, unFinishedOIs);
            
            log.info(wave.getCode() + "." + step.getName() + " end. ");
        }
        
        /* 处理未能完全分配的OI */
        dealwithNotEnoughOIs(lastOi_qty, oh_pkds);
        
        /* 再一次检查订单是否已经完全分配了。 */
        removeNotCompletelyOHs(oh_pkds);
        
        /* persist pkd */
        List<OperationItem> allPKDs = new ArrayList<OperationItem>();
        for(List<OperationItem> pkds : oh_pkds.values()) {
            allPKDs.addAll(pkds);
        }
        
        if( allPKDs.isEmpty()) {
        	wave.setStatus(WMS.W_STATUS_05); // 库存不足
            return wave;
        }
        
        persistPKDs(allPKDs, rule); 
 
        /* 将没有完成分配的订单从【主波次】中剔除掉 */
        log.info(wave.getCode() + " remove OHs which not fully allocated from the main mave.");
        orderIds.removeAll( oh_pkds.keySet() );  // 除去分配完成的，剩下的订单都未完成。
        if( orderIds.size() > 0 ) {
        	removeOHsFromWave(wave, orderIds);
        }
        
        /* 创建子波次 */
        log.info(wave.getCode() + " start creating subwave by rule!");
        createSubwaves(_Util.getIDs(allPKDs), rule, wave);
        
        /* 处理【跨子波次】的订单的拣货明细*/
        orderTransSubwave(oh_pkds, wave);
        
        /* 分配确认：因为波次分配是不允许部分分配的，所以最后生成的PKD所在的OI、OH可以直接置为分配完成。 */
        List<OperationH> subwaves = operationDao.getSubwaves(wave.getId());
		for(OperationH sw : subwaves) {
			List<OperationItem> items = operationDao.getItems(sw.getId());
			invOperation.execOperations( sw, items );
		}
        
        wave.setStatus(WMS.W_STATUS_03); // 在新建完波次后再把状态直接改成“分配完成”
        return wave;
    }
    
    /**
     * 预拣货确认。
     * 因为波次分配是不允许部分分配的，所以最后生成的PKD所在的OrderItem、OrderH可以直接置为分配完成。
     */
    private void persistPKDs(List<OperationItem> pkds, WaveRule rule) {
    	for (OperationItem pkd : pkds) {
    		pkd.setStatus(WMS.OP_STATUS_01);
        	orderDao.createObject(pkd);
        	rule.persitedPKDs.put(pkd.getId(), pkd);
        	
            OrderItem oi = pkd.getOrderitem();
            oi.setQty_allocated( MathUtil.addDoubles(oi.getQty_allocated(), pkd.getQty()) );
            oi.getOrder().setStatus(WMS.O_STATUS_03);
        }
    	orderDao.flush();
    }
    
    /** 
     * 将没有完成分配的OH从主波次中剔除 到一个独立的波次。 
     * @param wave 主波次
     * @param ohIDs 未完成分配的订单ID列表
     */
    private void removeOHsFromWave(OrderWave wave, List<Long> ohIDs) {
        List<OrderH> notEnoughOHs = orderDao.getOrders(ohIDs);
        
        OrderWave second = new OrderWave();
        second.setCode( SerialNOer.get("Nxxxx") );
        second.setTotal( ohIDs.size() );
        second.setOrigin( WMS.W_ORIGIN_02 );
        second.setStatus( WMS.W_STATUS_05) ;
        second.setWarehouse( wave.getWarehouse() );
        orderDao.createObject(second);
        
        for(OrderH oh : notEnoughOHs) {
            oh.setWave( second );
            oh.setTag( (String) EasyUtils.checkNull(oh.getTag(), "未参与分配") );
        }
        wave.setTotal(wave.getTotal() - notEnoughOHs.size());
    }

    private List<OrderItem> queryOIsByStep(Step step, WaveRule rule, List<Long> orderIds) {
        String sql = step.sql;
        if( sql != null ) {
            if( step.insertTemps.size() > 0 ) {
                orderIds = new ArrayList<Long>();
                for(String key : step.insertTemps) {
                    Set<Long> temps = rule.step_ois.get(key.trim());
                    orderIds.addAll( _Util.fixCollection(temps) );
                }
            } 
            
            orderDao.insertIds2TempTable(orderIds);
            sql = MacrocodeCompiler.run(sql, rule.properties);
            
            step.oiIDs.addAll( queryDataBySQL(sql) );
        }
        for(String key : step.includes) {
            Set<Long> temps = rule.step_ois.get(key.trim());
            step.oiIDs.addAll( _Util.fixCollection(temps) );
        }
        for(String key : step.excludes) {
            Set<Long> temps = rule.step_ois.get(key.trim());
            step.oiIDs.removeAll( _Util.fixCollection(temps) );
        }
        
        // 把查询结果按step存起来
        rule.step_ois.put(step.getName() + ".OIs", step.oiIDs);
        
        List<OrderItem> oiList = orderDao.getOrderItemsByIds(step.oiIDs);
        log.debug("【" + step.getName() + "】.oItems = " + oiList);
        
        return oiList;
    }
    
    /**
     * 循环检查lastOi_qty里OI的待分配量，如果大于0（说明未完成分配），
     * 则释放其所在订单产生的PKDs（如果有的话，包括同OH的所有OI的PKD，即不允许部分分配）。
     * 同时为库存不足的 OrderItem 和 Order 打上库存不足的标记。
     */
    private void dealwithNotEnoughOIs( Map<Long, Double> lastOi_qty, Map<Long, List<OperationItem>> oh_PKDs ) {
        for(Long oiID : lastOi_qty.keySet()) {
        	Double shortageQty = lastOi_qty.get(oiID);
            if(shortageQty == 0) continue;
            
            // 设置库存不足标记
            OrderItem oi = (OrderItem) orderDao.getEntity(OrderItem.class, oiID); 
            oi.setRemark("缺货量=" + shortageQty);
            
            OrderH oh = oi.getOrder();
            oh.setTag( WMS.INV_SHORT );
            
            //  移除已经生成的PKD，无需往lastInv_qty归还可用量，因后续没有分配操作了。
            oh_PKDs.remove(oh.getId());
            
            log.debug("库存不足, 单号: " + oh.getOrderno() + ", sku: " + oi.getSku().getCode() + ", (" + oi.getRemark() + " )");
        }
    }
    
    /**
     * 剔除掉未完全分配的订单，及其PKDs。
     * 多品项的订单，其部分OI可能在所有的Step中都没被选中（规则配置失误），这样的订单需要剔除出去。
     */
    void removeNotCompletelyOHs(Map<Long, List<OperationItem>> oh_PKDs) {
        /* 检查SO的所有PKD的分配量总和是否满足 OH的total下单量，不满足的话剔除，不允许出现部分分配的订单。*/
    	List<Long> ohIDs = new ArrayList<Long>( oh_PKDs.keySet() ); // oh_PKDs.remove
        for( Long ohId :  ohIDs ) {
            List<OperationItem> pkds = oh_PKDs.get(ohId);
            
            Double totalAllocatedQty = 0d;
            for(OperationItem pkd : pkds) {
                totalAllocatedQty = MathUtil.addDoubles(totalAllocatedQty, pkd.getQty());
            }
            
            OrderH oh = orderDao.getEntity(ohId);
            if( totalAllocatedQty < oh.getQty() ) {
                oh_PKDs.remove(ohId);
            }
        }
    }

    /**
     * 同一订单的PKD分配到了不同子波，直接从子波次里剔除，合并到一个独立的子波次里去
     */
    void orderTransSubwave(Map<Long, List<OperationItem>> oh_PKDs, OrderWave wave) {
        log.info(wave.getCode() + " start dealwith order trans subwave!");
        
        List<OperationItem> transPKDs = new ArrayList<OperationItem>();
        for(List<OperationItem> pkds : oh_PKDs.values()) {
            Set<Long> subwaveIDs = new HashSet<Long>();
            for(OperationItem pkd : pkds) {
                if(pkd.getOperation() != null) {
                    subwaveIDs.add(pkd.getOperation().getId());
                }
            }
            
            // 如果一个Order的PKD列表中存在不同的subwave，则说明该订单跨了subwave了。
            if( subwaveIDs.size() > 1 ) {
                transPKDs.addAll(pkds);
            }
        }
        
        // 新建一个专门存放跨子波次订单PKDs的独立子波次
        if( transPKDs.size() > 0 ) {
            for(OperationItem pkd : transPKDs) {
                pkd.setUdf3(pkd.getOperation().getOpno()); // 打上原子波次的信息
            }
            saveSubWave(wave, transPKDs, "Trans");
        }
    }

    private void createSubwaves(Set<Long> all_pkds, WaveRule rule, OrderWave wave) {
        rule.subwave_pkds.put(WaveRule.ALL_PKDS, all_pkds);
        
         // insert PKD ids into temp table
        orderDao.insertIds2TempTable(all_pkds);
        
        for(Subwave subwave : rule.subWavesMap.values()) {
            Collection<Long> pkdIDs = _createSubWave(subwave, rule, wave);
            rule.subwave_pkds.put(subwave.name + ".PKDs", pkdIDs);
        }
    }
    
    /**
     * 创建子波次。
     * 可限制每个子波次的Order个数（用于集货墙，格子个数有限），超出的循环处理，即每 maxOHNum 建一个子波次
     */
    private Collection<Long> _createSubWave(Subwave subwave, WaveRule rule, OrderWave wave) {
        // 根据子波次规则查询出满足当前子波次规则条件的PKD列表
        List<OperationItem> pkds = queryPKDs4SubWave(subwave, rule);
        List<Long> idList = new ArrayList<Long>();
 
        Map<Long, List<OperationItem>> oh_PKDs = new HashMap<Long, List<OperationItem>>();
        for(OperationItem pkd : pkds) {
            OrderH oh = pkd.getOrderitem().getOrder();
            Long ohId = oh.getId();
            List<OperationItem> tempList = oh_PKDs.get(ohId);
            if(tempList == null) {
                oh_PKDs.put(ohId, tempList = new ArrayList<OperationItem>());
            }
            tempList.add(pkd);
            idList.add(pkd.getId());
        }

        /*
         *  需满足：minOHNumPerSubwave < 单个波次单量 < maxOHNumPerSubwave
         *  都不符合的PKD要回滚，从主波次中剔除。
         */
        List<Long> ohIds = new ArrayList<Long>( oh_PKDs.keySet() );
        
        int totalOhNum = ohIds.size();
        int maxOHNum = subwave.getMaxOHNum();
        int times = totalOhNum / maxOHNum;
        if( totalOhNum % maxOHNum > 0 ) {
            times = times + 1;
        }
        for(int index = 0; index < times; index++) {
            List<OperationItem> tempPKDs = new ArrayList<OperationItem>();
            
            int fromIndex = index * maxOHNum;
            int endIndex = (index + 1) * maxOHNum;
            endIndex = Math.min(endIndex, totalOhNum);
            
            List<Long> _ohIds_ = ohIds.subList(fromIndex, endIndex);
            for(Long ohId : _ohIds_) {
                tempPKDs.addAll( oh_PKDs.get(ohId) );
            }
            
            String perfix = subwave.name + EasyUtils.checkTrue(index > 0, "-"+index, "");
            saveSubWave(wave, tempPKDs, perfix);
        }
        
        return idList;
    }
    
    void saveSubWave(OrderWave wave, List<OperationItem> pkds, String perfix) {
        OperationH subwave = new OperationH();
        subwave.setWave(wave);
        subwave.setWarehouse(wave.getWarehouse());
        subwave.setOpno(perfix + wave.getCode());
        subwave.setOptype( WMS.opType(WMS.OP_TYPE_FP) );
        subwave.setStatus( WMS.W_STATUS_01 ); // 状态“新建”
        orderDao.createObject(subwave);
        
        for(OperationItem pkd : pkds) {
            pkd.setOperation(subwave);
        }
        log.info("subwave: " + subwave.getOpno() + " has been created.");
    }
    
    List<OperationItem> queryPKDs4SubWave(Subwave subwave, WaveRule rule) {
        List<Long> pkdIDs = new ArrayList<Long>();
        
        String queryPkdSQL = subwave.sql;
        if(queryPkdSQL != null) {
            queryPkdSQL = MacrocodeCompiler.run(queryPkdSQL, rule.properties);
            pkdIDs.addAll( queryDataBySQL(queryPkdSQL) );
        }
        
        for(String key : subwave.includes) {
            pkdIDs.addAll( rule.getPkdIDsByKey(key) );
        }
        
        for(String key : subwave.excludes) {
            pkdIDs.removeAll( rule.getPkdIDsByKey(key) );
        }
        
        List<OperationItem> pkds = new ArrayList<>();
        for(Long pkdId : pkdIDs) {
            OperationItem pkd = rule.persitedPKDs.get(pkdId);
            if( pkd != null && pkd.getOperation() == null) { // 如果PKD已经被其他子波次捷足先登了，则不再加入到当前子波次。即先到先得。
            	pkds.add(pkd);
            }
        }
        
        return pkds;
    }
    
    private List<Long> queryDataBySQL(String sql) {
        /* 
         * 检查sql是否被注入，过滤掉一些危害性大的关键字，比如: delete、truncate、drop等
         * 通过加上 select count(*) from (sql), 确定其是一个select sql 
         */
    	orderDao.getEntitiesByNativeSql("select count(*) from (" + sql + ") t");
        
        sql = sql.replaceAll("TBL_TEMP_", "(select id from TBL_TEMP_ where thread = " +Environment.threadID()+ ")");
        log.debug(sql);
        List<?> resultList = orderDao.getEntitiesByNativeSql(sql);
        
        List<Long> result = new ArrayList<Long>();
        for(Object temp : resultList) {
            result.add( EasyUtils.obj2Long(temp) );
        }
        
        return result;
    }
}
