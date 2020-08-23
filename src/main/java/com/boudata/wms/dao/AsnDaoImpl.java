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

import org.springframework.stereotype.Repository;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.sso.Environment;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OperationItem;

@Repository("AsnDao")
@SuppressWarnings("unchecked")
public class AsnDaoImpl extends BaseDao<Asn> implements AsnDao {
	
	public AsnDaoImpl() {
		super(Asn.class);
	}
	
    public Asn getAsn(String code) {
    	 String hql = "from Asn where asnno in (" + DMUtil.insertSingleQuotes(code) + ") and domain = ?";
    	 List<?> list = getEntities(hql, Environment.getDomain());
    	 
    	 return (Asn) (list.isEmpty() ? null : list.get(0));
    }
    
	public List<AsnItem> getItems(Long asnId) {
    	String hql = " from AsnItem o where o.asn.id = ? ";
		return (List<AsnItem>) getEntities(hql, asnId);
    }

	public AsnItem getAsnItem(Long itemId) {
		return (AsnItem) getEntity(AsnItem.class, itemId);
	}
	
	public List<AsnItem> getAsnItems(String hql, Object...params) {
    	return (List<AsnItem>) getEntities(hql, params);
    }
	
	public List<OperationItem> getOpItems(Long asnId, String opType) {
		String hql = "from OperationItem o where o.asnitem.asn.id = ? and o.operation.optype.text = ? and qty > 0 "; // qty < 0 为取消入库的作业明细
		return (List<OperationItem>) getEntities(hql, asnId, opType);
	}
}
