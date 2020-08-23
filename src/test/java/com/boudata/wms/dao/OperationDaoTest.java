/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;

public class OperationDaoTest extends AbstractTest4WMS {
	
	@Autowired protected OperationDao dao;
	
	@Test
	public void test() {
		
		try {
			dao.getOperation(W1.getId(), "xxx");
			Assert.fail();
		} 
		catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.OP_ERR_2, "xxx"), e.getMessage() );
		}

		try {
			dao.getOwner(999L);
			Assert.fail();
		} 
		catch(Exception e) {
			Assert.assertEquals( WMS.OWNER_ERR_1, e.getMessage());
		}
		
		dao.delete(OW2);
		Assert.assertEquals(OW1, dao.getOwner(999L));
	}
	
}
