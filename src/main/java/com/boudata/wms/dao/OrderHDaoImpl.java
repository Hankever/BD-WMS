/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.BoxItem;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Sku;

@Repository("OrderHDao")
@SuppressWarnings("unchecked")
public class OrderHDaoImpl extends BaseDao<OrderH> implements OrderHDao {

	public OrderHDaoImpl() {
		super(OrderH.class);
	}
	
	public void updateOrderAfterOutbound(OrderH order, String status) {
		order.setStatus(status);
		order.setOutbound_date(new Date());
		order.setWorker(Environment.getUserCode());
		super.update(order);
		
		// 触发出库完成事件（ 回调奇门ERP; 修改OMS的订单状态为“已发货”，通知开单人员等）
		if(  WMS.O_STATUS_06.equals(status) ) {
			// TODO
		}
	}
	
    public OrderH getOrder(String code) {
    	 String hql = "from OrderH where orderno = ? and domain = ?";
    	 List<?> list = getEntities(hql, code, Environment.getDomain());
    	 
    	 return (OrderH) (list.isEmpty() ? null : list.get(0));
    }
	
	public List<OrderH> getOrders(Collection<Long> orderIds) {
		String hql = " from OrderH o where o.id in (" +list2Str(orderIds)+ ")";
		return (List<OrderH>) getEntities(hql);
	}
    
	public List<OrderH> getOrderByWave(Long soWaveId) {
		String hql = " from OrderH o where o.wave.id = ?";
		return (List<OrderH>) getEntities(hql, soWaveId);
	}
    
    public OrderWave getWave(Long id) {
    	return (OrderWave) getEntity(OrderWave.class, id);
    }
    
    public OrderItem getOrderItem(Long itemId) {
    	return (OrderItem) getEntity(OrderItem.class, itemId);
    }

    public List<OrderItem> getOrderItems(Long orderId) {
    	return this.getOrderItems( Arrays.asList(orderId) );
    }
    
    public List<OrderItem> getOrderItems(Collection<Long> orderIds) {
		String hql = " from OrderItem o where o.order.id in (" +list2Str(orderIds)+ ")";
		return (List<OrderItem>) getEntities(hql);
	}
	
	public List<OrderItem> getOrderItemsByIds(Collection<Long> soiIds) {
		String hql = " from OrderItem o where o.id in (" +list2Str(soiIds)+ ")";
		return (List<OrderItem>) getEntities(hql);
	}
    
    public List<OrderItem> getOrderItems(String hql, Object...params) {
    	return (List<OrderItem>) getEntities(hql, params);
    }
	
	public List<OperationItem> getOrderPKDs(Collection<Long> orderIds) {
		String hql = " from OperationItem o where o.orderitem.order.id in (" +list2Str(orderIds)+ ")";
		return (List<OperationItem>) getEntities(hql);
	}
	
	public List<Box> getBoxsByOrder(Long orderId) {
		return (List<Box>) getEntities(" from Box where order.id = ?", orderId);
	}
	
	public List<Box> getBoxs(String boxIds) {
		return (List<Box>) getEntities( " from Box where id in (" + boxIds + ")" );
	}
	
	// 按托盘单找出未出库的Box
	public List<Box> getBoxsByPallent(String pallet, Long whId) {
		String hql = " from Box o where o.pallet = ? and o.order.warehouse.id = ? and outTime is null";
		return (List<Box>) getEntities(hql, pallet, whId);
	}
	
	public Box getBox(Long boxId) {
		return (Box) getEntity(Box.class, boxId);
	}
	
	public List<BoxItem> getBoxItems(Long boxId) {
		String hql = "from BoxItem o where o.box.id = ?";
		return (List<BoxItem>) getEntities(hql, boxId);
	}
    
    private String list2Str(Collection<Long> list) {
    	return (String) EasyUtils.checkTrue( EasyUtils.isNullOrEmpty(list) , "-999", EasyUtils.list2Str(list));
    }
    
    public Box createBox(Box box, OrderH order) {
 		box.setOrder(order);
 		box.setBoxdate( new Date() );
 		box.setBoxer(Environment.getUserCode());
 		if( box.getBoxno() == null ) {
 			box.setBoxno( box._genBoxNo() );
 		}
 		
 		box = (Box) createRecordObject(box);
		return box;
	}
    
    public BoxItem createBoxItem(Box box, Double qty, OrderItem oItem, OperationItem opi) {
		BoxItem boxItem = new BoxItem();
		boxItem.setOrderitem(oItem);
		boxItem.setBox(box);
		boxItem.setSku( oItem.getSku() );
		boxItem.setQty(qty);
		if(opi != null && opi.getOpinv() != null) {
			boxItem.copyLotAtt(opi.getOpinv());
		} else {
			boxItem.copyLotAtt(oItem);
		}
		
		int index = box.items.indexOf(boxItem);
		if (index >= 0 ) {
			BoxItem old = box.items.get(index);
			old.setQty( MathUtil.addDoubles(old.getQty(), qty) );
			updateRecordObject(old);
		}
		else {
			createRecordObject(boxItem);
			box.items.add(boxItem);
		}
		
		return boxItem;
	}
    
    // 货品有维护体积、重量，box上的体积重量自动计算
 	public void fixBoxInfo(Box box) {
 		Double qty = 0D, money = 0D, weight = 0D, cube = 0D;
 		Set<String> skus = new HashSet<>();
 		
 		List<BoxItem> items = box.items;
 		for( BoxItem bItem : items ) {
 			_Sku sku = bItem.getSku();
 			Double biQty = bItem.getQty();
			weight = MathUtil.addDoubles(weight, biQty * sku.getWeight());
 			cube   = MathUtil.addDoubles(cube,   biQty * sku.getCube());
 			
 			qty = MathUtil.addDoubles(qty, biQty);
 			OrderItem oItem = bItem.getOrderitem();
 			money = MathUtil.addDoubles(money, oItem.getMoney() * (biQty / oItem.getQty() )  );
 			skus.add( sku.getCode() );
 		}
 		box.setQty( Math.round(qty*100)*1.0 / 100 );
 		box.setMoney(money);
 		box.setSkus(skus.size());
 		box.setWeight( Math.round(weight*1000)*1.0 / 1000 );
 		box.setCube( Math.round(cube*1000)*1.0 / 1000 );
 		
 		update(box);
 	}
}
