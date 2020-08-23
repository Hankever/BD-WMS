/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.List;

import com.boubei.tss.modules.param.Param;
import com.boudata.wms.WMS;
import com.boudata.wms._OpEvent;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;

/**
 * 标准库内作业事件监听
 */
public class InvOpEvent extends _OpEvent {
	
	public void excute(OperationH op, List<OperationItem> items) {
		Param optype = op.getOptype();
		
		// 库存发生调整
		if( WMS.opType( WMS.OP_TYPE_TZ ).equals(optype) ) {
			// 通知仓库主管
			
			return;
		}
	}

}
