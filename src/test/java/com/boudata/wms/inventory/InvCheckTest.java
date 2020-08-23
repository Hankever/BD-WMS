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
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.InvCheck;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;

public class InvCheckTest extends AbstractTest4WMS {
	
	@Autowired protected InventoryAction action;
	@Autowired AsnDao asnDao;
	@Autowired OrderHDao orderDao;
	
	@Test
	public void testInvCheck() {  // 盘点
		super.createNewInv(_ONE_INV, 10D, JX_LOC, "批次1", null);
		super.createNewInv(_ONE_INV, 20D, CC_LOC_2, "批次2", null);
		
		// 1.1、创建（初盘）盘点单 --------------------------------------------------------------------------------------------
		List<Inventory> invs = invDao.getInvs("from Inventory where qty > 0 and domain = ?", Environment.getDomain());
		List<JSONObject> items = new ArrayList<>();
		for(Inventory inv : invs) {
			JSONObject item = new JSONObject();
			item.put("inv_id", inv.getId());
			items.add(item);
		}
		items.add(new JSONObject());
		
		InvCheck wo = action.createInvCheck(W1.getId(), OW1.getId(), null, items.toString(), 0, 3, 1);
		
		// 1.2、认领（初盘）工单
		OperationH round1_op = wo.getRound1();
		action.takeJob( W1.getId(), round1_op.getOpno() );
		Assert.assertEquals(Environment.getUserCode(), round1_op.getWorker());
		
		action.getCheckLoc(W1.getId(), round1_op.getOpno());
		
		// 1.3、录入（初盘）结果
		items = new ArrayList<>();
		List<OperationItem> opiList = operationDao.getItems(round1_op.getId());
		for(OperationItem opi : opiList) {
			JSONObject item = new JSONObject();
			item.put("id", opi.getId());
			item.put("toqty", opi.getQty() - 1);
			items.add(item);
		}
		JSONObject newItem = new JSONObject();
		newItem.put("skucode", skuList.get(9).getCode());
		newItem.put("loccode", CC_LOC_2.getCode());
		newItem.put("lotatt03", "第三批");
		newItem.put("toqty", 3);
		items.add(newItem); // 盘盈的库存
		
		action.recordCheckResult(wo.getId(), 1, items.toString());
		
		// 2.1、创建（复盘）盘点单 ------------------------------------------------------------------------------------------
		opiList = operationDao.getItems(round1_op.getId());
		for(OperationItem opi : opiList) {
			JSONObject item = new JSONObject();
			item.put("inv_id", opi.getOpinv().getId());
			items.add(item);
		}
		action.createInvCheck(W1.getId(), OW1.getId(), wo.getCode(), items.toString(), 0, 3, 2);
		
		// 2.2、认领（复盘）工单
		OperationH round2_op = wo.getRound2();
		action.takeJob( W1.getId(), round2_op.getOpno() );
		Assert.assertEquals(Environment.getUserCode(), round2_op.getWorker());
		
		action.getCheckLoc(W1.getId(), round2_op.getOpno());
		
		// 2.3、录入（复盘）结果
		items = new ArrayList<>();
		opiList = operationDao.getItems(round2_op.getId());
		for(OperationItem opi : opiList) {
			JSONObject item = new JSONObject();
			item.put("id", opi.getId());
			item.put("toqty", opi.getQty() - 1);
			items.add(item);
		}
		items.add(newItem); // 盘盈的库存
		
		action.recordCheckResult(wo.getId(), 2, items.toString());
		
		// 3.1、创建（终盘）盘点单 ------------------------------------------------------------------------------------------
		opiList = operationDao.getItems(round2_op.getId());
		for(OperationItem opi : opiList) {
			JSONObject item = new JSONObject();
			item.put("inv_id", opi.getOpinv().getId());
			items.add(item);
		}
		
		action.createInvCheck(W1.getId(), OW1.getId(), wo.getCode(), items.toString(), 0, 3, 3);
		
		// 3.2、认领（终盘）工单
		OperationH round3_op = wo.getRound3();
		action.takeJob( W1.getId(), round3_op.getOpno() );
		Assert.assertEquals(Environment.getUserCode(), round3_op.getWorker());
		
		action.getCheckLoc(W1.getId(), round3_op.getOpno());
		
		// 3.3、录入（终盘）结果
		items = new ArrayList<>();
		opiList = operationDao.getItems(round3_op.getId());
		int index = 0;
		for(OperationItem opi : opiList) {
			JSONObject item = new JSONObject();
			item.put("id", opi.getId());
			if(index == 1) {
				item.put("toqty", opi.getQty() - index);
			} else {
				item.put("toqty", opi.getQty() + index);
			}
			items.add(item);
			
			index++;
		}
		items.add(newItem); // 盘盈的库存
		
		action.recordCheckResult(wo.getId(), 3, items.toString());
		action.getCheckLoc(W1.getId(), round3_op.getOpno());
		
		opiList = operationDao.getItems(round3_op.getId());
		List<Long> profitItems = new ArrayList<>(),
				   lossesItems = new ArrayList<>(),
				   changeItems = new ArrayList<>();
		index = 0;
	    for(OperationItem opi : opiList) {
	    	Long opi_id = opi.getId();
	    	if(index == 0) {
	    		changeItems.add(opi_id);
	    		profitItems.add(opi_id);
	    		lossesItems.add(opi_id);
	    	} else if(index == 1) {
	    		lossesItems.add(opi_id);
	    	} else if(index == 2) {
	    		profitItems.add(opi_id);
	    	} else {
	    		changeItems.add(opi_id);
	    	}
	    	index++;
	    }
		
		// 4、修改库存
		action.adjustInvByCheckResult(wo.getId(), round3_op.getId(), EasyUtils.list2Str(changeItems), "");
		Map<String, Object> asn_map = action.adjustInvByCheckResult(wo.getId(), round3_op.getId(), EasyUtils.list2Str(profitItems), "盘盈入库"); // 盘盈
		Map<String, Object> order_map = action.adjustInvByCheckResult(wo.getId(), round3_op.getId(), EasyUtils.list2Str(lossesItems), "盘亏出库"); // 盘亏

		action.adjustInvByCheckResult(wo.getId(), round3_op.getId(), "", "其他");	
		
		Asn asn = (Asn) asn_map.get("asn");
		Assert.assertEquals(2D, asn.getQty());
		
		List<AsnItem> aItems = asnDao.getItems(asn.getId());
		Assert.assertEquals( 1, aItems.size() );
		
		OrderH order = (OrderH) order_map.get("order");
		Assert.assertEquals(1D, order.getQty());
		
		List<OrderItem> oItems = orderDao.getOrderItems(order.getId());
		Assert.assertEquals( 1, oItems.size() );
		
		printTable("Inventory");
		super.assertInvQty(36D, 0D);
		
		action.queryOpLog( round3_op.getId() );
		action.queryInvLog( round3_op.getId() );
		
		action.closeInvCheck(wo.getId());
	}
	
	@Test
	public void testInvCheck2() {  // 盘点
		super.createNewInv(_ONE_INV, 10D, JX_LOC, "批次1", null);
		super.createNewInv(_ONE_INV, 20D, CC_LOC_2, "批次2", null);
		
		// 1.1、创建（初盘）盘点单 --------------------------------------------------------------------------------------------
		List<Inventory> invs = invDao.getInvs("from Inventory where qty > 0 and domain = ?", Environment.getDomain());
		List<JSONObject> items = new ArrayList<>();
		for(Inventory inv : invs) {
			JSONObject item = new JSONObject();
			item.put("inv_id", inv.getId());
			items.add(item);
		}
		items.add(new JSONObject());
		
		InvCheck wo = action.createInvCheck(W1.getId(), OW1.getId(), null, items.toString(), 0, 3, 1);
		
		// 1.2、认领（初盘）工单
		OperationH round1_op = wo.getRound1();
		action.takeJob( W1.getId(), round1_op.getOpno() );
		Assert.assertEquals(Environment.getUserCode(), round1_op.getWorker());
		
		action.getCheckLoc(W1.getId(), round1_op.getOpno());
		
		// 1.3、录入（初盘）结果
		items = new ArrayList<>();
		List<OperationItem> opiList = operationDao.getItems(round1_op.getId());
		for(OperationItem opi : opiList) {
			JSONObject item = new JSONObject();
			item.put("id", opi.getId());
			item.put("toqty", opi.getQty() - 1);
			items.add(item);
		}
		JSONObject newItem = new JSONObject();
		newItem.put("skucode", skuList.get(9).getCode());
		newItem.put("loccode", CC_LOC_2.getCode());
		newItem.put("lotatt03", "第三批");
		newItem.put("toqty", 3);
		items.add(newItem); // 盘盈的库存
		
		action.recordCheckResult(wo.getId(), 1, items.toString());
		
		// 2.1、创建（复盘）盘点单 ------------------------------------------------------------------------------------------
		opiList = operationDao.getItems(round1_op.getId());
		for(OperationItem opi : opiList) {
			JSONObject item = new JSONObject();
			item.put("inv_id", opi.getOpinv().getId());
			items.add(item);
		}
		action.createInvCheck(W1.getId(), OW1.getId(), wo.getCode(), items.toString(), 0, 3, 2);
		
		// 2.2、认领（复盘）工单
		OperationH round2_op = wo.getRound2();
		action.takeJob( W1.getId(), round2_op.getOpno() );
		Assert.assertEquals(Environment.getUserCode(), round2_op.getWorker());
		
		action.cancelInvCheck(wo.getId());
		action.closeInvCheck(wo.getId());
	}
}
