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
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._SkuX;

public class InvActionTest extends AbstractTest4WMS {
	
	@Autowired protected InventoryAction action;
	
	@Test
	public void testQueryInv() {
		super.createNewInv(_ONE_INV, 10D, CC_LOC_1, "批次1", null);
		super.createNewInv(_ONE_INV, 20D, CC_LOC_2, "批次2", null);
		
		_Sku sku = _ONE_INV.getSku();
		
		InventorySo invSo = new InventorySo();
		invSo.setQtyfrom(1D);
		invSo.setQtyto(100D);
		invSo.setBarCode( sku.getBarcode() );
		invSo.setSkuCode( sku.getCode() );
		invSo.setLocationCode(CC_LOC_2.getCode());  // 库位精确查询
		invSo.setLoccode( "CC" ); // 库位模糊查询
		invSo.setZone( CC_LOC_2.getZone() );
		invSo.setWarehouseCode(W1.getCode());
		invSo.setWarehouseId(W1.getId());
		invSo.setOwner(OW1.getName());
		invSo.setOwnerId(OW1.getId());
		invSo.setCategory(null);
		invSo.setBrand(null);
		invSo.setSkuName(sku.getName());
		invSo.setSkuBrand(null);
		invSo.setExpiredate1(null);
		invSo.setExpiredate2(null);
		invSo.setCreatedate1(null);
		invSo.setCreatedate2(null);
		invSo.setDomain(sku.getDomain());
		invSo.setInvId(null);
		
		Map<String,Object> m = action.queryInventory(invSo, 1, 10);
		Assert.assertEquals(1, m.get("total"));
		
		invSo.setQtyLocked(0D); // 查询含冻结量的库存记录
		m = action.queryInventory(invSo, 1, 10);
		Assert.assertEquals(0, m.get("total"));
		
		// test queryInv (rf)
		request.addParameter("warehouseId", W1.getId().toString());
		request.addParameter("skuCode", sku.getCode());
		request.addParameter("loccode", "CC");
		request.addParameter("qtyfrom", "1");
		request.addParameter("with_skux", "false");
		m = action.queryInv(request);
		Assert.assertEquals(2, m.get("total"));
		
		login(ow1);
		m = action.queryInventory(invSo, 1, 10);
		Assert.assertEquals(0, m.get("total"));
	}
	
	@Test
	public void testMove() {
		
		request.addParameter("loccode", JX_LOC.getCode());
		request.addParameter("toloccode", JX_LOC.getCode());
		try {
			action.move(request, W1.getId());
			Assert.fail();
		} 
		catch(Exception e) {
			Assert.assertEquals( WMS.OP_ERR_3, e.getMessage() );
		}
		
		// test 整库移库
		request.removeParameter("loccode");
		request.addParameter("loccode", CC_LOC_1.getCode());
		request.addParameter("type", "整库移库");
		
		Map<String,Object> result = action.move(request, W1.getId());
		Assert.assertEquals("移库失败，起始库位【" + CC_LOC_1.getCode() + "】上没有库存记录", result.get("message"));
		
		super.createNewInv(_ONE_INV, 10D, CC_LOC_1, "批次1", null);
		super.createNewInv(_ONE_INV, 20D, CC_LOC_1, "批次2", null);
		result = action.move(request, W1.getId());
		Assert.assertEquals("移库成功", result.get("message"));
		
		// 指派审核人
		String approver = "13735547815";
		String opId = result.get("opId").toString();
		request.addParameter("approver", approver);
		request.removeParameter("type");
		request.addParameter("type", "move");
		request.addParameter("codeOrId", opId);
		action.approveOp(request);
		
		OperationH op = operationDao.getEntity(EasyUtils.obj2Long(opId));
		Assert.assertEquals(approver, op.getApprover());
		
		// test 普通移库
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		String hql = " from Inventory where location = ? and qty > 0";
		List<Inventory> invList = invDao.getInvs(hql, JX_LOC);
		for(Inventory inv : invList) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("qty", inv.getQty());
			map.put("skucode", inv.getSku().getCode());
			map.put("owner_id", inv.getOwner().getId());
			map.put("inv_id", inv.getId());
			
			list.add(map);
			break;
		}
		
		request.removeAllParameters();
		request.addParameter("loccode", JX_LOC.getCode());
		request.addParameter("toloccode", CC_LOC_2.getCode());
		request.addParameter("items", EasyUtils.obj2Json(list));
		result = action.move(request, W1.getId());
		Assert.assertEquals("移库成功", result.get("message"));
		
		InventorySo invSo = new InventorySo();
		invSo.setQtyfrom(1D);
		invSo.setQtyto(100D);
		invSo.setBarCode( _ONE_INV.getSku().getBarcode() );
		invSo.setLocationCode(CC_LOC_2.getCode());
		invSo.setWarehouseId(W1.getId());
		Map<String,Object> m = action.queryInventory(invSo , 1, 10);
		Assert.assertEquals(1, m.get("total"));
		
		printTable("Inventory");
	}
	
	@Test
	public void testMoveByContainer() {
		request.addParameter("type", WMS.OP_TYPE_RQSJ);
		request.addParameter("child_loc", CC_LOC_2.getCode());
		request.addParameter("toloccode", CC_LOC_2.getCode());
		
		try {
			action.containerMove(request, W1.getId());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( WMS.OP_ERR_11, e.getMessage());
		}
		
		Inventory inv1 = super.createNewInv(_ONE_INV, 10D, MV_LOC, "批次1", null);
		request.removeParameter("child_loc");
		request.addParameter("child_loc", MV_LOC.getCode());
		action.containerMove(request, W1.getId());
		
		printInvs();
		Assert.assertEquals(CC_LOC_2, MV_LOC.getParent());
		
		InventorySo so = new InventorySo();
		so.setWarehouseId(W1.getId());
		so.setLocationCode(CC_LOC_2.getCode());
		PageInfo pi = invDao.search(so);
		Assert.assertEquals(1, pi.getItems().size());
		Inventory inv2 = (Inventory) pi.getItems().get(0);
		Assert.assertEquals(inv1.getQty(), inv2.getQty());
		
		request.removeAllParameters();
		request.addParameter("type", WMS.OP_TYPE_RQXJ);
		request.addParameter("child_loc", MV_LOC.getCode());
		request.addParameter("toloccode", CC_LOC_2.getCode());
		action.containerMove(request, W1.getId());
		
		Assert.assertNull( MV_LOC.getParent() );
		Assert.assertEquals(0, invDao.search(so).getItems().size());
		
		request.removeAllParameters();
		request.addParameter("type", WMS.OP_TYPE_RQSJ);
		request.addParameter("child_loc", MV_LOC.getCode());
		request.addParameter("toloccode", CC_LOC_2.getCode());
		action.containerMove(request, W1.getId());
		inv2 = (Inventory) invDao.search(so).getItems().get(0);
		Assert.assertEquals(inv1.getQty(), inv2.getQty());
	}
	
	@Test
	public void testAdjustInv() { // 无单盘点（并调整库存） 
		List<JSONObject> list = new ArrayList<JSONObject>();
		
		// 修改数量
		JSONObject obj = new JSONObject();
		obj.put( "inv_id", _ONE_INV.getId() );
		obj.put("qty_actual", 100D);
		list.add( obj );
		
		// 从无到有
		obj = new JSONObject();
		obj.put("qty_actual", 111D);
		obj.put("loccode", JX_LOC.getCode());  // 移动端调整
		obj.put("skucode", skuOne.getCode());
		obj.put("owner_id", OW1.getId());
		list.add( obj );
		
		obj = new JSONObject();
		obj.put("qty_actual", 22D);
		obj.put("location_id", MV_LOC.getId());  // 网页上调整
		obj.put("skucode", skuOne.getCode());
		obj.put("owner_id", OW1.getId());
		list.add( obj );
		
		// 数量不变
		obj = new JSONObject();
		obj.put( "inv_id", _ONE_INV.getId() );
		obj.put("qty_actual", 0D);
		list.add( obj );
		
		request.addParameter("items", list.toString());
		request.addParameter("remark", "测试");
		action.adjustInv4rf(request, W1.getId());
		
		action.unlock(_ONE_INV.getId(), -9D);
		action.unlock(_ONE_INV.getId(), 7D);
		
		printTable("Inventory");
		assertInvQty(233D, 2D);
		
		
		list = new ArrayList<JSONObject>();
		obj = new JSONObject();
		obj.put("qty_actual", 111D);
		obj.put("loccode", JX_LOC.getCode()); 
		obj.put("skucode", skuOne.getCode());
		list.add( obj );
		request.removeParameter("items");
		request.addParameter("items", list.toString());
		try {
			action.adjustInv4rf(request, W1.getId());
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("未填写货主") > 0 );
		}
		
		// 只改批次属性（装箱量）
		action.unlock(_ONE_INV.getId(), _ONE_INV.getQty_locked());
		obj = new JSONObject();
		obj.put("inv_id", _ONE_INV.getId());
		obj.put("lotatt02", 20D);
		list = new ArrayList<JSONObject>();
		list.add( obj );
		Double oldQty = _ONE_INV.getQty();
		action.adjustInv(W1.getId(), list.toString(), "修改装箱量");
		Assert.assertEquals(0D, _ONE_INV.getQty());
		
		InventorySo so = new InventorySo();
		so.setSkuId(_ONE_INV.getSku().getId());
		so.setLotatt02("20");
		PageInfo pi = invDao.search(so);
		Assert.assertTrue(pi.getItems().size() > 0);
		Inventory nInv = (Inventory) pi.getItems().get(0);
		Assert.assertEquals(oldQty, nInv.getQty());
		
		try {
			obj = new JSONObject();
			obj.put("inv_id", _ONE_INV.getId());
			obj.put("qty_actual", 19D);
			list = new ArrayList<JSONObject>();
			list.add( obj );
			
			request.getSession().setAttribute("js_inv_check_approve", "1");
			action.adjustInv(W1.getId(), list.toString(), "test");
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( WMS.INV_ERR_1, e.getMessage());
		}
		
		list = new ArrayList<JSONObject>();
		action.adjustInv(W1.getId(), list.toString(), "test");
	}
	
	@Test
	public void testSkuX() {
		_Sku pSku = skuList.get(0);
		
		for( int i = 1; i <= 3; i++ ) {
			_SkuX skux = new _SkuX();
			skux.setId(null);
			skux.setParent( pSku );
			skux.setSku( skuList.get(i) );
			skux.setWeight(10);
			skux.setPrivate_loc(null);
			EasyUtils.obj2Json(skux);
			commonDao.create(skux);
		}
		
		Inventory inv = super.createNewInv(_ONE_INV, 2D, CC_LOC_3, null, null);
		Long bigInvId = inv.getId();
		
		InventorySo invSo = new InventorySo();
		invSo.setBarCode(skuList.get(1).getBarcode());
		invSo.setQtyfrom(0D);
		Map<String, Object> m = action.queryInventory(invSo, 1, 1);
		Assert.assertEquals(1, m.get("total"));
		
		request.addParameter("barcode", invSo.getBarCode());
		request.addParameter("rows", "1");
		action.queryInv(request);
		
		action.skuxSplit(bigInvId, 1.0);
		
		invSo = new InventorySo();
		invSo.setQtyfrom(0D);
		invSo.setQtyto(100D);
		invSo.setWarehouseId(W1.getId());
		invSo.setLocationCode(CC_LOC_3.getCode());
		
		printTable("Inventory");
		m = action.queryInventory(invSo, 1, 10);
//		System.out.println( m.get("rows") );
		
		Assert.assertEquals(4, m.get("total"));
		assertInvQty(31D, 0D);
		
		action.skuxSplit(bigInvId, 1.0);
	}
}
