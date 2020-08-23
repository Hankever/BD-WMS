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
import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Warehouse;

public interface OperationDao extends IDao<OperationH> {
	
	OperationItem getOpItem(Long itemId);
	
	OperationH getOperation(Long id);
	
	OperationH getOperation(Long whId, String opNo);
	
	OperationH getOperation(String docNo, String opType);
	
	OperationH getOperation(String docNo, String opType, String exclude_status);

	/**
	 * 获取作业单的所有作业明细
	 */
	List<OperationItem> getItems(Long opId);
	
	List<OperationItem> getItems(String hql, Object...params);
	
	/**
	 * 获取波次生成的所有子波次作业单
	 */
	List<OperationH> getSubwaves(Long waveId);
	
	List<OpException> getOpExcetions(String hql, Object...params);
	
	_Owner getOwner(Long ownerId);
	
	OperationItem createCheckOpItem(OrderItem oItem, OperationH op, Double qty_this);
	
	String getPKRule(_Warehouse warehouse, _Owner owner, String ruleCode, List<_Rule> rList);

}
