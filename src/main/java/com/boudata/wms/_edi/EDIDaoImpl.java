/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._edi;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.BeanUtil;
import com.boudata.wms.entity._Customer;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;

@SuppressWarnings("unchecked")
@Repository("EDIDao")
public class EDIDaoImpl extends BaseDao<IEntity> implements EDIDao {
	
	public EDIDaoImpl() {
        super(IEntity.class);
    }
	
	public List<EDIKey> searchKeyByPlatform(String platform) {
		String hql = " from EDIKey o where o.platform = ? and o.status = 1 and o.domain = ?";
		return (List<EDIKey>) getEntities(hql, platform, Environment.getDomain());
	}
 
	public _Sku checkSku(String domain, Long owner_id, String skuName, String skuCode, String barcode, Map<String, ?> skuAttibutes) {
		_Sku sku;
		List<?> skus = this.getEntities("from _Sku where code = ? and domain = ?", skuCode, domain);
		if(skus.isEmpty()) {
			if( skuName == null ) {
				throw new BusinessException("WMS里编码为【" +skuCode+ "】的SKU不存在，请先维护");
			}
			sku = new _Sku();
			sku.setName(skuName);
			sku.setCode(skuCode);
			sku.setBarcode(barcode);
			sku.setStatus(ParamConstants.TRUE);
			sku.setRemark("edi create");
			sku.setDomain(domain);
			sku.setCreator(Environment.getUserCode());
			sku.setCreateTime(new Date());
			sku.setOwner( new _Owner(owner_id) );
			
			this.create(sku);
			sku.isNew = true;
		}
		else {
			sku = (_Sku) skus.get(0);
			_Owner owner = sku.getOwner();
			if( owner != null && !owner.getId().equals(owner_id) ) {
				throw new BusinessException("货品【" +sku.getName()+ "】属于货主【" +owner.getName()+ "】");
			}
			
			if( sku.getStatus() == ParamConstants.FALSE ) {
				sku.setStatus(ParamConstants.TRUE); // 重新上架货品
				sku.setName(skuName);
				this.update(sku);
			}
			sku.isNew = false;
		}
		
		if( skuAttibutes != null) {
			BeanUtil.setDataToBean(sku, skuAttibutes, true);
			this.update(sku);
		}
		
		return sku;
	}
	
	public _Warehouse getWarehouse(String warehouse, String domain) {
		if( warehouse == null ) {
			List<?> list = getEntities("from _Warehouse where domain = ? and status = 1", domain);
			if( list.size() == 1 ) {
				return (_Warehouse) list.get(0);  // 只有一个仓库，调用接口时可以不传仓库参数
			}
			throw new BusinessException("仓库参数不能为空");
		}
		
		List<?> list  = getEntities("from _Warehouse where ? in (name,code) and domain = ? and status = 1", warehouse, domain);
		if( list.isEmpty() ) {
			throw new BusinessException("库【" +warehouse+ "】不存在");
		}
		return (_Warehouse) list.get(0);
	}
 
	public _Owner getOwner(String owner, String domain) {
		if( owner == null ) {
			List<?> list = getEntities("from _Owner where domain = ? and status = 1", domain);
			if( list.size() == 1 ) {
				return (_Owner) list.get(0);  // 只有一个货主，调用接口时可以不传货主参数
			}
			throw new BusinessException("货主参数不能为空");
		}

		List<?> list = getEntities("from _Owner where ? in (name,code) and domain = ? and status = 1", owner, domain);
		if( list.isEmpty() ) {
			throw new BusinessException("货主【" +owner+ "】不存在");
		}
		return (_Owner) list.get(0);
	}

	public _Customer checkCustomer(String domain, String code, Map<String, Object> attibutes) {
		_Customer c;
		List<?> list = this.getEntities("from _Customer where code = ? and domain = ?", code, domain);
		if( list.isEmpty() ) {
			c = new _Customer();
			c.setCode(code);
			c.setStatus(ParamConstants.TRUE);
			c.setRemark("edi create");
			c.setDomain(domain);
			c.setCreator(Environment.getUserCode());
			c.setCreateTime(new Date());
			
			BeanUtil.setDataToBean(c, attibutes, true);
			this.create(c);
		}
		else {
			c = (_Customer) list.get(0);
			BeanUtil.setDataToBean(c, attibutes, true);
			this.update(c);
		}
		
		return c;
	}
}
