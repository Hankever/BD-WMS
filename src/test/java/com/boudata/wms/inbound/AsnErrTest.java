/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inbound;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.inventory.OperationService;

public class AsnErrTest extends AbstractTest4WMS {
	
	@Autowired AsnService asnService;
	@Autowired OperationService opService;
	
	@Test
	public void test1() {
		String asnNo = "Asn001";
		Asn asn = new Asn();
		asn.setAsnno(asnNo);
		asn.setOwner(OW2);
		asn.setWarehouse(W1);
		asn.setAsnday( new Date() );
		asn.setSupplier("YC");
		asn.setType("普通入库");
		
		skuOne.setOwner(OW1);
		AsnItem item = new AsnItem();
		item.setSku( skuOne );
		item.setQty( 100D );
		
		ArrayList<AsnItem> items = new ArrayList<AsnItem>();
		items.add(item);
		try {
			asnService.createAsn(asn, items);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_11, "OW-2", "SKU-0", "OW-1"), e.getMessage() );
		}
	}
	
	@Test
	public void test2() {
		String asnNo = "ASN002";
		Asn asn = super.createAsn(asnNo, OW1, W1);
		OperationH op = asnService.assign( asn.getId(), worker1.getLoginName(), WMS.OP_IN );
		
		opService.acceptConfirm(op.getId(), WMS.OP_STATUS_06, "拒绝工单");
		asnService.assign( asn.getId(), cg.getLoginName(), WMS.OP_IN );
		
		asn.setStatus( WMS.ASN_STATUS_04 );
		commonDao.update(asn);
		try {
			asnService.inbound(asn.getId(), asn.items);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_7, "ASN002","已完成"), e.getMessage() );
		}
		
		asn = super.createAsn("ASN003", OW1, W1);
		for(AsnItem item : asn.items) {
			item.setQty_this( 1D );
			item.setLoccode( MV_LOC.getCode() );
		}
		asn.items.get(0).setQty_this(0D);
		asnService.inbound(asn.getId(), asn.items);
		
		IN_LOC.setType(WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_CC));
		commonDao.update(IN_LOC);
		asn = super.createAsn("ASN004", OW1, W1);
		for(AsnItem item : asn.items) {
			item.setQty_this( 1D );
		}
		try {
			asnService.inbound(asn.getId(), asn.items);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.ASN_ERR_8, e.getMessage() );
		}
	}
	
	@Test
	public void test3() {
		Asn asn = super.createAsn("ASN012", OW1, W1);
		for(AsnItem item : asn.items) {
			item.setQty_this( 1D );
			item.setLoccode( MV_LOC.getCode() );
		}
		asn.items.get(0).setQty_this(0D);
		asnService.inbound(asn.getId(), asn.items);
		
		asn.setStatus(WMS.ASN_STATUS_00);
		commonDao.update(asn);
		try {
			asnService.cancelInbound(asn.getId());
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_6, "ASN012", "关闭"), e.getMessage() );
		}
		
		try {
			asnService.cancelAsn(asn.getId(), "test cancel");
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_10, "ASN012", "关闭"), e.getMessage() );
		}
	}
	
	@Test
	public void test4() {
		Asn asn = super.createAsn("ASN012", OW1, W1);
		for(AsnItem item : asn.items) {
			item.setQty_this( 1D );
			item.setLoccode( MV_LOC.getCode() );
		}
		asn.items.get(0).setQty_this(0D);
		asnService.inbound(asn.getId(), asn.items);
		
		asnService.assign( asn.getId(), worker1.getLoginName(), WMS.OP_XH );
		asnService.assign( asn.getId(), worker1.getLoginName(), "xxx" );
		asnService.cancelAsn(asn.getId(), "test cancel Asn");
	}
}
