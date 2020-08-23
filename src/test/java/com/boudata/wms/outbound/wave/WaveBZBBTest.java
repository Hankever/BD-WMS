/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.util.DateUtil;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.outbound.AbstractTest4PK;
import com.boudata.wms.outbound.wave.WaveRule.Subwave;

import junit.framework.Assert;

/**
 * 边摘边播
 * 
 * 推着集货墙（车）去拣货，拣货明细的目的库位直接指定为“出库位”
 */
public class WaveBZBBTest extends AbstractTest4PK {
	
	@Autowired protected WaveRuleEngine engine;
	
	@Test
	public void test1() {
		
		createNewInv(_ONE_INV, 9d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		createNewInv(_ONE_INV, 12d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		
		OrderH order1 = createOrder("BD_001", OW1, W1, 1, 10D);
		OrderH order2 = createOrder("BD_002", OW1, W1, 1, 10D);
		
		OperationH pkh1 = orderService.allocate(order1.getId());
		OperationH pkh2 = orderService.allocate(order2.getId());
		List<OperationItem> pkds1 = operationDao.getItems(pkh1.getId());
		List<OperationItem> pkds2 = operationDao.getItems(pkh2.getId());
		
		Map<Long, List<OperationItem>> oh_PKDs = new HashMap<Long, List<OperationItem>>();
		oh_PKDs.put(order1.getId(), pkds1);
		oh_PKDs.put(order2.getId(), pkds2);
		
		order1.setQty(999D);
		engine.removeNotCompletelyOHs(oh_PKDs);
		
		
		OrderWave wave = new OrderWave();
		wave.setWarehouse(W1);
		wave.setCode("Wave1");
		commonDao.createObject(wave);
		
		OperationItem pkd1 = pkds1.get(0);
		OperationItem pkd2 = pkds1.get(1);
		OperationItem pkd3 = pkds2.get(0);
		pkd3.setOperation(null);
		pkd1.setOperation(pkh2);
		
		oh_PKDs.put(order1.getId(), Arrays.asList(pkd1, pkd2));
		oh_PKDs.put(order2.getId(), Arrays.asList(pkd3));
		
		engine.orderTransSubwave(oh_PKDs, wave);
		
		Subwave subwave = new Subwave();
		WaveRule rule = new WaveRule();
		subwave.sql = "select id from wms_operation_item";
		
		rule.persitedPKDs.put(pkd1.getId(), pkd1);
		rule.persitedPKDs.put(pkd2.getId(), pkd2);
		rule.persitedPKDs.put(pkd3.getId(), pkd3);
		List<OperationItem> list = engine.queryPKDs4SubWave(subwave , rule);
		Assert.assertEquals(1, list.size());
	}
}
