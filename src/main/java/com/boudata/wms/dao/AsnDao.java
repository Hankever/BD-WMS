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
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OperationItem;

public interface AsnDao extends IDao<Asn> {
	
	Asn getAsn(String code);
	
	AsnItem getAsnItem(Long itemId);
	
	List<AsnItem> getItems(Long asnId);
	
	List<AsnItem> getAsnItems(String hql, Object...params);
	
	List<OperationItem> getOpItems(Long asnId, String opType);
	
}
