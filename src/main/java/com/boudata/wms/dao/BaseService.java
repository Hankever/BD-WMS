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
import java.util.Map;

import com.boubei.tss.um.entity.Group;
import com.boudata.wms.entity._Sku;

public interface BaseService {
	
	_Sku skux(String barcode, Long ownerId);
	
	List<_Sku> sku(String barcode, Long ownerId);
	
	List<Group> _saveWarehouse(List<Map<String, Object>> json_list);
	
	Object syncWarehouseGroup(Long whId, String roles);

}
