/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.entity._Sku;

public class SkuTest extends AbstractTest4WMS {
	
	@Test
	public void test1() {
		List<_Sku> list = new ArrayList<_Sku>();
		for(int i = 1; i <= 888; i++ ) {
			_Sku sku = createSku("SKU-T-" + i);
			list.add(sku);
		}
		
		Assert.assertEquals(1, commservice.getList("from _Sku where code = ?", "SKU-T-100").size());
		Assert.assertEquals(888, commservice.getList("from _Sku where code like 'SKU-T-%'").size());
	}
}
