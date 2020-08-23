/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.edi;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.util.DateUtil;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms._edi.EDI;
import com.boudata.wms._edi.EDIDao;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity._Sku;

import junit.framework.Assert;

public class EDITest extends AbstractTest4WMS {
	
	@Autowired EDIDao dao;
	@Autowired EDI edi;
	@Autowired AsnDao asnDao;
	@Autowired OrderHDao orderDao;
	
	@Test
	public void testImportOrder() {
		String code = "O202011001";
		
		JSONObject param = new JSONObject();
		param.put("code", code);
		param.put("warehouse", W1.getName());
		param.put("owner", OW1.getName());
		param.put("orderday", DateUtil.format(new Date()));
		param.put("d_receiver", "张三");
		param.put("d_mobile", "13388877666");
		param.put("d_addr", "浙江杭州一号大街68号");
		param.put("remark", "test");
		param.put("udf1", "udf1");
		param.put("udf2", "udf2");
		param.put("udf3", "udf3");
		param.put("udf4", "udf4");
		
		List<JSONObject> items = new ArrayList<>();
		for(int i = 10; i <= 12; i++) {
			JSONObject item = new JSONObject();
			item.put("code", "s" + i);
			item.put("name", "s" + i);
			item.put("barcode", "1010101010" + i);
			item.put("qty", 7);
			items.add(item);
		}
		param.put("items", items);
 
		String data = param.toString();
		edi.receiveOrder(data);
		
		edi.receiveOrder(data); // 二次推送
		
		// 将单据状态设置为作业中
		commonDao.executeHQL("update OrderH set status = '已分配' where orderno = ? and domain = ?", code, domain);
		Map<String, Object> result = edi.receiveOrder(data);
	    Assert.assertTrue( result.get("message").toString().indexOf("已存在并已经开始作业") > 0 );
		
	    commonDao.executeHQL("update OrderH set status = '新建' where orderno = ? and domain = ?", code, domain);
		String reason = "test";
		edi.cancelOrder(code, reason); // 被取消订单的创建人需和取消人相同
	}
	
	@Test
	public void testImportAsn() {
		String code = "A202011001";
		
		JSONObject param = new JSONObject();
		param.put("code", code);
		param.put("warehouse", W1.getName());
		param.put("owner", OW1.getName());
		param.put("asnday", DateUtil.format(new Date()));
		param.put("supplier", "张三");
		param.put("type", "采购入库");
		param.put("remark", "test");
		param.put("udf1", "udf1");
		param.put("udf2", "udf2");
		param.put("udf3", "udf3");
		param.put("udf4", "udf4");
		
		List<JSONObject> items = new ArrayList<>();
		for(int i = 10; i <= 12; i++) {
			JSONObject item = new JSONObject();
			item.put("code", "s" + i);
			item.put("name", "s" + i);
			item.put("barcode", "1010101010" + i);
			item.put("qty", 7);
			items.add(item);
		}
		param.put("items", items);
 
		String data = param.toString();
		edi.receiveAsn(data);
		
		edi.receiveAsn(data); // 二次推送
		
		// 将单据状态设置为作业中
		commonDao.executeHQL("update Asn set status = '已完成' where asnno = ? and domain = ?", code, domain);
		Map<String, Object> result = edi.receiveAsn(data);
	    Assert.assertTrue( result.get("message").toString().indexOf("已存在并已经开始作业") > 0 );
		
	    commonDao.executeHQL("update Asn set status = '新建' where asnno = ? and domain = ?", code, domain);
		String reason = "test";
		edi.cancelAsn(code, reason);
	}
	
	@Test
	public void testReceiveSKUs() {
		List<JSONObject> items = new ArrayList<>();
		for(int i = 10; i <= 12; i++) {
			JSONObject item = new JSONObject();
			item.put("owner", OW1.getName());
			item.put("code", "s" + i);
			item.put("name", "s" + i);
			item.put("barcode", "1010101010" + i);
			item.put("qty", 7);
			items.add(item);
		}
		
		String data = items.toString();
		edi.receiveSKUs(data);
	}
	
	@Test
	public void testCheckSku() {
		String barcode = "111";
		Long owner_id = OW1.getId();
		String skuCode = "111";
		String skuName = "111";
		_Sku sku = dao.checkSku(domain, owner_id, skuName, skuCode, barcode, null);
		dao.checkSku(domain, owner_id, skuName, skuCode, barcode, null);
		sku.setStatus(0);
		dao.checkSku(domain, owner_id, skuName, skuCode, barcode, null);
		
		try {
			dao.checkSku(domain, OW2.getId(), skuName, skuCode, barcode, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("货品【111】属于货主【OW-1】", e.getMessage());
		}
		
		edi.queryInv(W1.getCode(), OW1.getCode(), "111", null);
		edi.queryInv(W1.getCode(), OW1.getCode(), "111", "良品");
		
		try {
			dao.checkSku(domain, OW1.getId(), null, "A11", barcode, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("WMS里编码为【A11】的SKU不存在，请先维护", e.getMessage());
		}
	}

	@Test
	public void testGetOwner() {
		Assert.assertEquals( OW1, dao.getOwner(OW1.getName(), domain) );
		try {
			dao.getOwner(null, domain);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("货主参数不能为空", e.getMessage());
		}
		try {
			dao.getOwner("xxx", domain);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("货主【xxx】不存在", e.getMessage());
		}
		
		dao.delete(OW2);
		Assert.assertEquals( OW1, dao.getOwner(null, domain) );
	}
	
	@Test
	public void testGetWarehouse() {
		Assert.assertEquals( W1, dao.getWarehouse(W1.getName(), domain) );
		try {
			dao.getWarehouse(null, domain);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("仓库参数不能为空", e.getMessage());
		}
		try {
			dao.getWarehouse("xxx", domain);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("库【xxx】不存在", e.getMessage());
		}
		
		dao.delete(W2);
		Assert.assertEquals( W1, dao.getWarehouse(null, domain) );
	}
	
	@Test
	public void testReceiveCustomers() {
		List<JSONObject> items = new ArrayList<>();
		for(int i = 10; i <= 12; i++) {
			JSONObject item = new JSONObject();
			item.put("code", "s" + i);
			item.put("name", "s" + i);
			item.put("type", "门店");
			items.add(item);
		}
		
		String data = items.toString();
		edi.receiveCustomers(data);
 
		String code = "111";
		HashMap<String, Object> p = new HashMap<String, Object>();
		p.put("name", "河马神仙");
		p.put("type", "供货方");
		
		dao.checkCustomer(domain, code, p);
		dao.checkCustomer(domain, code, p);
	}
}
