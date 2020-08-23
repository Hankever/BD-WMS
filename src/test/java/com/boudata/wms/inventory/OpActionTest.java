/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.inbound.AsnAction;
import com.boudata.wms.inbound.AsnService;
import com.boudata.wms.outbound.AbstractTest4PK;
import com.boudata.wms.outbound.OrderAction;
import com.boudata.wms.outbound.OrderService;

@SuppressWarnings("unchecked")
public class OpActionTest extends AbstractTest4PK {
	
	@Autowired InventoryAction action;
	@Autowired AsnService asnService;
	@Autowired OrderService orderService; 
	@Autowired AsnAction asnAction;
	@Autowired OrderAction orderAction;
	
	@Test
	public void testASNOp() { // 入库按单作业
		// test asn op
		Asn asn = super.createAsn("A001", OW1, W1);
		
		Map<String, Object> m1 = action.takeIOJob(W1.getId(), asn.getAsnno(), "asn");
		OperationH op = (OperationH) m1.get("op");
		
		m1 = action.takeJob(W1.getId(), op.getOpno());
		Assert.assertEquals("-1", m1.get("code"));
		
		op.setStatus( WMS.O_STATUS_01 );
		m1 = action.takeJob(W1.getId(), op.getOpno());
		Assert.assertEquals("200", m1.get("code"));
		
		try {
			op.setStatus(WMS.OP_STATUS_03);
			action.closeOp( op.getId() );
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals(EX.parse(WMS.OP_ERR_5, op.getOpno(), WMS.OP_STATUS_03), e.getMessage());
		}
		op.setStatus( WMS.O_STATUS_01 );
		action.closeOp( op.getId() );
		
		Map<String, Object> m2 = action.takeIOJob(W1.getId(), asn.getAsnno(), "asn");
		op = (OperationH) m2.get("op");
		Assert.assertEquals("200", m2.get("code"));
		
		request.addParameter("content", "111111");
		request.addParameter("remark", "222222");
		action.opCheckException(request, null, asn.getId(), null);
		
		// test auto fix lotatts
		SQLExcutor.excuteInsert("insert into dm_record_field (code,label,defaultValue,checkReg,tbl,domain) values (?,?,?,?,?,?)", 
				new Object[] { "createdate", "入库日期", "today-0", null, null, domain }, "connectionpool");
		SQLExcutor.excuteInsert("insert into dm_record_field (code,label,defaultValue,checkReg,tbl,domain) values (?,?,?,?,?,?)", 
				new Object[] { "lotatt02", "装箱量", null, "[1-9]{2}", null, domain }, "connectionpool");
		SQLExcutor.excuteInsert("insert into dm_record_field (code,label,defaultValue,checkReg,tbl,domain, nullable) values (?,?,?,?,?,?,?)", 
				new Object[] { "lotatt03", "批号", "111", null, null, domain, "false" }, "connectionpool");
		SQLExcutor.excuteInsert("insert into dm_record_field (code,label,defaultValue,checkReg,tbl,domain, nullable) values (?,?,?,?,?,?,?)", 
				new Object[] { "lotatt04", "批号2", null, null, null, domain, "true" }, "connectionpool");
		
		List<AsnItem> items = (List<AsnItem>) commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		
		// 批次属性格式校验
		for(AsnItem ai : items ) {
			ai.setQty_this( 1D );
			ai.setLotatt02("2");
		}
		try {
			asnService.inbound(asn.getId(), items);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.INV_ERR_2, "装箱量", 2), e.getMessage() );
		}
		
		// 批次属性要求非空
		for(AsnItem ai : items ) {
			ai.setQty_this( ai.getQty() - 1 );
			ai.setLotatt02("24");
		}
		try {
			asnService.inbound(asn.getId(), items);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.INV_ERR_3, "批号"), e.getMessage() );
		}
		
		for(AsnItem ai : items ) {
			ai.setQty_actual(0D);
			ai.setLotatt03("AAA");
		}
		asnService.inbound(asn.getId(), items);
		
		printTable("Inventory");
		SQLExcutor.excute("delete from dm_record_field", "connectionpool");
	}
	
	@Test
	public void testOrderOp() { // 出库按单作业
			
		// test order op
		OrderH order = super.createOrder("O001", OW1, W1, 1);
		Long orderId = order.getId();
		
		Map<String, Object> m1 = action.takeIOJob(W1.getId(), order.getOrderno(), "order");
		OperationH op = (OperationH) m1.get("op");
		
		m1 = action.takeJob(W1.getId(), op.getOpno());
		Assert.assertEquals("-1", m1.get("code"));
		
		op.setStatus( WMS.O_STATUS_01 );
		m1 = action.takeJob(W1.getId(), op.getOpno());
		Assert.assertEquals("200", m1.get("code"));
		
		Map<String, Object> m3 = action.takeIOJob(W1.getId(), order.getOrderno(), "order");
		op = (OperationH) m3.get("op");
		Assert.assertEquals("200", m3.get("code"));
		
		action.opAcceptConfirm(request, op.getId());
		request.addParameter("status", WMS.OP_STATUS_06);
		action.opAcceptConfirm(request, op.getId());

		request.addParameter("content", "111111");
		request.addParameter("remark", "222222");
		action.opCheckException(request, null, null, order.getId());
		action.opCheckException(request, op.getId(), null, order.getId());
		action.opCheckException(request, null, null, null);
		
		OperationH pkh0 = orderService.allocate(orderId);
		Assert.assertTrue( pkh0.getErrorMsg().indexOf("分配失败") > 0 );
		
		Inventory inv = super.createNewInv(_ONE_INV, 100D, CC_LOC_3, null, null);
		
		OperationH pkh = orderService.allocate(orderId);
		Assert.assertEquals(1, pkh.items.size());
		OperationItem pkd = pkh.items.get(0);
		Long pkdId = pkd.getId();
		
		action.takeJob(W1.getId(), pkh.getOpno());
		
		request.addParameter("type", "库存异常");
		action.opException(request, "");
		OpException ope = action.opException(request, pkdId.toString());
		action.closeException(ope.getId(), pkdId, request);
		action.closeException(ope.getId(), null, request);
		
		action.changeOPIInv(pkdId, inv.getId());
		
		pkd.getOperation().setStatus(WMS.OP_STATUS_04);
		try {
			action.changeOPIInv(pkdId, inv.getId());
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.OP_ERR_6, e.getMessage() );
		}
	}
	
	
	@Test
	public void test1() { // 无单入库 指派审核人
		JSONObject obj = new JSONObject();
		obj.put("sku_id", skuOne.getId());
		obj.put("qty", "2");
		obj.put("price", "10.0");
		obj.put("money", "20.0");
		obj.put("snlist", "");
		String items = "[" + obj.toString() + "]";
		
		request.addParameter("items", items);
		request.addParameter("toloccode", IN_LOC.getCode());
		Map<String,Object> map = asnAction.createAndInbound(request, W1.getId(), OW1.getId());
		
		// 指派审核人
		String approver = "13735547815";
		String asnno = map.get("asnno").toString();
		request.addParameter("approver", approver);
		request.addParameter("type", "asn");
		request.addParameter("codeOrId", asnno);
		action.approveOp(request);
		
		OperationH op = operationDao.getOperation(asnno, WMS.OP_IN);
		Assert.assertEquals(approver, op.getApprover());
	}
	
	@Test
	public void test2() { // 无单出库 指派审核人
		JSONObject obj = new JSONObject();
		obj.put("sku_id", skuOne.getId());
		obj.put("qty", "2");
		obj.put("price", "10.0");
		obj.put("money", "20.0");
		obj.put("snlist", "");
		String items = "[" + obj.toString() + "]";
		
		request.addParameter("items", items);
		request.addParameter("toloccode", IN_LOC.getCode());
		Map<String,Object> map = orderAction.createAndOutbound(request, W1.getId(), OW1.getId());
		
		// 指派审核人
		String approver = "13735547815";
		String orderno = map.get("orderno").toString();
		request.addParameter("approver", approver);
		request.addParameter("type", "order");
		request.addParameter("codeOrId", orderno);
		action.approveOp(request);
		
		OperationH op = operationDao.getOperation(orderno, WMS.OP_OUT);
		Assert.assertEquals(approver, op.getApprover());
	}

}
