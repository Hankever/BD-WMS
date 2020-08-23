/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Warehouse;
import com.boudata.wms.outbound.OrderService;

@Controller("WaveAction")
@RequestMapping("/wave")
public class WaveAction {
	
	@Autowired private WaveService waveService;
	@Autowired private OrderService orderService;
	@Autowired private OrderHDao orderDao;
	@Autowired private OperationDao operationDao;
	
	@RequestMapping(value = "/create", method = RequestMethod.POST)
	@ResponseBody
	public OrderWave createWave(Long whId, String order_ids) {
		OrderWave wave = new OrderWave();
		wave.setCode( SerialNOer.get("Wxxxx") );
		wave.setWarehouse( new _Warehouse(whId) ); 
		wave.setOrigin(WMS.W_ORIGIN_02);
		
		List<Long> orderIds = new ArrayList<>();
		List<String> ids = EasyUtils.toList(order_ids);
		for(String id : ids) {
			orderIds.add( Long.parseLong(id) );
		}

		return waveService.createWave(wave, orderIds);
	}
	
	@RequestMapping(value = "/cancel/{id}", method = RequestMethod.POST)
	@ResponseBody
	public OrderWave cancelWave(@PathVariable Long id) {
		return waveService.cancelWave(id);
	}
	
	@RequestMapping(value = "/allocate/{id}", method = RequestMethod.POST)
	@ResponseBody
	public List<OperationH> allocate(@PathVariable Long id) {
		return waveService.allocate(id);
	}
	
	@RequestMapping(value = "/split_pkh", method = RequestMethod.POST)
	@ResponseBody
	public List<OperationH> splitPKH(String ordernos, Long opId) {
		return waveService.splitPKH(ordernos, opId);
	}
	
	@RequestMapping(value = "/cancelAllocate/{id}", method = RequestMethod.POST)
	@ResponseBody
	public List<OrderH> cancelAllocate(@PathVariable Long id) {
		return waveService.cancelAllocate(id);
	}
	
	@RequestMapping(value = "/change", method = RequestMethod.POST)
	@ResponseBody
	public void changeWave(Long waveId, String oIds, String type) {
		List<Long> orderIds =  _Util.ids2List( oIds );
		
		waveService.changeWave(waveId, orderIds, type);
	}
	
	@RequestMapping(value = "/assign", method = RequestMethod.POST)
	@ResponseBody
	public void assign(Long pkhId, String pkdIds, String worker) {
		List<OperationItem> pkds = operationDao.getItems(" from OperationItem where id in (" + pkdIds + ")");
		waveService.assign(pkhId, pkds, worker);
	}
	
	/**
	 * 不走拣货流程，直接按波次出库
	 */
	@RequestMapping(value = "/outbound/{id}", method = RequestMethod.POST)
	@ResponseBody
	public List<String> outboundByWave(@PathVariable Long id) {
		List<OrderH> orderList = orderDao.getOrderByWave(id);
		List<Long> orderIds = new ArrayList<>();
		for(OrderH order : orderList) {
			orderIds.add(order.getId());
		}
		return outboundBatch( EasyUtils.list2Str(orderIds) );
	}
	
	/**
	 * 直接选定一批订单出库
	 */
	@RequestMapping(value = "/outboundBatch", method = RequestMethod.POST)
	@ResponseBody
	public List<String> outboundBatch(String orderIds) {
		List<Long> ids = _Util.ids2List(orderIds);
		
		List<String> rtList = new ArrayList<>();
		for(Long orderId : ids) {
			try {
				orderService.outboundDirect(orderId);
			} 
			catch (Exception e) {
				// 记录出库失败的订单，及失败原因
				OrderH order = orderDao.getEntity(orderId);
				rtList.add( "【" +order.getOrderno()+ "】" + EasyUtils.checkNull(order.getTag(), e.getMessage()));
				e.printStackTrace();
			}
		}
		
		return rtList;
	}
	
	/**
	 * 不走验货流程，拣货完直接按波次验货完成（+自动出库，如配置为自动验货出库）
	 */
	@RequestMapping(value = "/check/{id}", method = RequestMethod.POST)
	@ResponseBody
	public void checkByPKH(@PathVariable Long id) {
		orderService.checkByPKH(id);
	}
}
