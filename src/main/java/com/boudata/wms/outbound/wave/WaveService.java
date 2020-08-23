/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.List;

import com.boubei.tss.modules.log.Logable;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderWave;

public interface WaveService {
	
	@Logable(operateObject="订单波次", operateInfo="新建了订单波次 ${returnVal} ")
	OrderWave createWave(OrderWave wave, List<Long> orderIds);

	OrderWave excutingWave(OrderWave wave, List<Long> orderIds);
	
	@Logable(operateObject="订单波次", operateInfo="取消了订单波次 ${returnVal} ")
	OrderWave cancelWave(Long waveId);
	
	/**
	 * 为波次分配库存，生成拣货指导单
	 * 
	 * @param waveId
	 * @return
	 */
	List<OperationH> allocate(Long waveId);
	
	
	/**
	 * 拆分拣货指导单
	 * 
	 * @param opItemIds
	 * @param waveId
	 * @return
	 */
	List<OperationH> splitPKH(String ordernos, Long opId);
	
	/**
	 * 取消分配
	 */
	List<OrderH> cancelAllocate(Long waveId);
	
	void changeWave(Long waveId, List<Long> orderIds, String type);
	
	/**
	 * 挑选部分拣货明细给特定拣货员（即拣货分组）
	 */
	void assign(Long pkhId, List<OperationItem> pkds, String worker);
}
