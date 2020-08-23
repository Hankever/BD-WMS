/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.boudata.wms.entity.Inventory;

import junit.framework.Assert;

public class InvMapTest {
	
	@Test
	public void test() {
		InvMap im = new InvMap();
		im.clear();
		
		Inventory inv = new Inventory();
		inv.setId(1L);
		Long invID = inv.getId();
		im.getOriginalinv(invID);
		im.getTerminalinv(invID);
		
		im.setNeedPersist(true);
		im.put(null);
		im.put(inv);
		
		
		Assert.assertTrue( im.containsKey(invID) );
		im.containsValue( im.get(invID) );
		im.entrySet();
		im.getOriginalinv(invID);
		im.getTerminalinv(invID);
		Assert.assertTrue( !im.isEmpty() );
		Assert.assertTrue( im.size() == 1 );
		
		im.historyOfinvs();
		
		Map<Long, Inventory> m = new HashMap<Long, Inventory>();
		m.put(invID, inv);
		
		im.putAll(m );
		im.remove(invID);
		im.remove(invID);
		
		im.setNeedPersist(false);
		im.put(inv);
	}

}
