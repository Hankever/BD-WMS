/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms;

import java.util.List;

import org.apache.log4j.Logger;

import com.boubei.tss.modules.param.ParamConfig;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;

public class _OpEvent implements AfterOpEvent {
 
	Logger log = Logger.getLogger(this.getClass());

	public void excute(OperationH op, List<OperationItem> items) {
		log.debug("------------------- AfterOpEnvet excuting ----------------------");
	}

	public void afterDeleteAsn(Asn asn) {
	
	}

	public void afterDeleteOrder(OrderH order) {
		
	}

	public void afterCloseAsn(Asn asn, String oldStatus) {
		
	}

	public void afterCloseOrder(OrderH order, String oldStatus) {
		
	}

	public static AfterOpEvent create() {
		String afterOpEnvet = ParamConfig.getAttribute("sys_op_event");
    	return create(afterOpEnvet);
	}
	
	public static AfterOpEvent create(String evClazz) {
		evClazz = (String) EasyUtils.checkNull(evClazz, _OpEvent.class.getName());
		try {
			return (AfterOpEvent) BeanUtil.newInstanceByName(evClazz);
		}
    	catch(Exception e) {
    		return new _OpEvent();
    	}
	}
}
