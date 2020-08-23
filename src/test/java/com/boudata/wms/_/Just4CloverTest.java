/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._;

import org.junit.Test;

import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;

public class Just4CloverTest {
	
	@Test
	public void test() {
		AsnItem ai = new AsnItem();
		ai.setId(null);
		ai.setMoney(100000D);
		
		OperationH h = new OperationH();
		h.setId(null);
		h.setUdf1(null);
		h.setUdf2(null);
		h.setUdf3(null);
		
		OperationItem item = new OperationItem();
		item.setId( null );
		item.toString();
		
		InventoryLog ilog = new InventoryLog();
		ilog.setId(null);
		ilog.setWriteBack(false);
		
		System.out.println( EasyUtils.obj2Json(h));
		System.out.println( EasyUtils.obj2Json(ilog));
		System.out.println( EasyUtils.obj2Json(new OperationLog()));
	}

}
