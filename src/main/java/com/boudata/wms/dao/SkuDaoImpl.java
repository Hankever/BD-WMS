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
import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.sso.Environment;
import com.boudata.wms.WMS;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;

@Repository("SkuDao")
public class SkuDaoImpl extends BaseDao<_Sku> implements SkuDao {

	public SkuDaoImpl() {
		super(_Sku.class);
	}
	
    public _Sku getSku(String code) {
    	 String hql = "from _Sku where ? in (code, barcode, barcode2) and domain = ?";
    	 List<?> list = getEntities(hql, code, Environment.getDomain());
    	 if(list.isEmpty()) {
       		 throw new BusinessException( EX.parse(WMS.SKU_ERR_1, code) );
       	 }
    	 
    	 return (_Sku) list.get(0);
    }
 
	public _Sku getSku(String barcode, Long ownerId) {
		return getSkus(barcode, ownerId).get(0);
	}

	@SuppressWarnings("unchecked")
	public List<_Sku> getSkus(String code, Long ownerId) {
		
		boolean pointedOwner = ownerId != null && ownerId > 0;
		
		String hql = "from _Sku where ? in (code, barcode, barcode2) and domain = ?";
		List<?> list = new ArrayList<>();
		if( pointedOwner ) {
			list = getEntities(hql + " and owner.id = ? ", code, Environment.getDomain(), ownerId);
			if( list.isEmpty() ) {
				list = getEntities(hql + " and owner is null ", code, Environment.getDomain());
			}
		} 
		else {
			list = getEntities(hql, code, Environment.getDomain());
		}
		
		if (list.isEmpty()) {
			String errorMsg = EX.parse(WMS.SKU_ERR_2, code);
			if( pointedOwner ) {
				_Owner owner = (_Owner) getEntity(_Owner.class, ownerId);
				errorMsg = EX.parse(WMS.SKU_ERR_3, owner.getName(), code);
			}
			throw new BusinessException(errorMsg);
		}
		return (List<_Sku>) list;
	}
}
