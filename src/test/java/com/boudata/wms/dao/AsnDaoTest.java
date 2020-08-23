/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.inbound.AsnService;

public class AsnDaoTest extends AbstractTest4WMS {
	
	@Autowired protected AsnService asnService;
	@Autowired protected AsnDao asnDao;
	
	@Test
	public void test() {
		String asnno = "ASN001";
		
		// 1、创建Asn单
		Asn asn = createAsn(asnno, OW1, W1);
		
		// 2.1、查询【-001】的入库单
		Asn asn_1 = asnDao.getAsn("X001");
		Assert.assertNull(asn_1);
		
		// 2.2、查询【ASN-001】的入库单
		asn_1 = asnDao.getAsn(asnno);
		Assert.assertEquals(asnno, asn_1.getAsnno());
		
		// 3、查询Asn的items
		List<AsnItem> asnItems = asnDao.getItems(asn.getId());
		Assert.assertTrue(asn.getSkus() == asnItems.size());
		
		Double qty_total = 0D;
		for(AsnItem asnItem : asnItems) {
			qty_total += asnItem.getQty();
		}
		Assert.assertEquals(asn.getQty(), qty_total);
		
		// 4、查询Asn的某条item
		AsnItem asnItem = asnDao.getAsnItem( asnItems.get(0).getId() );
		Assert.assertEquals(asnItem.getSku(), asnItems.get(0).getSku());
		
		// 5、hql查询Asn的items
		asnItems = asnDao.getAsnItems("from AsnItem where id = ?", asnItem.getId());
		Assert.assertTrue(asnItems.size() == 1);
		Assert.assertEquals(asnItem.getSku(), asnItems.get(0).getSku());
		
	}

}
