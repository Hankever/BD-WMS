/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inbound;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._OpEvent;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.dao.LocationDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.InvSN;
import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;
import com.boudata.wms.inventory.InvOperation;
import com.boudata.wms.inventory.InventoryService;

@Service("AsnService")
public class AsnServiceImpl implements AsnService {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired private AsnDao asnDao;
	@Autowired private LocationDao locDao;
	@Autowired private OperationDao operationDao;
	@Autowired private InvOperation invOperation;
	@Autowired private InventoryService invService;
 
	public Asn getAsn(Long id) {
		return (Asn) operationDao.getEntity(Asn.class, id);
	}
	
	public Asn createAsn(Asn asn, List<AsnItem> items) {
		Long asnId = asn.getId();
		if( asnId == null ) {
			asn.setStatus( WMS.ASN_STATUS_01 );
			asnDao.create(asn);
		} 
		else {
			asnDao.update(asn);
		}
		
		for(AsnItem item : items) {
			item.setAsn(asn);
			asnDao.createObject(item);
			
			_Sku sku = (_Sku) item.getSku();
			_Owner skuOwner = sku.getOwner();
			if( skuOwner != null && !skuOwner.equals( asn.getOwner() ) ) {
				throw new BusinessException( EX.parse(WMS.ASN_ERR_11, asn.getOwner().getName(), sku.getName(), skuOwner.getName()) );
			}
		}
		
		return asn;
	}

	public Asn deleteAsn(Long id) {
		Asn asn = getAsn(id);
		String status = asn.getStatus();
		
		// 只有新建或取消状态Asn能被删除
		if( !WMS.ASN_STATUS_01.equals(status) && !WMS.ASN_STATUS_02.equals(status) ) {
			throw new BusinessException( EX.parse(WMS.ASN_ERR_3, asn.getAsnno(), status) );
		}
		
		asn.items = asnDao.getItems(id);
		asnDao.deleteAll( asn.items );
		asnDao.delete(asn);
		
		_OpEvent.create().afterDeleteAsn(asn);
		
		return asn;
	}
	
	public OperationH assign(Long asnId, String worker, String type) {
		Asn asn = getAsn(asnId);
		_Warehouse warehouse = asn.getWarehouse();
		String opType = null;
		switch(type) {
			case "XH":
				asn.setUnloader(worker);
				opType = WMS.OP_TYPE_ZX;
				break;
			case "IN":
			default:
				asn.setWorker(worker);
				opType = WMS.OP_TYPE_IN;
				break;
		}
		asn.setStatus(WMS.ASN_STATUS_01); // 取消的订单重新指派作业人员后变回新建状态
		asnDao.update(asn);
		
		OperationH op = operationDao.getOperation(asn.getAsnno(), type); // 已接受的工单也重新拿出来分配
		if(op != null) {
			String hql = "from OpException where operation_id = ? and status = ?";
			List<OpException> opes = operationDao.getOpExcetions(hql, op.getId(), WMS.OpExc_STATUS_01 );
			for(OpException ex : opes) {
				ex.setStatus(WMS.OpExc_STATUS_02);
				ex.setResult("已重新指派");
				operationDao.update(ex);
			}
		} 
		else {
			op = new OperationH();
			op.setOpno( asn.genOpNo(type) );
			op.setOptype( WMS.opType(opType) );
			op.setWarehouse( warehouse );
			operationDao.create(op);
		}
		
		op.setQty(asn.getQty()); // asn数量、明细可能有修改
		op.setSkus(asn.getSkus());
		op.setStatus( WMS.OP_STATUS_01 ); // 已经存在的作业单会重置为“新建”状态
		op.setWorker( worker );
		op.setUdf1("");  // 清空拒绝原因
		
		operationDao.update(op);
		return op;
	}
	
	public void createAndInbound(Asn asn, List<AsnItem> items) {
		Asn asn_ = this.createAsn(asn, items);
		this.inbound(asn_.getId(), items);
	}
	
	public void inbound(Long asnId, List<AsnItem> items) {
		this._inbound(asnId, items, false, null);
	}
	
	public void asnCKInbound(Long id, List<AsnItem> items) {
		this._inbound(id, items, true, null);
	}
	
	// 移动端按单入库已指定作业单
	public void asnCKInbound4rf(Long id, List<AsnItem> items, OperationH op) {
		this._inbound(id, items, true, op);
	}
	
	private void _inbound(Long asnId, List<AsnItem> items, boolean isCheckInbound, OperationH op) {
		if( WMS.isSupplier() ) {
			throw new BusinessException(WMS.ASN_ERR_15);
		}
		
		Asn asn = getAsn(asnId);
		String status = asn.getStatus();
		_Warehouse warehouse = asn.getWarehouse();
		Long whId = warehouse.getId();
		
		// 取消状态ASN不能做入库（取消的订单重新指派作业人员后变回新建状态）
		if( !WMS.ASN_STATUS_01.equals(status) && !WMS.ASN_STATUS_03.equals(status) && !WMS.ASN_STATUS_05.equals(status) ) {
			throw new BusinessException( EX.parse(WMS.ASN_ERR_7, asn.getAsnno(), status) );
		}
		
		if(op == null) {
			op = operationDao.getOperation(asn.getAsnno(), WMS.OP_IN);
			if( op == null ) {
				op = new OperationH();
				op.setOpno( asn.genOpNo(WMS.OP_IN) );
				op.setOptype( WMS.opType(WMS.OP_TYPE_IN) );
				op.setWarehouse(warehouse);
				op.setStatus( WMS.OP_STATUS_04 );
				op.setWorker( Environment.getUserCode() );
				operationDao.createObject(op);
			}
			else {
				op.setWorker( Environment.getUserCode() );
				operationDao.update( op );
			}
		}
		
		List<OperationItem> opItems = new ArrayList<OperationItem>();
		
		String inLocCode = null; // 收货区（默认入库库位）
		try {
			inLocCode = locDao.getInLoc(whId); // 收货库位可以不存在
		} catch(Exception e) { }
		
		for(AsnItem _item : items) {
			Double qty_this = _item.getQty_this();
			if( qty_this <= 0 ) continue;
			
			AsnItem item = asnDao.getAsnItem( _item.getId() );
			item.setQty_actual( MathUtil.addDoubles(item.getQty_actual() , qty_this) );
			item.copyLotAtt(_item);
			
			String loccode = _item.getLoccode();
			if( loccode != null && !loccode.equals(inLocCode) ) {
				_Location inLoc = locDao.getLoc(whId, loccode);
				if( inLoc.isContainer() ) {
					// 如果指定的是一个停用的容器号，则重新启用此容器号
					locDao.restartLoc(whId, loccode);
					item.setPstatus("已入中转容器");
				}
				else {
					item.setPstatus("已直接入库上架");
				}
			}
			operationDao.update(item);
			_Sku sku = item.getSku();
			
			OperationItem opItem = new OperationItem();
			opItem.setOperation(op);
			opItem.setOwner(asn.getOwner());
			opItem.setQty( qty_this );
			String receiveLoc = (String) EasyUtils.checkNull(loccode, inLocCode);
			if( receiveLoc == null ) {
				throw new BusinessException( WMS.ASN_ERR_8 );
			}
			opItem.setLoccode( receiveLoc );  // 没有指定收货库位，则默认收到收货区
			opItem.setSkucode(sku.getCode());
			opItem.setAsnitem(item);
			opItem.copyLotAtt( isCheckInbound ? _item : item ); // 验货时，可能会修正实际批次信息，以实际传过来的批次为
			opItem.setStatus( WMS.OP_STATUS_04 );
			operationDao.createObject(opItem);
			
			opItems.add(opItem);
			
			// 保存序列号（如果有的话）
			if( _item.snlist.size() > 0 ) {
				String barcode = sku.getBarcode();
				List<?> l = asnDao.getEntities("select sn from InvSN where barcode = ? "
						+ " and sn in (" +DMUtil.insertSingleQuotes( EasyUtils.list2Str(_item.snlist) )+ ") and outTime is null", barcode);
				if( l.size() > 0 ) {
					throw new BusinessException( EX.parse(WMS.ASN_ERR_9, EasyUtils.list2Str(l)) );
				}
				
				for( String sncode : _item.snlist ) {
					InvSN isn = new InvSN();
					isn.setSn(sncode);
					
					isn.setBarcode( barcode );
					isn.setSkuname( sku.getName() );
					isn.setAsn(asn.getAsnno());
					isn.setInTime(new Date());
					
					asnDao.createObject(isn);
				}
			}
		}
		
		invOperation.fixLotatts(warehouse, opItems);
		invOperation.execOperations(op, opItems);
		
		// 检查入库单是否已完成入库
		boolean isDone = asnDao.getAsnItems("from AsnItem where asn.id = ? and ifnull(qty, 0) > ifnull(qty_actual, 0)", asnId).isEmpty();
		asn.setStatus( isDone ? WMS.ASN_STATUS_04 : WMS.ASN_STATUS_03 );
		asn.setWorker(Environment.getUserCode());
		asn.setInbound_date( new Date() );
		asnDao.update(asn);
		
		if( !WMS.OP_STATUS_04.equals(op.getStatus()) ) {
			op.setStatus(  (String) EasyUtils.checkTrue(isDone, WMS.OP_STATUS_04, WMS.OP_STATUS_03) );
		}
		operationDao.update(op);
		
		// 触发入库完成事件（ 回调奇门ERP、通知开单人员等）
		if( isDone ) {
			// TODO
		}
	}

	public void cancelInbound(Long asnId) {
		Asn asn = getAsn(asnId);
		String status = asn.getStatus();
		
		// 入库中、入库完成的单子可以做取消操作，通过调整单来处理
		if( !WMS.ASN_STATUS_03.equals(status) && !WMS.ASN_STATUS_04.equals(status) ) {
			throw new BusinessException( EX.parse(WMS.ASN_ERR_6, asn.getAsnno(), status) );
		}
		
		// 已做过上架的ASN单禁止入库取消, 
		OperationH sjOp = operationDao.getOperation(asn.getAsnno(), WMS.OP_SJ);
		if( sjOp != null && WMS.OP_STATUS_04.equals(sjOp.getStatus()) ) {
			throw new BusinessException( EX.parse(WMS.ASN_ERR_5, asn.getAsnno()) );
		}
		
		// 清除序列号
		operationDao.executeHQL("delete from InvSN where asn = ? and domain = ?", asn.getAsnno(), asn.getDomain());
		
		// 找出入库工单，取消
		OperationH in_op = operationDao.getOperation(asn.getAsnno(), WMS.OP_IN);
		in_op.setUdf1("入库取消");
		in_op.setOpno( asn.genOpNo("C") );
		operationDao.update(in_op);
		
		// 新建一笔负数的入库记录
		String hql = "select opi from OperationItem opi, AsnItem ai " +
				" where opi.asnitem.id = ai.id and ai.asn.id=? and opi.operation.optype=? and IFNULL(opi.status, '已完成') = ? and opi.qty > 0";
		List<OperationItem> opItems = operationDao.getItems(hql, asnId, WMS.opType(WMS.OP_TYPE_IN), WMS.OP_STATUS_04);
		List<OperationItem> opis_new = new ArrayList<>();
		for( OperationItem opi_old : opItems ) {
			
			AsnItem item = opi_old.getAsnitem();
			item.setQty_actual(0D);
			operationDao.update(item);
			
			OperationItem opi_new = new OperationItem();
			opi_new.copy(opi_old);
			opi_new.setOperation(in_op);
			opi_new.setQty( opi_old.getQty() * -1 );
			opi_new.setStatus( WMS.OP_STATUS_04 );
			operationDao.createObject(opi_new);
			
			opi_old.setAsnitem(null);              // 斩断和AsnItem的联系
			opi_old.setStatus( WMS.OP_STATUS_02 ); // 把明细状态置为已取消
			operationDao.update(opi_old);
			
			opis_new.add(opi_new);
		}
		invOperation.execOperations(in_op, opis_new);
		
		asn.setStatus(WMS.ASN_STATUS_05);
		asn.setWorker(null);
		asnDao.update(asn);
	}
	
	public void cancelAsn(Long asnId, String reason) {
		Asn asn = getAsn(asnId);
		String status = asn.getStatus();
		
		// 新建的单子可以做取消操作，删除对应作业工单
		if( !WMS.ASN_STATUS_01.equals(status) && !WMS.ASN_STATUS_02.equals(status) ) {
			throw new BusinessException( EX.parse(WMS.ASN_ERR_10, asn.getAsnno(), status) );
		}
		
		asn.setStatus(WMS.ASN_STATUS_02);
		asn.setRemark( "取消原因：" + reason + ";" + EasyUtils.obj2String(asn.getRemark()) );
		
		OperationH op = operationDao.getOperation(asn.getAsnno(), WMS.OP_IN);
		if( op != null ) {
			op.setStatus(WMS.OP_STATUS_02);
			operationDao.update(op);
		}
		
		asnDao.update(asn);
	}

	public Asn closeAsn(Long id) {
		Asn asn = getAsn(id);
		String oldStatus = asn.getStatus();

		asn.setStatus( WMS.ASN_STATUS_00 );
		operationDao.update(asn);
		
		_OpEvent.create().afterCloseAsn(asn, oldStatus);
		
		return asn;
	}  
	
	public List<OperationItem> prePutaway(Long asnId, List<_Rule> rList) {
		Asn asn = getAsn(asnId);
		List<OperationItem> sjItems = new PwRuleEngine(asnDao).excute(asn, rList);
		
		return sjItems;
	}
	
	// 保存上架单，并执行
	public OperationH putaway(Asn asn, List<OperationItem> items) {
		_Warehouse wh = asn.getWarehouse();
		
		OperationH op = operationDao.getOperation(asn.getAsnno(), WMS.OP_SJ);
		if(op == null) {
			op = new OperationH();
			op.setOpno( asn.genOpNo(WMS.OP_SJ)  );
			op.setOptype( WMS.opType(WMS.OP_TYPE_SJ) );
			op.setWarehouse( wh );
			op.setStatus( WMS.OP_STATUS_04 ); // 已完成
			operationDao.createObject(op);
		}
		
		List<OperationItem> _items = new ArrayList<OperationItem>();
		String inLocCode = locDao.getInLoc(wh.getId()); // 收货区
		for(OperationItem item : items) {
			item.setOperation(op);
			item.setLoccode( (String) EasyUtils.checkNull(item.getLoccode(), inLocCode) );
			operationDao.createObject(item);
			
			_items.add(item);
		}
		
		return invService.execOperations(op, _items);
	}
}
