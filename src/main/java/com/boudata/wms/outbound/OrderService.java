/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound;

import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.modules.log.Logable;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;

public interface OrderService {
	
	OrderH getOrder(Long id);
	
	PageInfo search(OrderSo so);
	
	@Logable(operateObject="出库单", operateInfo="新建：${returnVal} ")
	OrderH createOrder(OrderH order, List<OrderItem> items);

	@Logable(operateObject="出库单", operateInfo="删除：${args[0]} : ${returnVal} ")
	OrderH deleteOrder(Long id);
	
	/**
	 * 查询 出库单明细
	 * 
	 * @param opId
	 * @return
	 */
	List<OrderItem> fetchOrderItems(Long orderId);
	
	/**
	 * 网页 出库单指派出库或拣货作业人
	 * 
	 * @param orderId
	 * @param worker
	 */
	@Logable(operateObject="出库单", operateInfo="指派出库工单 ${returnVal} ")
	OperationH assign(Long orderId, String worker, String type, Long opId);
	
	/**
	 * 小程序 作业单出库操作
	 * 
	 * @param orderId
	 * @param opId
	 * @param list
	 */
	void outbound4rf(Long orderId, Long opId, List<_DTO> list);
	
	/**
	 * 为单个出库单分配库存，生成拣货指导单
	 */
	@Logable(operateObject="出库单", operateInfo="出库单分配: ${args[0]} ")
	OperationH allocate(Long orderId);
	
	/**
	 * 取消分配
	 * 
	 * @param orderId
	 * @param outCancel 外部订单取消
	 * @return
	 */
	@Logable(operateObject="出库单", operateInfo="出库单分配取消: ${args[0]} ")
	OrderH cancelAllocate(Long orderId, boolean outCancel);

	/**
	 * 拣货单确认
	 */
	void pickup(OperationH sw, List<OperationItem> pkds);
	void cancelPickup(Long opId, String pkdIds);
	
	/**
	 * 验货
	 * 
	 * @param orderId 
	 * @param items 拣货作业明细拷贝
	 * @return
	 */
	OperationH check(Long orderId, List<OperationItem> items, String snlist);
	void checkByPKH(Long pkhId);
	
	/**
	 * 网页 验货出库 封箱
	 * 
	 * @param orderId
	 * @param items
	 * @param boxMap {boxno: "", qty: ,skus: ,money: }
	 */
	OperationH checkAndBox(Long orderId, List<_DTO> list, Box box, String snlist, int scanfactor);
	
	/**
	 * 验货出库 取消封箱
	 * 
	 * @param orderId
	 * @param boxId
	 */
	void cancelCheckAndBox(Long orderId, Long boxId);
	
	/**
	 * 小程序 拣货+验货/拣货+封箱
	 * 
	 * @param opId
	 * @param orderId
	 * @param opType
	 * @param list
	 */
	void pickupAndCheck(Long opId, Long orderId, String opType, List<_DTO> list, Box box);
	
	/**
	 * 箱绑定成托
	 * 
	 * @param boxIds
	 * @param pallet
	 */
	Map<String, Object> palletBoxs(String boxIds, String pallet);
	
	/**
	 * 按托出库
	 */
	void palletOutbound(String pallet, Long whId);
	
	/**
	 * 小程序/RF 无单出库
	 * 
	 * @param order
	 * @param itemList
	 */
	void createAndOutbound(OrderH order, List<_DTO> list);

	/**
	 * 直接出库
	 */
	void outbound(Long orderId, List<_DTO> list);
	
	/**
	 * 拣货验货出库
	 */
	void pickupOutbound(Long orderId, String snlist);
	
	void outboundDirect(Long orderId);

	void cancelOutbound(Long orderId);
	
	/**
	 * 取消出库单
	 */
	@Logable(operateObject="出库单", operateInfo="取消: ${returnVal} ")
	OrderH cancelOrder(Long orderId, String reason);

	/**
	 * 关闭出库单
	 */
	@Logable(operateObject="出库单", operateInfo="关闭: ${returnVal} ")
	OrderH closeOrder(Long orderId);
}
