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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
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
import com.boudata.wms.inventory.OperationService;

@SuppressWarnings("unchecked")
public class OrderActionTest extends AbstractTest4PK {

	@Autowired OperationService opService;
	@Autowired OperationDao operationDao;
	@Autowired OrderHDao orderDao;
	@Autowired OrderAction orderAction;
	
	@Test
	public void testCreateAndOutbound() {
		JSONObject obj = new JSONObject();
		obj.put("sku_id", skuOne.getId());
		obj.put("qty", "2");
		obj.put("price", "10.0");
		obj.put("money", "20.0");
		obj.put("snlist", "");
		String items = "[" + obj.toString() + "]";
		
		request.addParameter("items", items);
		request.addParameter("toloccode", IN_LOC.getCode());
		orderAction.createAndOutbound(request, W1.getId(), OW1.getId());
		
		String code = "O001";
		request.addParameter("orderno", code);
		orderAction.createAndOutbound(request, W1.getId(), OW1.getId());
		
		obj.put("snlist", "1,2,3,4,5");
		items = "[" + obj.toString() + "]";
		request.removeParameter("items");
		request.addParameter("items", items);
		request.addParameter("asnno", code);
		orderAction.createAndOutbound(request, W1.getId(), OW1.getId());
		
//		List<?> result = commonDao.getEntities("select count(*) from InvSN where barcode = ?", skuOne.getBarcode());
		
		try {
			login(worker1);
			request.getSession().setAttribute("no_order_outbound", "0");
			orderAction.createAndOutbound(request, W1.getId(), OW1.getId());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( WMS.O_ERR_1, e.getMessage());
		}
	}
	
	@Test
	public void testCreateAndOutbound2() {
		// 先建一个空的出库单，再对此单进行无单出库（ 空单出库 ）
		String orderNo = "O002";
		OrderH order = new OrderH();
		order.setOrderno(orderNo);
		order.setOwner(OW1);
		order.setWarehouse(W1);
		order.setOrderday( new Date() );
		order.setType("空单出库");
		orderService.createOrder(order, new ArrayList<OrderItem>());
		
		JSONObject obj = new JSONObject();
		obj.put("sku_id", skuOne.getId());
		obj.put("qty", "2");
		String items = "[" + obj.toString() + "]";
		
		request.addParameter("items", items);
		request.addParameter("toloccode", IN_LOC.getCode());
		request.addParameter("orderno", orderNo);
		orderAction.createAndOutbound(request, W1.getId(), OW1.getId());
		Assert.assertEquals(WMS.NO_ORDER, order.getTag());
		
		try {
			orderService.deleteOrder(order.getId());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_3, orderNo, order.getStatus()), e.getMessage() );
		}
	}
	
	@Test
	public void testPickCheck() {
		OrderH order = createOrder("SO0011", OW1, W1, 1);
		Long orderId = order.getId();
		
		List<Inventory> invs = (List<Inventory>) orderDao.getEntities(" from Inventory where domain = ?", Environment.getDomain());
		Inventory inv = invs.get(0);
		inv.setQty(1000D);
		orderDao.update(inv);
		
		OperationH sw = orderService.allocate( orderId );
		
		List<OperationItem> items = operationDao.getItems(sw.getId());
		List<Map<Object, Object>> items_ = new ArrayList<Map<Object, Object>>();
		Double qty = 0D;
		for(OperationItem opi : items) {
			qty += opi.getQty();
			Map<Object, Object> map = new HashMap<Object, Object>();
			map.put("opi_id", opi.getId());
			map.put("id", opi.getOrderitem().getId());
			map.put("inv_id", opi.getOpinv().getId());
			map.put("qty_this", opi.getQty());
			items_.add(map);
		}
		
		Box box = new Box();
		box.setBoxno(order.getOrderno() + "-001");
		box.setQty(qty);
		box.setQty(1D);
		orderAction.pickupAndCheck4rf(orderId, sw.getId(), box, "fx", EasyUtils.obj2Json(items_));
	}
	
	@Test
	public void testSealing() {
		OrderH order = createOrder("SO0011", OW1, W1, 1);
		Long orderId = order.getId();
		
		List<Inventory> invs = (List<Inventory>) orderDao.getEntities(" from Inventory where domain = ?", Environment.getDomain());
		Inventory inv = invs.get(0);
		inv.setQty(1000D);
		orderDao.update(inv);
		
		OperationH sw = orderService.allocate( orderId );
		
		List<OperationItem> items = operationDao.getItems(sw.getId());
		List<Map<Object, Object>> items_ = new ArrayList<Map<Object, Object>>();
		Double qty = 0D;
		for(OperationItem opi : items) {
			qty += opi.getQty();
			Map<Object, Object> map = new HashMap<Object, Object>();
			map.put("opi_id", opi.getId());
			map.put("id", opi.getOrderitem().getId());
			map.put("inv_id", opi.getOpinv().getId());
			map.put("qty_this", opi.getQty());
			items_.add(map);
		}
		Box box = new Box();
		box.setBoxno(order.getOrderno() + "-001");
		box.setQty(qty);
		box.setQty(1D);
		
		orderAction.checkAndBox(orderId, EasyUtils.obj2Json(items_), box, "", 0);
		
//		List<Box> boxs = orderHDao.getBoxsByOrder(orderId);
		
		orderAction.cancelCheckAndBox(orderId, null);
	}

	@Test
	public void testPick() {
		OrderH order = createOrder("SO0011", OW1, W1, 1);
		Long orderId = order.getId();
		
		List<Inventory> invs = (List<Inventory>) orderDao.getEntities(" from Inventory where domain = ?", Environment.getDomain());
		Inventory inv = invs.get(0);
		inv.setQty(1000D);
		orderDao.update(inv);
		
		OperationH sw = orderAction.allocate( orderId );
		
		orderAction.cancelAllocate(orderId);
		commonDao.executeHQL("delete from OperationItem where status = ?", "取消");
		commonDao.executeHQL("delete from OperationH where status = ?", "分配取消");
		
		sw = orderAction.allocate( orderId );
		
		List<OperationItem> items = operationDao.getItems(sw.getId());
		String pkdIds = EasyUtils.list2Str(_Util.getIDs(items));
		orderAction.pickup(sw.getId(), pkdIds);
		
		orderAction.queryPKOperation(orderId);
		
		orderAction.cancelPickup(sw.getId(), pkdIds);
		
		orderAction.pickup(sw.getId(), pkdIds);
		
		orderAction.check(orderId, "");
		
		orderAction.pickupOutbound(orderId, "");
	}
	
	@Test
	public void testOrderCheckout() { 
		OrderH order = createOrder("SO0011", OW1, W1, 1);
		Long orderId = order.getId();
		
		orderAction.assign(order.getId(), "13735547815", WMS.OP_TYPE_OUT, null);
		
		OperationH sw = operationDao.getOperation(order.getOrderno(), WMS.OP_OUT);
		
		List<Inventory> invs = (List<Inventory>) orderDao.getEntities(" from Inventory where domain = ?", Environment.getDomain());
		Inventory inv = invs.get(0);
		inv.setQty(1000D);
		orderDao.update(inv);
		
		List<OrderItem> odis = orderDao.getOrderItems(orderId);
		List<Map<Object, Object>> items_ = new ArrayList<Map<Object, Object>>();
		for(OrderItem odi : odis) {
			Map<Object, Object> map = new HashMap<Object, Object>();
			map.put("id", odi.getId());
			map.put("inv_id", inv.getId());
			map.put("qty_this", odi.getQty());
			map.put("snlist", "");
			items_.add(map);
		}
		request.addParameter("items", EasyUtils.obj2Json(items_));
		request.getSession().setAttribute("outbound_confirm", "1");
		try {
			orderAction.checkAdnOutbound4rf(request, orderId, sw.getId());
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals(WMS.O_ERR_18, e.getMessage());
		}
		
		request.getSession().setAttribute("outbound_confirm", "0");
		orderAction.checkAdnOutbound4rf(request, orderId, sw.getId());
	}
	
	@Test
	public void testQuery() { 
		String pallet = "P001";
		
		OrderH order = createOrder("SO0011", OW1, W1, 1);
		List<OrderItem> odis = orderDao.getOrderItems(order.getId());
		
		OrderSo so = new OrderSo();
		so.setId(order.getId());
		so.setWarehouseId(order.getWarehouse().getId());
		so.setWarehouseCode(order.getWarehouse().getCode());
		so.setOrderno(order.getOrderno());
		so.setOrderdayFrom(DateUtil.subDays(order.getOrderday(), 1));
		so.setOrderdayTo(DateUtil.addDays(order.getOrderday(), 1));
		so.setOwnerId(order.getOwner().getId());
		so.setCreator(order.getCreator());
		so.setStatus(order.getStatus());
		so.setDomain(order.getDomain());
		
		orderAction.queryOrders(so, 1, 100);
		
		orderAction.queryOrderItem(order.getOrderno());
		
		Box box = new Box();
		box.setBoxno("Box001");
		box.setOrder(order);
		box.setType("整箱");
		box.setQty(1D);
		box.setSkus(1);
		box.setPallet(pallet);
		orderDao.createObject(box);
		
		BoxItem boxItem = new BoxItem();
		boxItem.setBox(box);
		boxItem.setOrderitem(odis.get(0));
		boxItem.setSku(odis.get(0).getSku());
		boxItem.setQty(1D);
		orderDao.createObject(boxItem);
		
		orderAction.queryBoxItem(box.getId());
		
		orderAction.queryPallet(box.getBoxno());
		
		orderAction.palletOrders(order.getId().toString(), pallet);
	}
	
	@Test
	public void testOutbound() {
		OrderH order = createOrder("SO0011", OW1, W1, 1, 10D);
		
		List<Inventory> invs = (List<Inventory>) orderDao.getEntities(" from Inventory where domain = ?", Environment.getDomain());
		Inventory inv = invs.get(0);
		inv.setQty(1000D);
		orderDao.update(inv);
		
		Long orderId = order.getId();
		List<OrderItem> oItems = orderDao.getOrderItems(orderId);
		List<_DTO> items = new ArrayList<>();
		for(OrderItem oItem : oItems) {
			_DTO item = new _DTO();
			item.id = oItem.getId();
			item.inv_id = inv.getId();
			item.qty_this = order.getQty();
			items.add(item);
		}
		
		orderAction.outbound(orderId, EasyUtils.obj2Json(items));
		orderAction.cancelOutbound(orderId);
		
		items = new ArrayList<>();
		for(OrderItem oItem : oItems) {
			_DTO item = new _DTO();
			item.id = oItem.getId();
			item.inv_id = inv.getId();
			item.qty_this = 3D;
			items.add(item);
			
			item = new _DTO();
			item.id = oItem.getId();
			item.inv_id = inv.getId();
			item.qty_this = 0D;
			items.add(item);
		}
		orderAction.outbound(orderId, EasyUtils.obj2Json(items));
	}
	
	@Test
	public void testOrder() {
		OrderH order = createOrder("SO0011", OW1, W1, 1);
		orderAction.deleteOrder(order.getId());
		
		OrderH order_1 = createOrder("SO0012", OW1, W1, 1);
		orderAction.cancelOrder(order_1.getId(), "出库取消");
		orderAction.closeOrder(order_1.getId());

		order = createOrder("SO013", OW1, W1, 1);
		List<OrderItem> orderItems = orderDao.getOrderItems( order.getId() );
		OrderItem item = orderItems.get(0);
		item.setInv_id( _ONE_INV.getId() );
		
		Map<Object, Object> result = orderAction.queryOrderItem(order.getOrderno());
		orderItems = (List<OrderItem>) result.get("items");
		item = orderItems.get(0);
		Assert.assertEquals(_ONE_INV.getLocation().getCode(), item.out_loc);
		
		OperationH op = orderService.assign(order.getId(), worker1.getLoginName(), null, null);
		opService.acceptConfirm(op.getId(), WMS.OP_STATUS_06, "拒绝工单");
		orderService.assign(order.getId(), worker1.getLoginName(), null, null);
	}
}
