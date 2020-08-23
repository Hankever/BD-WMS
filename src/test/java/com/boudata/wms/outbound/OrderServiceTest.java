/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.InvSN;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;

import junit.framework.Assert;

public class OrderServiceTest extends AbstractTest4PK {
	
	@Autowired OrderHDao orderDao;
	
	@Test
	public void test1() {
		OrderH order = createOrder("SO01", OW1, W1, 3, 1D);
		Long orderId = order.getId();
		OperationH pkh = orderService.allocate(orderId);
		Assert.assertNotNull( pkh.getId() );
		
		List<OperationItem> pkds = operationDao.getItems(pkh.getId());
		orderService.pickup(pkh, Arrays.asList(pkds.get(0))); // 部分拣货
		
		order = orderService.cancelAllocate(orderId, false); // 对部分拣货的单子取消分配
		Assert.assertNull(order.getWave());
	}
	
	@Test
	public void test2() {
		OrderH order = createOrder("SO02", OW1, W1, 3, 1D);
		Long orderId = order.getId();
		OperationH pkh = orderService.allocate(orderId);
		Assert.assertNotNull( pkh.getId() );
		
		List<OperationItem> pkds = operationDao.getItems(pkh.getId());
		OperationItem lastPkd = pkds.get(2);
		lastPkd.setToloccode( lastPkd.getLoccode() );  // 模拟发货区拣货（拣货和目的库位相同）
		
		orderService.pickup(pkh, pkds); // 完全拣货
		
		Assert.assertEquals(order.getStatus(), WMS.O_STATUS_42); // 完全拣货
		orderService.cancelPickup(pkh.getId(), pkds.get(0).getId().toString());
		Assert.assertEquals(order.getStatus(), WMS.O_STATUS_41); // 部分拣货
	}
	
	@Test
	public void test3() {
		OrderH order = createOrder("SO02", OW1, W1, 3, 1D);
		Long orderId = order.getId();
		OperationH pkh = orderService.allocate(orderId);
		Assert.assertNotNull( pkh.getId() );
		
		List<OperationItem> pkds = operationDao.getItems(pkh.getId());
		orderService.pickup(pkh, pkds); // 完全拣货
		
		request.getSession().setAttribute("auto_create_box", "0"); // 不自动封箱
		List<?> result = commonDao.getEntities("select id from InvSN where asn = ?", "A_001");
		Assert.assertEquals(3, result.size());
		orderService.check(orderId, pkds, EasyUtils.list2Str(result));
		for(Object id : result) {
			InvSN sn = (InvSN) commonDao.getEntity(InvSN.class, (Serializable) id);
			Assert.assertEquals(order.getOrderno(), sn.getSorder());
		}
		
		List<?> boxs = commonDao.getEntities("from Box where order.id = ?", orderId);
		Assert.assertTrue( boxs.isEmpty() );
	}
	
	@Test
	public void test4() {
		OrderH order = createOrder("SO02", OW1, W1, 1, 20D);
		Long orderId = order.getId();
		OperationH pkh = orderService.allocate(orderId);
		Assert.assertNotNull( pkh.getId() );
		
		List<OperationItem> pkds = operationDao.getItems(pkh.getId());
		orderService.pickup(pkh, pkds); // 完全拣货
		
		List<_DTO> list = new ArrayList<>();
		for(OperationItem opi : pkds) {
			_DTO dto = new _DTO();
			dto.id = opi.getOrderitem().getId();
			dto.qty_this = 10D;
//			dto.inv_id = opi.getOpinv().getId();
//			dto.opi_id = opi.getId();  // 验货只针对 订单明细，不知道拣货明细
			list.add(dto);
		}
		
		Box box = new Box();
		box.setBoxno(order.getOrderno() + "-001");
		box.setQty( 10D );
		OperationH op = orderService.checkAndBox(orderId, list , box , null, 10);  // 部分验货
		Assert.assertEquals(WMS.O_STATUS_09, order.getStatus());
		Assert.assertEquals(WMS.OP_STATUS_03, op.getStatus());
		Assert.assertEquals(Box.TYPE_1, box.getType());
		Long box1Id = box.getId();
		
		box = new Box();
		box.setBoxno(order.getOrderno() + "-002");
		box.setQty( 10D );
		op = orderService.checkAndBox(orderId, list , box , null, 10); // 完全验货
		Assert.assertEquals(WMS.O_STATUS_05, order.getStatus());
		Assert.assertEquals(WMS.OP_STATUS_04, op.getStatus());
		Long box2Id = box.getId();
		
		orderService.cancelCheckAndBox(orderId, box2Id);
		orderService.cancelCheckAndBox(orderId, box1Id);
	}
	
	@Test
	public void test5() {
		OrderH order = createOrder("SO02", OW1, W1, 1, 20D);
		Long orderId = order.getId();
		OperationH pkh = orderService.allocate(orderId);
		Assert.assertNotNull( pkh.getId() );
		
		List<OperationItem> pkds = operationDao.getItems(pkh.getId());
		orderService.pickup(pkh, pkds); // 完全拣货
		
		List<_DTO> list = new ArrayList<>();
		for(OperationItem opi : pkds) {
			_DTO dto = new _DTO();
			dto.id = opi.getOrderitem().getId();
			dto.qty_this = opi.getQty();
			list.add(dto);
		}
		
		Box box = new Box();
		box.setBoxno(order.getOrderno() + "-001");
		box.setQty( order.getQty() );
		
		// 验货完成直接出库
		request.getSession().setAttribute("auto_check_outbound", "1");
		orderService.checkAndBox(orderId, list , box , null, 1);
		Assert.assertEquals(WMS.O_STATUS_06, order.getStatus());
	}
	
	@Test
	public void test52() {
		OrderH order = createOrder("SO02", OW1, W1, 1, 20D);
		Long orderId = order.getId();
		OperationH pkh = orderService.allocate(orderId);
		Assert.assertNotNull( pkh.getId() );
		
		List<OperationItem> pkds = operationDao.getItems(pkh.getId());
		orderService.pickup(pkh, pkds); // 完全拣货
		
		List<_DTO> list = new ArrayList<>();
		for(OperationItem opi : pkds) {
			_DTO dto = new _DTO();
			dto.id = opi.getOrderitem().getId();
			dto.qty_this = opi.getQty();
			list.add(dto);
		}
		
		Box box = new Box();
		box.setBoxno(order.getOrderno() + "-001");
		box.setQty( order.getQty() );
				
		// 外部取消订单		
		order.setStatus( WMS.O_STATUS_02 );
		try {
			orderService.checkAndBox(orderId, list , box , null, 1);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("订单已被外部取消，中止出库", e.getMessage());
		}
	}
	
	@Test
	public void test6() {
		OrderH order1 = createOrder("SO03", OW1, W1, 1, 20D);
		OperationH op = orderService.assign(order1.getId(), cg.getLoginName(), null, null);
		
		List<OrderItem> items = orderDao.getOrderItems(order1.getId());
		OrderItem orderItem = items.get(0);
		
		List<_DTO> list = new ArrayList<>();
		list.add( new _DTO() ); // 空
		_DTO dto = new _DTO();
		dto.id = orderItem.getId();
		dto.qty_this = 1D;
		list.add( dto );
		
		try {
			orderService.outbound4rf(order1.getId(), op.getId(), list );
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_19, 2, orderItem.getSku().getCode()) , e.getMessage());
		}

		_ONE_INV.setQty(10D);
		commonDao.update(_ONE_INV);
		List<?> result = commonDao.getEntities("select id from InvSN where asn = ?", "A_001");
		dto.snlist = result.get(0).toString();
		dto.inv_id = _ONE_INV.getId();
		orderService.outbound4rf(order1.getId(), op.getId(), list );
		
		Assert.assertEquals(WMS.O_STATUS_08, order1.getStatus());
		Assert.assertEquals(WMS.OP_STATUS_03, op.getStatus());
		
		try {
			orderService.cancelOrder(order1.getId(), "test");
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_9, order1.getOrderno()), e.getMessage() );
		}
	}
	
	@Test
	public void test7() {
		OrderH order1 = createOrder("SO04", OW1, W1, 1, 20D);
		order1.setStatus( WMS.O_STATUS_06 );
		orderDao.update(order1);
		
		Long orderId = order1.getId();
		
		try {
			orderService.cancelOrder(orderId, "test");
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_10, order1.getOrderno(), order1.getStatus()), e.getMessage());
		}
		
		createNewInv(_ONE_INV, 100d, CC_LOC_1, null, null);
		orderService.allocate(orderId);
		orderService.cancelOrder(orderId, "test");
		
		OperationH out_op = orderService.assign(orderId, cg.getLoginName(), null, null);
		orderService.cancelOrder(orderId, "test");
		Assert.assertEquals(WMS.OP_STATUS_02, out_op.getStatus());
	}
	
    /** 准备好库存数据。*/
    protected void prepareTestData() {
        super.prepareTestData();
        
        Date today = DateUtil.today();

        createNewInv(_ONE_INV, 100d, CC_LOC_1, "黑色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 200d, CC_LOC_2, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 300d, CC_LOC_3, "白色", DateUtil.subDays(today, 15));
        
        Asn asn = createAsn("A_001", OW1, W1, 3);
		List<?> items = commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		
		List<AsnItem> list = new ArrayList<>();
		for(Object obj : items ) {
			AsnItem ai = (AsnItem) obj;
			ai.setQty_this( ai.getQty() );
			ai.setLoccode( locations.get(list.size()).getCode() );
			ai.snlist.add("sn199");
			list.add(ai);
		}
		asnService.inbound( asn.getId(), list );
        
        printTable(Inventory.class.getName());
    }
    
}
