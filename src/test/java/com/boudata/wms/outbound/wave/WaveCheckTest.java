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
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.outbound.AbstractTest4PK;

import junit.framework.Assert;

/**
 * 直接按波次快速出库（拣货确认 和 验货，直接操作完成）
 */
public class WaveCheckTest extends AbstractTest4PK {
	
	@Autowired WaveAction waveAction;
	
	@Test
	public void test1() {
		createNewInv(_ONE_INV, 21d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		
		OrderH order1 = createOrder("BD_001", OW1, W1, 1, 9D);
//		OrderH order2 = createOrder("BD_002", OW1, W1, 1, 10D);
		
		List<Long> orderIds = Arrays.asList( order1.getId() );
		
		OrderWave wave1 = waveAction.createWave(W1.getId(), EasyUtils.list2Str(orderIds));
		Long id = wave1.getId();
		
		List<OperationH> pkhs = waveAction.allocate(id);
		OperationH pkh = pkhs.get(0);
		Assert.assertEquals(WMS.OP_STATUS_01, pkh.getStatus());
		Assert.assertEquals(WMS.O_STATUS_03, order1.getStatus());
		
		waveAction.checkByPKH( pkh.getId() );
		Assert.assertEquals(WMS.OP_STATUS_04, pkh.getStatus());
		Assert.assertEquals(WMS.O_STATUS_05, order1.getStatus());
	}
	
	@Test
	public void test2() {
		createNewInv(_ONE_INV, 21d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		
		OrderH order1 = createOrder("BD_001", OW1, W1, 1, 10D);
		OrderH order2 = createOrder("BD_002", OW1, W1, 1, 10D);
		
		List<Long> orderIds = Arrays.asList(order1.getId(), order2.getId());
		
		OrderWave wave1 = waveAction.createWave(W1.getId(), EasyUtils.list2Str(orderIds));
		Long id = wave1.getId();
		
		List<OperationH> pkhs = waveAction.allocate(id);
		OperationH pkh = pkhs.get(0);
		Assert.assertEquals(WMS.OP_STATUS_01, pkh.getStatus());
		Assert.assertEquals(WMS.O_STATUS_03, order1.getStatus());
		
		// 验货完成直接出库
		request.getSession().setAttribute("auto_check_outbound", "1");
		
		waveAction.checkByPKH( pkh.getId() );
		Assert.assertEquals(WMS.OP_STATUS_04, pkh.getStatus());
		Assert.assertEquals(WMS.O_STATUS_06, order1.getStatus());
	}
}
