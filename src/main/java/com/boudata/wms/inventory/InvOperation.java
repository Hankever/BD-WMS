/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._OpEvent;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.dao.LocationDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.SkuDao;
import com.boudata.wms.entity.AbstractLotAtt;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.InventoryTemp;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;

@Component
public class InvOperation {
	
	protected Logger log = Logger.getLogger(this.getClass());  
	
	@Autowired protected InventoryDao invDao;
	@Autowired protected SkuDao skuDao;
	@Autowired protected LocationDao locDao;
	@Autowired protected OperationDao operationDao;
    
    /**
     * 根据仓库、用户、货物、包装、批次、库位等多个维度信息来获取库存对象。
     */
    @SuppressWarnings("unchecked")
	private List<Inventory> searchInvs(String owner, Long skuId, AbstractLotAtt lotAtt, Long locationId) {
 
        InventorySo so = new InventorySo();
        so.setOwner(owner);
        so.setSkuId(skuId);
        so.setLocationId(locationId);   /* 库位 */
        
        if( lotAtt != null) {
        	so.setInvstatus(lotAtt.getInvstatus());
        	so.setLotatt01(lotAtt.getLotatt01());
        	so.setLotatt02(lotAtt.getLotatt02());
        	so.setLotatt03(lotAtt.getLotatt03());
        	so.setLotatt04(lotAtt.getLotatt04());
        }

        return (List<Inventory>) invDao.search(so).getItems();
    }
    
    /**
     * 用最小粒度去精确到 owner，sku，lotatt，locationId => 特定一条Inv记录
     */
    public Inventory searchInv( String owner, Long skuId, AbstractLotAtt lotAtt, Long locationId) {
        List<Inventory> invs = searchInvs(owner, skuId, lotAtt, locationId);
        return invs != null && invs.size() > 0 ? invs.get(0) : null;
    }
    
    /**
     * 根据InventoryTemp新建一个空的inv，不持久化，等后续操作批量创建。
     */
    public Inventory createNewInv(InventoryTemp t) {
        return createNewInv(t, true);
    }
    
    /**
     * 根据InventoryTemp新建一个空的Inv，根据参数persist判断是否持久化；并缓存到InvMap中。
     * 
     * @param t
     * @param invsMap
     * @param persist
     * @return
     */
    protected Inventory createNewInv(InventoryTemp t, boolean persist) {
        Inventory entity = new Inventory();
        entity.setId(null);
        entity.setOwner( new _Owner(t.getOwnerId()) );
        entity.setSku( new _Sku(t.getSkuId()) );
        entity.setLocation( new _Location(t.getLocationId()) );
        entity.setWh( new _Warehouse(t.getWhId()) );
        
        entity.copyLotAtt(t);
        
        if(persist) {
            entity = invDao.create(entity);
        }
        
        return entity;
    }
    
    /**
     * 判断库存对象是否为空，不为空的话将它放入到invsMap中，
     * @param inv
     * @param invsMap
     * @return true / false 不抛异常
     */
    static boolean checkInvIsNull(Inventory inv, InvMap invsMap) {
        if(inv != null) {
            if(invsMap != null && !invsMap.containsKey(inv.getId())) { 
                invsMap.put(inv);
            }
            return false;
        }
        return true;
    }
    
    // 取域自定义批次默认值（新入库时）
    public void fixLotatts(_Warehouse warehouse, List<OperationItem> opItems) {
		String sql = "select code, label, defaultValue as val, checkReg as reg, nullable "
				+ "	from dm_record_field "
				+ "	where tbl is null and domain = ? and code in ('createdate', 'expiredate', 'invstatus', 'lotatt01', 'lotatt02', 'lotatt03', 'lotatt04')";
		List<Map<String, Object>> _fields = SQLExcutor.queryL(sql, Environment.getDomain());
    	for(Map<String, Object> field : _fields) {
    		String code  = (String) field.get("code");
    		String label = (String) field.get("label");
			Object defaultVal = field.get("val");
			String checkReg = (String) field.get("reg");
			String nullable = (String) field.get("nullable");
			
			if( "入库日期".equals(label) ) {
				defaultVal = EasyUtils.checkNull(defaultVal, DateUtil.format( new Date() ));
			}
			if( !EasyUtils.isNullOrEmpty(defaultVal) ) {
				defaultVal = DMUtil.fmParse(defaultVal.toString());   // freemarker解析
				defaultVal = DateUtil.fastCast(defaultVal.toString());
				
				if( "createdate".equals(code) || "expiredate".equals(code) ) {
					defaultVal = DateUtil.parse(defaultVal.toString());
				}
			}
			
			for( OperationItem opItem : opItems ) {
	    		Object eVal = BeanUtil.getPropertyValue(opItem, code);
	    		if( eVal == null) { // 原批次属性值为空，设为默认值
	    			try {
						PropertyUtils.setProperty(opItem, code, defaultVal); 
					} catch (Exception e) {}
	    			
	    			// 判断是否有批次属性必填
	    			if( "false".equals(nullable) ) {
	    				throw new BusinessException( EX.parse(WMS.INV_ERR_3, label) );
	    			}
	    		} 
	    		else if( !EasyUtils.isNullOrEmpty(checkReg) ) {
	    			checkReg = checkReg.replaceAll("\\\\","\\\\\\\\");  // JS 正则转换为 JAVA正则
        	        Pattern p = Pattern.compile(checkReg);  
        	        if( !p.matcher(eVal.toString()).matches() ) {
        	        	throw new BusinessException( EX.parse(WMS.INV_ERR_2, label, eVal) );
        	        }
	    		}
	    	}
    	}
    }
    
    /********************************************* 以下为库存交易保存及其日志和对应的单据日志的记录 ***********************************************/
   
    /**
     * 批量保存invs，并创建单据日志和批量创建库存日志
     * @param invsMap
     */
    protected void saveInvBatch(InvMap invsMap) {
        log.debug("saveInvBatch starting ......");
        
        Collection<Inventory> lastInvs = invsMap.values();
        
        // 批量更新库存
        for(Inventory inv : lastInvs) {
        	invDao.update(inv);
        }
        
        //创建单据日志和批量创建库存日志
        createLogs(invsMap);
        
        log.debug("saveInvBatch end.");
    }

    /**
     * 创建单据日志和批量创建库存日志。 
     * 注：不可根据lastInvs来创建单据日志，如果多单同操作一inv，lastInvs只会记录该Inv最后一次被修改的单号，从而丢掉之前的单号。
     *    还会造成部分invLog.opLog = null，因为这些invLog在opLogMap里找不到单据日志。
     * 
     * @param invsMap
     * @param docType
     * @param operateType
     */
    private void createLogs(InvMap invsMap) {
        log.debug("createLogs starting ......");
        
        // 记录 单据日志，一次操作可能涉及多个单据
        Map<String, OperationLog> opLogMap = new HashMap<String, OperationLog>();
        
        // 记录 单据日志 和 库存交易日志
        for(Long invId : invsMap.keySet()) {
            // 存放着被多次修改的同一inv的不同阶段的值
            List<Inventory> list = invsMap.getList(invId); 
            Inventory previous = null;
            for(Inventory current : list) {
                if(previous == null) { // 第一条不需要记Log
                    previous = current;
                    continue;
                }
                
                // 先创建单据日志，根据current Inv上记录的docNo值来。注：previous Inv上无docNo，其一般为第一次取出或新建的Inv。
                OperationH operation = current.operation;
                Double qty = 0D;
                Set<String> skus = new HashSet<String>();
                List<OperationItem> opItems = operationDao.getItems(operation.getId());
                for(OperationItem opItem : opItems) {
                	Double toqty = EasyUtils.obj2Double(opItem.getToqty());
					qty = MathUtil.addDoubles(qty, MathUtil.subDoubles(opItem.getQty(), toqty));
                	skus.add(opItem.getSkucode());
                }
                if( operation.getQty() == null || Math.abs(operation.getQty()) < Math.abs(qty)) {
                	operation.setQty( Math.abs(qty) );
                    operation.setSkus(skus.size());
                }
                
                String opNo = current.opNo;
                if( !opLogMap.containsKey(opNo) ) {  
                    // 一次操作里，一个单据只需记一条单据日志即可
                    OperationLog opLog = new OperationLog();
                    opLog.setOperation(operation);
                    opLog.setOpType(operation.getOptype());
                    opLog.setOperateTime( new Date() );
                    opLog.setRemark( operation.getUdf1() );
                    
                    invDao.createObjectWithoutFlush(opLog);
                    opLogMap.put(opNo, opLog);
                }
                
                // 再记录库存交易日志，并和对应的单据日志相关联
                InventoryLog invLog = createInvLog(current, previous, opLogMap);
                invDao.createObjectWithoutFlush(invLog);
                previous = current;
            }
        }
        
        invDao.flush();
        log.debug("createLogs end.");
    }

    /**
     * 创建库存交易日志
     * 
     * @param newInv
     * @param oldInv
     * @param docLogMap
     * @return
     */
    private InventoryLog createInvLog(Inventory newInv, Inventory oldInv, Map<String, OperationLog> docLogMap) {
        InventoryLog invLog = new InventoryLog();
        OperationLog opLog = docLogMap.get( newInv.opNo );
        invLog.setOpLog(opLog);
        invLog.setInv(newInv);
        invLog.setCreateTime(opLog.getCreateTime());
        
        invLog.setToQty( newInv.getQty() );
        invLog.setToQtyLocked( newInv.getQty_locked() );
        invLog.setQty( oldInv.getQty() );
        invLog.setQtyLocked( oldInv.getQty_locked() );
        
        return invLog;
    }
    
    
    /**
     * 根据明细里的维度信息，批量得去查找 目标 的库存（如果存在了的话）。
     */
    protected Map<Long, Inventory> searchItemMappingInv(List<InventoryTemp> list) {
        
        List<Object[]> data = invDao.searchInvs(list);
        
        Map<Long, Inventory> itemMappingInv = new HashMap<Long, Inventory>();
        for(Object[] objs : data) {  // [inv, itemId]
            Inventory inv = (Inventory) objs[0];
            Long detailId = EasyUtils.obj2Long( objs[1] );
            
            /* 此处不能过滤掉盘点冻结的Inv，否则盘点操作也会建新库存出来 */
            itemMappingInv.put(detailId, inv);
        }
        
        // 对直接根据 明细 维度信息无法找到已存在目标库存的情况，创建一条新的空库存，然后放入itemMappingInv。
        List<Inventory> newInvList = new ArrayList<Inventory>();
        L : for(InventoryTemp item : list) {
            Long itemId = item.getId();
            Inventory inv = itemMappingInv.get(itemId);
            if(inv == null) {
                inv = createNewInv(item, false);
                
                for(Inventory temp : newInvList) {
                    if(inv.compare(temp)) {
                        itemMappingInv.put(itemId, temp);
                        continue L; // 如果批量新建的库存里有维度一致的，则共用一条。
                    }
                }
                
                itemMappingInv.put(itemId, inv);
                newInvList.add(inv);
                invDao.create(inv);
            }
        }
        
        return itemMappingInv;
    }
    
    public Set<Long> execOperations(OperationH op, List<OperationItem> items) {
    	if(op == null) {
    		throw new BusinessException( WMS.OP_ERR_7 );
    	}
    	if(EasyUtils.isNullOrEmpty(items)) {
    		throw new BusinessException( WMS.OP_ERR_1 );
    	}
    	
    	Long whId = op.getWarehouse().getId();
    	
    	Map<Long, OperationItem> itemsMap = new HashMap<Long, OperationItem>();
    	List<InventoryTemp> list = new ArrayList<InventoryTemp>();
    	List<InventoryTemp> toList = new ArrayList<InventoryTemp>();
    	Map<Long, Inventory> item_opInv = new HashMap<>();
    	
    	for(OperationItem item : items) {
    		String loccode = item.getLoccode();
    		Long itemID = item.getId();
			itemsMap.put(itemID, item);
    		
    		InventoryTemp t = new InventoryTemp(op, item);
    		t.setSkuId( skuDao.getSku(t.getSkuCode()).getId() );
			t.setLocationId( locDao.getLoc(whId, loccode, true).getId() );
    		
    		Inventory opinv = item.getOpinv();
			if( opinv != null && opinv.getLocation().getCode().equals(loccode) ) {
    			item_opInv.put(itemID, opinv);
    		} else {
    			list.add(t);
    		}
    		
			if( op.isMove() ) {
				InventoryTemp t2 = new InventoryTemp();
				BeanUtil.copy(t2, t);
				t2.setLocationId( locDao.getLoc(whId, item.getToloccode(), true).getId() );
				toList.add(t2);
			}
    	}
    	
    	if( list.size() > 0 ) {
    		item_opInv.putAll( searchItemMappingInv(list) );
    	}
    	Map<Long, Inventory> item_toInv = searchItemMappingInv(toList);
    	
    	InvMap invsMap = new InvMap();
    	
    	// 加减库存
    	for(Long itemId : item_opInv.keySet()) {
    		OperationItem item = itemsMap.get(itemId);
    		Inventory inv = item_opInv.get(itemId);
    		checkInvIsNull(inv, invsMap);
    		
    		Double deltaQty = item.getQty(), oldQty = inv.getQty();
			if( op.isAdjust() ) {
				deltaQty = MathUtil.addDoubles(item.getToqty(), item.getQty() * -1);
			}
			
			// 判断是否为移库，移库涉及两条库存的修改 
			if( op.isMove() ) {
				Inventory toInv = item_toInv.get(itemId);
				if( inv.equals(toInv) ) { // 拣货时，可能货物本就在发货区，如此拣货库位 和 目的库位是同一个，操作的将是同一条库存
					item_toInv.put(itemId, toInv = inv); // 防止指向两个不同引用，以至save时乐观锁
				}
				
				checkInvIsNull(toInv, invsMap); // false ---> throw new BusinessException(item + "移库时没有找到或生成目的库存");
				
				inv.setQty( MathUtil.addDoubles(inv.getQty(), deltaQty * -1) );
				toInv.setQty( MathUtil.addDoubles(toInv.getQty(), deltaQty) );
				
				if( deltaQty < 0 ) { // 拣货取消等
					checkInvQty(item, toInv);
				}
				checkInvLoc(item, toInv, oldQty);
				
				toInv.operation = op;
				item.setToinv(toInv);
				invsMap.put(toInv, op.getOpno());
			} 
			else {
				if( op.lockInv() ) { // 判断当前操作是否需要锁定库存（比如分配，冻结等）, 总库存量保持不变
					inv.setQty_locked( MathUtil.addDoubles(inv.getQty_locked(), deltaQty) );
				} else {
					inv.setQty( MathUtil.addDoubles(inv.getQty(), deltaQty) );
				}
			}
			
			if( op.unLockInv() ) { // 拣货等操作，要把分配锁定的量释放
				inv.setQty_locked( MathUtil.addDoubles(inv.getQty_locked(), deltaQty * -1) );
			}
			
			checkInvQty(item, inv);
			checkInvLoc(item, inv, oldQty);
    		
			inv.operation = op;
			item.setOpinv(inv);  // 关联被操作的库存至操作明细
    		invsMap.put(inv, op.getOpno());
    	}
    	
    	saveInvBatch(invsMap);
    	
    	// 执行自定义事件
        fireOpEvent(op, items);
        
        // 保存一遍op，记录单据的最后操作时间，便于查询
        op.setUpdateTime( new Date() );
        op.setWorker( (String) EasyUtils.checkNull(op.getWorker(), Environment.getUserCode()) );
    	
    	return invsMap.keySet();
    }
    
	public static void fireOpEvent(OperationH op, List<OperationItem> items) {
		// 【Global级】执行一组标准的作业反馈（只是库内作业，出、入库等另行处理）
		_OpEvent.create( InvOpEvent.class.getName() ).excute(op, items);
		
		// 【系统级】定制接口
    	_OpEvent.create().excute(op, items);
    	
    	// 【域级】定制事件，eg：定制推送接口 （ eg：出库单推送到下游TMS ）
    	_OpEvent.create( (String) Environment.getDomainInfo("op_event_class") ).excute(op, items);
	}
    
    // 检查负库存
    private void checkInvQty(OperationItem item, Inventory inv) {
    	Double qty = inv.getQty();
    	
		if( qty < 0 ) {
			throw new BusinessException( EX.parse(WMS.OP_ERR_8, inv) );
		}
		if( qty < inv.getQty_locked() ) {
			throw new BusinessException( EX.parse(WMS.OP_ERR_9, inv) );
		}
    }
    
    private void checkInvLoc(OperationItem item, Inventory inv, Double oldQty) {
		_Location location = inv.getLocation();
		String locCode = location.getCode();
		
		/* 锁定、非盘点：许进不许出 */
		if( location._isHolding() && !location._isChecking() && inv.getQty() > oldQty ) { 
			return;
		}
		
		/* 判断该库位是否被冻结、盘点；该库存是否被盘点。 */
        if( location.isFrozen() ) {
        	throw new BusinessException( EX.parse(WMS.OP_ERR_10, locCode) );
        }
    }
   
}
