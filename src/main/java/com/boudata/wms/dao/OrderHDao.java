/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.Collection;
import java.util.List;

import com.boubei.tss.framework.persistence.IDao;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.BoxItem;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;

public interface OrderHDao extends IDao<OrderH> {
	
	void updateOrderAfterOutbound(OrderH order, String status);
	
	OrderH getOrder(String code);
	
	List<OrderH> getOrders(Collection<Long> orderIds);

	List<OrderH> getOrderByWave(Long soWaveId);
	
	OrderWave getWave(Long id);

	OrderItem getOrderItem(Long itemId);
	
	List<OrderItem> getOrderItems(Long orderId);

	List<OrderItem> getOrderItems(Collection<Long> orderIds);
	
	List<OrderItem> getOrderItemsByIds(Collection<Long> soiIds);
	
	List<OrderItem> getOrderItems(String hql, Object...params);
	
	List<OperationItem> getOrderPKDs(Collection<Long> orderIds);
	
	List<Box> getBoxsByOrder(Long orderId);
	
	List<Box> getBoxs(String boxIds);
	
	List<Box> getBoxsByPallent(String pallet, Long whId);
	
	Box getBox(Long boxId);
	
	List<BoxItem> getBoxItems(Long boxId);
	
	Box createBox(Box box, OrderH order);
	
	BoxItem createBoxItem(Box box, Double qty, OrderItem oItem, OperationItem opi);
	
	void fixBoxInfo(Box box);
}
