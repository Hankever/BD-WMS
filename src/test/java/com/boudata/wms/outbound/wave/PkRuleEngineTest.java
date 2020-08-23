/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.dm.record.RecordField;
import com.boubei.tss.util.DateUtil;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.inventory.PkRuleEngine;
import com.boudata.wms.outbound.AbstractTest4PK;

import junit.framework.Assert;

public class PkRuleEngineTest extends AbstractTest4PK {
	
	@Autowired protected OrderHDao orderDao;
	@Autowired protected PkRuleEngine engine;

	/** 
	 * 测试拣货分配流程：严格批次、先生产先出、先拣选后存储、拣选位小库位优先、存储位大库位优先
	 */
	@Test
    public void test1() {
		RecordField f = new RecordField();
		f.setCode("lotatt03");
		f.setLabel("尺码");
		orderDao.createObject(f);
		
    	String ruleTxt = 
        		" 9 from 库存表 where 货物状态 严格 and 货物批次01 严格 and 批号 严格 and 装箱量 严格 and 生产日期 > {'2018-07-10'} and 过期日期 < {'2020-07-10'} and 库存量 >= 订单量 order by 生产日期 升序, 过期日期 升序, 库存量 升序 into 候选集1;" +
        		" 20 from (copy)候选集1 where 库位用途 in ( 拣选区 ) and 生产日期 > {'2018-07-10'} and 库存量 >= 订单量 order by 库存量 升序, 生产日期 降序, 过期日期 升序 into 最终结果集; " +
        		" 15 from (copy)xxx集1 into 最终结果集; " +
        		" 30 from (move)候选集1 where 库位用途 in ( 存储区 ) and 过期日期 > {'2018-07-10'} and 货物状态 严格 and 货物批次01 严格 and 批号 严格 and 装箱量 严格 order by 库存量 降序 into 最终结果集;;;" +
        		" 35 from 候选集1 where 货物状态 严格 into 最终结果集;" + 
        		" 40 from 候选集1 into 最终结果集;";
        
    	OrderH o1 = createOrder("SO-001", OW1, W1, 1);
    	
		List<OrderItem> oItems = (List<OrderItem>) orderDao.getOrderItems(o1.getId());
    	
    	Map<Long, List<Inventory>> rt = engine.excuteRulesPool(oItems, ruleTxt);
    	for(Long soiId : rt.keySet()) {
    		List<Inventory> list = rt.get(soiId);
    		for(Inventory inv : list) {
    			System.out.println("	" + inv);
    		}
    	}
    	
    	ruleTxt = " 9 from 库存表 where 生产日期 > {'xxxx'} into 最终结果集;"; // 带有错误的SQL
    	try {
    		engine.excuteRulesPool(oItems, ruleTxt);
    		Assert.fail();
    	} 
    	catch(Exception e) {
    		Assert.assertTrue( e.getMessage().indexOf("could not execute query") >= 0 );
    	}
    }
    
    /** 准备好库存数据。*/
    protected void prepareTestData() {
        super.prepareTestData();
        
        // 清空货物状态，用以测试 llu.货物状态 严格， 是否在支持 null(sod) 对 null(llu)
        _ONE_INV.setInvstatus(null); 
        
        Date today = DateUtil.today();
        _ONE_INV.setCreatedate(DateUtil.subDays(today, 9)); // 生产日期为9天前
        _ONE_INV.setExpiredate(DateUtil.subDays(today, -365)); // 过期时间为12个月
        
        createNewInv(_ONE_INV, 1d, JX_LOC, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 1d, JX_LOC, "白色", DateUtil.subDays(today, 11));
        createNewInv(_ONE_INV, 2d, JX_LOC, "黑色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 3d, JX_LOC, "红色", DateUtil.subDays(today, 9));
        
        createNewInv(_ONE_INV, 4d, MV_LOC, "白色", DateUtil.subDays(today, 9));
        
        createNewInv(_ONE_INV, 90d, CC_LOC_1, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 150d, CC_LOC_1, "黑色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 200d, CC_LOC_2, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 300d, CC_LOC_3, "白色", DateUtil.subDays(today, 11));
        
        commonDao.flush();
    }

}
