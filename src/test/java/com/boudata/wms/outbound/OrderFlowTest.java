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

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;
import com.boudata.wms.RuleType;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.inventory.InventoryService;
import com.boudata.wms.outbound.OrderService;
import com.boudata.wms.outbound.wave.WaveService;

@SuppressWarnings("unchecked")
public class OrderFlowTest extends AbstractTest4PK {
	
	@Autowired protected OrderService orderService;
	@Autowired protected WaveService waveService;
	@Autowired protected InventoryService invService;
	
	@Test
	public void test() {
		
		// 创建Order单
		OrderH order = createOrder("SO01", OW1, W1, 3);
		
		// 删除Order单并重新创建
		orderService.deleteOrder( order.getId() );
		
		order = createOrder("SO02", OW1, W1, 1);
		Long orderId = order.getId();
		
		List<Long> orderIds = new ArrayList<Long>();
		orderIds.add(orderId);
		
		// 创建波次并分配
		OrderWave wave = new OrderWave();
		wave.setId(null);
		wave.setCode( SerialNOer.get("WV") );
		wave.setWarehouse(W1);
		wave = waveService.excutingWave(wave, orderIds);
		
		Assert.assertEquals("已分配", wave.getStatus());
		log.info( EasyUtils.obj2Json(wave) );
		
		List<OperationH> subwaves = operationDao.getSubwaves(wave.getId());
		OperationH sw = subwaves.get(0);
		
		// 按单分配
		waveService.cancelWave(wave.getId());
		sw = orderService.allocate(orderId);
		Assert.assertEquals(WMS.OP_STATUS_01, sw.getStatus());
		
		// 取消分配
		orderService.cancelAllocate(orderId, false);
		commonDao.executeHQL("delete from OperationItem where status = ?", "取消");
//		commonDao.executeHQL("delete from OperationH where status = ?", "分配取消");
		
		// 重新分配
		System.out.println(orderId);
		sw = orderService.allocate(orderId);
		System.out.println(sw.getStatus());
		Assert.assertEquals(WMS.OP_STATUS_01, sw.getStatus());
		
		// 拣货
		orderService.pickup(sw, operationDao.getItems(sw.getId()) );
		
		// 验货
		String hql = "from OperationItem where orderitem.order.id = ?";
		List<OperationItem> items = (List<OperationItem>) operationDao.getEntities(hql, orderId);
		for(OperationItem opItem : items) {
			commonDao.evict(opItem);
			opItem.setLoccode(opItem.getToloccode());
			opItem.setToloccode(null);
		}
		orderService.check(orderId, items, null);
				
		// 出库
		orderService.pickupOutbound( orderId, null );
		// 出库取消
		orderService.cancelOutbound( orderId );
		orderService.cancelOrder(orderId, "test");
		
		// 关闭
		orderService.closeOrder( orderId );
		
		printTable(OrderH.class.getName());
		printTable(OrderItem.class.getName());
		printTable(OperationH.class.getName());
		printTable(OperationItem.class.getName());
		printTable(OperationLog.class.getName());
		printTable(Inventory.class.getName());
		printTable(InventoryLog.class.getName());
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
        createNewInv(_ONE_INV, 300d, CC_LOC_3, "白色", DateUtil.subDays(today, 15));
        
        commonDao.flush();
        
        printTable(Inventory.class.getName());
        
        initRule();
    }
    
    protected void initRule() {
    	String d1 = DateUtil.format(DateUtil.subDays(DateUtil.today(), 10));
    	String pkRuleTxt = 
        		" 9 from 库存表 where 货物批次01 严格 and 生产日期 > {'" +d1+ "'} order by 生产日期 升序 into 候选集1;" +
        		" 20 from (copy)候选集1 where 库位用途 in ( 拣选区 ) order by 库存量 升序, 生产日期 降序 into 最终结果集; " +
        		" 30 from (move)候选集1 where 库位用途 in ( 存储区 ) order by 库存量 降序 into 最终结果集;" +
        		" 40 from 候选集1 order by 库存量 降序 into 最终结果集;";
    	
        _Rule pkRule = createRule("pk-rule1", pkRuleTxt, RuleType.PICKUP_RULE);
        W1.setPickupr(pkRule);
        
        String filePath = URLUtil.getResourceFileUrl("rules/WaveRule_1.txt").getPath();
		String waveRuleTxt = FileHelper.readFile(filePath );
		_Rule waveRule = createRule("wv-rule1", waveRuleTxt, RuleType.WAVE_RULE);
        W1.setWaver(waveRule);
    }
}
