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
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.util.DateUtil;
import com.boudata.wms.WMS;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Box;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;

import junit.framework.Assert;

public class BoxPalletTest extends AbstractTest4PK {
	
	@Autowired OrderHDao orderDao;
	@Autowired OrderAction orderAction;
	
	@Test
	public void test1() {
		OrderH order1 = createOrder("SO03", OW1, W1, 1, 20D);
		OrderH order2 = createOrder("SO04", OW1, W1, 1, 20D);
		OperationH pkh1 = orderService.allocate(order1.getId());
		OperationH pkh2 = orderService.allocate(order2.getId());
		
		List<OperationItem> pkds1 = operationDao.getItems(pkh1.getId());
		List<_DTO> list1 = new ArrayList<>();
		for(OperationItem opi : pkds1) {
			_DTO dto = new _DTO();
			dto.opi_id = opi.getId();
			dto.id = opi.getOrderitem().getId();
			dto.inv_id = opi.getOpinv().getId();
			dto.qty_this = 10D;
			list1.add(dto);
		}
		Box box1 = new Box();
		box1.setBoxno(order1.getOrderno() + "-001");
		box1.setQty( 10D );
		orderService.pickupAndCheck(pkh1.getId(), order1.getId(), "fx", list1, box1);
		
		Box box2 = new Box();
		box2.setBoxno(order1.getOrderno() + "-002");
		box2.setQty( 10d );
		orderService.pickupAndCheck(pkh1.getId(), order1.getId(), "fx", list1, box2);
		
		
		List<OperationItem> pkds2 = operationDao.getItems(pkh2.getId());
		List<_DTO> list2 = new ArrayList<>();
		for(OperationItem opi : pkds2) {
			_DTO dto = new _DTO();
			dto.opi_id = opi.getId();
			dto.id = opi.getOrderitem().getId();
			dto.inv_id = opi.getOpinv().getId();
			dto.qty_this = 10D;
			list2.add(dto);
		}
		Box box3 = new Box();
		box3.setBoxno(order2.getOrderno() + "-001");
		box3.setQty( 10D );
		orderService.pickupAndCheck(pkh2.getId(), order2.getId(), "fx", list2, box3);
		
		Box box4 = new Box();
		box4.setBoxno(order2.getOrderno() + "-002");
		box4.setQty( 10d );
		orderService.pickupAndCheck(pkh2.getId(), order2.getId(), "fx", list2, box4);
		
		// 封箱
		Map<String, Object> result = orderService.palletBoxs(box1.getId() +","+ box2.getId() +","+ box3.getId(), "p1");
		Assert.assertEquals("组托成功", result.get("message"));
		List<Box> boxs = orderAction.getBoxsByPallent("p1", W1.getId());
		Assert.assertEquals(3, boxs.size());
		Assert.assertNotNull( boxs.get(0).getPalleter() );
		Assert.assertNull( boxs.get(0).getOutTime() );
		
		result = orderService.palletBoxs(box1.getId() +","+ box2.getId() +","+ box4.getId(), "p1");
		Assert.assertEquals("组托成功", result.get("message"));
		boxs = orderAction.getBoxsByPallent("p1", W1.getId());
		Assert.assertEquals(3, boxs.size());
		Assert.assertEquals(box4, boxs.get(2) );
		
		orderAction.palletOutbound("p1", W1.getId());
		Assert.assertEquals(WMS.O_STATUS_06, order1.getStatus());
		Assert.assertEquals(WMS.O_STATUS_05, order2.getStatus());
		Assert.assertNotNull( box4.getOutTime() );
		
		try {
			orderAction.palletOutbound("p1", W1.getId());
			Assert.fail();
		} 
		catch( Exception e ) {
			Assert.assertEquals( EX.parse(WMS.P_ERR_2, "p1") , e.getMessage());
		}
	}
	
    /** 准备好库存数据。*/
    protected void prepareTestData() {
        super.prepareTestData();
        
        Date today = DateUtil.today();

        createNewInv(_ONE_INV, 100d, CC_LOC_1, "黑色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 200d, CC_LOC_2, "白色", DateUtil.subDays(today, 9));
        createNewInv(_ONE_INV, 300d, CC_LOC_3, "白色", DateUtil.subDays(today, 15));
        
        Asn asn = createAsn("A_001", OW1, W1, 3);
		List<?> items = commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		
		List<AsnItem> list = new ArrayList<>();
		for(Object obj : items ) {
			AsnItem ai = (AsnItem) obj;
			ai.setQty_this( ai.getQty() );
			ai.setLoccode( locations.get(list.size()).getCode() );
			ai.snlist.add("sn199");
			list.add(ai);
		}
		asnService.inbound( asn.getId(), list );
        
        printTable(Inventory.class.getName());
    }
    
}
