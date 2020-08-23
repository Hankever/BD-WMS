/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity._Location;
import com.boudata.wms.inventory.InventorySo;

import junit.framework.Assert;

public class InventoryDaoTest extends AbstractTest4WMS {

	@Autowired protected InventoryDao inventoryDao;
	
	@Test
	public void test() {
		InventorySo so = new InventorySo();
		so.setOwnerId(OW1.getId());
		
		System.out.println(_ONE_INV);
		
		Set<Long> idList = new HashSet<>();
		idList.add( _ONE_INV.getId() );
		idList.add( 0L );
		List<Inventory> list = inventoryDao.searchInvsByIDs(idList );
		Assert.assertEquals(1, list.size());
		
		inventoryDao.getInvs("from Inventory where wh.id = ?", 1L);
		
		// test parent location
		_Location container1 = super.createLocation("MC1", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_MV), W1);
		container1.setParent( super.CC_LOC_1 );
		
		Inventory inv1 = super.createNewInv(_ONE_INV, 199D, container1, null, null);
		so = new InventorySo();
		so.setWarehouseId(W1.getId());
		so.setLocationCode(CC_LOC_1.getCode());
		List<?> invs = inventoryDao.search(so).getItems();
		Assert.assertEquals(2, invs.size());
		Assert.assertEquals(_ONE_INV, invs.get(0));
		Assert.assertEquals(inv1, invs.get(1));
		
		so = new InventorySo();
		so.setLocationId(CC_LOC_1.getId());
		invs = inventoryDao.search(so).getItems();
		Assert.assertEquals(2, invs.size());
		Assert.assertEquals(inv1, invs.get(1));
		
		so = new InventorySo();
		so.setWarehouseId(W1.getId());
		so.setLoccode("CC");
		invs = inventoryDao.search(so).getItems(); 
		Assert.assertEquals(2, invs.size());
		Assert.assertEquals(inv1, invs.get(1));
		
		so.setLoccode("CCxx");
		invs = inventoryDao.search(so).getItems(); 
		Assert.assertEquals(0, invs.size());
		
		so = new InventorySo();
		so.setWarehouseId(W1.getId());
		so.setZone("C");
		invs = inventoryDao.search(so).getItems(); 
		Assert.assertEquals(2, invs.size());
		Assert.assertEquals(inv1, invs.get(1));
		
		so.setZone("CXX");
		invs = inventoryDao.search(so).getItems(); 
		Assert.assertEquals(0, invs.size());
		
		so.setLocationCode("CXX-001");
		invs = inventoryDao.search(so).getItems(); 
		Assert.assertEquals(0, invs.size());
	}
}
