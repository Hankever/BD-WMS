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
import java.util.Map;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.SubAuthorize;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.DateUtil;
import com.boudata.wms.entity._Location;

public class WmsAPITest extends AbstractTest4WMS {
	
	@Autowired WmsAPI api;
	
	@Test
	public void test1() {
		login("BD");
		
		api.genAsnNO(OW1.getId(), "3");
		api.genAsnNO(OW1.getId(), "2");
		api.genAsnNO(OW1.getId(), "1");
		
		OW1.setCode("{BDxxxx}");
		commonDao.update(OW1);
		Assert.assertEquals("11BD001", api.genAsnNO(OW1.getId(), "1") );
		
		api.skux(skuOne.getBarcode(), OW1.getId());
		api.sku(skuOne.getBarcode(), OW1.getId());
		
		Map<String, _Location> m = api.locs("CC_1,CC_2,CC_3", W1.getId());
		Assert.assertEquals(3, m.size());
		
		// tes saveWarehouse & syncWarehouseGroup
		api.syncWarehouseGroup(W1.getId(), null);
		api.syncWarehouseGroup(W1.getId(), null);
		
		List<JSONObject> items = new ArrayList<>();
		JSONObject item1 = new JSONObject();
		item1.put("id", W1.getId());
		item1.put("name", W1.getName());
		item1.put("code", W1.getCode());
		items.add(item1);
		
		JSONObject item2 = new JSONObject();
		item2.put("name", "W100");
		item2.put("code", "W100");
		item2.put("status", 0);
		items.add(item2);
		
		request.addParameter("items", items.toString());
		List<Group> groups = api.saveWarehouse(request);
		Assert.assertEquals(2, groups.size());
		Assert.assertEquals("W100", groups.get(1).getName());
		
		request.removeAllParameters();
		request.addParameter("groupName", "W101");
		try{
			api.getUserByGroup(request);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("W101不存在", e.getMessage());
		}
		
		request.removeAllParameters();
		request.addParameter("groupName", "W100");
		api.getUserByGroup(request);
		
		request.removeAllParameters();
		request.addParameter("groupName", "BD");
		api.getUserByGroup(request);
		
		List<User> users = api.getBranchUsers(null);
		Assert.assertTrue( users.size() > 0 );
		
		users = api.getBranchUsers(WMS.ROLE_CG);
		Assert.assertTrue( users.size() > 0 );
		
		String sql = "select sa from SubAuthorize sa, ModuleUser mu "
				+ " where sa.moduleId = mu.moduleId and sa.buyerId = mu.userId "
				+ " and mu.domain = ? and sa.endDate > now()";
		List<?> list = commservice.getList(sql, Environment.getDomainOrign());
		int index = 0;
		for( Object o : list ) {
			SubAuthorize sa = (SubAuthorize) o;
			sa.setEndDate( DateUtil.today() );
			
			if(index++ == 0)
				commonDao.update(sa);
			else 
				commonDao.delete(sa);
		}
		
		login(domainUser);  // 域管理员正常登陆
		try{
			login(cg);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals("您所在域的购买账号已经过期，请联系管理员续费后再登录", e.getMessage());
		}
	}
}
