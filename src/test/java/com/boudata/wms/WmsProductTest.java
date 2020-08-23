/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.entity.ModuleDef;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

import junit.framework.Assert;

public class WmsProductTest extends AbstractTest4WMS {
	
	@Test
	public void test() {
		
		ModuleDef module1 = new ModuleDef();
		module1.setKind("物流");
		module1.setModule("WMS");
		module1.setCode("WMS");
		module1.setRoles("-1");
		module1.setReports("11,12,13");
		module1.setRecords("14,15,16");
		module1.setStatus("opened");
		module1.setPrice(300D);
		module1.setPrice_def("${account_num}*${month_num}*300");
		module1.setTry_days(30);
		module1.setAccount_limit("1,99");
		module1.setMonth_limit("1,36");
		module1.setCashback_ratio(10D);
		module1.setProduct_class(WmsProduct.class.getName());
		commonDao.createObject(module1);
		
		// 下单1，已注册的域管理员购买
		login(super.domainUser);
		CloudOrder co1 = new CloudOrder();
		co1.setId(null);
		co1.setModule_id(module1.getId());
		co1.setAccount_num(3); // 两个账号
		co1.setMonth_num(24);
		co1.setType(module1.getProduct_class());
		
		setParameters(co1);
		co1 = oAction.createOrder(request);
		
		login( UMConstants.ADMIN_USER );
		oAction.payedOrders(co1.getOrder_no(), co1.getMoney_cal());
		
		// 下单2，已注册的域管理员购买
		login(super.domainUser);
		CloudOrder co2 = new CloudOrder();
		co2.setModule_id(module1.getId());
		co2.setAccount_num(1); // 两个账号
		co2.setMonth_num(12);
		co2.setType(module1.getProduct_class());
		
		setParameters(co2);
		co2 = oAction.createOrder(request);
		
		login( UMConstants.ADMIN_USER );
		oAction.payedOrders(co2.getOrder_no(), co2.getMoney_cal());
		
		login(super.domainUser);
		User customer3 = createUser("Customer-1", customerGroup.getId(), null);
		try {
			login(customer3);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals(WMS.SYS_ERR_1, e.getMessage());
		}
	}
	
	protected void setParameters(Object o){
		Map<String,Object> map = BeanUtil.getProperties(o);
		for (Entry<String, ?> entry : map.entrySet()) {
			String key = entry.getKey();
			if( !Arrays.asList("createTime","order_date").contains(key) ) {
				request.setParameter(key, EasyUtils.obj2String(entry.getValue()));
			}
		}
	}
	
}
