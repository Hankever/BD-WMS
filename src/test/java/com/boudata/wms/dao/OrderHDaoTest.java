/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boudata.wms.WMS;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.BoxItem;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.outbound.AbstractTest4PK;

public class OrderHDaoTest extends AbstractTest4PK {

	@Autowired protected OrderHDao orderHDao;
	
	@Test
	public void test() {
		String orderno = "SO01", waveno = "SW001", opNo = "SO001-P0116001", boxno = "Box001";
		
		// 1、创建Order单
		OrderH order = createOrder(orderno, OW1, W1, 3);
		
		// 2.1、查询【-001】的出库单
		OrderH order_1 = orderHDao.getOrder("-001");
		Assert.assertNull(order_1);
		
		// 2.2、查询【SO01】的出库单
		order_1 = orderHDao.getOrder(orderno);
		Assert.assertEquals(orderno, order_1.getOrderno());
		
		// 2.3、按ID号查询出库单
		List<OrderH> orders = orderHDao.getOrders( Arrays.asList(order.getId()) );
		Assert.assertTrue(orders.size() == 1);
		Assert.assertEquals(orderno, orders.get(0).getOrderno());
		
		// 2.5、按波次查询出库单 + 查询波次信息
		OrderWave wave = new OrderWave();
		wave.setWarehouse(W1);
		wave.setCode(waveno);
		wave.setTotal(1);
		wave.setStatus(WMS.W_STATUS_01);
		wave.setOrigin(WMS.W_ORIGIN_01);
		orderHDao.createObject(wave);
		
		order.setWave(wave);
		orderHDao.update(order);
		
		orders = orderHDao.getOrderByWave(wave.getId());
		Assert.assertTrue(orders.size() == 1);
		Assert.assertEquals(orderno, orders.get(0).getOrderno());
		
		OrderWave wave_1 = orderHDao.getWave(wave.getId());
		Assert.assertEquals(wave.getCode(), wave_1.getCode());
		
		// 3.1、按照order.id查询OrderHItem
		List<OrderItem> orderItems = orderHDao.getOrderItems(order.getId());
		Assert.assertTrue(order.getSkus() == orderItems.size());
		
		Double qty_total = 0D;
		for(OrderItem orderItem : orderItems) {
			qty_total += orderItem.getQty();
		}
		Assert.assertEquals(order.getQty(), qty_total);
		
		// 3.2、按照item.id查询OrderHItem
		OrderItem orderItem = orderHDao.getOrderItem( orderItems.get(0).getId() );
		Assert.assertEquals(orderItem.getSku(), orderItems.get(0).getSku());
		
		// 3.3、按照item.ids查询orderHItem
		List<OrderItem> orderItems_1 = orderHDao.getOrderItemsByIds( Arrays.asList(orderItems.get(0).getId()) );
		Assert.assertTrue(orderItems_1.size() == 1);
		Assert.assertEquals(orderItem.getSku(), orderItems_1.get(0).getSku());
		
		// 3.4、hql查询OrderHItem
		orderItems_1 = orderHDao.getOrderItems("from OrderItem where id = ?", orderItems.get(0).getId());
		Assert.assertTrue(orderItems_1.size() == 1);
		Assert.assertEquals(orderItem.getSku(), orderItems_1.get(0).getSku());
		
		// 4、按照order.id查询OperationItem
		OperationH op = new OperationH();
		op.setWarehouse(W1);
		op.setOpno(opNo);
		op.setOptype(WMS.opType( WMS.OP_TYPE_OUT ));
		op.setStatus(WMS.OP_STATUS_01);
		op.setWave(wave);
		orderHDao.createObject(op);
		
		OperationItem opi = new OperationItem();
		opi.setOperation(op);
		opi.setOrderitem(orderItem);
		opi.setOwner(OW1);
		opi.setSkucode(orderItem.getSku().getCode());
		opi.setLoccode(CC_LOC_1.getCode());
		opi.setQty(orderItem.getQty());
		orderHDao.createObject(opi);
		
		List<OperationItem> opis = orderHDao.getOrderPKDs( Arrays.asList(order.getId()) );
		Assert.assertTrue(opis.size() == 1);
		Assert.assertEquals(orderItem, opis.get(0).getOrderitem());
		
		// 5.1、按照order.id查询Box
		Box box = new Box();
		box.setBoxno(boxno);
		box.setOrder(order);
		box.setType("拼箱");
		box.setQty(orderItem.getQty());
		box.setSkus(1);
		orderHDao.createObject(box);
		
		BoxItem boxItem = new BoxItem();
		boxItem.setBox(box);
		boxItem.setOrderitem(orderItem);
		boxItem.setSku(orderItem.getSku());
		boxItem.setQty(orderItem.getQty());
		orderHDao.createObject(boxItem);
		
		List<Box> boxs = orderHDao.getBoxsByOrder(order.getId());
		Assert.assertTrue(boxs.size() == 1);
		Assert.assertEquals(boxno, boxs.get(0).getBoxno());
		
		// 5.2、按照box.id查询Box
		boxs = orderHDao.getBoxs( box.getId().toString() );
		Assert.assertTrue(boxs.size() == 1);
		Assert.assertEquals(boxno, boxs.get(0).getBoxno());
		
		// 5.3、按照box.id查询BoxItem
		List<BoxItem> boxItems = orderHDao.getBoxItems( box.getId() );
		Assert.assertTrue(boxItems.size() == 1);
		Assert.assertEquals(box, boxItems.get(0).getBox());
	}
	
}
