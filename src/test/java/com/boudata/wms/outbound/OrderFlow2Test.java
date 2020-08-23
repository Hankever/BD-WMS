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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.FileHelper;
import com.boubei.tss.util.URLUtil;
import com.boudata.wms.RuleType;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Box;
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
import com.boudata.wms.outbound.wave.WaveService;

public class OrderFlow2Test extends AbstractTest4PK {
	
	@Autowired protected OrderService orderService;
	@Autowired protected WaveService waveService;
	@Autowired protected InventoryService invService;
	@Autowired protected OperationDao operationDao;
	@Autowired protected OrderHDao orderHDao;
	
	@Test
	public void test() {
		
		// 1、创建Order单
		OrderH order = createOrder("SO01", OW1, W1, 3);
		
		// 2、取消订单
		orderService.cancelOrder(order.getId(), "取消出库单");
		
		// 3、指派出库员
		orderService.assign(order.getId(), "13735547815", WMS.OP_TYPE_OUT, null);
		// 3.1、重新指派出库员
		orderService.assign(order.getId(), "13735547815,123", WMS.OP_TYPE_OUT, null);
		
		// 4、删除Order单并重新创建
		orderService.deleteOrder( order.getId() );
		order = createOrder("SO02", OW1, W1, 1);

		List<Long> orderIds = new ArrayList<Long>();
		Long orderId = order.getId();
		orderIds.add(orderId);
		
		// 5、创建波次并分配
		OrderWave wave = new OrderWave();
		wave.setId(null);
		wave.setCode( SerialNOer.get("WV") );
		wave.setWarehouse(W1);
		wave = waveService.excutingWave(wave, orderIds);
		
		List<OperationH> subwaves = operationDao.getSubwaves(wave.getId());
		OperationH sw = subwaves.get(0);
		
		// 6、取消自主创建波次分配
		waveService.cancelWave(wave.getId());
		 
		// 7、重新按单分配
		sw = orderService.allocate(orderId);
		
		// 8、取消分配
		orderService.cancelAllocate(orderId, false);
		commonDao.executeHQL("delete from OperationItem where status = ?", "取消");
		commonDao.executeHQL("delete from OperationH where status = ?", "分配取消");
		
		// 9、重新分配
		sw = orderService.allocate(orderId);
		
		// 9.1、指派拣货员
		orderService.assign(order.getId(), "13735547815", WMS.OP_TYPE_JH, sw.getId());
		
		// 10、拣货
		List<OperationItem> items = operationDao.getItems(sw.getId());
		orderService.pickup(sw, items);
		
		// 11、取消拣货
		String pkdIds = EasyUtils.list2Str(_Util.getIDs(items));
		orderService.cancelPickup(sw.getId(), pkdIds);
		
		// 12、重新拣货
		orderService.pickup(sw, items);
		
		// 13、验货
		sw.items.clear();
		orderService.check(orderId, items, null);
				
		// 14、出库
		orderService.pickupOutbound( orderId, null );
		
		// 15、出库取消
		orderService.cancelOutbound( orderId );
		orderService.cancelOrder(orderId, "test");
		
		// 16.1、重新新的Order单
		OrderH order_ = createOrder("SO002", OW1, W1, 1);
		orderId = order_.getId();

		// 16.2、按单分配
		OperationH sw_ = orderService.allocate( orderId );
		
		// 16.3、拣货验货
		orderService.pickupAndCheck(sw_.getId(), orderId, "yh", null, null);
				
		// 16.4、出库
		orderService.pickupOutbound(orderId, null);
		
		orderService.cancelOutbound( orderId );
		orderService.cancelOrder(orderId, "test");
		
		// 17.1、重新新的Order单
		OrderH order_1 = createOrder("SO005", OW1, W1, 1);
		orderId = order_1.getId();

		// 17.2、按单分配
		OperationH sw_1 = orderService.allocate( orderId );
		
		items = operationDao.getItems(sw_1.getId());
		List<_DTO> items_ = new ArrayList<>();
		Double qty = 0D;
		for(OperationItem opi : items) {
			qty += opi.getQty();
			_DTO dto = new _DTO();
			dto.opi_id = opi.getId();
			dto.id = opi.getOrderitem().getId();
			dto.inv_id = opi.getOpinv().getId();
			dto.qty_this = opi.getQty();
			items_.add(dto);
		}
		Box box = new Box();
		box.setBoxno(order_1.getOrderno() + "-001");
		box.setQty(qty);
		box.setQty(1D);
		
		// 17.3、拣货封箱
		orderService.pickupAndCheck(sw_1.getId(), orderId, "fx", items_, box); // super.printTable("OperationItem")
				
		// 17.4、取消封箱
		List<Box> boxs = orderHDao.getBoxsByOrder(orderId);
		orderService.cancelCheckAndBox(orderId, boxs.get(0).getId());
		
		// 17.5、重新拣货封箱
		box.setId(null);
		orderService.pickupAndCheck(sw_1.getId(), orderId, "fx", items_, box);
		
		// 17.6、封箱绑定成托
		boxs = orderHDao.getBoxsByOrder(orderId);
		orderService.palletBoxs(boxs.get(0).getId().toString(), "T1");
		
		// 17.7、按托出库
		boxs = orderHDao.getBoxsByOrder(orderId);
		orderService.palletOutbound(boxs.get(0).getPallet(), W1.getId());
		
		orderService.cancelOutbound( orderId );
		orderService.cancelOrder(orderId, "test");
		
		// 18.1、重新新的Order单
		OrderH order_2 = createOrder("SO004", OW1, W1, 1);
		orderId = order_2.getId();

		// 18.2、按单分配
		OperationH sw_2 = orderService.allocate( orderId );
		
		// 18.3、拣货
		items = operationDao.getItems(sw_2.getId());
		orderService.pickup(sw_2, items);
		
		// 18.4、验货再出库
		order = (OrderH) commonDao.getEntity(OrderH.class, orderId);
		order.setStatus(WMS.O_STATUS_05);
		commonDao.update(order);
		orderService.pickupOutbound( orderId, null );
		
		orderService.cancelOutbound( orderId );
		orderService.cancelOrder(orderId, "test");
		
		// 19、关闭订单
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
        createNewInv(_ONE_INV, 300d, CC_LOC_2, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 500d, CC_LOC_3, "白色", DateUtil.subDays(today, 15));
        
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
