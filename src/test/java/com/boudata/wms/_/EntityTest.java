/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.EX;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._edi.EDIKey;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;

public class EntityTest {
	
	@Test
	public void testAbstractLotatt() {
		Inventory inv = new Inventory();
		inv.setQty( 100D );
		Assert.assertNull( inv.getQty_uom() );
		
		inv.setLotatt02("1");
		Assert.assertNull( inv.getQty_uom() );
		
		inv.setLotatt02("20");
		Assert.assertEquals(true, inv.getQty_uom() == 5);
	}

	@Test
	public void testEntities() {

		String entityPackage = "com/boudata/wms/entity";
		String entityDir = URLUtil.getResourceFileUrl(entityPackage).getPath();
		List<String> list = FileHelper.listFiles(entityDir);
		
		for (String clazz : list) {
			try {
				clazz = entityPackage.replaceAll("/", ".") + "." + clazz.substring(0, clazz.indexOf("."));
				if( clazz.indexOf("Abstract") >= 0 ) continue;
				
				System.out.print( clazz );
				
				Object o1 = BeanUtil.newInstanceByName(clazz);
				Object o2 = BeanUtil.newInstanceByName(clazz);

				BeanUtil.copy(o1, o2);
				EasyUtils.obj2Json(o1);
				
				System.out.println( " ------------------- " );
			} 
			catch (Exception e) {
				System.out.println( "  " );
				e.printStackTrace();
			}
		}
		
		Object o1 = new EDIKey();
		Object o2 = new EDIKey();
		BeanUtil.copy(o1, o2);
		EasyUtils.obj2Json(o1);
		
		o1 = new _DTO();
		o2 = new _DTO();
		BeanUtil.copy(o1, o2);
		EasyUtils.obj2Json(o1);
		
		_Sku sku = new _Sku();
		sku.setName("sku1");
		sku._price( 1D );
		sku._price( null );
		
		AsnItem ai = new AsnItem();
		ai.setSku(sku);
		ai.setQty(100D);
		Asn asn1 = new Asn();
		asn1.setWarehouse( new _Warehouse() );
		ai.setAsn(asn1);
		try {
			ai.setQty_actual(200D);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.ASN_ERR_13, sku.getName()), e.getMessage() );
		}
		ai.setLotatt03("");
		ai.setLotatt04("xxx");
		ai.getLot();
		ai.compareLotAtt(ai);

		Asn asn = new Asn();
		asn.setInbound_date(null);
		asn.setUnloader(null);
		
		OrderItem oi = new OrderItem();
		oi.setSku(sku);
		oi.setQty(100D);
		OrderH order1 = new OrderH();
		order1.setWarehouse(new _Warehouse());
		oi.setOrder(order1);
		try {
			oi.setQty_send(200D);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.O_ERR_20, sku.getName()), e.getMessage() );
		}
		oi.getOrderno();
	}
}
