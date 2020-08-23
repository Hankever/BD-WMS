/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms;

import org.junit.Test;

import junit.framework.Assert;

public class _UtilTest {
	
	@Test
	public void testUtilErr() {
		try {
			_Util.json2List("xxx");
			Assert.fail();
		} catch(Exception e) {
		}
		
		Assert.assertNull( _Util.json2Map(null) );
		
		try {
			_Util.json2Map("xxx");
			Assert.fail();
		} catch(Exception e) {
		}
		
		try {
			_Util.json2Map("{xxx}");
			Assert.fail();
		} catch(Exception e) {
		}
		
		Assert.assertNotNull( _Util.getIDs(null) );
		
		try {
			_Util.dataXml2Doc("<?xml version=\"1.0\" encoding=\"UTF-8\"?>{xxx}");
			Assert.fail();
		} catch(Exception e) {
			System.out.print(e.getMessage());
		}
		
		Assert.assertNotNull( _OpEvent.create("123456") );
	}

}
