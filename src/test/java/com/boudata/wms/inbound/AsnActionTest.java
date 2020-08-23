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
import java.util.Map;

import junit.framework.Assert;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;

public class AsnActionTest extends AbstractTest4WMS {
	
	@Autowired AsnAction asnAction;
	@Autowired AsnDao asnDao;
	
	@Test
	public void testCreateAndInbound() {
		JSONObject obj = new JSONObject();
		obj.put("sku_id", skuOne.getId());
		obj.put("qty", "2");
		obj.put("price", "10.0");
		obj.put("money", "20.0");
		obj.put("snlist", "");
		String items = "[" + obj.toString() + "]";
		
		request.addParameter("items", items);
		request.addParameter("toloccode", IN_LOC.getCode());
		asnAction.createAndInbound(request, W1.getId(), OW1.getId());
		
		String asnNo = "Asn001";
		request.addParameter("asnno", asnNo);
		asnAction.createAndInbound(request, W1.getId(), OW1.getId());
		Asn asn = asnDao.getAsn(asnNo);
		Assert.assertEquals(WMS.NO_ASN, asn.getTag());
		
		asn.setTag(null);
		try {
			asnAction.createAndInbound(request, W1.getId(), OW1.getId());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_2, asnNo, 2), e.getMessage() );
		}
		asn.setTag(WMS.NO_ASN);
		
		obj.put("snlist", "1,2,3,4,5");
		items = "[" + obj.toString() + "]";
		request.removeParameter("items");
		request.addParameter("items", items);
		request.addParameter("asnno", asnNo);
		asnAction.createAndInbound(request, W1.getId(), OW1.getId());
		
		List<?> result = commonDao.getEntities("select count(*) from InvSN where barcode = ?", skuOne.getBarcode());
		Assert.assertEquals(5L, result.get(0));
		
		try {
			asnAction.createAndInbound(request, W1.getId(), OW1.getId());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_9, "1,2,3,4,5"), e.getMessage() );
		}
		
		try {
			login(worker1);
			request.getSession().setAttribute("no_asn_inbound", "0");
			asnAction.createAndInbound(request, W1.getId(), OW1.getId());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( WMS.ASN_ERR_1, e.getMessage());
		}
	}
	
	@Test
	public void testCreateAndInbound2() {
		// 先建一个空的Asn单，再对此单进行无单入库（YC模式）
		String asnNo = "Asn002";
		Asn asn = new Asn();
		asn.setAsnno(asnNo);
		asn.setOwner(OW1);
		asn.setWarehouse(W1);
		asn.setAsnday( new Date() );
		asn.setSupplier("YC");
		asn.setType("普通入库");
		asnService.createAsn(asn, new ArrayList<AsnItem>());
		
		JSONObject obj = new JSONObject();
		obj.put("sku_id", skuOne.getId());
		obj.put("qty", "2");
		obj.put("price", "10.0");
		obj.put("money", "20.0");
		obj.put("snlist", "");
		
		JSONObject obj2 = new JSONObject();
		obj2.put("sku_id", skuOne.getId());
		obj2.put("qty", "");
		
		List<JSONObject> objs = new ArrayList<>();
		objs.add(obj);
		objs.add(obj2);
		
		request.addParameter("items", objs.toString());
		request.addParameter("toloccode", IN_LOC.getCode());
		request.addParameter("id", asn.getId().toString());
		request.addParameter("asnno", asnNo + "---err");
		asnAction.createAndInbound(request, W1.getId(), OW1.getId());
		asn = asnDao.getAsn(asnNo);
		Assert.assertEquals(WMS.NO_ASN, asn.getTag());
		
		try {
			asnService.deleteAsn(asn.getId());
			Assert.fail();
		} catch (Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_3, asnNo, asn.getStatus()), e.getMessage() );
		}
	}
	
	@Test
	public void testInboundInAsnHtml() {
		Asn asn = createAsn("ASN_001", OW1, W1);
		Long asnId = asn.getId();
		
		List<JSONObject> list = new ArrayList<JSONObject>();
		for( AsnItem item : asn.items ) {
			JSONObject obj = new JSONObject();
			obj.put("id", item.getId());
			obj.put("qty_this", "2");
			obj.put("toloccode", IN_LOC.getCode());
			obj.put("invstatus", "良品");
			obj.put("snlist", "1,2");
			
			list.add( obj );
		}
		
		String items = list.toString();
		request.addParameter("items", items);
		
		try {
			request.addParameter("items", items);
			request.addParameter("asnno", asn.getAsnno());
			asnAction.createAndInbound(request, W1.getId(), OW1.getId());
			Assert.fail();
		} 
		catch(Exception e) { 
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_2, asn.getAsnno(), 1), e.getMessage());
		}
		
		asnAction.inboundInAsnHtml(asnId, items);
		asnAction.cancelReceiveAsn(asnId);
		
		request.getSession().setAttribute("js_checkin_with_lot", "0");
		asnAction.inboundInAsnHtml(asnId, items);
	}
	
	@Test
	public void testAsnCKInbound() {
		Asn asn = createAsn("Asn001", OW1, W1);
		List<JSONObject> list = new ArrayList<JSONObject>();
		for( AsnItem item : asn.items ) {
			JSONObject obj = new JSONObject();
			obj.put("id", item.getId());
			obj.put("qty_this", "2");
			obj.put("lotatt01", "xxx");
			
			list.add( obj );
		}
		
		String items = list.toString();
		asnAction.asnCKInbound(asn.getId(), items, IN_LOC.getCode());
	}
	
	@Test
	public void testAsnCKInbound4rf() {
		Asn asn = createAsn("Asn001", OW1, W1);
		List<JSONObject> list = new ArrayList<JSONObject>();
		for( AsnItem item : asn.items ) {
			JSONObject obj = new JSONObject();
			obj.put("id", item.getId());
			obj.put("qty_this", "2");
			obj.put("lotatt01", "xxx");
			
			list.add( obj );
		}
		
		Long asnId = asn.getId();
		OperationH op = asnAction.assign(asnId, worker1.getLoginName(), WMS.OP_IN);
		
		String items = list.toString();
		request.addParameter("items", items);
		
		asnAction.asnCKInbound4rf(request, asnId, IN_LOC.getCode(), op.getId());
	}
	
	@Test
	public void testOther() {
		Asn asn = createAsn("Asn001", OW1, W1);
		Map<Object, Object> map = asnAction.queryAsnItem(asn.getAsnno());
		
		@SuppressWarnings("unchecked")
		List<AsnItem> items = (List<AsnItem>) map.get("items");
		Assert.assertEquals(asn.items.size(), items.size());
		
		try {
			asnAction.queryAsnItem("Asn000000");
			Assert.fail();
		} catch(Exception e) { }
		
		asnAction.deleteAsn(asn.getId().toString());
		
		asn = createAsn("Asn002", OW1, W1);
		Long asnId = asn.getId();
		asnAction.cancelAsn(asnId, "test");
		asnAction.closeAsn(asnId);
		
		login(supplier1);
		try {
			asnService.inbound(asnId, null);
			Assert.fail();
		} 
		catch(Exception e) { 
			Assert.assertEquals( WMS.ASN_ERR_15, e.getMessage());
		}
	}
	
	@Test
	public void testPutaway() {
		Asn asn = createAsn("Asn007", OW1, W1);
		Long asnId = asn.getId();
		
		List<JSONObject> list = new ArrayList<JSONObject>();
		for( AsnItem item : asn.items ) {
			JSONObject obj = new JSONObject();
			obj.put("id", item.getId());
			obj.put("qty_this", item.getQty());
			obj.put("toloccode", IN_LOC.getCode());
			
			list.add( obj );
		}
		
		asnAction.putawayInfo(asnId);
		
		String items = list.toString();
		request.addParameter("items", items);
		asnAction.inboundInAsnHtml(asnId, items);
		
		List<?> opItems1 = asnAction.asnOpItems(asnId.toString(), "入库");
		List<?> opItems2 = asnAction.asnOpItems(asnId.toString(), "上架");
		Assert.assertEquals(1, opItems1.size());
		Assert.assertEquals(0, opItems2.size());
		
		asnAction.putawayInfo(asnId);
		
		list = new ArrayList<JSONObject>();
		for( AsnItem item : asn.items ) {
			JSONObject obj = new JSONObject();
			obj.put("asnitem_id", item.getId());
			obj.put("qty", "0");
			obj.put("toloccode", CC_LOC_1.getCode());
			
			list.add( obj );
		}
		try {
			asnAction.putaway( list.toString() );
			Assert.fail();
		} catch(Exception e) {
			e.printStackTrace();
			Assert.assertEquals(WMS.OP_ERR_1, e.getMessage());
		}
		
		list = new ArrayList<JSONObject>();
		for( AsnItem item : asn.items ) {
			JSONObject obj = new JSONObject();
			obj.put("asnitem_id", item.getId());
			obj.put("qty", item.getQty());
			obj.put("toloccode", CC_LOC_1.getCode());
			
			String hql = " from OperationItem where asnitem = ? and operation.optype.text = ? order by id";
			List<OperationItem> rkItems = operationDao.getItems(hql, item, WMS.OP_TYPE_IN); // 入库作业明细
			obj.put("inv_id", rkItems.get(0).getOpinv().getId());
			
			list.add( obj );
		}
		
		printTable("Inventory");
		asnAction.putaway( list.toString() );
		
		asnAction.putawayInfo(asnId);
		
		try {
			asnService.cancelInbound(asn.getId());
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_5, asn.getAsnno()), e.getMessage() );
		}
	}
}
