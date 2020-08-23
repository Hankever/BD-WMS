/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.modules.cloud.CloudAction;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.AbstractTest4;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.dao.LocationDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryTemp;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;
import com.boudata.wms.inbound.AsnService;
import com.boudata.wms.inventory.InvOperation;

public abstract class AbstractTest4WMS extends AbstractTest4 {
	
	protected _Warehouse W1;
	protected _Warehouse W2;
	protected _Owner OW1;
	protected _Owner OW2;
	
    protected _Location JX_LOC;
    protected _Location MV_LOC;
    protected _Location CC_LOC_1;
    protected _Location CC_LOC_2;
    protected _Location CC_LOC_3;
    
    protected _Location IN_LOC;
    protected _Location OUT_LOC;
    
    protected _Location AGS_LOC;
    protected _Location RGS_LOC;
    
    protected List<_Location> locations;
    
    protected List<_Sku> skuList;
    protected _Sku skuOne;
    
    protected Inventory _ONE_INV;
	
	@Autowired protected ICommonService commservice;
	@Autowired protected AsnService asnService;
	@Autowired protected InvOperation invOperation;
	@Autowired protected OperationDao operationDao;
	@Autowired protected InventoryDao invDao;
	@Autowired protected LocationDao  locDao;
	@Autowired protected CloudAction oAction;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		prepareTestData();
	}
	
    /** 准备好测试的数据 */
    protected void prepareTestData() {
    	
    	W1 = createWarehouse("W-1");
    	W2 = createWarehouse("W-2");
    	OW1 = createOwner("OW-1");
    	OW2 = createOwner("OW-2");
    	OW2.setEmail("xxx");
    	
    	locations = new ArrayList<_Location>();
        
        // 设置库位用途为“拣货库位”
        JX_LOC = createLocation("JX_1", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_PK), W1);
        locations.add(JX_LOC);
        
        // 设置库位用途为“存储库位”
        Param ccLocType = WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_CC);
		CC_LOC_1 = createLocation("CC_1", ccLocType, W1);
		CC_LOC_1.setZone("C");
        locations.add(CC_LOC_1);
        
        CC_LOC_2 = createLocation("CC_2", ccLocType, W1);
        CC_LOC_1.setZone("C");
        locations.add(CC_LOC_2);
        CC_LOC_3 = createLocation("CC_3", ccLocType, W1);
        CC_LOC_1.setZone("C");
        locations.add(CC_LOC_3);
        
        // 收、发库位
        IN_LOC = createLocation("IN_0", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_IN), W1);
        locations.add(IN_LOC);
        OUT_LOC = createLocation("OU_0", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OUT), W1);
        locations.add(OUT_LOC);
        
        // 设置库位用途为“中转库位”
        MV_LOC = createLocation("MV_1", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_MV), W1);
        locations.add(MV_LOC);
        
        // 设置其他用途（快速周转(异常处理库位)、快速周转(退货库位)等）
        Param comboParamOT = WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OT);
        AGS_LOC = createLocation("AGS_0", comboParamOT, W1);
        locations.add(AGS_LOC);
        RGS_LOC = createLocation("RGS_0", comboParamOT, W1);
        locations.add(RGS_LOC);
        
        // SKU
		skuList = new ArrayList<_Sku>();
		for(int i = 0; i < 88; i++ ) {
			_Sku sku = createSku("SKU-" + i);
			skuList.add( sku );
		}
		skuOne = skuList.get(0);
		
		// inv
		_ONE_INV = new Inventory();
		_ONE_INV.setWh(W1);
		_ONE_INV.setOwner(OW1);
		_ONE_INV.setLocation(CC_LOC_1);
		_ONE_INV.setSku(skuList.get(0));
		_ONE_INV.setQty(0D);
		commonDao.create(_ONE_INV);
		log.info( EasyUtils.obj2Json(_ONE_INV) );
    }
    
    protected void printInvs() {
    	printTable(Inventory.class.getName());
    }
    protected void printTable(String t) {
		System.out.println();
		List<?> list = commonDao.getEntities("from " + t);
		for(Object temp : list) {
			System.out.println( temp );
		}
	}
    
    protected void assertInvQty(Double qty, Double qty_locked) {
		String hql = "select sum(qty), sum(qty_locked) from Inventory";
		Object[] result = (Object[]) invDao.getEntities(hql).get(0);
		
		Assert.assertEquals(qty, result[0]);
		Assert.assertEquals(qty_locked, result[1]);
	}
    
    protected void printJSON(String t) {
		System.out.println();
		List<?> list = commonDao.getEntities("from " + t);
		for(Object temp : list) {
			System.out.println( EasyUtils.obj2Json(temp) );
		}
	}
    
    protected void printTime(long start) {
        log.info("-----------------------本步测试耗时：" + (System.currentTimeMillis() - start) + "(毫秒)");
    }
    
    protected void excuteSQL(String sql) {
        commonDao.executeSQL(sql);
    }
    
    protected Long calculateCostTime(Long start, Long expectMaxCostTime) {
        long costTime = System.currentTimeMillis() - start;
        Assert.assertTrue(costTime <= expectMaxCostTime * 1000); 
        
        log.info("------------------本步测试耗时：" + (costTime) + "(ms)");
        return System.currentTimeMillis();
    }
	
	protected _Warehouse createWarehouse(String code) {
		_Warehouse w = new _Warehouse();
		w.setId(null);
		w.setCode(code);
		w.setAddress("Hangzhou JiuBao 1 Street");
		w.setName(code);
		
		commservice.create(w);
		Assert.assertNotNull(w.getId());
		
		commservice.update(w);
		Assert.assertNotNull(w.getId());
		
		return w;
	}

	protected _Owner createOwner(String code) {
		_Owner owner = new _Owner();
		owner.setId(null);
		owner.setCode(code);
		owner.setAddress("Hangzhou JiuBao 1 Street");
		owner.setName(code);
		owner.setRemark("test");
		owner.setMobile("13588833834");
		owner.setEmail("jinhetss@163.com");
		
		commservice.create(owner);
		Assert.assertNotNull(owner.getId());
		
		commservice.update(owner);
		Assert.assertNotNull(owner.getId());
		
		return owner;
	}
	
	protected _Location createLocation(String code, Param type, _Warehouse w) {
		_Location l = new _Location();
		l.setId(null);
		l.setType(type);
		l.setCode(code);
		l.setName(code);
		l.setChecking(0);
		l.setHolding(0);
		l.setLen(1.2d);
		l.setWidth(1.5d);
		l.setHeight(2.0d);
		l.setRemark("test");
		
		String[] a = code.split("-");
		if(a.length == 2) {
			l.setRack(a[0]);
		}
		if(a.length == 3) {
			l.setZone(a[0]);
			l.setRack(a[1]);
		}
		
		l.setWarehouse(w);
		commservice.create(l);
		
		return l;
	}
	
	protected _Sku createSku(String code) {
		_Sku sku = new _Sku();
		sku.setId(null);
		sku.setCode(code);
		sku.setName(code);
		sku.setBarcode("" + code.hashCode());
		sku.setBrand("卜贝");
		sku.setRemark("test");
		
		sku.setSafety_qty( 120D );
		
		sku.setPrice( (double) MathUtil.randomInt(1000) + 10 );
		sku.setPrice0(sku.getPrice() * 0.7);
		sku.setPrice2(sku.getPrice() * 1.1);
		
		sku.setCube(  (double) MathUtil.randomInt(100)  );
		sku.setWeight( (double) MathUtil.randomInt(160) );
		
		sku.setHeight( (double) MathUtil.randomInt(50) );
		sku.setWidth( (double) MathUtil.randomInt(50) );
		sku.setLen( (double) MathUtil.randomInt(50) );
		
		sku.setStatus(1);
		sku.setBrand("仙居东魁");
		sku.setUom("箱");
		sku.setGuige("七斤一箱");
		sku.setShelflife(180);
		sku.setUdf1("udf1");
		sku.setUdf2("udf2");
		sku.setUdf3("udf3");
		sku.setUdf4("udf4");
		
		commservice.create(sku);
		
		return sku;
	}
	
	protected _Rule createRule(String code, String ruleTxt, RuleType ruleType) {
		_Rule rule = new _Rule();
		rule.setId(null);
		rule.setCode(code);
		rule.setName(code);
		rule.setContent(ruleTxt);
		rule.setRemark("test");
		rule.setType(ruleType.toString());
		rule.setStatus(ParamConstants.TRUE);
		
		commservice.create(rule);
		log.info( EasyUtils.obj2Json(rule) );
		
		return rule;
	}
	
    protected Inventory createNewInv(Inventory inv, Double qty, _Location loc, String lotatt1, Object createDate) {
    	
    	InventoryTemp t = new InventoryTemp();
    	t.setPK(null);
    	t.setId(null);
    	t.setWhId( inv.getWh().getId() );
    	t.setOwnerId( inv.getOwner().getId() );
    	t.setSkuId( inv.getSku().getId() );
    	t.setLocationId( loc.getId() );
    	t.setLotatt01( lotatt1 );
    	
    	if(createDate instanceof String) {
    		createDate = DateUtil.parse( createDate.toString() );
    	}
    	t.setCreatedate( (Date) createDate );
    	
    	// 可选字段
    	t.setLocationCode( loc.getCode() );
    	t.setSkuCode( inv.getSku().getCode() );
    	t.setUdf1(null);
		t.setUdf2(null);
		t.setUdf3(null);
		log.info("InventoryTemp: " + EasyUtils.obj2Json(t) );
    	
		Inventory newInv = invOperation.createNewInv(t);
		newInv.setQty(qty);
		invDao.update(newInv);
		
		return newInv;
	}
    
    protected Asn createAsn(String asnNo, _Owner owner, _Warehouse w) {
    	return createAsn(asnNo, owner, w, 2);
    }
    protected Asn createAsn(String asnNo, _Owner owner, _Warehouse w, int num) {
		Asn asn = new Asn();
		asn.setId(null);
		asn.setAsnno(asnNo);
		asn.setOwner(owner);
		asn.setWarehouse(w);
		asn.setAsnday( new Date() );
		asn.setUdf1(null);
		asn.setUdf2(null);
		asn.setUdf3(null);
		asn.setUdf4(null);
		asn.setSupplier("东岭下");
		asn.setRemark("test asn");
		asn.setType("采购入库");
		
		List<AsnItem> items = new ArrayList<AsnItem>();
		Double qty_total = 0D;
		for(int i = 0; i < num; i++) {
			AsnItem item = new AsnItem();
			item.setSku( skuList.get(i+1) );
			item.setQty( MathUtil.randomInt(10)*10D + 10 );
			
			item.setInvstatus("良品");
			item.setLotatt01(i == 0 ? "白色" : "黑色");
			item.setCreatedate( DateUtil.parse("2016-05-0" + i) );
			
			qty_total += item.getQty();
			items.add(item);
		} 
		
		asn.setQty(qty_total);
		asn.setSkus(2);
		asn.setMoney(100000D);
		asn.setWeight(1D);
		asn.setCube(1D);
		
		asnService.createAsn(asn, items);
		asn.items = items;
		
		log.info( EasyUtils.obj2Json(asn) );
		log.info( EasyUtils.obj2Json(items) );
		
		return asn;
	}
}
