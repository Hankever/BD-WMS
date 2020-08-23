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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.util.BeanUtil;
import com.boudata.wms.WMS;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;

public class OrderErrTest extends AbstractTest4PK {

	@Autowired OrderService orderService;
	@Autowired protected OrderHDao orderHDao;
	@Autowired protected OrderAction orderAction;
	
	@Test
	public void test1() {
		String orderNo = "Order001";
		OrderH order = new OrderH();
		order.setOrderno(orderNo);
		order.setOwner(OW2);
		order.setWarehouse(W1);
		order.setOrderday( new Date() );
		order.setType("普通出库");
		
		skuOne.setOwner(OW1);
		OrderItem item = new OrderItem();
		item.setSku( skuOne );
		item.setQty( 100D );
		
		ArrayList<OrderItem> items = new ArrayList<OrderItem>();
		items.add(item);
		try {
			orderService.createOrder(order, items);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_11, "OW-2", "SKU-0", "OW-1"), e.getMessage() );
		}
	}
	
	@Test
	public void test2() {
		String orderNo = "Order001", status = "无";
		OrderH order = new OrderH();
		order.setOrderno(orderNo);
		order.setOwner(OW2);
		order.setWarehouse(W1);
		order.setOrderday( new Date() );
		order.setType("普通出库");
		order.setStatus(status);
		orderHDao.create(order);
		
		Long orderId = order.getId();
		try {
			orderService.deleteOrder(orderId);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_3, orderNo, status), e.getMessage() );
		}
		
		try {
			orderService.assign(orderId, "13735547815", WMS.OP_TYPE_OUT, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_13, orderNo, status), e.getMessage() );
		}
		
		try {
			orderService.cancelAllocate(orderId, false);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_14, status), e.getMessage() );
		}
		
		try {
			orderService.cancelCheckAndBox(orderId, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.O_ERR_15, e.getMessage() );
		}
		
		try {
			orderService.outbound4rf(orderId, null, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_7, orderNo, status), e.getMessage() );
		}
		
		try {
			orderService.outbound(orderId, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_7, orderNo, status), e.getMessage());
		}
		
		try {
			orderService.pickupOutbound(orderId, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_16, orderNo, status), e.getMessage() );
		}
		
		try {
			orderService.cancelOutbound(orderId);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_6, orderNo, status), e.getMessage() );
		}
		
		try {
			orderService.closeOrder(orderId);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_4, orderNo, status), e.getMessage() );
		}
	}
	
	@Test
	public void test3() {
		OrderH order = new OrderH();
		order.setOrderno("Order001");
		order.setOwner(OW1);
		order.setWarehouse(W1);
		order.setOrderday( new Date() );
		order.setType("普通出库");
		order.setStatus(WMS.O_STATUS_01);
		orderHDao.create(order);
		
		List<_DTO> list = new ArrayList<_DTO>();
		_DTO dto = new _DTO();
		dto.sku_id = skuOne.getId();
		dto.qty = -1D;
		dto.price = 10.0D;
		dto.money = 20.0D;
		dto.snlist = "";
		list.add(dto);
		try {
			orderService.createAndOutbound(order, list);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.O_ERR_17, e.getMessage() );
		}
		
		dto.qty = null;
		try {
			orderService.createAndOutbound(order, list);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.OP_ERR_1, e.getMessage() );
		}
		
		OrderH order2 = new OrderH();
		BeanUtil.copy(order2, order);
		order2.setId(null);
		order2.setOrderno("Order002");
		dto.qty = 1D;
		_DTO dto2 = new _DTO();
		dto2.sku_id = skuOne.getId();
		dto2.qty = 1D;
		list.add(dto2);
		orderService.createAndOutbound(order2, list);
		
		OrderH order3 = new OrderH();
		BeanUtil.copy(order3, order);
		order3.setId(null);
		order3.setOrderno("Order003");
		Inventory inv = createNewInv(_ONE_INV, 100d, CC_LOC_1, "黑色", null);
		dto.inv_id = inv.getId();
		orderService.createAndOutbound(order3, Arrays.asList(dto));
	}
	
	@Test
	public void testErr1() {
		String orderNo = "Order001";
		OrderH order = new OrderH();
		order.setOrderno(orderNo);
		order.setOwner(OW2);
		order.setWarehouse(W1);
		order.setOrderday( new Date() );
		order.setType("普通出库");
		order.setStatus(WMS.O_STATUS_01);
		order.setQty(1D);
		orderHDao.create(order);
		request.addParameter("orderno", orderNo);
		try {
			orderAction.createAndOutbound(request, null, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_2, orderNo, 1), e.getMessage() );
		}
		
		order.setStatus(WMS.O_STATUS_02);
		orderHDao.update(order);
		try {
			orderAction.createAndOutbound(request, null, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_2, orderNo, 2), e.getMessage() );
		}
		
		orderAction.queryPKOperation(order.getId());
	}
	
	@Test
	public void testErr2() {
		String code = "O_00";
		try {
			orderAction.queryOrderItem( code );
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_12, code), e.getMessage());
		}
	}
}
