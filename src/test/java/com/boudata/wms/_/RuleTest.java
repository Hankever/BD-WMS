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

import com.boubei.tss.EX;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.RuleType;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.entity._Rule;

public class RuleTest extends AbstractTest4WMS {
	
	@Test
	public void test1() {
		String pkruleCode = "pk-rule-1";
		_Rule rule = createRule(pkruleCode, "pk rule content", RuleType.PICKUP_RULE);
		
		Assert.assertEquals(1, commservice.getList("from _Rule where code = ?", pkruleCode).size());
		
		commservice.delete(_Rule.class, rule.getId());
		Assert.assertEquals(0, commservice.getList("from _Rule where code = ?", pkruleCode).size());
		
		_Rule rule1 = createRule("pk-rule-1", "pk rule content", RuleType.PICKUP_RULE);
		_Rule rule2 = createRule("wv-rule-1", "mv rule content", RuleType.WAVE_RULE);
		_Rule rule3 = createRule("pw-rule-1", "pw rule content", RuleType.PUTAWAY_RULE);
		
		// 默认
		List<_Rule> rList = new ArrayList<_Rule>();
		Assert.assertEquals(_Rule.DEFAULT_WV_RULE, _Util.ruleDef(rList, _Rule.DEFAULT_WV_RULE, W1.getWaver(), OW1.getWaver()));
		Assert.assertEquals(_Rule.DEFAULT_PW_RULE, _Util.ruleDef(rList, _Rule.DEFAULT_PW_RULE, W1.getPutawayr(), OW1.getPutawayr()));
		Assert.assertEquals(_Rule.DEFAULT_PK_RULE, operationDao.getPKRule(W1, OW1, null, rList));
		Assert.assertEquals(_Rule.DEFAULT_PK_RULE, operationDao.getPKRule(W2, OW2, null, rList));
		
		// 域级
		Assert.assertEquals(rule1.getContent(), operationDao.getPKRule(W2, OW2, pkruleCode, rList));
		
		try {
			operationDao.getPKRule(W2, OW2, "xxx", rList);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.RULE_ERR_1, "xxx") , e.getMessage());
		}
		
		// 仓库级
		W1.setWaver(rule2);
		Assert.assertEquals(rule2.getContent(), _Util.ruleDef(rList, _Rule.DEFAULT_WV_RULE, W1.getWaver(), OW1.getWaver()));
		
		// 货主级
		W1.setWaver(null);
		OW1.setWaver(rule2);
		Assert.assertEquals(rule2.getContent(), _Util.ruleDef(rList, _Rule.DEFAULT_WV_RULE, W1.getWaver(), OW1.getWaver()));
		
		W1.setPutawayr(rule3);
		OW1.setPutawayr(rule3);
		Assert.assertEquals(rule3.getContent(), _Util.ruleDef(rList, _Rule.DEFAULT_PW_RULE, W1.getPutawayr(), OW1.getPutawayr()));
	}

}
