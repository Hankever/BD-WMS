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

import com.boubei.tss.framework.persistence.IDao;
import com.boudata.wms.entity._Location;

public interface LocationDao extends IDao<_Location> {
	
	_Location getLoc(Long whId, String code);
	
	_Location getLoc(Long whId, String code, boolean exDisabed);
	
	String getInLoc(Long whId);
	
	String getOutLoc(Long whId);
	
	List<_Location> getCCLocs(Long whId);
	
	List<_Location> queryLocs(String hql, Object...params);
	
	List<_Location> queryLocsAndContainers(String hql, Object...params);
	
	void restartLoc(Long whId, String loccode);
}
