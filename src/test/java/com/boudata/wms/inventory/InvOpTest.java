/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.Arrays;
import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.DateUtil;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.InventoryTemp;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.inventory.InventoryService;


public class InvOpTest extends AbstractTest4WMS {
	
	@Autowired InventoryService invService;
	@Autowired InventoryAction invAction;
	
	// 移库、理货、补货
	@Test
	public void testMove() {
		OperationH op = createOperationH("MV", WMS.OP_TYPE_MV);
		createOperationItem(op, OUT_LOC.getCode(), 12d, 0d);
		
		invAction.assignOp(W1.getId(), op.getOpno(), "JK");
		invAction.queryOpi( op.getId() );
		invAction.queryOpi(W1.getId(), op.getOpno());	
		
		invService.execOperations( op.getId() );
		
		op.setStatus( WMS.OP_STATUS_04 );
		super.commonDao.update(op);
		invAction.assignOp(W1.getId(), op.getOpno(), "JK");
		
		printTable(OperationH.class.getName());
		printTable(OperationItem.class.getName());
		printTable(OperationLog.class.getName());
		printTable(InventoryLog.class.getName());
	}

	// 调整
	@Test
	public void testAdjust() {
		OperationH op = createOperationH("AJ", WMS.OP_TYPE_TZ);
		createOperationItem(op, null, 90d, 108d);
		invService.execOperations( op.getId() );
		
		printTable(OperationH.class.getName());
		printTable(OperationItem.class.getName());
		printTable(OperationLog.class.getName());
		printTable(InventoryLog.class.getName());
	}
	
	// 冻结、分配
	@Test
	public void testHold() {
		// 分配冻结
		OperationH op = createOperationH("DJ", WMS.OP_TYPE_DJ);
		createOperationItem(op, null, 30d, 0d);
		invService.execOperations( op.getId() );
		
		// 拣货
		op = createOperationH("JH", WMS.OP_TYPE_JH);
		createOperationItem(op, OUT_LOC.getCode(), 30d, 0d);
		invService.execOperations( op.getId() );
		
		printTable(OperationH.class.getName());
		printTable(OperationItem.class.getName());
		printTable(OperationLog.class.getName());
		printTable(InventoryLog.class.getName());
	}
	
	@Test
	public void testInvOperation() {
		
		Inventory inv = invOperation.searchInv(OW1.getCode(), skuOne.getId(), null, JX_LOC.getId());
		Assert.assertNotNull(inv);
		
		Assert.assertTrue( InvOperation.checkInvIsNull(null, new InvMap()) );
		
		try {
			invOperation.execOperations(null, null);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.OP_ERR_7, e.getMessage() );
		}
		
		OperationH op = createOperationH("MV", WMS.OP_TYPE_MV);
		createOperationItem(op, OUT_LOC.getCode(), 300d, 0d);
		try {
			invService.execOperations( op.getId() );
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("系统库存不足") >= 0 );
		}
		
		op = createOperationH("MV", WMS.OP_TYPE_MV);
		createOperationItem(op, OUT_LOC.getCode(), 3d, 0d);
		_ONE_INV.setQty_locked( _ONE_INV.getQty() );
		try {
			invService.execOperations( op.getId() );
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("部分库存锁定中") >= 0 );
		}
		
		_ONE_INV.setQty_locked(0D);
		_ONE_INV.setQty(10D);
		OUT_LOC.setHolding(1);
		OUT_LOC.setChecking(1);
		try {
			invService.execOperations( op.getId() );
			Assert.fail();
		} catch(Exception e) {
			Assert.assertTrue( e.getMessage().indexOf("库位【OU_0】被冻结") >= 0 );
		}
		
		/* 锁定、非盘点：许进不许出 */
		OUT_LOC.setChecking(0);
		invService.execOperations( op.getId() );
	}
	
	private OperationH createOperationH(String code, String opType) {
		OperationH op = new OperationH();
		op.setOpno( SerialNOer.get(code) );
		op.setOptype(WMS.opType(opType));
		op.setStatus(WMS.OP_STATUS_01); // 新建
		op.setWarehouse( W1 );
		commonDao.createObject(op);
		
		return op;
	}
	
	private OperationItem createOperationItem(OperationH op, String toLoc, Double qty, Double toQty) {
		_ONE_INV.setQty(qty);
		
		OperationItem opItem = new OperationItem();
		opItem.setOwner(OW1);
		opItem.setSkucode( _ONE_INV.getSku().getCode() );
		opItem.setLoccode( CC_LOC_1.getCode() );
		opItem.setToloccode( toLoc );
		opItem.setQty( qty );
		opItem.setToqty( toQty );
		opItem.setLotatt01("白色");
		opItem.setCreatedate(DateUtil.subDays(DateUtil.today(), 9));
		opItem.setOperation(op);
		if( qty < 100 ) {
			opItem.setOpinv( _ONE_INV );
		}
		
		commonDao.createObject(opItem);
		return opItem;
	}
	
	@Test
	public void test0() {
		Long skuId = _ONE_INV.getSku().getId();
		Long locationId = JX_LOC.getId();
		
		InventoryTemp t = createInventoryTemp(skuId, locationId); // invDao.getEntities("from InventoryTemp")
		Assert.assertEquals(1, invDao.searchInvs( Arrays.asList(t) ).size() );
		
		t = createInventoryTemp(skuId, locationId);
		t.setCreatedate(null);
		Assert.assertEquals(2, invDao.searchInvsIgnoreLot( Arrays.asList(t) ).size() );
		
		t = createInventoryTemp(skuId, locationId);
		t.setCreatedate(null);
		t.setLotatt01(null);
		Assert.assertEquals(4, invDao.searchInvsIgnoreLot( Arrays.asList(t) ).size() );
		
		Inventory inv1 = invOperation.searchInv(OW1.getCode(), skuId, _ONE_INV, locationId);
		Assert.assertNotNull(inv1);
		
		inv1.setQty_locked(0d);
		
		Assert.assertTrue( inv1.compare(inv1) );
		
		Inventory inv2 = inv1.clone();
		Inventory inv3 = inv1.mockClone(null);
		Assert.assertTrue( inv2.compare(inv3) );
		
		inv2.setId(null);
		inv2.setOwner(OW2);
		Assert.assertFalse( inv1.compare(inv2) );
		
		inv2.setOwner( inv1.getOwner() );
		inv2.setSku( new _Sku(-1L) );
		Assert.assertFalse( inv1.compare(inv2) );
		
		inv2.setSku( inv1.getSku() );
		inv2.setLocation( new _Location(-1L) );
		Assert.assertFalse( inv1.compare(inv2) );
		
		inv2.setLocation( inv1.getLocation() );
		Assert.assertTrue( inv1.compare(inv2) );
		
		inv1.setLotatt02(" ");
		Assert.assertTrue( inv1.compare(inv2) );
		
		inv2.setLotatt02(" 批次2 ");
		Assert.assertFalse( inv1.compare(inv2) );
	}

	static Long seqence = 1L;
	private InventoryTemp createInventoryTemp(Long skuId, Long locationId) {
		
		InventoryTemp t = new InventoryTemp();
		t.setId(seqence++);
		
		_Location loc = locDao.getEntity(locationId);
		
		t.setWhId(loc.getWarehouse().getId());
		t.setOwnerId(OW1.getId());
		t.setSkuId(skuId);
		t.setLocationId(locationId);
		t.setLotatt01( "白色" );
		t.setCreatedate(_ONE_INV.getCreatedate());
		t.setInvstatus(_ONE_INV.getInvstatus());
		
		return t;
	}
	
    /** 准备好库存数据。*/
    protected void prepareTestData() {
        super.prepareTestData();
        
        // 清空货物状态，用以测试 llu.货物状态 严格， 是否在支持 null(sod) 对 null(llu)
        _ONE_INV.setInvstatus(null); 
        
        Date today = DateUtil.today();
        _ONE_INV.setCreatedate(DateUtil.subDays(today, 9)); // 生产日期为9天前
        _ONE_INV.setExpiredate(DateUtil.subDays(today, -365)); // 过期时间为12个月
        
        createNewInv(_ONE_INV, 1d, JX_LOC, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 1d, JX_LOC, "白色", DateUtil.subDays(today, 11));
        createNewInv(_ONE_INV, 2d, JX_LOC, "黑色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 3d, JX_LOC, "红色", DateUtil.subDays(today, 9));
        
        createNewInv(_ONE_INV, 4d, MV_LOC, "白色", DateUtil.subDays(today, 9));
        
        createNewInv(_ONE_INV, 90d, CC_LOC_1, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 150d, CC_LOC_1, "黑色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 200d, CC_LOC_2, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 300d, CC_LOC_3, "白色", DateUtil.subDays(today, 11));
        
        commonDao.flush();
    }

}
