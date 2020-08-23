/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.boubei.tss.modules.param.Param;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Warehouse;

public class LocationTest extends AbstractTest4WMS {
	
	@Test
	public void test1() {
		_Warehouse w = createWarehouse("asia-2");
		
		_Location inL = createLocation("IN_LOC", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_IN), w);
		_Location outL = createLocation("OUT_LOC", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OUT), w);
		
		Param mv = WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_MV);
		_Location mvL1 = createLocation("M-1", mv, w);
		_Location mvL2 = createLocation("M-2", mv, w);
		_Location mvL3 = createLocation("M-3", mv, w);
		
		List<_Location> locList = new ArrayList<_Location>();
		locList.add(inL);
		locList.add(outL);
		locList.add(mvL1);
		locList.add(mvL2);
		locList.add(mvL3);
		
		// 库位编码规则： 库区 + 货架 + 层 + 位 + 货架属性
		String zones[] = "A,B,C".split(",");
		String racks[] = "01,02,03,04,05,06,07,08,09".split(",");
		
		// 三个库区A、B、C
		for(int i=0; i < 3; i++) {
			String zoneCode = zones[i];
			// 每个库区9个货架
			for(int j=0; j < 9; j++) {
				String rackCode = racks[j];
				// 每个货架4层
				for(int m=1; m <= 4; m++) {
					Object cengCode = m;
					// 每层4个库位
					for(int n=0; n <= 4; n++) {
						String locCode = zoneCode + "-" + rackCode + "-" + cengCode + "-" + n;
						
						// 每个库区前面三个货架的下面两层作为拣选区
						String locType = ( j < 3 && m < 3 ? WMS.LOC_TYPE_PK : WMS.LOC_TYPE_CC);
						_Location loc = createLocation(locCode, WMS.comboParam(WMS.LOC_TYPE, locType), w);
						loc.setZone(zoneCode);
						loc.setRack(rackCode);
						
						locList.add( loc );
						
						System.out.println( loc.getCode() );
					}
				}
			}
		}
		
		List<?> list = commservice.getList("from _Location where warehouse.id = ?", w.getId());
		Assert.assertEquals(locList.size(), list.size());
		log.info( EasyUtils.obj2Json(list.get(0)) );
		
		Assert.assertFalse( ((_Location)list.get(0)).isFrozen() );
	}
	
}
