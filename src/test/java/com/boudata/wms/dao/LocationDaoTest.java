/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.modules.param.ParamConstants;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.dto.LocationDTO;
import com.boudata.wms.entity._Location;

public class LocationDaoTest extends AbstractTest4WMS {

	@Autowired protected LocationDao locationDao;
	
	@Test
	public void test() {
		String loccode = "LOC-01-01";
		
		// 1、创建库位
		_Location loc = this.createLocation(loccode, WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		Long whId = W1.getId();
		
		// 2、按照loccode查询库位
		_Location loc_1 = locationDao.getLoc(whId, loccode);
		Assert.assertEquals(loccode, loc_1.getCode());
		
		// 3、查询 入库库位 出库库位 存储库位
		String inLocCode = locationDao.getInLoc(whId);
		Assert.assertEquals(IN_LOC.getCode(), inLocCode);
		
		String outLocCode = locationDao.getOutLoc(whId);
		Assert.assertEquals(OUT_LOC.getCode(), outLocCode);
		
		List<_Location> ccLocs = locationDao.getCCLocs(whId);
		Assert.assertTrue(ccLocs.size() == 3);
		Assert.assertEquals(CC_LOC_1, ccLocs.get(0));
		Assert.assertEquals(CC_LOC_2, ccLocs.get(1));
		Assert.assertEquals(CC_LOC_3, ccLocs.get(2));
		
		String no_code = "ijbjj";
		try {
			locationDao.getLoc(whId, no_code , true);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.LOC_ERR_1, no_code), e.getMessage() );
		}
		
		// 4、hql查询库位
		List<_Location> locs = locationDao.queryLocs("from _Location where id = ?", loc.getId());
		Assert.assertTrue(locs.size() == 1);
		Assert.assertEquals(loc, locs.get(0));
		
		// 5、重启停用库位
		loc.setStatus(ParamConstants.FALSE);
		locationDao.update(loc);
		
		try {
			locationDao.getLoc(whId, loc.getCode(), true);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( EX.parse(WMS.LOC_ERR_2, loc.getCode()), e.getMessage() );
		}
		
		locationDao.restartLoc(whId, loc.getCode());
		Assert.assertTrue(loc.getStatus() == ParamConstants.TRUE);
		
		locationDao.restartLoc(whId, no_code);
		
		// 没有收（发）货位
		try {
			locationDao.getInLoc(-999L);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.LOC_ERR_3, e.getMessage() );
		}
		try {
			locationDao.getOutLoc(-999L);
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals( WMS.LOC_ERR_4, e.getMessage() );
		}
	}
	
	@Test
	public void testLocationDTO() {
		_Location loc1 = this.createLocation("A-1-01", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		_Location loc2 = this.createLocation("A-1-02", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		_Location loc3 = this.createLocation("A-2-01", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		_Location loc4 = this.createLocation("B-3-01", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		_Location loc5 = this.createLocation("001", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		_Location loc6 = this.createLocation("D-01", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		_Location loc7 = this.createLocation("D-02", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT), W1);
		
		createNewInv(_ONE_INV, 10D, loc1, null, null);
		
		Collection<_Location> locs = new ArrayList<>();
		locs.add(loc1);
		locs.add(loc2);
		locs.add(loc3);
		locs.add(loc4);
		locs.add(loc5);
		locs.add(loc6);
		locs.add(loc7);
		Collection<LocationDTO> locDTOs = LocationDTO.buildLocTree(locs);
		Assert.assertEquals(4, locDTOs.size());
		
		LocationDTO temp = locDTOs.iterator().next();
		System.out.println( temp );
		temp.equals(temp);
		temp.equals(null);
		
		loc7.setZone("D");
		loc7.setRack(null);
		locDTOs = LocationDTO.buildLocTree(locs);
	}
}
