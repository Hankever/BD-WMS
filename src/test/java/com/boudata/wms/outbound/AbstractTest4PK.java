/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Warehouse;
import com.boudata.wms.outbound.OrderService;

public class AbstractTest4PK extends AbstractTest4WMS { 
	
	@Autowired protected OrderService orderService;
	@Autowired protected OrderHDao orderDao;
        
    /**
     * 创建一个SO单，包含num个SOD
     * @return
     */
	protected OrderH createOrder(String orderNo, _Owner owner, _Warehouse w, int num) {
		return createOrder(orderNo, owner, w, num, null);
	}
	protected OrderH createOrder(String orderNo, _Owner owner, _Warehouse w, int num, Double qty) {
		OrderH order1 = new OrderH();
		order1.setId(null);
		order1.setOrderno(orderNo);
		order1.setOwner(owner);
		order1.setWarehouse(w);
		order1.setOrderday( new Date() );
		order1.setD_receiver("JK");
		order1.setD_mobile("13398761212");
		order1.setD_addr("hanghou 1 street");
		order1.setD_province("浙江省");
		order1.setD_city("杭州市");
		order1.setD_district("下沙");
		order1.setD_street("一号大街");
		order1.setRemark("test order");
		order1.setUdf1(null);
		order1.setUdf2(null);
		order1.setUdf3(null);
		order1.setUdf4(null);
		
		List<OrderItem> items = new ArrayList<OrderItem>();
		for(int i = 0; i < num; i++) {
			OrderItem item = new OrderItem();
			item.setId(null);
			item.setSku( skuList.get(i) );
			
			qty = (Double) EasyUtils.checkNull(qty, MathUtil.randomInt(10)*3D + 10);
			item.setQty( qty );
			
			item.setInvstatus("良品");
			item.setLotatt01("白色");
			
			item.setQty_allocated(0D);
			item.setMoney(88D);
			
			items.add(item);
		} 
		
		orderService.createOrder(order1, items);
		
		return order1;
	}
       
}
