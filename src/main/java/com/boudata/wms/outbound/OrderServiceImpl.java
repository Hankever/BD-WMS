/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.persistence.pagequery.PaginationQueryByHQL;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._OpEvent;
import com.boudata.wms._Util;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.dao.LocationDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.dao.SkuDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.BoxItem;
import com.boudata.wms.entity.InvSN;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;
import com.boudata.wms.inventory.InvOperation;
import com.boudata.wms.inventory.InventoryService;
import com.boudata.wms.outbound.wave.InvOperation4Order;
import com.boudata.wms.outbound.wave.WaveService;

@Service("OrderHService")
public class OrderServiceImpl implements OrderService {
	
	protected Logger log = Logger.getLogger(this.getClass()); 
	
	@Autowired private SkuDao skuDao;
	@Autowired private LocationDao locDao;
	@Autowired private OrderHDao orderDao;
	@Autowired private InventoryDao invDao;
	@Autowired private OperationDao operationDao;
	@Autowired private InvOperation4Order invOperation;
	@Autowired private WaveService waveService;
	@Autowired private InventoryService invService;
	
	public PageInfo search(OrderSo so) {
        String hql = " from OrderH o where 1=1 " + so.toConditionString();
        
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(orderDao.em(), hql, so);
        PageInfo page = pageQuery.getResultList();
        page.setItems(page.getItems());
        return page;
    }
	
	public OrderH getOrder(Long id) {
		return orderDao.getEntity(id);
	}

	public OrderH createOrder(OrderH order, List<OrderItem> items) {
		order.setStatus( (String) EasyUtils.checkNull(order.getStatus(), WMS.O_STATUS_01) );
		orderDao.create(order);
		
		Set<Long> skuIds = new HashSet<Long>();
		Double qty = 0D;
		double money = 0;
		for(OrderItem item : items) {
			item.setOrder(order);
			orderDao.createObject(item);
			
			_Sku sku = (_Sku) item.getSku();
			_Owner skuOwner = sku.getOwner();
			if( skuOwner != null && !skuOwner.equals( order.getOwner() ) ) {
				throw new BusinessException( EX.parse(WMS.O_ERR_11, order.getOwner().getName(), sku.getName(), skuOwner.getName()) );
			}
			
			skuIds.add(item.getSku().getId());
			money = MathUtil.addDoubles(money, item.getMoney());
			qty   = MathUtil.addDoubles(qty, item.getQty());
		}
		
		order.setSkus(skuIds.size());
		order.setMoney(money);
		order.setQty(qty);
		
		orderDao.update(order);
		return order;
	}

	public OrderH deleteOrder(Long id) {
		OrderH order = getOrder(id);
		String status = order.getStatus();
		
		// 只有新建或取消状态订单能被删除
		if( !("新建,取消".indexOf( status ) >= 0) ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_3, order.getOrderno(), status) );
		}
		
		order.items = orderDao.getOrderItems(id);
		orderDao.deleteAll( order.items );
		orderDao.delete(order);
		orderDao.flush();
		
		_OpEvent.create().afterDeleteOrder(order);
		
		return order;
	}
	
	public List<OrderItem> fetchOrderItems(Long orderId) {
		return orderDao.getOrderItems( Arrays.asList(orderId) );
	}

	public OperationH assign(Long orderId, String worker, String type, Long opId) {
		OrderH order = getOrder(orderId);
		String status = order.getStatus();
		_Warehouse warehouse = order.getWarehouse();
		
		OperationH op = null;
		
		// 拣货指导单已经生成
		if( WMS.OP_TYPE_JH.equals(type)  && opId != null ) {
			op = operationDao.getEntity(opId);
			op.setOptype( WMS.opType(type) );
		} 
		else { 
			// 只有新建或取消的单子可以指派出库员
			if( !("新建,取消,出库取消".indexOf( status ) >= 0) ) {
				throw new BusinessException( EX.parse(WMS.O_ERR_13, order.getOrderno(), status) );
			}
			order.setStatus(WMS.O_STATUS_01); // 指派后取消的单子重新变回“新建”
			
			op = operationDao.getOperation(order.getOrderno(), WMS.OP_OUT); // 已接受的工单也重新拿出来分配
			if(op == null) {
				op = new OperationH(); 
				op.setOpno( order.genOpNo(WMS.OP_OUT) );
				op.setOptype( WMS.opType(WMS.OP_TYPE_OUT) );
				op.setWarehouse(warehouse);
			}
		}
		
		op.setQty(order.getQty()); // 重新指派时，order数量可能有更新
		op.setSkus(order.getSkus());
		op.setStatus( WMS.OP_STATUS_01 );  // 已经存在的作业单会重置为“新建”状态
		op.setWorker( worker );
		op.setUdf1("");  // 清空拒绝原因
		
		if( op.getId() == null ) {
			orderDao.createObject(op);
		} 
		else {
			orderDao.update(op);
			
			String hql2 = " from OpException where operation.id = ? and status = ?";
			List<OpException> opes = operationDao.getOpExcetions(hql2, op.getId(), WMS.OpExc_STATUS_01 );
			for(OpException ope : opes) {
				ope.setStatus(WMS.OpExc_STATUS_02);
				ope.setResult("已重新指派");
				orderDao.update(ope);
			}
		}
		
		order.setWorker(worker);
		orderDao.update(order);
		
		return op;
	}
	
	
	public OperationH allocate(Long orderId) {
		OrderH order = getOrder(orderId);
		
		OrderWave wave = new OrderWave();
		wave.setCode( SerialNOer.get("1Wxxxx") + "-" + order.getOrderno() ); // 便于操作拣货指导单的时候获取 出库单信息
		wave.setWarehouse(order.getWarehouse());
		wave = waveService.excutingWave(wave , _Util.idArray2List(orderId));
		orderDao.flush();
		
		// 查询所得拣货作业单
		List<OperationH> pkhs = operationDao.getSubwaves(wave.getId());
		if( pkhs.isEmpty() ) {
			order.setWave(null);
			orderDao.delete(wave);
			
			OperationH rt_pkh = new OperationH();
			rt_pkh.setQty(order.getQty());
			rt_pkh.setSkus(order.getSkus());
			rt_pkh.setWarehouse(order.getWarehouse());
			rt_pkh.setOptype( WMS.opType(WMS.OP_TYPE_FP) );
			rt_pkh.setErrorMsg("订单库存分配失败，库存可能不足，详情请查看订单明细备注");
			return rt_pkh;
		}
		
		if( order.isShort() ) {
			order.setTag(null);
			List<OrderItem> items = fetchOrderItems(orderId);
			for(OrderItem item : items) {
				item.setRemark(null);
			}
		}
		
		order.setTag( WMS.W_ORIGIN_01 );
		orderDao.update(order);
		
		return pkhs.get(0);
	}
	
	public OrderH cancelAllocate(Long orderId, boolean outCancel) {
		OrderH order = orderDao.getEntity(orderId);
		OrderWave wave = order.getWave();
		
		String status = order.getStatus();
		if( !WMS.O_STATUS_03.equals( status ) && !WMS.O_STATUS_41.equals( status ) ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_14, status) );
		}
		
		List<OperationItem> unFinishedPkds = new ArrayList<>();
		if( outCancel ) {
			wave.setTotal( wave.getTotal() - 1);
			
			// 外部取消可能， pkh 可能是子波次，类型为“拣货”，无法用来直接做取消分配
			unFinishedPkds = operationDao.getItems("from OperationItem where orderitem.order = ?", order);
			for(OperationItem pkd : unFinishedPkds) {
				pkd.setStatus(WMS.OP_STATUS_02);
				
				if( pkd.getToloccode() == null ) { // 未拣货的解锁库存 (已拣货的不管)
					Inventory opinv = pkd.getOpinv();
					invService.unlock(opinv.getId(), pkd.getQty());
				}
				operationDao.update(pkd);
			}
			
			operationDao.deleteAll( unFinishedPkds );
		}
		else {
			if( wave != null && wave.isWave() ) {
				throw new BusinessException(WMS.O_ERR_21);
			}
			
			Long waveId = wave.getId();
			OperationH pkh = operationDao.getSubwaves(waveId).get(0);
			pkh.setStatus(WMS.OP_STATUS_02);
			pkh.setUdf1("分配取消");
			pkh.setOpno( SerialNOer.get(pkh.getOpno() + "xxxx") );
			pkh.setWave(null);
			pkh.setOptype( WMS.opType(WMS.OP_TYPE_FP) );
			
			List<OperationItem> pkds = operationDao.getItems(pkh.getId());
			
			for(OperationItem pkd : pkds) {
				if( WMS.OP_STATUS_04.equals( pkd.getStatus() )) { 
					pkh.setOptype( WMS.opType(WMS.OP_TYPE_FP) );  // 已部分拣货，此时单据类型为“拣货单”
				}
				else {
					pkd.setQty( pkd.getQty() * -1 );
					pkd.setStatus(WMS.OP_STATUS_02);
					unFinishedPkds.add(pkd);
				}
			}
			
			invOperation.execOperations(pkh, unFinishedPkds);
			orderDao.delete(wave);
		}

		order.setWave(null);
		order.setStatus(WMS.O_STATUS_01);
		orderDao.update(order);
		
		return order;
	}

	public void pickup(OperationH pkh, List<OperationItem> pkds) {
		pkh.fixOpType(); // 原先为分配 --> 拣货
		
		// 本次拣货确认的订单（可能是波次中某一个或几个）
		Set<OrderH> orders = new HashSet<>();
				
		// 如果没有指定的拣货中转容器，则默认拣货到出货库位（eg：集货墙）
		Long whId = pkh.getWarehouse().getId();
		String outLoc = locDao.getOutLoc(whId);
		for(OperationItem pkd : pkds) {
			pkd.setToloccode( (String) EasyUtils.checkNull(pkd.getToloccode(), outLoc) ); 
			orders.add(pkd.getOrderitem().getOrder());
		}
		
		invOperation.execOperations(pkh, pkds);
		
		// 更新作业明细状态
		for(OperationItem pkd : pkds) {
			Double qty_checked = pkd.getQty_checked();
			
			// PC端拣货没有 qty_checked， 移动端有
			if( qty_checked == null ) {
				pkd.setStatus(WMS.OP_STATUS_04);
			} 
			else {
				boolean finished = EasyUtils.checkNull(pkd.getQty_old(), pkd.getQty()).equals(qty_checked);
				pkd.setStatus( finished ? WMS.OP_STATUS_04 : WMS.OP_STATUS_03);
				pkd.setQty( qty_checked );
			}
			orderDao.update(pkd);
		}
		
		// 检测是否所有作业明细都 已完成
		String op_status = WMS.OP_STATUS_04, wave_status = WMS.W_STATUS_04;
		List<OperationItem> all_pkds = operationDao.getItems(pkh.getId());
		Map<Long, Double> orderPkQty = new HashMap<Long, Double>();
		for(OperationItem pkd : all_pkds) {
			if( !WMS.OP_STATUS_04.equals(pkd.getStatus()) ) {
				op_status = WMS.OP_STATUS_03;
				wave_status = WMS.O_STATUS_41;
				break;
			}
			else {
				Long orderId = pkd.getOrderitem().getOrder().getId();
				orderPkQty.put(orderId, MathUtil.addDoubles(orderPkQty.get(orderId), pkd.getQty()));
			}
		}
		
		// 更新子波次、波次、订单的状态
		pkh.setStatus( op_status ); // 作业完成
		orderDao.update(pkh);
		
		OrderWave wave = pkh.getWave();
		wave.setStatus( wave_status ); // 拣货完成
		orderDao.update(wave);
		
		for( OrderH order : orders ) {
			Double pickedQty = orderPkQty.get(order.getId());
			if( pickedQty != null ) {
				boolean finished = pickedQty < order.getQty();
				order.setStatus( finished ? WMS.O_STATUS_41 : WMS.O_STATUS_42 ); // 部分拣货 | 拣货完成
				orderDao.update(order);
			}
		}
	}
	
	public void cancelPickup(Long opId, String pkdIds) {
		Double cancelingQty = 0D, pickedQty = 0D;
		List<OperationItem> all = operationDao.getItems(" from OperationItem where operation.id = ? and status = ?", opId, WMS.OP_STATUS_04);
		for( OperationItem pkd : all ) {
			pickedQty = MathUtil.addDoubles(pickedQty, pkd.getQty());
		}
		
		String hql = " from OperationItem where id in (" + pkdIds + ")";
		List<OperationItem> pkds = operationDao.getItems(hql);
		for( OperationItem pkd : pkds ) {
			cancelingQty = MathUtil.addDoubles(cancelingQty, pkd.getQty());
			pkd.setQty( pkd.getQty() * -1 ); // 先置为负
		}
		
		OperationH op = operationDao.getEntity(opId);
		this.pickup(op, pkds);
		
		op = operationDao.getEntity(opId);
		op.setUdf1("拣货取消");
		op.setStatus(WMS.OP_STATUS_01);
		operationDao.update(op);
		
		
		List<OrderH> orders = new ArrayList<>();
		pkds = operationDao.getItems(hql);
		for( OperationItem pkd : pkds ) {
			pkd.setQty( pkd.getQty() * -1 ); // 重新置为正
			pkd.setStatus(WMS.OP_STATUS_01);
			pkd.setToloccode( null );
			operationDao.update(pkd);
			
			orders.add( pkd.getOrderitem().getOrder() );
		}
		
		for(OrderH order : orders) {
			order.setTag("拣货取消");
			order.setStatus( cancelingQty < pickedQty ?  WMS.O_STATUS_41 : WMS.O_STATUS_03);
			orderDao.update(order);
		}
		
		OrderWave wave = op.getWave();
		wave.setStatus(  cancelingQty < pickedQty ?  WMS.W_STATUS_04 : WMS.W_STATUS_03 ); 
		orderDao.update(wave);
	}
	
	private void bindSN2Order(OrderH order, String snlist) {
		if( EasyUtils.isNullOrEmpty(snlist) ) return;
		
		String[] snIDs = snlist.split(",");
		for( String snID : snIDs ) {
			InvSN isn = (InvSN) invDao.getEntity(InvSN.class, EasyUtils.obj2Long(snID));
			isn.setOutTime(new Date());
			isn.setSorder(order.getOrderno());
			invDao.update(isn);
		}
	}
	
	// 清除序列号记录里的出库时间
	private void unBindSN2Order(OrderH order) {
		String hql = "update InvSN set outTime = null, sorder = null where sorder = ? and domain = ?";
		invDao.executeHQL(hql, order.getOrderno(), Environment.getDomain());
	}
	
	public void checkByPKH(Long pkhId) {
		String hql = "from OperationItem o where o.orderitem.order = ? and o.operation.id = ?";
		List<?> orderList = orderDao.getEntities(" select distinct o.orderitem.order from OperationItem o where o.operation.id = ?", pkhId);
		for(Object order : orderList) {
			List<OperationItem> pkds = operationDao.getItems(hql, order, pkhId);
			check( ((OrderH)order).getId(), pkds, null).items.clear();
		}
	}
	
	// 只是验货
	public OperationH check(Long orderId, List<OperationItem> items, String snlist) {
		OrderH order = getOrder(orderId);
		bindSN2Order(order, snlist);
		
		OperationH ck_op = new OperationH();
		ck_op.setOpno( order.genOpNo(WMS.OP_YH) );
		ck_op.setOptype( WMS.opType(WMS.OP_TYPE_YH) );
		ck_op.setWarehouse( order.getWarehouse() );
		ck_op.setStatus( WMS.OP_STATUS_04 );
		ck_op.setQty(0D);
		ck_op.setWorker(Environment.getUserCode());
		orderDao.createObject(ck_op);
		
		List<OperationItem> ck_items = new ArrayList<>();
		for(OperationItem opi : items) {
			OperationItem ck_opi = OperationItem.copy(opi, ck_op);
			orderDao.createObject(ck_opi);
			ck_items.add(ck_opi);
			
			// 修改OrderItem上的 actualQty 值
			OrderItem oItem = orderDao.getOrderItem( opi.getOrderitem().getId() );
			oItem.setQty_checked( MathUtil.addDoubles(oItem.getQty_checked(), opi.getQty()) );
			orderDao.update(oItem);
			
			ck_op.setQty( MathUtil.addDoubles(ck_op.getQty(), opi.getQty()) );
		}
		ck_op.setSkus(items.size());
		
		// 自动封箱
		autoBox(order);
		
		//  检查订单对应的拣货单 是否还未完全确认拣货，是的话 一并拣货确认掉
 		OperationH pkh = getPKH(order);
 		if( pkh != null && pkh.items.size() > 0 ) {
			pkh.fixOpType();
			this.pickup(pkh, pkh.items);
 		}
 		
 		order.setStatus( WMS.O_STATUS_05 ); // 验货完成
		orderDao.update(order);
		
		// 验货完成后直接出库
 		if( WMS.auto_check_outbound() ) {
 			pickupOutbound(orderId, snlist);
 		}
		
		// 验货不改变库存，只需记录下作业日志即可
		OperationLog opLog = new OperationLog();
        opLog.setOperation(ck_op);
        opLog.setOpType(ck_op.getOptype());
        opLog.setOperateTime( new Date() );
        orderDao.createObject(opLog);
		
		InvOperation.fireOpEvent(ck_op, ck_items); // 触发自定义（绩效等）
		
		return ck_op;
	}
	
	// 根据配置项【出库自动封箱】，确定是否需要默认封箱
	private void autoBox(OrderH order) {
		if( !WMS.auto_create_box() ) return;
		
		Box box = orderDao.createBox(new Box(), order);
		
		List<OrderItem> oItems = orderDao.getOrderItems( order.getId() );
		for( OrderItem oItem : oItems ) {
			orderDao.createBoxItem(box, oItem.getQty(), oItem, null);
		}
		box.setType( oItems.size() > 1 ? Box.TYPE_2 : null );
		box.setRemark("自动封箱");
		
		orderDao.fixBoxInfo(box);
	}
	
	// 验货封箱
	public OperationH checkAndBox(Long orderId, List<_DTO> list, Box box, String snlist, int scanfactor) {
		OrderH order = getOrder(orderId);
		String orderno = order.getOrderno();
		
		// TODO 外部取消订单 拦截： 此订单已被外部取消，请录入货物临时存放库位：扫描一个中转库位，将货物放入其中
		if( WMS.O_STATUS_02.equals(order.getStatus()) ) {
			throw new BusinessException("订单已被外部取消，中止出库");
		}
		
		bindSN2Order(order, snlist);
		
		// 生成验货工单
		OperationH op = operationDao.getOperation(orderno, WMS.OP_YH, WMS.OP_STATUS_02);
		if(op == null) {
			op = new OperationH();
			op.setOpno( order.genOpNo(WMS.OP_YH) );
			op.setOptype( WMS.opType(WMS.OP_TYPE_YH) );
			op.setWarehouse( order.getWarehouse() );
			op.setWorker(Environment.getUserCode());
			op.setQty(0D);
			op = (OperationH) orderDao.createObject(op); 
		}
		
		// 生成箱数据
		box = orderDao.createBox(box, order);
		
		// 根据 订单明细 生成对应 作业单明细
		for( _DTO item : list) {
			Double qty_this = item.qty_this;
			OrderItem oItem = orderDao.getOrderItem( item.id );
			
			// 验货作业明细
			operationDao.createCheckOpItem(oItem, op, qty_this);
			
			// 修改 OrderItem上的 actualQty 值
			oItem.setQty_checked( MathUtil.addDoubles(oItem.getQty_checked(), qty_this) );
			
			// 封箱明细
			OperationItem pkd = item.opi_id != null ? operationDao.getOpItem(item.opi_id) : null;
			orderDao.createBoxItem(box, qty_this, oItem, pkd);
		}
		op.setSkus(list.size());
		
		if(box.getType() == null) {
			box.setType( list.size() == 1 && scanfactor > 1 ? Box.TYPE_1 : Box.TYPE_2 );
		}
		orderDao.fixBoxInfo(box);
 		
		// 验货不改变库存，只需记录下作业日志即可
		OperationLog opLog = new OperationLog();
        opLog.setOperation(op);
        opLog.setOpType(op.getOptype());
        opLog.setOperateTime( new Date() );
        orderDao.createObject(opLog);
        
        // 检测是否所有作业明细都 已完成
 		String hql = "from OrderItem where order.id = ? and ifnull(qty, 0) > ifnull(qty_checked, 0)";
		boolean finished = orderDao.getOrderItems(hql, orderId).isEmpty();
 		op.setStatus( finished ? WMS.OP_STATUS_04 : WMS.OP_STATUS_03 );
 		
 		//  检查订单对应的拣货单 是否还未完全确认拣货，是的话 一并拣货确认掉
 		OperationH pkh = getPKH(order);
 		if( finished && pkh != null && pkh.items.size() > 0 ) {
			pkh.fixOpType();
			this.pickup(pkh, pkh.items);
 		}
 		
 		order.setStatus(  finished ? WMS.O_STATUS_05 : WMS.O_STATUS_09  );	
 		orderDao.update(order);
 		
 		// 验货完成后直接出库
 		if( finished && WMS.auto_check_outbound() ) {
 			this.pickupOutbound(orderId, snlist);
 		}
 		
 		return op;
	}
	
	// 找出订单所在的波次拣货单
	private OperationH getPKH( OrderH order ) {
		String hql = " from OperationItem o where o.orderitem.order = ? and o.operation.wave is not null";
		List<OperationItem> pkds = operationDao.getItems(hql, order);
		OperationH pkh = null;
		for( OperationItem pkd : pkds ) {
			pkh = pkd.getOperation();
			pkh.items.remove(pkd);
			if( order.equals(pkd.getOrderitem().getOrder()) && pkd.getToloccode() == null ) { // 筛选出未拣货完成的 属于 当前订单的 PKD
				pkh.items.add(pkd);  // toloccode = null，拣货未完成
			}
		}
 
		return pkh;
	}

	public void cancelCheckAndBox(Long orderId, Long boxId) {
		OrderH order = getOrder(orderId);
		OperationH op = operationDao.getOperation(order.getOrderno(), WMS.OP_YH, WMS.OP_STATUS_02);
		if( op == null ) {
			throw new BusinessException( WMS.O_ERR_15 );
		}
		unBindSN2Order(order);
		
		// 按单取消封箱
		if(boxId == null) {
			List<OrderItem> oItems = orderDao.getOrderItems("from OrderItem where order.id = ? and ifnull(qty_checked,0) > 0", orderId);
			for(OrderItem oItem : oItems) {
				oItem.setQty_checked(0D);
				orderDao.update(oItem);
			}
			
			op.setStatus(WMS.OP_STATUS_02);
			operationDao.update(op);
			
			List<Box> boxs = orderDao.getBoxsByOrder(orderId);
			for(Box box : boxs) {
				orderDao.deleteAll( orderDao.getBoxItems(box.getId()) );
				orderDao.delete(box);
			}
		}
		// 按箱取消封箱
		else {
			Box box = orderDao.getBox(boxId);
			List<BoxItem> bItems = orderDao.getBoxItems(box.getId());
			for(BoxItem bItem : bItems) {
				OrderItem oItem = bItem.getOrderitem();
				Double biQty = bItem.getQty();
				oItem.setQty_checked( MathUtil.subDoubles(oItem.getQty_checked(), biQty) );
				
				OperationItem opi = operationDao.getItems("from OperationItem where operation = ? and orderitem = ?", op, oItem).get(0);
				opi.setQty( MathUtil.subDoubles(opi.getQty(), biQty) );
				operationDao.update(opi);

				orderDao.delete(bItem);
			}
			orderDao.delete(box);
		}
		
		/* 查看是否还有验货量，确定订单状态是 部分验货 or 已拣货 or 部分拣货  */
		List<OrderItem> oItems = orderDao.getOrderItems(orderId);
		Double qty_checked = 0D;
		for(OrderItem oItem : oItems) {
			qty_checked = MathUtil.addDoubles(qty_checked, oItem.getQty_checked());
		}
		if( qty_checked > 0 ) {
			order.setStatus(WMS.O_STATUS_09);
		}
		else {
			// 查看拣货工单的状态，如果是部分完成，则订单状态为“部分拣货”，否则为“已拣货”
			OperationH pkh = getPKH(order); 
			boolean partPicked = pkh != null && pkh.items.size() > 0;
			order.setStatus( (String) EasyUtils.checkTrue(partPicked, WMS.O_STATUS_41, WMS.O_STATUS_42) );
		}
		orderDao.update(order);
	}
	
	/* 
	 * items <[id, opi_id, qty_this];
	 * boxMap { boxno, qty, money, skus(品项数), scanfactor}
	 */
	public void pickupAndCheck(Long opId, Long orderId, String opType, List<_DTO> list, Box box) {
		OperationH pkh = operationDao.getEntity(opId);
		
		// 拣货 + 验货
		if("yh".equals(opType)) {
			List<OperationItem> pkds = operationDao.getItems(opId); 
			for(OperationItem opi : pkds) {
				opi.setQty_checked(opi.getQty());
			}
			
			this.pickup(pkh, pkds);   // 拣货
			
			pkh.items.clear();
			this.check(orderId, operationDao.getItems(opId), null); // 验货
			
			return;
		} 
		
		// “拣货 + 验货 + 封箱” 
		List<OperationItem> pkds = new ArrayList<OperationItem>();
		Double qty_total = 0D;
		for (_DTO item : list) {
			Long pkd_id = item.opi_id;
			Double qty_this = item.qty_this;
			
			OperationItem pkd = operationDao.getOpItem( pkd_id );
			if( pkd.getQty_old() == null ) {
				pkd.setQty_old( pkd.getQty() ); // 原分配的拣货量
			}
			pkd.setQty( qty_this );
			pkd.setQty_checked( qty_this );
			pkds.add(pkd);
			
			Inventory opInv = pkd.getOpinv();
			boolean isZX = opInv != null && opInv.getLotatt02() != null && list.size() == 1;
			box.setType( (String) EasyUtils.checkTrue(isZX, Box.TYPE_1, box.getType()) );
			
			qty_total = MathUtil.addDoubles(qty_total, qty_this);
		}
		box.setQty( qty_total );
		
		this.pickup(pkh, pkds); // 拣货
		this.checkAndBox(orderId, list, box, null, 0); // 验货
 	}
	
	// 允许托盘后 重名存在 (前台输入托盘码，如果码已存在且未出库，先加载，在其上增/减Box)
	public Map<String, Object> palletBoxs(String boxIds, String pallet) {
		List<Box> boxs = orderDao.getBoxs(boxIds); 
		
		Long whId = boxs.get(0).getOrder().getWarehouse().getId();
		List<Box> olds = orderDao.getBoxsByPallent(pallet, whId);
		
		for(Box box : olds) {
			box.setPallet( null );
			box.setPalleter( null );
			box.setPalletTime( null );
		}
		orderDao.flush();
		
		String opNo = pallet + "." + Math.abs(boxIds.hashCode()) + "-" + WMS.OP_ZH; // 装货工单号
		_Warehouse warehouse = null;
		Double total = 0D;
		for(Box box : boxs) {
			warehouse = box.getOrder().getWarehouse();
			box.setPallet(pallet);
			box.setPalleter(Environment.getUserName());
			box.setPalletTime( new Date() );
			
			total = MathUtil.addDoubles(total, box.getQty());
			box.setUdf1(opNo);
		}
		
		// 记录组托工单及日志
		OperationH pallet_op = new OperationH();
		pallet_op.setOpno( opNo );
		pallet_op.setOptype( WMS.opType(WMS.OP_TYPE_ZX) );
		pallet_op.setWarehouse( warehouse );
		pallet_op.setStatus( WMS.OP_STATUS_04 );
		pallet_op.setQty(total);
		pallet_op.setUdf1("组托");
		pallet_op.setUdf2( String.valueOf( Math.abs(boxs.size() - olds.size()) ) ); // 变化箱数（多了或少了）
		pallet_op.setUdf3( "boxs=" + EasyUtils.objAttr2List(boxs, "id") );
		pallet_op.setWorker(Environment.getUserCode());
		orderDao.createObject(pallet_op);

		OperationLog opLog = new OperationLog();
        opLog.setOperation(pallet_op);
        opLog.setOpType(pallet_op.getOptype());
        opLog.setOperateTime( new Date() );
        orderDao.createObject(opLog);
		
		return _Util.toMap("200", "组托成功");
	}
	
	public void palletOutbound(String pallet, Long whId) {
		List<Box> boxs = orderDao.getBoxsByPallent(pallet, whId);
		if(boxs.size() == 0) {
			throw new BusinessException( EX.parse(WMS.P_ERR_2, pallet) );
		}
		
		Set<OrderH> orders = new HashSet<>();
		Long warehouseId = null;
		for(Box box : boxs) {
			box.setSender(Environment.getUserName());
			box.setOutTime(new Date());
			orderDao.update(box);
			
			OrderH order = box.getOrder();
			warehouseId = order.getWarehouse().getId();
			orders.add( order );
		}
		
		/* 可能一个订单打成了多个箱子，在组到了多个托上，只有此订单所有箱子都出库了，才调用 pickupOutbound 接口扣库存，否则只是给Box打出库标记 */
		String hql = " from Box where order = ? and outTime is null ";
		for( OrderH order : orders ) {
			if( orderDao.getEntities(hql, order).isEmpty() ) {
				pickupOutbound(order.getId(), null);
			}
		}
		
		// 记录托盘出库日志
		String opNo = boxs.get(0).getUdf1();
		OperationH pallet_op = operationDao.getOperation(warehouseId, opNo);
		
		OperationLog opLog = new OperationLog();
        opLog.setOperation(pallet_op);
        opLog.setOpType( WMS.opType(WMS.OP_TYPE_OUT) ); 
        opLog.setOperateTime( new Date() );
        orderDao.createObject(opLog);
	}
	
	public void createAndOutbound(OrderH order, List<_DTO> list) {
		if( order.getId() == null ) {
			order.setStatus( WMS.O_STATUS_01 );
			order = orderDao.create(order);
		} 
		List<OrderItem> orderItems = fetchOrderItems(order.getId());
		
		OperationH adjustOp = null;
		Double _qty = 0D, _money = 0D;
		Set<Long> skuIds = new HashSet<>();
		
		for(OrderItem orderItem : orderItems) {
			skuIds.add(orderItem.getSku().getId());
		}
		
		for (_DTO item : list) {
			Long invID = item.inv_id;
			Long skuId = item.sku_id;
			Double qty = item.qty;
			
			if( qty == null) continue;
			if( qty < 0 ) throw new BusinessException( WMS.O_ERR_17 );
			
			_Sku sku = skuDao.getEntity(skuId);
			skuIds.add(skuId);
			
			Double price = item.price;
			Double money = item.money;
			_qty = MathUtil.addDoubles(_qty, qty);
			_money = MathUtil.addDoubles(_money, money);
			
			// PC无单出库时，库存无记录，但实物存在；需先进行调整
			if( invID == null ) { 
				if( adjustOp == null ) {
					adjustOp = new OperationH();
					adjustOp.setOpno( order.genOpNo(WMS.OP_CC) );
					adjustOp.setOptype( WMS.opType(WMS.OP_TYPE_TZ) );
					adjustOp.setWarehouse(order.getWarehouse());
					adjustOp.setStatus( WMS.OP_STATUS_04 );
					adjustOp.setWorker( Environment.getUserCode() );
					operationDao.createObject(adjustOp);
				}
				
				OperationItem opi = new OperationItem();
				opi.setOperation(adjustOp);
				opi.setOwner( order.getOwner() );
				opi.setSkucode( sku.getCode() );
				opi.setLoccode( locDao.getOutLoc(order.getWarehouse().getId()) );
				opi.setQty(0D);
				opi.setToqty(qty);
				opi.copyLotAtt(item);
				operationDao.createObject(opi);
				
				List<OperationItem> adjustItems = new ArrayList<OperationItem>();
				adjustItems.add(opi);
				invID = invOperation.execOperations(adjustOp, adjustItems).iterator().next();
				item.inv_id = invID;
			}
			
			OrderItem oi = new OrderItem();
			oi.setInv_id(invID);
			oi.setSku( sku );
			oi.setQty( qty );
			oi.setPrice(price);
			oi.setMoney(money);
			oi.copyLotAtt(item);
			oi.setOrder(order);
			oi = (OrderItem) orderDao.createObject(oi);
			item.id = oi.getId();
		}
		order.setMoney(_money);
		order.setQty( MathUtil.addDoubles(order.getQty(), _qty) );
		order.setSkus( skuIds.size() );
		orderDao.update(order);
		
		this.outbound(order.getId(), list);
		
		autoBox(order);
	}
	
	// 直接按单出库（允许部分出库）
	public void outbound4rf(Long orderId, Long opId, List<_DTO> list) {
		OrderH order = getOrder(orderId);
		checkOrderStatus(order);
		
		OperationH op = operationDao.getOperation(opId);
		List<OperationItem> opItems = new ArrayList<OperationItem>();
		int index = 0;
		for(_DTO item : list) {
			index ++;
			Long itemId = item.id;
			Long inv_id = item.inv_id;
			String snList = item.snlist;
			Double qty_this;
			
			if( !EasyUtils.isNullOrEmpty(snList) ) {
				bindSN2Order(order, snList);
				List<String> sns = Arrays.asList(snList.trim().split(","));
				qty_this = sns.size() * 1.0;
			} else {
				qty_this = item.qty_this;
			}
			if( qty_this == null || qty_this == 0) continue;
			
			OrderItem orderItem = orderDao.getOrderItem(itemId);
			
			if( inv_id == null ) { // 直接在页面上填写数量，做按单出库，报错
				String sku = orderItem.getSku().getName();
				throw new BusinessException( EX.parse(WMS.O_ERR_19, index, sku) );
			}
			Inventory inv = invDao.getEntity(inv_id);
			
			OperationItem opItem = new OperationItem(inv, qty_this * -1 );
			opItem.setOrderitem(orderItem);
			opItem.setOperation(op);
			opItem.setToqty( 0D );
			operationDao.createObject(opItem);
			
			opItems.add(opItem);
			orderItem.setQty_send( MathUtil.addDoubles(orderItem.getQty_send(), qty_this) );
		}
		
		invOperation.execOperations(op, opItems);
		setOrderStatus(order, op);
	}
	
	private void setOrderStatus( OrderH order, OperationH op ) {
		// 检查出库单是否已完成出库
		String hql = "from OrderItem where order = ? and ifnull(qty,0) > ifnull(qty_send,0)";
		boolean finished = orderDao.getEntities(hql, order).isEmpty();
		
		orderDao.updateOrderAfterOutbound(order, finished ?  WMS.O_STATUS_06 : WMS.O_STATUS_08);
		
		op.setStatus(finished ? WMS.OP_STATUS_04 : WMS.OP_STATUS_03 );
		orderDao.update(op);
	}
	
	private void checkOrderStatus(OrderH order) {
		// 【新建、部分完成、出库取消】状态能直接出库
		String status = order.getStatus();
		if( !WMS.O_STATUS_01.equals(status) && !WMS.O_STATUS_08.equals(status) && !WMS.O_STATUS_07.equals(status) ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_7, order.getOrderno(), status) );
		}
		if( WMS.outbound_confirm() ) {
			throw new BusinessException( WMS.O_ERR_18 );
		}
	}
	
	public void outbound(Long orderId, List<_DTO> list) {
		OrderH order = getOrder(orderId);
		checkOrderStatus(order);
		
		OperationH op = operationDao.getOperation(order.getOrderno(), WMS.OP_OUT);
		if(EasyUtils.isNullOrEmpty(op)) {
			op = new OperationH();
			op.setOpno( order.genOpNo(WMS.OP_OUT) );
			op.setOptype( WMS.opType(WMS.OP_TYPE_OUT) );
			op.setWarehouse( order.getWarehouse() );
			op.setWorker( Environment.getUserCode() );
			operationDao.create(op);
		}
		
		List<OperationItem> opItems = new ArrayList<OperationItem>();
		for(_DTO item : list) {
			Long oiId = item.id;
			if(oiId == null) continue;
			
			OrderItem oItem = orderDao.getOrderItem( oiId );
			
			Double qty_this;
			if( item.qty_this != null ) {
				if( item.qty_this.doubleValue() == 0 ) continue; 
				qty_this = item.qty_this;
			}
			else {
				qty_this = oItem.getQty(); // 移动端无单出库，orderItem为新创建，数量即等于_DTO.qty
			}
			
			Inventory inv = invDao.getEntity( item.inv_id );
			OperationItem opItem = new OperationItem(inv,  qty_this * -1 );
			opItem.setOrderitem(oItem);
			opItem.setOperation(op);
			opItem.setToqty( 0D );
			opItem.setStatus( WMS.OP_STATUS_04 );
			operationDao.createObject(opItem);
			opItems.add(opItem);
			
			oItem.setQty_send( MathUtil.addDoubles(oItem.getQty_send(), qty_this) );
		}
		
		invOperation.execOperations(op, opItems);
		
//		this.autoBox(order);
		setOrderStatus(order, op);
	}

	// 出库前提是已经验货完成（也就意味不支持部分出库），即所需订单货量都已经被准确拣货到出货台，并已封箱打包
	public void pickupOutbound(Long orderId, String snlist) {

		OrderH order = getOrder(orderId);
		String status = order.getStatus();
		_Warehouse warehouse = order.getWarehouse();
		
		bindSN2Order(order, snlist);
		
		// 【验货完成】状态才能出库
		if( !WMS.O_STATUS_05.equals( status ) ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_16, order.getOrderno(), status) );
		}
		
		OperationH op = new OperationH();
		op.setOpno( order.genOpNo(WMS.OP_OUT) );
		op.setOptype( WMS.opType(WMS.OP_TYPE_OUT) );
		op.setWarehouse(warehouse);
		op.setStatus( WMS.OP_STATUS_04 ); 
		operationDao.create(op);
		
		List<OperationItem> opItems = new ArrayList<OperationItem>();
		
		List<OrderItem> oiList = orderDao.getOrderItems(orderId);
		orderDao.insertEntityIds2TempTable(oiList);
		
		// 找出拣货明细，生成出库明细 (验货明细没有库位信息)
		String hql = "select distinct opi from OperationItem opi, OrderItem oi, Temp t" +
				" where opi.orderitem.id = oi.id and oi.id=t.id and t.thread =  " + Environment.threadID()
				+ " and opi.operation.optype in (?,?) and toloccode is not null";  // toloccode = null，拣货未完成
		List<OperationItem> items = operationDao.getItems(hql, WMS.opType(WMS.OP_TYPE_JH), WMS.opType(WMS.OP_TYPE_BCJH) );
		for(OperationItem opItem : items) {
			OperationItem opItem2 = new OperationItem();
			BeanUtil.copy(opItem2, opItem, "id,operation,orderItem,creatorId,creatorName,updatorId,updatorName".split(","));
			opItem2.setOperation(op);
			opItem2.setQty( opItem2.getQty() * -1 );
			opItem2.setToqty( 0D );
			
			opItem2.setLoccode( opItem.getToloccode() );
			opItem2.setToloccode( null );
			
			operationDao.createObject(opItem2);
			opItems.add(opItem2);
		}
		
		invOperation.execOperations(op, opItems);
		
		// 修改实际出库量
		for (OperationItem opItem : opItems) {
			OrderItem orderItem = opItem.getOrderitem();
			Double thisQty = Math.abs( opItem.getQty() );
			orderItem.setQty_send( MathUtil.addDoubles(orderItem.getQty_send(), thisQty)  );
		}
		orderDao.updateOrderAfterOutbound(order, WMS.O_STATUS_06);
	}
	
	public void outboundDirect(Long orderId) {
		OrderH order = orderDao.getEntity(orderId);
		OrderWave wave = order.getWave();
		order.setWave(null);
		
		OperationH sw = this.allocate(orderId); // 分配
		List<OperationItem> items = operationDao.getItems(sw.getId()); // 拣货
		this.pickup(sw, items);
		order.setStatus(WMS.O_STATUS_05); // 直接设置为验货完成
		order.setWave(wave);
		orderDao.update(order);

		this.pickupOutbound(orderId, null); // 出库
	}

	// 利用出库单，生成一份调整单，同时修改订单的状态
	public void cancelOutbound(Long orderId) {

		OrderH order = getOrder(orderId);
		String status = order.getStatus();
		
		// 出库完成的单子可以做取消操作，通过调整单来处理
		if( !WMS.O_STATUS_06.equals( status ) && !WMS.O_STATUS_08.equals( status ) ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_6, order.getOrderno(), status) );
		}
		
		unBindSN2Order(order);
		
		// 找出出库工单，取消
		OperationH out_op = operationDao.getOperation(order.getOrderno(), WMS.OP_OUT);
		out_op.setUdf1("出库取消");
		out_op.setOpno( order.genOpNo("C") );
		operationDao.update(out_op);
		
		// 新建一笔正数的出库记录（出库为负数）
		String hql = "select opi from OperationItem opi, OrderItem oi " +
				" where opi.orderitem.id = oi.id and oi.order.id=? and opi.operation.optype=? and IFNULL(opi.status, '已完成') = ? and opi.qty < 0";
		List<OperationItem> opItems = operationDao.getItems(hql, orderId, WMS.opType(WMS.OP_TYPE_OUT), WMS.OP_STATUS_04);
		List<OperationItem> opis_new = new ArrayList<>();
		for( OperationItem opi_old : opItems ) {
			
			OrderItem item = opi_old.getOrderitem();
			item.setQty_send(0D);
			operationDao.update(item);
			
			OperationItem opi_new = new OperationItem();
			opi_new.copy(opi_old);
			opi_new.setOperation(out_op);
			opi_new.setQty( opi_old.getQty() * -1 );
			opi_new.setStatus( WMS.OP_STATUS_04 );
			operationDao.createObject(opi_new);
			
			opi_old.setOrderitem(null);             // 斩断和OrderItem的联系
			opi_old.setStatus( WMS.OP_STATUS_02 ); // 把明细状态置为已取消
			operationDao.update(opi_old);
			
			opis_new.add(opi_new);
		}
		invOperation.execOperations(out_op, opis_new);
		
		// 拣货工单也斩断
		hql = "select opi from OperationItem opi, OrderItem oi where opi.orderitem.id = oi.id and oi.order.id=? and opi.operation.optype=?";
		List<?> pkds = orderDao.getEntities(hql, orderId, WMS.opType(WMS.OP_TYPE_JH) );
		for(Object t : pkds) {
			OperationItem pkd = (OperationItem) t;
			pkd.setOrderitem(null);  // 斩断和OrderItem的联系
			operationDao.update(pkd);
		}
		
		// 斩断和封箱数据的联系
		List<Box> boxs = orderDao.getBoxsByOrder(orderId);
		for( Box box : boxs) {
			box.setOrder(null);
			box.setBoxno("C" + box.getId());
			box.setRemark(order.getOrderno() + "出库取消");
			orderDao.update(box);
		}
		
		order.setWave(null);
		order.setStatus( WMS.O_STATUS_07 ); // 出库取消
		order.setWorker( null );
		order.setTag( EasyUtils.obj2String(order.getTag()).replaceAll("confirm", "") );
		order.setTms_code(null);
		order.setTms_error(null);
		orderDao.update(order);
	}
	
	public OrderH cancelOrder(Long orderId, String reason) {
		OrderH order = getOrder(orderId);
		String status = order.getStatus();
		
		// 已完成/关闭 的订单 不可以取消； 已完成需先取消出库，再取消订单
		if( WMS.O_STATUS_06.equals(status) || WMS.O_STATUS_00.equals(status)  ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_10, order.getOrderno(), status) );
		}
		
		// 如果是已验货状态，则取消封箱数据，变回已拣货
		
		// 如果是已分配库存状态（或部分拣货），则释放锁定量；如果已完成拣货，则不管
		if( WMS.O_STATUS_03.equals(status) || WMS.O_STATUS_41.equals(status) ) {
			cancelAllocate(orderId, true);
		}
		
		OperationH op = operationDao.getOperation(order.getOrderno(), WMS.OP_OUT);
		if( op != null ) {
			String opStatus = op.getStatus();
			List<String> statusList = Arrays.asList(WMS.OP_STATUS_03, WMS.OP_STATUS_04, WMS.OP_STATUS_07);
			if( statusList.contains(opStatus) ) {
				throw new BusinessException( EX.parse(WMS.O_ERR_9, order.getOrderno()) );
			}
			op.setStatus(WMS.OP_STATUS_02);
			operationDao.update(op);
		}
		order.setStatus( WMS.O_STATUS_02 );
		order.setRemark( "取消原因：" + reason + ";" + EasyUtils.obj2String(order.getRemark()) );
		orderDao.update(order);
		
		return order;
	}

	public OrderH closeOrder(Long orderId) {
		OrderH order = getOrder(orderId);
		String oldStatus = order.getStatus();
		
		// 可关闭订单状态：新建、取消、出库取消、部分出库、已完成
		List<String> statusList = new ArrayList<>();
		statusList.add(WMS.O_STATUS_01);
		statusList.add(WMS.O_STATUS_02);
		statusList.add(WMS.O_STATUS_08);
		statusList.add(WMS.O_STATUS_07);
		statusList.add(WMS.O_STATUS_06);
		if( !statusList.contains(oldStatus) ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_4, order.getOrderno(), oldStatus) );
		}
		
		order.setStatus( WMS.O_STATUS_00 );
		orderDao.update(order);
		
		_OpEvent.create().afterCloseOrder(order, oldStatus);
		
		return order;
	}
}
