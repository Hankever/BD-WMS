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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.DateUtil;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.inbound.AsnService;
import com.boudata.wms.outbound.AbstractTest4PK;

import junit.framework.Assert;

public class WaveErrTest extends AbstractTest4PK {
	
	@Autowired WaveService waveService;
	@Autowired AsnService asnService;
	
	List<Long> orderIds;
	
	@Test
	public void testErr() {
		
		orderIds = new ArrayList<>();
		
		OrderWave wave = new OrderWave();
		wave.setCode( SerialNOer.get("Wxxxx") );
		wave.setWarehouse( W1 ); 
		wave.setOrigin(WMS.W_ORIGIN_02);
		waveService.excutingWave(wave, orderIds);
		
		OrderH o1 = super.createOrder("BD_001", OW1, W1, 1, 3D);
		OrderH o2 = super.createOrder("BD_002", OW1, W1, 3, 1D);
		OrderH o3 = super.createOrder("BD_003", OW1, W1, 1, 12D);
		
		orderIds.add(o1.getId());
		orderIds.add(o2.getId());
		orderIds.add(o3.getId());
		
		wave = new OrderWave();
		wave.setCode( SerialNOer.get("Wxxxx") );
		wave.setWarehouse( W1 ); 
		wave.setOrigin(WMS.W_ORIGIN_02);
		wave = waveService.createWave(wave, orderIds);
		o3.setWave(wave);
		
		wave = new OrderWave();
		wave.setCode( SerialNOer.get("Wxxxx") );
		wave.setWarehouse( W1 ); 
		wave.setOrigin(WMS.W_ORIGIN_02);
		try {
			waveService.createWave(wave, orderIds);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("请先从波次中剔除") > 0 );
		}
		
		o3.getWave().setOrigin(WMS.W_ORIGIN_01);
		wave = new OrderWave();
		wave.setCode( SerialNOer.get("Wxxxx") );
		wave.setWarehouse( W1 ); 
		wave.setOrigin(WMS.W_ORIGIN_01);
		o3.setStatus(WMS.O_STATUS_00);
		waveService.createWave(wave, orderIds);
		
		Inventory inv = createNewInv(_ONE_INV, 100d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		inv.setInvstatus("良品");
		
		o3.setStatus(WMS.O_STATUS_01);
		wave = new OrderWave();
		wave.setCode( SerialNOer.get("Wxxxx") );
		wave.setWarehouse( W1 ); 
		wave.setOrigin(WMS.W_ORIGIN_02);
		wave = waveService.createWave(wave, Arrays.asList( o1.getId(), o3.getId() ));
		
		List<OperationH> pkhs = waveService.allocate(wave.getId());
		OperationH pkh = pkhs.get(0);
		Assert.assertEquals(2, pkh.items.size());
		
		o1.setStatus(WMS.O_STATUS_42);
		try {
			waveService.cancelAllocate(wave.getId());
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("取消库存分配失败") >= 0 );
		}
		
		o1.setStatus(WMS.O_STATUS_03);
		pkh.setStatus( WMS.OP_STATUS_02 );
		pkh.items.get(0).setStatus(WMS.OP_STATUS_04);
		waveService.cancelAllocate(wave.getId());
		
		try {
			waveService.splitPKH("", 1L);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("明细行为空，请选择要拆分的拣货明细行") >= 0 );
		}
		
		try {
			waveService.splitPKH("-0001", 1L);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("明细行为空，请选择要拆分的拣货明细行") >= 0 );
		}
	}
}
