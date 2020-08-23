/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._SkuX;

public class SkuDaoTest extends AbstractTest4WMS {
	
	@Autowired protected SkuDao skuDao;
	@Autowired protected BaseService baseService;
	
	@Test
	public void test() {
		String code = "123123123";
		
	 	// 1、创建Sku Owner为空
		_Sku sku = createSku(code);
	     String barcode = sku.getBarcode();
	     Long ownerId = OW1.getId();
	     
	    // 2.1、按照code查询Sku
		_Sku sku_1 = skuDao.getSku(code);
		Assert.assertEquals(code, sku_1.getCode());
		
		// 2.2、按照code owner.id 查询Sku
		
		// owner.id 为空
		_Sku sku_2 = skuDao.getSku(barcode, null);
		Assert.assertEquals(code, sku_2.getCode());
		
		// owner.id 不为空
		sku.setOwner(OW1);
		skuDao.update(sku);
		sku_2 = skuDao.getSku(barcode, ownerId);
		Assert.assertEquals(code, sku_2.getCode());
		
		// 找不到SKU
		String no_code = "9ijkm";
		try {
			skuDao.getSku(no_code);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.SKU_ERR_1, no_code), e.getMessage() );
		}
		
		try {
			skuDao.getSkus(no_code, ownerId);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.SKU_ERR_3, "OW-1", no_code), e.getMessage() );
		}
		
		try {
			skuDao.getSkus(no_code, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.SKU_ERR_2, no_code), e.getMessage() );
		}
		
		// 3、查询skux
		_Sku sku_children = createSku("C123123123");
		
		_SkuX skux = new _SkuX();
		skux.setParent(sku);
		skux.setSku(sku_children);
		skux.setWeight(1);
		skuDao.createObject(skux);
		
		_Sku sku_x = baseService.skux(barcode, ownerId);
		Assert.assertTrue(sku_x.skuxList.size() == 1);
		Assert.assertEquals(sku_children, sku_x.skuxList.get(0).getSku());
		
		List<_Sku> skus = baseService.sku(barcode, ownerId);
		Assert.assertTrue(skus.size() == 1);
		Assert.assertEquals(sku, skus.get(0));
	}
	
}
