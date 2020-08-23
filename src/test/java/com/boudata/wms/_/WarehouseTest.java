/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.entity._Warehouse;

public class WarehouseTest extends AbstractTest4WMS {
	
	@Test
	public void test1() {
		_Warehouse w = createWarehouse("asia-1");
		log.info( EasyUtils.obj2Json(w) );
		
		List<?> list = commservice.getList("from _Warehouse where code = ?", "asia-1");
		Assert.assertEquals(1, list.size());
		
		commservice.delete(_Warehouse.class, w.getId());
		
		list = commservice.getList("from _Warehouse where code = ?", "asia-1");
		Assert.assertEquals(0, list.size());
	}

}
