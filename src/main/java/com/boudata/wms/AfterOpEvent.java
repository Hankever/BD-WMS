/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */
package com.boudata.wms;

import java.util.List;

import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;

/**
 * 工单作业完成后自定义事件
 */
public interface AfterOpEvent {
	
	void excute(OperationH op, List<OperationItem> items);
	
	void afterDeleteAsn(Asn asn);
	
	void afterDeleteOrder(OrderH order);
	
	void afterCloseAsn(Asn asn, String oldStatus);
	
	void afterCloseOrder(OrderH order, String oldStatus);

}
