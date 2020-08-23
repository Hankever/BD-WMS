/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.BoxItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Warehouse;

@Controller("OrderAction")
@RequestMapping( {"/wmsorder", "/wms/api"} )
public class OrderAction {
	
	protected Logger log = Logger.getLogger(this.getClass()); 
	
	@Autowired private OrderService orderService;
	@Autowired private OperationDao operationDao;
	@Autowired private OrderHDao 	orderDao;
	@Autowired private InventoryDao invDao;
	
	/**
	 * 1、单号自动创建
	 * 2、指定既有单号为出库单号，此单号在数据库不存在
	 * 3、指定既有单号为出库单号，此单号在数据库已存在 && 状态 == 已完成（即多次用同一单号出库）
	 * 4、指定既有单号为出库单号，此单号在数据库已存在 && 状态 == 新建
	 */
	@RequestMapping(value = "/createAndOutbound", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> createAndOutbound(HttpServletRequest request, Long whId, Long ownerId) {
		
		if( !WMS.no_order_outbound() && WMS.isWorker() ) {
			throw new BusinessException( WMS.O_ERR_1 );
		}
		
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		OrderH order = null;
		String orderno = params.get("orderno");
		if( !EasyUtils.isNullOrEmpty(orderno) ) {
			order = orderDao.getOrder(orderno);
			if( order != null ) {
				String status = order.getStatus();
				if( WMS.O_STATUS_01.equals(status) ) {
					if(EasyUtils.obj2Double(order.getQty()) > 0) {
						throw new BusinessException( EX.parse(WMS.O_ERR_2, orderno, 1) );
					}
				} else if( !WMS.NO_ORDER.equals(order.getTag()) ) {
					throw new BusinessException( EX.parse(WMS.O_ERR_2, orderno, 2) );
				}
				
				order.setStatus(  (String) EasyUtils.checkTrue( WMS.O_STATUS_06.equals(status) , WMS.O_STATUS_08, status) );
			}
		} 
		
		if( order == null ) {
			_Owner owner = operationDao.getOwner(ownerId);
			order = new OrderH();
			order.setOrderno((String) EasyUtils.checkNull(orderno, _Util.genDocNO(owner, "0", false)));
			order.setOrderday(DateUtil.today());
			order.setWarehouse(new _Warehouse(whId));
			order.setOwner( operationDao.getOwner(ownerId) );
			params.remove("id");
			params.remove("orderno");
		}
		BeanUtil.setDataToBean(order, params);
		order.setTag(WMS.NO_ORDER);
		
		List<_DTO> list = _DTO.parse(params);

		orderService.createAndOutbound(order, list);
		
		Map<String, Object> result = _Util.toMap("200", "出库成功");
		result.put("orderno", order.getOrderno());
		
		return result;
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	@ResponseBody
	public void deleteOrder(@PathVariable Long id) {
		orderService.deleteOrder(id);
	}
	
	@RequestMapping(value = "/cancel/{id}", method = RequestMethod.POST)
	@ResponseBody
	public OrderH cancelOrder(@PathVariable Long id, String reason) {
		return orderService.cancelOrder(id, reason);
	}
	
	@RequestMapping(value = "/close/{id}", method = RequestMethod.POST)
	@ResponseBody
	public void closeOrder(@PathVariable Long id) {
		orderService.closeOrder(id);
	}
	
	@RequestMapping(value = "/assignOrder", method = RequestMethod.POST)
	@ResponseBody
	public void assign(Long id, String worker, String type, Long opId) {
		orderService.assign(id, worker, type, opId);
	}
	
	@RequestMapping(value = "/allocate/{id}", method = RequestMethod.POST)
	@ResponseBody
	public OperationH allocate(@PathVariable Long id) {
		return orderService.allocate(id);
	}
	
	@RequestMapping(value = "/cancelAllocate/{id}", method = RequestMethod.POST)
	@ResponseBody
	public OrderH cancelAllocate(@PathVariable Long id) {
		return orderService.cancelAllocate(id, false);
	}
	
	@RequestMapping(value = "/pickup/{opId}", method = RequestMethod.POST)
	@ResponseBody
	public  Map<String, Object> pickup(@PathVariable Long opId, String pkdIds) {
		List<OperationItem> pkds = operationDao.getItems(" from OperationItem where id in (" + pkdIds + ")");
		OperationH op = operationDao.getEntity(opId);
		orderService.pickup(op, pkds);
		
		return _Util.toMap("200", "拣货成功");
	}
	
	@RequestMapping(value = "/pickup/cancel/{opId}", method = RequestMethod.POST)
	@ResponseBody
	public void cancelPickup(@PathVariable Long opId, String pkdIds) {
		orderService.cancelPickup(opId, pkdIds);
	}
	
	// { boxno: this.data.orderno + '-' + this.getBoxno(), qty: this.data.count_num, skus: skus }
	@RequestMapping(value = "/pickCheck", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> pickupAndCheck4rf(Long orderId, Long opId, Box box, String type, String items) {
		List<_DTO> list = _DTO.parse(items);
		orderService.pickupAndCheck(opId, orderId, type, list, box);
		return _Util.toMap("200", "拣货验货成功");
	}
	
	@RequestMapping(value = {"/checkAndBox", "/sealing"}, method = RequestMethod.POST)
	@ResponseBody
	public void checkAndBox(Long orderId, String items, Box box, String snlist, Integer scanfactor) {
		List<_DTO> list = _DTO.parse(items);
		orderService.checkAndBox(orderId, list, box, snlist, EasyUtils.obj2Int(scanfactor));
	}
	
	@RequestMapping(value = {"/cancelCheckAndBox", "/cancelSealing"}, method = RequestMethod.POST)
	@ResponseBody
	public void cancelCheckAndBox(Long orderId, Long boxId) {
		orderService.cancelCheckAndBox(orderId, boxId);
	}
	
	@RequestMapping(value = "/check/{orderId}", method = RequestMethod.POST)
	@ResponseBody
	public void check(@PathVariable Long orderId, String snlist) {
		String hql = "from OperationItem where orderitem.order.id = ? and operation.status != ?";
		List<OperationItem> items = operationDao.getItems(hql, orderId, WMS.OP_STATUS_02);
		orderService.check(orderId, items, snlist);
	}
	
	@RequestMapping(value = "/pickupOutbound/{orderId}", method = RequestMethod.POST)
	@ResponseBody
	public Object pickupOutbound(@PathVariable Long orderId, String snlist) {
		log.debug( orderId + ", snlist = " + snlist);
		
		orderService.pickupOutbound(orderId, snlist); // 出库交接，扫描货品序列号
		return _Util.toMap("200", "出库交接完成");
	}
	

	@RequestMapping(value = {"/checkOutbound", "/orderCheckout"}, method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> checkAdnOutbound4rf(HttpServletRequest request, Long id, Long opId) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		orderService.outbound4rf(id, opId, _DTO.parse(params));
		
		return _Util.toMap("200", "验货出库成功");
	}
	
	@RequestMapping(value = "/outbound/{id}", method = RequestMethod.POST)
	@ResponseBody
	public void outbound(@PathVariable Long id, String items) {
		List<_DTO> list = _DTO.parse(items);
		orderService.outbound(id, list);
	}

	@RequestMapping(value = "/cancelOutbound/{id}", method = RequestMethod.POST)
	@ResponseBody
	public void cancelOutbound(@PathVariable Long id) {
		orderService.cancelOutbound(id);
	}
		
	/* ------------------------------------------------------- 组托出库 ------------------------------------------------ */
	
	/**
	 * 网页端 出库单绑定成托
	 */
	@RequestMapping(value = "/palletOrders", method = RequestMethod.POST)
	@ResponseBody
	public void palletOrders(String orderIds, String pallet) {
		List<?> boxIds = orderDao.getEntities("select id from Box where order.id in (" + orderIds + ")");
		palletBoxs(EasyUtils.list2Str(boxIds), pallet);
	}
	
	@RequestMapping(value = "/palletBoxs", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> palletBoxs(String boxIds, String pallet) {
		return orderService.palletBoxs(boxIds, pallet);
	}
	
	@RequestMapping(value = "/pallet/box", method = RequestMethod.GET)
	@ResponseBody
	public List<Box> getBoxsByPallent(String pallet, Long whId) {
		return orderDao.getBoxsByPallent(pallet, whId);
	}
	
	/* 组托相关接口: 
	 * 1、查找未出库托盘列表： /api/data/sql/palletList
	 */
	
	// 用于移动端按托出库，只需扫描托盘上的任一箱码
	@RequestMapping(value = "/query_pallet/{boxno}", method = RequestMethod.POST)
	@ResponseBody
	public String queryPallet(@PathVariable String boxno) {
		String sql = "select pallet from wms_box where boxno = ?";
		return (String) SQLExcutor.queryVL(sql, "pallet", boxno);
	}
	
	@RequestMapping(value = "/palletOutbound", method = RequestMethod.POST)
	@ResponseBody
	public Object palletOutbound( String pallet, Long whId ) {
		orderService.palletOutbound( pallet, whId );
		return _Util.toMap("200", "按托出库完成");
	}
	
	/* ------------------------------------------------------- 查询类接口 ------------------------------------------------ */
	
	@RequestMapping(value = "/query")
	@ResponseBody
	public Map<String,Object> queryOrders(OrderSo so, int page, int rows) {
		so.getPage().setPageNum(page);
		so.getPage().setPageSize(rows);
		return orderService.search(so).toEasyUIDataGrid();
	}
	
	@RequestMapping(value = "/queryOrderItem", method = RequestMethod.GET)
	@ResponseBody
	public Map<Object, Object> queryOrderItem(String code){
		OrderH order = orderDao.getOrder(code);
		if( order == null ) {
			throw new BusinessException( EX.parse(WMS.O_ERR_12, code) );
		}
		
		Long ownerId = order.getOwner().getId();
		Long orderId = order.getId();
		List<OrderItem> orderItems = orderDao.getOrderItems( orderId );
		
		/* 显示订单指定库存的库位 */
		for( OrderItem oi : orderItems ) {
			Long inv_id = oi.getInv_id();
			if( inv_id != null ) {
				Inventory inv = invDao.getEntity(inv_id);
				oi.out_loc = inv.getLocation().getCode();
			}
		} 
		
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put("orderId", orderId);
		map.put("ownerId", ownerId);
		map.put("order", order);
		map.put("items", orderItems);
		map.put(orderId + "-" + ownerId, orderItems );
		return map;
	}
	
	@RequestMapping(value = "/queryBoxItem", method = RequestMethod.GET)
	@ResponseBody
	public List<BoxItem> queryBoxItem(Long boxId){
		return orderDao.getBoxItems(boxId);
	}
	
	@RequestMapping(value = "/pk_operation/{orderId}", method = RequestMethod.POST)
	@ResponseBody
	public OperationH queryPKOperation(@PathVariable Long orderId) {
		OrderH order = orderDao.getEntity(orderId);
		OrderWave wave = order.getWave();
		if( wave == null ) {
			return new OperationH();
		}
		
		OperationH subwave = operationDao.getSubwaves(wave.getId()).get(0);
		List<OperationItem> pkds = operationDao.getItems(subwave.getId());
		for( OperationItem pkd : pkds ) {
			if( pkd.getOrderitem().getOrder().equals(order) ) {
				subwave.items.add( pkd );
			}
		}
		
		return subwave;
	}
}
