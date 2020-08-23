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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SqlConfig;
import com.boubei.tss.modules.api.BI;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;
import com.boudata.wms.RuleType;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.inbound.AsnService;
import com.boudata.wms.inventory.PkRuleEngine;
import com.boudata.wms.outbound.AbstractTest4PK;
import com.boudata.wms.outbound.OrderService;

public class WaveTest extends AbstractTest4PK {
	
	@Autowired WaveAction waveAction;
	@Autowired AsnService asnService;
	@Autowired OrderService orderService;
	@Autowired BI bi;
	@Autowired InvOperation4Order ipo;
	@Autowired PkRuleEngine pkRuleEngine;
	
	List<Long> orderIds;
	
	 protected void prepareTestData() {
		 super.prepareTestData();
		 
		 initRule();
		 prepareData(1);
	 }
	
	@Test
	public void test1() {
		/* SQL 查不到 Hibernate 写入的数据, so is waveOrderItem */
		Map<String, String> fmParams = new HashMap<String, String>();
		fmParams.put("whId", W1.getId().toString());
		String sql = SqlConfig.getScript("waveOrderSum");
		sql = DMUtil.fmParse(sql, fmParams);
		List<?> rtList = commonDao.getEntitiesByNativeSql(sql);
		Assert.assertEquals(1, rtList.size());
		
		
		OrderWave wave1 = waveAction.createWave(W1.getId(), EasyUtils.list2Str(orderIds));
		Long id = wave1.getId();
		
		waveAction.changeWave(id, EasyUtils.list2Str(orderIds), "delete");
		waveAction.changeWave(id, EasyUtils.list2Str(orderIds), "add");
		
		waveAction.cancelWave(id);
		
		wave1 = waveAction.createWave(W1.getId(), EasyUtils.list2Str(orderIds));
		id = wave1.getId();
		
		List<OperationH> pkhs = waveAction.allocate(id);
		Assert.assertEquals("订单库存分配失败，库存可能不足，详情请查看订单明细备注", pkhs.get(0).getErrorMsg());
		
		Inventory inv = createNewInv(_ONE_INV, 100d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		inv.setInvstatus("良品");
		
		pkhs = waveAction.allocate(id);
		Assert.assertEquals( 1, pkhs.size() );
		Assert.assertNull( pkhs.get(0).getErrorMsg() );
		Assert.assertEquals( 2, pkhs.get(0).items.size() );
		
		try {
			orderService.cancelAllocate( orderIds.get(0), false );
			Assert.fail();
		} catch(Exception e) {
			Assert.assertEquals(WMS.O_ERR_21, e.getMessage());
		}
		
		waveAction.cancelAllocate(id);
		
		// 新建三个出库单，且有足够的库存
		inbound();
		printInvs();
		prepareData(2);
		
		OrderWave wave2 = waveAction.createWave(W1.getId(), EasyUtils.list2Str(orderIds));
		id = wave2.getId();
		pkhs = waveAction.allocate(id);
		Assert.assertEquals( 2, pkhs.size() );
		
		OperationH pkh1 = pkhs.get(0);
		OperationH pkh2 = pkhs.get(1);
		Assert.assertEquals( 2, pkh1.items.size() );
		Assert.assertEquals( 3, pkh2.items.size() );
		
		List<?> opItemIds = operationDao.getEntities("select id from OperationItem where operation = ?", pkh1);
		waveAction.assign(pkh1.getId(), EasyUtils.list2Str(opItemIds), cg.getLoginName());
		
		opItemIds = operationDao.getEntities("select id from OperationItem where operation = ?", pkh2);
		waveAction.assign(pkh2.getId(), EasyUtils.list2Str(opItemIds), worker1.getLoginName());
	}
	
	private List<Long> prepareData(int index) {
		OrderH o1 = super.createOrder(index + "BD_001", OW1, W1, 1, 3D);
		OrderH o2 = super.createOrder(index + "BD_002", OW1, W1, 3, 1D);
		OrderH o3 = super.createOrder(index + "BD_003", OW1, W1, 1, 12D);
		
		orderIds = new ArrayList<>();
		orderIds.add(o1.getId());
		orderIds.add(o2.getId());
		orderIds.add(o3.getId());
		
		return orderIds;
	}
	
	private void inbound() {
		Asn asn = createAsn("A_001", OW1, W1, 3);
		List<?> items = commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		
		List<AsnItem> list = new ArrayList<>();
		for(Object obj : items ) {
			AsnItem ai = (AsnItem) obj;
			ai.setQty_this( ai.getQty() );
			ai.setLoccode( locations.get(list.size()).getCode() );
			
			list.add(ai);
		}
		asnService.inbound( asn.getId(), list );
	}
	
	protected void initRule() {
    	String pkRuleTxt = 
        		" 9 from 库存表 where 货物状态 严格 order by 生产日期 升序 into 候选集1;" +
        		" 20 from (copy)候选集1 where 库位用途 in ( 拣选区 ) order by 库存量 升序 into 最终结果集; " +
        		" 30 from (move)候选集1 where 库位用途 in ( 存储区 ) order by 库存量 降序 into 最终结果集;" +
        		" 40 from 候选集1 order by 库存量 降序 into 最终结果集;";
    	
        _Rule pkRule = createRule("pk-rule1", pkRuleTxt, RuleType.PICKUP_RULE);
        W1.setPickupr(pkRule);
        
        String filePath = URLUtil.getResourceFileUrl("rules/WaveRule_1.txt").getPath();
		String waveRuleTxt = FileHelper.readFile(filePath );
		_Rule waveRule = createRule("wv-rule1", waveRuleTxt, RuleType.WAVE_RULE);
        W1.setWaver(waveRule);
    }
	
	@Test
	public void test2() {
		
		Inventory inv = createNewInv(_ONE_INV, 100d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		inv.setInvstatus("良品");
		
		inbound();
		
		orderIds.add( super.createOrder(2 + "BD_001", OW2, W1, 1, 2D).getId() );
		orderIds.add( super.createOrder(2 + "BD_002", OW2, W1, 3, 1D).getId() );
		orderIds.add( super.createOrder(2 + "BD_003", OW2, W1, 1, 11D).getId() );
		
		orderIds.add( super.createOrder(3 + "BD_001", OW1, W1, 5, 1D).getId() );
		orderIds.add( super.createOrder(3 + "BD_002", OW1, W1, 1, 200D).getId() );
    	
        createRule("pk-rule_2", "10 from 库存表 where 货物状态 严格 and 库位用途 in ( 拣选区 ) order by 生产日期 升序 into 最终结果集;", RuleType.PICKUP_RULE);
        createRule("pk-rule_3", "10 from 库存表 where 货物状态 严格 and 库位用途 in ( 存储区 ) order by 生产日期 升序 into 最终结果集;", RuleType.PICKUP_RULE);
        
        String filePath = URLUtil.getResourceFileUrl("rules/WaveRule_2.txt").getPath();
		String waveRuleTxt = FileHelper.readFile(filePath );
		_Rule waveRule = createRule("wv-rule_2", waveRuleTxt, RuleType.WAVE_RULE);
        W1.setWaver(waveRule);
        commonDao.update(W1);
		
		OrderWave wave = waveAction.createWave(W1.getId(), EasyUtils.list2Str(orderIds));
		List<OperationH> pkhs = waveAction.allocate(wave.getId());
		Assert.assertEquals( 3, pkhs.size() );
//		Assert.assertEquals( 2, pkhs.get(0).getItems().size() );
//		Assert.assertEquals( 3, pkhs.get(1).getItems().size() );
//		Assert.assertEquals( 3, pkhs.get(2).getItems().size() );
	}
	
	@Test
	public void testInvOperation4Order() {
		
		Inventory inv1 = createNewInv(_ONE_INV, 9d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		Inventory inv2 = createNewInv(_ONE_INV, 12d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		
		OrderH order1 = createOrder("BD_001", OW1, W1, 1, 10D);
		OrderH order2 = createOrder("BD_002", OW1, W1, 1, 10D);
		
		List<Long> orderIds = new ArrayList<>();
		orderIds.add(order1.getId());
		orderIds.add(order2.getId());
		List<OrderItem> oiList = orderDao.getOrderItems(orderIds);
		
		Map<Long, List<Inventory>> oi_invs = pkRuleEngine.excuteRulesPool(oiList, "1 from 库存表 into 最终结果集;");
		Map<Long, Double> lastOi_qty  = new HashMap<>();
		Map<Long, Double> lastInv_qty = new HashMap<>();
		lastInv_qty.put(inv1.getId(), inv1.getQty_avaiable());
		lastInv_qty.put(inv2.getId(), inv2.getQty_avaiable());
		
		ipo.prePickup(oiList , oi_invs, lastInv_qty, lastOi_qty);
		ipo.prePickup(oiList , oi_invs, lastInv_qty, lastOi_qty);
	}
	
	@Test
	public void test3() { 
		OrderWave wave = waveAction.createWave(W1.getId(), EasyUtils.list2Str(orderIds));
		Long id = wave.getId();
		
		Inventory inv = createNewInv(_ONE_INV, 100d, CC_LOC_3, "白色", DateUtil.subDays(new Date(), 9));
		inv.setInvstatus("良品");
		
		List<OperationH> pkhs = waveAction.allocate(id);
		Assert.assertEquals( 1, pkhs.size() );
		
		// 拣货单拆分
		Long opId = pkhs.get(0).getId();
		String ordernos = orderService.getOrder(orderIds.get(0)).getOrderno();
		pkhs = waveAction.splitPKH(ordernos, opId);
		Assert.assertEquals( 2, pkhs.size() );
//		System.out.println(orderIds);
	}
}
