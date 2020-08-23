/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.outbound.AbstractTest4PK;

import junit.framework.Assert;

public class OrderBatchOutboundTest extends AbstractTest4PK {
	
	@Autowired WaveAction waveAction;
	 
	List<Long> orderIds;
	
	private void prepareData(int index) {
		OrderH o1 = super.createOrder(index + "BD_001", OW1, W1, 1, 3D);
		OrderH o2 = super.createOrder(index + "BD_002", OW1, W1, 3, 1D);
		OrderH o3 = super.createOrder(index + "BD_003", OW1, W1, 1, 12D);
		OrderH o4 = super.createOrder(index + "BD_004", OW1, W1, 1, 90D);
		
		orderIds = new ArrayList<>();
		orderIds.add(o1.getId());
		orderIds.add(o2.getId());
		orderIds.add(o3.getId());
		orderIds.add(o4.getId());
		
		Asn asn = createAsn("A_001", OW1, W1, 3);
		List<?> items = commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		
		List<AsnItem> list = new ArrayList<>();
		for(Object obj : items ) {
			AsnItem ai = (AsnItem) obj;
			ai.setQty_this( ai.getQty() );
			ai.setLoccode( locations.get(list.size()).getCode() );
			
			list.add(ai);
		}
		asnService.inbound( asn.getId(), list );
	}
	
	@Test
	public void test1() {
		createNewInv(_ONE_INV, 100d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		
		prepareData(1);
		List<String> result = waveAction.outboundBatch( EasyUtils.list2Str(orderIds) );
		Assert.assertEquals("【1BD_004】库存不足", result.get(0));
	}

	@Test
	public void test2() {
		createNewInv(_ONE_INV, 100d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		
		prepareData(1);
		OrderWave wave = waveAction.createWave(W1.getId(),  EasyUtils.list2Str(orderIds) );
		List<String> result = waveAction.outboundByWave(wave.getId());
		Assert.assertEquals("【1BD_004】库存不足", result.get(0));
	}
}
