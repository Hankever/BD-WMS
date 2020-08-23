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
import com.boudata.wms.entity._Owner;

public class OwnerTest extends AbstractTest4WMS {
	
	@Test
	public void test1() {
		_Owner owner = createOwner("boubei-1");
		
		List<?> list = commservice.getList("from _Owner where code = ?", "boubei-1");
		Assert.assertEquals(1, list.size());
		log.info( EasyUtils.obj2Json(list) );
		
		commservice.delete(_Owner.class, owner.getId());
		
		list = commservice.getList("from _Owner where code = ?", "boubei-1");
		Assert.assertEquals(0, list.size());
	}

}
