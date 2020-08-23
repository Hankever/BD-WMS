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
import java.util.Set;

import com.boubei.tss.framework.persistence.IDao;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryTemp;
import com.boudata.wms.inventory.InventorySo;

public interface InventoryDao extends IDao<Inventory> {
	
	PageInfo search(InventorySo so);

	List<Inventory> searchInvsByIDs(Set<Long> idList);

	List<Object[]> searchInvs(List<InventoryTemp> list);
	
	List<Object[]> searchInvsIgnoreLot(List<InventoryTemp> list);
	
	List<Inventory> getInvs(String hql, Object...params);

}
