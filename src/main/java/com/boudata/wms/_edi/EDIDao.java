/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._edi;

import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.persistence.IDao;
import com.boubei.tss.framework.persistence.IEntity;
import com.boudata.wms.entity._Customer;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;

public interface EDIDao extends IDao<IEntity> {
	
	List<EDIKey> searchKeyByPlatform(String platform);
	
	_Sku checkSku(String domain, Long owner_id, String skuName, String skuCode, String barcode, Map<String, ?> skuAttibutes);
	
	_Warehouse getWarehouse(String warehouse, String domain);
 
	_Owner getOwner(String owner, String domain);

	_Customer checkCustomer(String domain, String code, Map<String, Object> attibutes);
	
}
