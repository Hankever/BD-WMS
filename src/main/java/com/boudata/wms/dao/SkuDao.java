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
import com.boudata.wms.entity._Sku;

public interface SkuDao extends IDao<_Sku> {
	
	_Sku getSku(String code);
	
	_Sku getSku(String barcode, Long ownerId);
	
	List<_Sku> getSkus(String barcode, Long ownerId);
}
