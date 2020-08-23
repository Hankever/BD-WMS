/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.junit.Test;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;

import junit.framework.Assert;

public class WMSTest extends AbstractTest4WMS {
	
	@Test
	public void testCX() {
		
		String udf1 = "{\"js_in_without_job\":1,\"js_checkin_with_lot\":1,\"js_out_without_job\":1,\"js_pickup_check_out\":1,\"auto_create_box\":1,\"negative_inv_qty\":0,\"js_inv_check_approve\":0,\"js_qty_precision\":0,\"js_hide_money\":1,\"js_enable_sn\":0}";
		SQLExcutor.excuteInsert("insert into x_domain (udf1,domain) values (?,?)", 
				new Object[] { udf1, domain }, "connectionpool");
		
		login(cg);
		
		login(ow1);
		
		OW1.setName("OW-1111");
		commonDao.update(OW1);
		login(ow1);
		
		OW2.setWarehouse(W2);
		commonDao.update(OW2);
		login(ow2);
		
		login(cg);
		Assert.assertTrue( WMS.isManager() );
		Assert.assertFalse( WMS.isSupplier() );
		Assert.assertFalse( WMS.isOWner() );
		Assert.assertFalse( WMS.isWorker() );
		Assert.assertFalse( WMS.inv_check_approve() );
		Assert.assertFalse( WMS.negative_inv_qty() );
		
		Param group = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "组1");
		cx.addComboParam("Month", "月份", "一月,二月,三月".split(","), group);
		cx.addComboParam("Month", "月份", "二月,三月,四月".split(","), group);
		
		try {
			WMS.checkRoleX("AA", "BB");
			Assert.fail();
		} catch(Exception e) {
		}
		
		List<String> roleNames = new ArrayList<>();
		List<Long> roleIds = new ArrayList<>();
		
		HttpSession session = request.getSession();
		session.setAttribute(SSOConstants.USER_RIGHTS_L, roleIds );  
	    session.setAttribute(SSOConstants.USER_ROLES_L,  roleNames );
		
		new WmsLoginCustomizer().addRole2Session(session, WMS.ROLE_CG, roleNames);
		new WmsLoginCustomizer().addRole2Session(session, WMS.ROLE_CG, roleNames);
		Assert.assertEquals(1, roleNames.size());
	}
}
