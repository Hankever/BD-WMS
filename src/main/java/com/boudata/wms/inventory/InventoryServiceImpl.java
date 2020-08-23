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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.dao.LocationDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.SkuDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.AbstractLotAtt;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.InvCheck;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.InventoryTemp;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._SkuX;
import com.boudata.wms.entity._Warehouse;

@SuppressWarnings("unchecked")
@Service("InventoryService")
public class InventoryServiceImpl implements InventoryService {
	
	protected Logger log = Logger.getLogger(this.getClass());  
	
	@Autowired private InventoryDao invDao;
	@Autowired private InvOperation invOperation;
	@Autowired private OperationDao operationDao;
	@Autowired private SkuDao skuDao;
	@Autowired private LocationDao locationDao;
	
	public OperationH getOp(Long id) {
		return operationDao.getEntity(id);
	}
	
	public OperationH execOperations(Long opId) {
		OperationH op = operationDao.getEntity(opId);
		List<OperationItem> items = operationDao.getItems(opId);
		
		return execOperations(op, items);
	}
	
	public OperationH execOperations(OperationH op, List<OperationItem> items) {
		invOperation.execOperations(op, items);
		
		op.setStatus( WMS.OP_STATUS_04 ); // 完成
		op.setWorker( Environment.getUserCode() );
		invDao.update(op);
		
		return op;
	}
	
	// 保存移库单并执行移库
	public OperationH move(Long whId, String loccode, String toloccode, List<_DTO> mvItems) {
		Param opType = WMS.opType( WMS.OP_TYPE_MV );
		OperationH op = getFirstOpToday(whId, opType); // 当天同一人做的移库作业都归到同一个作业单
		if( op == null ) {
			op = new OperationH();
			op.setOpno( _Util.genOpNO(WMS.OP_MV) );
			op.setOptype( opType );
			op.setWarehouse(new _Warehouse(whId));
			op.setStatus( WMS.OP_STATUS_01 ); // 新建
			invDao.createObject(op);
		}
		
		List<OperationItem> items = new ArrayList<OperationItem>();
		for (_DTO item : mvItems) {
			// 如果指定了库存记录进行移动（普通移库）， 将库存批次信息也复制到item里
			Long inv_id = item.inv_id;
			if( inv_id != null ) {
				Inventory inv = invDao.getEntity(inv_id);
				item.copyLotatt(inv);;
			}
			
			OperationItem opi = new OperationItem();
			opi.copyLotAtt(item);
			opi.setSkucode( item.skucode);
			opi.setQty( item.qty );
			opi.setLoccode(loccode);
			opi.setToloccode(toloccode);
			opi.setOperation(op);
			
			Long ownerId = item.owner_id;
			opi.setOwner(new _Owner(ownerId));
			opi = (OperationItem) invDao.createObject(opi);
			
			items.add(opi);
		}
		
		return this.execOperations(op, items);	
	}
	
	public void containerMove(Long whId, String loccode, String toloccode, String type, List<_DTO> mvItems) { 
		Param opType = WMS.opType( type );
		OperationH op = getFirstOpToday(whId, opType); // 当天同一人做的移库作业都归到同一个作业单
		if( op == null ) {
			op = new OperationH();
			String opNo_  = WMS.OP_TYPE_RQSJ.equals(type) ? WMS.OP_RQSJ : WMS.OP_RQXJ;
			op.setOpno( _Util.genOpNO(opNo_) );
			op.setOptype( opType );
			op.setWarehouse( new _Warehouse(whId) );
			op.setStatus( WMS.OP_STATUS_04 );
			op.setWorker(Environment.getUserCode());
			invDao.createObject(op);
		}
		
		for (_DTO item : mvItems) {
			OperationItem opi = new OperationItem();
			opi.copyLotAtt(item);
			opi.setSkucode( item.skucode);
			opi.setQty( item.qty );
			opi.setLoccode(loccode);
			opi.setToloccode(toloccode);
			opi.setOperation(op);
			opi.copyLotAtt(item);
			opi.setOwner( new _Owner(item.owner_id) );
			opi = (OperationItem) invDao.createObject(opi);
		}

		_Location child_loc = locationDao.getLoc(whId, loccode);
		if (WMS.OP_TYPE_RQSJ.equals(type)) {
			_Location parent_loc = locationDao.getLoc(whId, toloccode);
			child_loc.setParent(parent_loc);
		} else {
			child_loc.setParent(null);
		}
		locationDao.update(child_loc);
	}
	
	private OperationH getFirstOpToday(Long whId, Param opType) {
		String hql = " from OperationH where creator = ? and optype = ? and createtime >= ?  and createtime < ? and warehouse.id = ?";
		List<OperationH> operationHs = (List<OperationH>) invDao.getEntities(hql, 
			Environment.getUserCode(), opType, DateUtil.today(), DateUtil.noHMS(DateUtil.addDays(1)), whId);

		// 当天同一人做的移库作业都归到同一个作业单（盘点也一样）
		return operationHs.isEmpty() ? null : operationHs.get(0);
	}
	
	// 保存盘点单并执行盘点
	public OperationH invCheckCommit(Long whId, List<OperationItem> items, String remark) {
		Param opType = WMS.opType(WMS.OP_TYPE_TZ);
		OperationH op = getFirstOpToday(whId, opType);
		if(op == null || "库存加工".equals(remark)) {  // 库存加工总是新生成一个工单
			op = new OperationH();
			op.setOpno( _Util.genOpNO(WMS.OP_TZ) );
			op.setOptype( opType );
			op.setWarehouse(new _Warehouse(whId));
			op.setStatus(WMS.OP_STATUS_01); // 新建
			op.setUdf1(remark);
			invDao.createObject(op);
		}

		for (OperationItem item : items) {
			item.setOperation(op);
			invDao.createObject(item);
		}
		
		return this.execOperations( op, items );
	}
	
	// 创建盘点任务单 及盘点单
	public OperationH createInvCheck(InvCheck invCheck, Long whId, List<OperationItem> items, int curr_round) {
		OperationH op = new OperationH();
		op.setOpno( _Util.genOpNO( invCheck.getCode() ) );
		op.setOptype( WMS.opType( WMS.OP_TYPE_PD ) );
		op.setWarehouse(new _Warehouse(whId));
		op.setStatus(WMS.OP_STATUS_01); // 新建
		invDao.createObject(op);

		Double qtys = 0D;
		Set<String> skuCodes = new HashSet<>();
		Set<String> locCodes = new HashSet<>();
		Set<_Location> locs = new HashSet<>();
		for (OperationItem item : items) {
			if(curr_round == 1) {
				qtys = MathUtil.addDoubles(qtys, item.getQty());
				skuCodes.add(item.getSkucode());
				locCodes.add(item.getLoccode());
			}
			
			item.setOperation(op);
			invDao.createObject(item);
			
			locs.add( item.getOpinv().getLocation() );
		}
		
		for( _Location loc : locs ) {
			loc.setChecking(ParamConstants.TRUE); // 库位 设置为“盘点中”
			invDao.update(loc);
		}
		
		if(curr_round == 1) {
			invCheck.setRound1(op);
			invCheck.setLocs(locCodes.size());
			invCheck.setQtys(qtys);
			invCheck.setSkus(skuCodes.size());
			invDao.createObject(invCheck);
		} 
		else if(curr_round == 2) {
			invCheck.setRound2(op);
			invCheck.setStatus(WMS.INV_STATUS_02);
			invDao.update(invCheck);
		} 
		else {
			invCheck.setRound3(op);
			invCheck.setStatus(WMS.INV_STATUS_03);
			invDao.update(invCheck);
		} 
		
		return op;
	}
	
	// 状态置为取消，释放库位
	public void cancelInvCheck(Long id) {
		InvCheck invCheck = (InvCheck) invDao.getEntity(InvCheck.class, id);
		invCheck.setStatus( WMS.INV_STATUS_00 );
		invDao.update(invCheck);
		
		OperationH op1 = invCheck.getRound1();
		Long opId = op1.getId();
		closeOp( opId );
		
		List<OperationItem> opItems = operationDao.getItems(opId);
		Set<_Location> locs = new HashSet<>();
		for (OperationItem item : opItems) {
			locs.add( item.getOpinv().getLocation() );
		}
		
		for( _Location loc : locs ) {
			loc.setChecking(ParamConstants.FALSE); // 不再是“盘点中”
			invDao.update(loc);
		}
	}

	// 将进行中的工单也关闭
	public void closeInvCheck(Long id) {
		InvCheck invCheck = (InvCheck) invDao.getEntity(InvCheck.class, id);
		invCheck.setStatus( WMS.INV_STATUS_05 );
		invDao.update(invCheck);

		closeOp( invCheck.getRound1() );
		closeOp( invCheck.getRound2() );
		closeOp( invCheck.getRound3() );
	}
	
	public void saveInvCheckData(List<_DTO> list, Long invCheckId, int round) {
		
		InvCheck invCheck = (InvCheck) invDao.getEntity(InvCheck.class, invCheckId);
		_Warehouse wh = invCheck.getWarehouse();
		_Owner owner = invCheck.getOwner();
		OperationH op1 = invCheck.getRound1(), 
				   op2 = invCheck.getRound2(), 
				   op3 = invCheck.getRound3();
		
		op1.setWorker( (String) EasyUtils.checkNull(op1.getWorker(), Environment.getUserCode()) );
		operationDao.update(op1);
		
		for (_DTO item : list) {
			Double toqty = item.toqty;
			OperationItem oi;
			
			if( item.id == null ) { // 盘盈新增出来的库存记录
				_Sku sku = skuDao.getSku( item.skucode );
				_Location loc = locationDao.getLoc(wh.getId(), item.loccode );
 
				AbstractLotAtt lotatt = new OperationItem();
				lotatt.copyLotAtt(item);
				Inventory inv = invOperation.searchInv(owner.getName(), sku.getId(), lotatt, loc.getId());
				if( inv == null ) {
					InventoryTemp t = new InventoryTemp();
			    	t.setWhId( wh.getId() );
			    	t.setOwnerId( owner.getId() );
			    	t.setSkuId( sku.getId() );
			    	t.setLocationId( loc.getId() );
			    	t.copyLotatt(lotatt);
			    	inv = invOperation.createNewInv(t); // 统一用此接口创建库存
				}
				
				oi = new OperationItem(inv, 0D);
				
				if(round == 1) {
					createOpItem(toqty, oi, op1);
				}
				
				if(round == 2) {
					createOpItem(0D, new OperationItem(inv, 0D), op1); // 复盘多出来的，补一条初盘的opi: 0 -> 0
					createOpItem(toqty, oi, op2);
				}
				
				if(round == 3) {
					createOpItem(0D, new OperationItem(inv, 0D), op1); // 补两条
					createOpItem(0D, new OperationItem(inv, 0D), op2);
					createOpItem(toqty, oi, op3);
				}
			} 
			else  {
				oi = operationDao.getOpItem( item.id );
				oi.setToqty( toqty );
				operationDao.update(oi);
			}
		}
	}

	private void createOpItem(Double toqty, OperationItem opi, OperationH op) {
		opi.setToqty( toqty );
		opi.setOperation( op );
		operationDao.createObject(opi);
	}

	public Map<String, Object> adjustInvByCheckResult(Long invCheckId, Long opId, String itemIds, String type) {
		Map<String, Object> map = new HashMap<String, Object>();
		
		InvCheck invCheck = (InvCheck) invDao.getEntity(InvCheck.class, invCheckId);
		invCheck.setStatus(WMS.INV_STATUS_04);
		
		OperationH op = operationDao.getEntity(opId);
		List<OperationItem> items;
		if(EasyUtils.isNullOrEmpty(itemIds)) {
			items = operationDao.getItems(opId);
		} else {
			items = operationDao.getItems("from OperationItem where id in ( " + itemIds + " ) and operation.id = ?", opId);
		}
		
		type = (String) EasyUtils.checkNull(type, "直接调整");
		if("直接调整".equals(type)) {
			for( Iterator<OperationItem> it = items.iterator(); it.hasNext(); ) {
				OperationItem item = it.next();
				if( op.isAdjust() && item.getQty().equals(item.getToqty()) ) {
					it.remove();
				}
				item.setUdf1(type);
				operationDao.update(item);
				_Location loc = item.getOpinv().getLocation();
				loc.setChecking(ParamConstants.FALSE);
				locationDao.update(loc);
			}
			
			OperationH _op = execOperations(op, items);
			map.put("op", _op);
		} else if ("盘盈入库".equals(type)) {
			Asn asn = inventoryProfit(type, invCheck, op, items);
			map.put("asn", asn);
		} else if ("盘亏出库".equals(type)) {
			OrderH order = inventoryLosses(type, invCheck, op, items);
			map.put("order", order);
		}
		
		return map;
	}

	private OrderH inventoryLosses(String type, InvCheck invCheck, OperationH op, List<OperationItem> items) {
		_Warehouse warehouse = invCheck.getWarehouse();
		_Owner owner = invCheck.getOwner();
		// 创建出库单
		OrderH order = new OrderH();
		order.setOwner( owner );
		order.setWarehouse( warehouse );
		order.setOrderno( _Util.genDocNO(owner, "0", false) );
		order.setStatus( WMS.O_STATUS_01 );
		order.setType( type );
		order.setRemark( "盘点任务单:" + invCheck.getCode());
		order = (OrderH) operationDao.createObject(order);
		
		Set<Long> skuIds = new HashSet<>();
		Double qty_total = 0D;
		for( Iterator<OperationItem> it = items.iterator(); it.hasNext(); ) {
			OperationItem item = it.next();
			
			Inventory inv = item.getOpinv();
			_Location loc = inv.getLocation();
			loc.setChecking(ParamConstants.FALSE);
			locationDao.update(loc);
			
			if( op.isAdjust() && item.getQty() <= item.getToqty() ) {
				it.remove();
				continue;
			}
			
			item.setUdf1(type);
			operationDao.update(item);
			
			_Sku sku = skuDao.getSku( item.getSkucode() );
			Double qty = MathUtil.subDoubles(item.getQty(), item.getToqty());
			// 创建出库明细
			OrderItem oItem = new OrderItem();
			oItem.setOrder(order);
			oItem.setSku(sku);
			oItem.setQty(qty);
			oItem.setInv_id(inv.getId());
			oItem.copyLotAtt(item);
			operationDao.createObject(oItem);
			
			skuIds.add(sku.getId());
			qty_total = MathUtil.addDoubles(qty_total, qty);
		}
		order.setSkus(skuIds.size());
		order.setQty(qty_total);
		operationDao.update(order);
		
		return order;
	}

	private Asn inventoryProfit(String type, InvCheck invCheck, OperationH op, List<OperationItem> items) {
		_Warehouse warehouse = invCheck.getWarehouse();
		_Owner owner = invCheck.getOwner();
		// 创建入库单
		Asn asn = new Asn();
		asn.setOwner( owner );
		asn.setWarehouse( warehouse );
		asn.setAsnno( _Util.genDocNO(owner, "1", false) );
		asn.setStatus( WMS.ASN_STATUS_01 );
		asn.setType( type );
		asn.setRemark( "盘点任务单:" + invCheck.getCode());
		asn = (Asn) operationDao.createObject(asn);
		
		Set<Long> skuIds = new HashSet<>();
		Double qty_total = 0D;
		for( Iterator<OperationItem> it = items.iterator(); it.hasNext(); ) {
			OperationItem item = it.next();
			
			_Location loc = item.getOpinv().getLocation();
			loc.setChecking(ParamConstants.FALSE);
			locationDao.update(loc);
			
			if( op.isAdjust() && item.getQty() >= item.getToqty() ) {
				it.remove();
				continue;
			}

			item.setUdf1(type);
			operationDao.update(item);
			
			_Sku sku = skuDao.getSku( item.getSkucode() );
			Double qty = MathUtil.subDoubles(item.getToqty(), item.getQty());
			// 创建入库明细
			AsnItem aItem = new AsnItem();
			aItem.setAsn(asn);
			aItem.setSku(sku);
			aItem.setQty(qty);
			aItem.setLoccode(loc.getCode());
			aItem.copyLotAtt(item);
			operationDao.createObject(aItem);
			
			skuIds.add(sku.getId());
			qty_total = MathUtil.addDoubles(qty_total, qty);
		}
		asn.setSkus(skuIds.size());
		asn.setQty(qty_total);
		operationDao.update(asn);
		
		return asn;
	}
	
	public OperationH closeOp(Long opId) {
		OperationH op = getOp(opId);
		closeOp(op);
		return op;
	} 
	
	private void closeOp(OperationH op) {
		if(op == null) return;
		
		// 不可关闭的作业单状态：部分完成
		String status = op.getStatus();
		if( WMS.OP_STATUS_03.equals(status) ) {
			throw new BusinessException( EX.parse(WMS.OP_ERR_5, op.getOpno(), status) );
		}
		
		op.setStatus( WMS.OP_STATUS_00 );
		operationDao.update(op);
	} 
	
	public List<OperationLog> operationLog(Long opId){
		String hql = "from OperationLog o where o.operation.id = ?";
		return (List<OperationLog>) operationDao.getEntities(hql,opId);
	}
	
	public List<InventoryLog> inventoryLog(Long opId) {
		String hql = "from InventoryLog i where i.opLog.operation.id = ?";
		return (List<InventoryLog>) operationDao.getEntities(hql,opId);
	}
	
	public Map<String, Object> search(InventorySo so, boolean with_skux) {
		PageInfo page = invDao.search(so);
		String domain = Environment.getDomain();
		
		if(!with_skux) return page.toEasyUIDataGrid();
		
		List<Inventory> invs = (List<Inventory>) page.getItems(); // 查询skucode的实际库存
		String hql = " select s.parent.id, sum(weight) from _SkuX s where domain = ? and s.parent is not null group by s.parent.id";
		List<?> list = invDao.getEntities(hql, domain);
		
		for(Inventory inv : invs) { 
			invDao.evict(inv);
			
			// 批量取出 组合SKU 进行循环
			for( Object obj : list ) {
				Object[] objs = (Object[]) obj;
				Long pSkuId = (Long) objs[0];
				Integer weight = EasyUtils.obj2Int( objs[1] );
				
				if( pSkuId.equals( inv.getSku().getId() ) ) {
					String lotatt01 = "组合" + weight + " " + EasyUtils.obj2String( inv.getLotatt01() );
					inv.setLotatt01( lotatt01 );
				}
			}
		}
		
		if( !EasyUtils.isNullOrEmpty(so.getBarCode()) ) {
			String barcode = so.getBarCode(); 
			String _hql = " from _SkuX s where ? in (s.sku.barcode, s.sku.barcode2, s.sku.code) and domain = ? and parent is not null";
			List<_SkuX> skuxs = (List<_SkuX>) invDao.getEntities(_hql, barcode, domain);
			for(_SkuX skux : skuxs) { // 如果是混箱的子skucode，则先获取主skucode的库存信息，再换算成子skucode对应库存量
				String p_barcode = skux.getParent().getBarcode();
				
				so.setBarCode(p_barcode);
				List<Inventory> _invs = (List<Inventory>) invDao.search(so).getItems(); // 主skucode的库存信息
				for(Inventory _inv : _invs) {
					Double qty = _inv.getQty() * skux.getWeight(); // 子skucode对应库存量
					String skuname = _inv.getSku().getName();
					String lotatt01 = skuname + EasyUtils.checkTrue( _inv.getLotatt01() == null, "", ","+_inv.getLotatt01());
					
					_inv.setId( null );
					_inv.setSku( skux.getSku() );
					_inv.setQty( qty );
					_inv.setLotatt01( lotatt01 );
					invDao.evict(_inv);
					
					invs.add(_inv);
				}
			}
		}

		page.setItems(invs);
		if( page.getPageNum() == 1 && invs.size() < page.getPageSize()) {
			page.setTotalRows(invs.size());
		}
		
		return page.toEasyUIDataGrid();
	}

	public void unlock(Long id, Double qty_unlock) {
		Inventory inv = invDao.getEntity(id);
		
		OperationH op = new OperationH();
		op.setOpno( _Util.genOpNO("UL") );
		op.setOptype( WMS.opType(WMS.OP_TYPE_DJ) );
		op.setWarehouse( inv.getWh() );
		op.setStatus( WMS.OP_STATUS_04 );
		op.setUdf1(qty_unlock > 0 ? "解锁库存" : "锁定库存");
		invDao.createObject(op);
		
		OperationItem opItem = new OperationItem(inv,  qty_unlock * -1 );
		opItem.setOperation(op);
		invDao.createObject(opItem);
		
		List<OperationItem> opItems = new ArrayList<OperationItem>();
		opItems.add(opItem);
		
		invOperation.execOperations(op, opItems);
	}
	
	public void changeOPIInv(Long opiId, Long toInvId) {
		Inventory toInv = invDao.getEntity(toInvId);
		OperationItem opi = (OperationItem) operationDao.getEntity(OperationItem.class, opiId);
		OperationH op = opi.getOperation();
		op.setUdf1("调整拣货库存");
		
		if( WMS.OP_STATUS_04.equals(op.getStatus()) ) {
			throw new BusinessException( WMS.OP_ERR_6 );
		}
		opi.setStatus("人工调整");  // 原来可能为异常 
		
		Param currOpType = op.getOptype();
		op.setOptype( WMS.opType( WMS.OP_TYPE_FP ) );  // 此时作业单类型可能已变为“拣货”（当指派拣货员后）
		
		Double qty = opi.getQty();
		List<OperationItem> opItems = new ArrayList<OperationItem>();
		opi.setQty( qty * -1 );
		opItems.add(opi);
		invOperation.execOperations(op, opItems);
		
		opi.setOpinv(toInv);
		opi.setLoccode(toInv.getLocation().getCode());
		opi.copyLotAtt(toInv);
		opi.setQty( qty );
		invOperation.execOperations(op, opItems);
		
		op.setOptype( currOpType );
	}
}
