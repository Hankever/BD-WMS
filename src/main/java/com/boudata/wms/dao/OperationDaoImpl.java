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
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.RuleType;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Warehouse;

@SuppressWarnings("unchecked")
@Repository("OperationDao")
public class OperationDaoImpl extends BaseDao<OperationH> implements OperationDao {

	public OperationDaoImpl() {
		super(OperationH.class);
	}
 
	public OperationItem getOpItem(Long itemId) {
		return (OperationItem) getEntity(OperationItem.class, itemId);
	}
	
	public OperationH getOperation(Long opId) {
		OperationH op = getEntity(opId);
		String hql = "from OperationItem where operation.id = ?";
		List<?> list = getEntities(hql, opId);
		
		op.items.clear();
		for(Object item : list) {
			op.items.add( (OperationItem)item );
		}
		
		return op;
	}

	public OperationH getOperation(Long whId, String opNo) {
		String hql = "select id from OperationH where opno = ? and warehouse.id = ?";
		List<?> list = getEntities(hql, opNo, whId);
		if( list.isEmpty() ) {
			throw new BusinessException( EX.parse(WMS.OP_ERR_2, opNo) );
		}

		return getOperation( (Long) list.get(0) );
	}
	
	public OperationH getOperation(String docNo, String opType, String exclude_status) {
		String opNo_prefix = docNo.toUpperCase() + "-" +opType+ "%"; // 不适用波次等批量作业生成的工单查找
		
		String hql = " from OperationH where opno like '" +opNo_prefix+ "' and domain = ?";
		List<?> list;
		if(exclude_status != null) {
			hql += " and status != ? ";
			list = getEntities(hql, Environment.getDomain(), exclude_status);
		}
		else {
			list = getEntities(hql, Environment.getDomain()); 
		}

		return (OperationH) (list.isEmpty() ? null : list.get(0));
	}
	
	public OperationH getOperation(String docNo, String opType) {
		return getOperation(docNo, opType, null);
	}

	public List<OperationItem> getItems(Long opId) {
		return new ArrayList<>( this.getOperation(opId).items );
	}
	
	public List<OperationItem> getItems(String hql, Object...params) {
		return (List<OperationItem>) getEntities(hql, params);
	}
	 
	public List<OperationH> getSubwaves(Long waveId) {
		String hql = "from OperationH where wave.id = ?";
		return (List<OperationH>) getEntities(hql, waveId);
	}
	
	public List<OpException> getOpExcetions(String hql, Object...params) {
		return (List<OpException>) getEntities(hql, params);
	}

	public _Owner getOwner(Long ownerId) {
		List<?> list1 = getEntities("from _Owner where domain = ? and ifnull(status, 1) = 1", Environment.getDomain());
		if(list1.size() == 1) {
			return (_Owner) list1.get(0);
		}
		List<?> list2 = getEntities("from _Owner where id = ?", ownerId);
		if(list2.size() == 1) {
			return (_Owner) list2.get(0);
		}
		throw new BusinessException( WMS.OWNER_ERR_1 );
	}
	
	public OperationItem createCheckOpItem(OrderItem oItem, OperationH op, Double qty_this) {
		OperationItem opItem;
		List<OperationItem> list = getItems("from OperationItem where orderitem = ? and operation = ?", oItem, op);
		if(list.size() > 0) {
			opItem = list.get(0);
			opItem.setQty( MathUtil.addDoubles(opItem.getQty(), qty_this) );
			updateRecordObject(opItem);
		} 
		else {
			opItem = new OperationItem();
			opItem.setOperation(op);
			opItem.setOrderitem(oItem);
			opItem.setOwner(oItem.getOrder().getOwner());
			opItem.setSkucode(oItem.getSku().getCode());
			opItem.setQty( qty_this );
			opItem.setLoccode("");
			createRecordObject(opItem);
		}
		
		op.setQty( MathUtil.addDoubles(op.getQty(), qty_this) );
		update(op);
		
		return opItem;
	}
	
	// 作业规则配置支持：域级、仓库级、货主级
	public String getPKRule(_Warehouse warehouse, _Owner owner, String ruleCode, List<_Rule> rList) {
		if( ruleCode != null ) {
			String hql = " from _Rule where  type = '" +RuleType.PICKUP_RULE+ "' and status = 1 and code = ? and domain = ? ";
			List<?> list = getEntities(hql, ruleCode, Environment.getDomain());
    		if( list.isEmpty() ) {
    			throw new BusinessException( EX.parse(WMS.RULE_ERR_1, ruleCode) );
    		}
    		_Rule rule = (_Rule) list.get(0);
    		rList.add(rule);
    		
    		return rule.getContent();
    	} 
    	else {
    		return _Util.ruleDef(rList, _Rule.DEFAULT_PK_RULE, owner.getPickupr(), warehouse.getPickupr());
    	}
    }
}
