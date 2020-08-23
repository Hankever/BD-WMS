/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inbound;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.util.MathUtil;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.inventory.InventoryService;

@SuppressWarnings("unchecked")
public class AsnFlowTest extends AbstractTest4WMS {
	
	@Autowired protected AsnService asnService;
	@Autowired protected InventoryService invService;
	@Autowired protected AsnDao asnDao;
	
	@Test
	public void test() {
		
		// 1、创建Asn单
		Asn asn = createAsn("ASN-001", OW1, W1);
		
		// 2、取消订单
		asnService.cancelAsn(asn.getId(), "取消入库单");
		
		// 3、指派入库员
		asnService.assign(asn.getId(), "13735547815", WMS.OP_IN);
		
		// 4、删除Asn单并重新创建
		asnService.deleteAsn( asn.getId() );
		asn = createAsn("ASN-002", OW1, W1);
		
		// 5、清点数量，执行入库
		List<AsnItem> items = (List<AsnItem>) commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		for(AsnItem ai : items ) {
			ai.setQty_this( ai.getQty() - 1 );
		}
		asnService.inbound( asn.getId(), items );
		
		// 6、取消入库
		asnService.cancelInbound( asn.getId() );
		
		try { Thread.sleep(1000); } catch (InterruptedException e) { }
		// 7、重新清点数量，执行入库
		
		items = (List<AsnItem>) commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		for(AsnItem ai : items ) {
			ai.setQty_this( ai.getQty() );
		}
		asnService.inbound( asn.getId(), items );
		
		// 8、关闭Asn单子
		asnService.closeAsn( asn.getId() );
		
		// 9、上架（使用库内移动）
		List<OperationItem> preItems = asnService.prePutaway(asn.getId(), new ArrayList<_Rule>());
		
		// 上架可以直接一次移库操作（收货区-->上架库位）；也可以操作两次移库，收货区-->中转容器-->上架库位
		asnService.putaway(asn, preItems);
		
		// 10、小程序入库
		asn = asnService.getAsn(asn.getId());
		asn.setStatus(WMS.ASN_STATUS_03);
		asnDao.update(asn);
		
		asn = asnService.getAsn(asn.getId());
		List<AsnItem> itemList = new ArrayList<AsnItem>();
		Double qty = 2.0;
		
		AsnItem ai = new AsnItem();
		ai.setSku(skuList.get(0));
		ai.setQty(qty);
		ai.setQty_this(qty);
		ai.setLoccode(IN_LOC.getCode());
		itemList.add(ai);
		
		asn.setSkus( asn.getSkus() + 1 );
		asn.setQty( MathUtil.addDoubles(asn.getQty(), qty) );
		
		asnService.createAndInbound(asn, itemList);
		
		printTable(Asn.class.getName());
		printTable(AsnItem.class.getName());
		printTable(Inventory.class.getName());
		printTable(OperationH.class.getName());
		printTable(OperationItem.class.getName());
		printTable(OperationLog.class.getName());
		printTable(InventoryLog.class.getName());
	}

	
}
