/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._edi;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity._Customer;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;

@Service("ImportService")
public class ImportServiceImpl implements ImportService {
	
	@Autowired EDIDao ediDao;
	@Autowired ICommonDao commDao;
	@Autowired OrderHDao orderDao;
	@Autowired AsnDao asnDao;

	public String importAsn(Long warehouse_id, Long owner_id, List<Map<String, String>> rows, Progress progress) {
		String domain = Environment.getDomainOrign();
		
		_Owner owner = (_Owner) commDao.getEntity(_Owner.class, owner_id);
		String _code = _Util.genDocNO(owner, "1", true); // 如果没有单号，则默认划归到一个统一的单子里
		int total = rows.size(), newSkuCount = 0;
		Set<Long> skuSet = new HashSet<Long>();
		
		checkCodes(rows, total, "asnno");
		
		Map<String, Asn> asnMap = new LinkedHashMap<String, Asn>();
		for(int index = 0; index < total; index++) {
			
			if (index % 10 == 0 || index == total) {
				progress.add(10); // 进度加10
			}
			
			Map<String, String> row = rows.get(index);

			String _day = row.get("asnday");
			if( _day != null ) {
				_code = owner.getId() + DateUtil.format( DateUtil.parse(_day) ) ;
			}
			String code = (String) EasyUtils.checkNull(row.remove("asnno"), _code);
			
			
			String skuName = row.get("skuname");
			String skuCode = row.get("skucode");
			String  barcode= row.get("barcode");
			Double qty = EasyUtils.obj2Double(row.remove("qty"));
			Map<String, String> skuAttibutes = new HashMap<>();
			skuAttibutes.put("category", row.get("sku_category"));
			skuAttibutes.put("brand", row.get("sku_brand"));
			skuAttibutes.put("shelflife", row.get("sku_shelflife"));
			skuAttibutes.put("uom", row.get("sku_uom"));
			skuAttibutes.put("guige", row.get("sku_guige"));
			skuAttibutes.put("udf1", row.get("sku_udf1"));
			skuAttibutes.put("udf2", row.get("sku_udf2"));
			skuAttibutes.put("udf3", row.get("sku_udf3"));
			skuAttibutes.put("udf4", row.get("sku_udf4"));

			Asn asn = asnMap.get(code);
			
			// 查询数据库里同单号是否已存在
			if( asn == null ) {
				asn = asnDao.getAsn(code);
				if( asn != null ) {
					asnMap.put(code, asn);
					asn.items.clear();
					
					if( !WMS.ASN_STATUS_01.equals(asn.getStatus()) && !WMS.ASN_STATUS_02.equals(asn.getStatus()) ) {
						throw new BusinessException("入库单【" +code+ "】已存在并已经开始作业，重新导入需先取消");
					}
					
					// 清除已有的明细数据
					List<AsnItem> items = asnDao.getItems(asn.getId());
					commDao.deleteAll(items);
					
					asn.setQty(0D);
					asn.setSkus(0);
					asn.setMoney(0D);
				}
			}
			
			if( asn == null ) {
				asnMap.put(code, asn = new Asn());
				asn.setAsnno(code);
				asn.setStatus(WMS.ASN_STATUS_01);
				asn.setDomain( domain );
			}
			
			String payer_id = row.get("payer_id");
			if( payer_id != null ) {
				asn.setPayer( (_Customer) commDao.getEntity(_Customer.class,  EasyUtils.obj2Long(payer_id) ) );
			}
			
			asn.setOwner( owner );
			asn.setWarehouse( new _Warehouse(warehouse_id) );
			Date asnDay = (Date) EasyUtils.checkNull(DateUtil.parse( row.remove("asnday") ), DateUtil.today());
			asnDay = (Date) EasyUtils.checkTrue( asnDay.before(DateUtil.subDays(DateUtil.today(), 366)), DateUtil.today(), asnDay);
			asn.setAsnday( asnDay );
			BeanUtil.setDataToBean(asn, row, true);
			
			if( EasyUtils.isNullOrEmpty(skuCode) ) continue; // 只有单头，没有明细（YC清单导入）
			
			// 导单同时新建商品
			_Sku sku = ediDao.checkSku(domain, owner_id, skuName, skuCode, barcode, skuAttibutes);
			if(sku.isNew) {
				newSkuCount++;
			}
			skuSet.add(sku.getId());
			
			AsnItem item = new AsnItem();
			item.setAsn(asn);
			item.setSku(sku);
			item.setQty(qty);
			item.setLoccode( row.get("loccode") ); // 批量导入入库单，可以指定库位；新库初始化时有用，或先入库后补单子
			item.setPrice( EasyUtils.obj2Double( row.get("price") ) );
			item.setMoney( EasyUtils.obj2Double( row.get("money") ) );
			item.copyLotAtt( row );
			
			asn.setSkus(asn.getSkus() + 1);
			asn.setQty( MathUtil.addDoubles(asn.getQty(), item.getQty()) );
			asn.setMoney( MathUtil.addDoubles(asn.getMoney(), item.getMoney()) );
			asn.items.add(item);
		}
		
		int asnCount = 0, updateCount = 0, itemCount = 0;
		for( Asn asn : asnMap.values() ) {
			asnCount++;
			if(asn.getId() == null) {
				commDao.createWithLog(asn);
			} else {
				commDao.updateWithLog(asn);
				updateCount++;
			}
			
			for( AsnItem item : asn.items ) {
				commDao.create(item);
				itemCount++;
			}
		}
		
		// 向前台返回成功信息
		return "parent.alert('共导入明细" +itemCount+ "行，合计订单" +asnCount+ "个，其中覆盖已存在订单" +updateCount+ "个；含货品项" +skuSet.size()+ "个，其中新增" +newSkuCount+ "个');"
				+ " parent.$('.progressBar').hide(); parent.prepareSKUs(parent.queryAsn);";
	}

	private void checkCodes(List<Map<String, String>> rows, int total, String codeName) {
		// 检查单号，不允许部分记录单号为空（要么所有为空）
		List<Integer> emptyCodeIndexs = new ArrayList<>();
		for(int index = 0; index < total; index++) {
			Map<String, String> row = rows.get(index);
			if( EasyUtils.isNullOrEmpty( row.get(codeName) ) ) {
				emptyCodeIndexs.add(index + 1);
			}
		}
		if( emptyCodeIndexs.size() > 0 && emptyCodeIndexs.size() < total ) {
			throw new BusinessException( EX.parse(WMS.EDI_ERR_1, emptyCodeIndexs) );
		}
	}
	
	public String importOrder(Long warehouse_id, Long owner_id, List<Map<String, String>> rows, Progress progress) {
		String domain = Environment.getDomainOrign();
		
		_Owner owner = (_Owner) commDao.getEntity(_Owner.class, owner_id);
		String _code = _Util.genDocNO(owner, "0", true); // 如果没有单号，则默认划归到一个统一的单子里
		int total = rows.size(), newSkuCount = 0;
		Set<Long> skuSet = new HashSet<Long>();
		
//		checkCodes(rows, total, "orderno");
		
		Map<String, String> d_map = new HashMap<String, String>();
		Map<String, OrderH> orderMap = new LinkedHashMap<String, OrderH>();
		for(int index = 0; index < total; index++) {
			
			if (index % 10 == 0 || index == total) {
				progress.add(10); // 进度加10
			}
			
			Map<String, String> row = rows.get(index);
			
			String code = row.remove("orderno");
			if( EasyUtils.isNullOrEmpty(code) ) {
				// 判断收件人和收件地址是否同一个，不同的话建多个订单
				String d_receiver = row.get("d_receiver");
			    String d_mobile = row.get("d_mobile");
			    String d_addr = row.get("d_addr");
			    if( !EasyUtils.isNullOrEmpty(d_receiver) || !EasyUtils.isNullOrEmpty(d_addr) ) {
			    	String d_info = d_receiver + ":" + d_mobile + ":" + d_addr;
			    	code = d_map.get(d_info);
			    	if( code == null ) {
			    		d_map.put(d_info, code = _code + (index+1) );
			    	}
			    }
			}
			String _day = row.get("orderday");
			if( _day != null ) {
				_code = owner.getId() + DateUtil.format( DateUtil.parse(_day) ) ;
			}
			code = (String) EasyUtils.checkNull(code, _code);
			
			String skuName = row.get("skuname");
			String skuCode = row.get("skucode");
			Double qty = EasyUtils.obj2Double(row.remove("qty"));
			
		    OrderH order = orderMap.get(code);
			
			// 查询数据库里同单号是否已存在
			if( order == null ) {
				order = orderDao.getOrder(code);
				if( order != null ) {
					orderMap.put(code, order);
					order.items.clear();
					
					if( !WMS.O_STATUS_01.equals(order.getStatus()) && !WMS.O_STATUS_02.equals(order.getStatus()) ) {
						throw new BusinessException("出库单【" +code+ "】已存在并已经开始作业，重新导入需先取消");
					}
					
					// 清除已有的明细数据
					List<?> items = orderDao.getOrderItems(order.getId());
					commDao.deleteAll(items);
					
					order.setQty(0D);
					order.setSkus(0);
					order.setMoney(0D);
				}
			}
			
			if( order == null ) {
				orderMap.put(code, order = new OrderH());
				order.setOrderno(code);
				order.setStatus(WMS.O_STATUS_01);
				order.setDomain( domain );
			}
			
			String payee_id = row.get("payee_id");
			if( payee_id != null ) {
				order.setPayee( (_Customer) commDao.getEntity(_Customer.class,  EasyUtils.obj2Long(payee_id) ) );
			}
			
			order.setOwner( new _Owner( owner_id ) );
			order.setWarehouse( new _Warehouse(warehouse_id) );
			Date orderDay = (Date) EasyUtils.checkNull(DateUtil.parse( row.remove("orderday") ), DateUtil.today());
			orderDay = (Date) EasyUtils.checkTrue( orderDay.before(DateUtil.subDays(DateUtil.today(), 366)), DateUtil.today(), orderDay);
			order.setOrderday(orderDay);
			BeanUtil.setDataToBean(order, row, true);
			
			// 导单同时新建商品
			_Sku sku = ediDao.checkSku(domain, owner_id, skuName, skuCode, null, null);
			if(sku.isNew) {
				newSkuCount++;
			}
			skuSet.add(sku.getId());
			
			OrderItem item = new OrderItem();
			item.setOrder(order);
			item.setSku(sku);
			item.setQty(qty);
			item.setPrice( EasyUtils.obj2Double( row.get("price") ) );
			item.setMoney( EasyUtils.obj2Double( row.get("money") ) );
			item.copyLotAtt( row );
			
			order.setSkus(order.getSkus() + 1);
			order.setQty( MathUtil.addDoubles(order.getQty(), item.getQty()) );
			order.setMoney( MathUtil.addDoubles(order.getMoney(), item.getMoney()) );
			order.items.add(item);
		}
		
		int orderCount = 0, updateCount = 0, itemCount = 0;
		for( OrderH order : orderMap.values() ) {
			orderCount++;
			if(order.getId() == null) {
				commDao.createWithLog(order);
			} else {
				commDao.updateWithLog(order);
				updateCount++;
			}
			
			for( OrderItem item : order.items ) {
				commDao.create(item);
				itemCount++;
			}
		}
		
		// 向前台返回成功信息
		return "parent.alert('共导入明细" +itemCount+ "行，合计订单" +orderCount+ "个，其中覆盖已存在订单" +updateCount+ "个；含货品项" +skuSet.size()+ "个，其中新增" +newSkuCount+ "个');"
				+ " parent.$('.progressBar').hide(); parent.prepareSKUs(parent.queryOrder);";
	}

}
