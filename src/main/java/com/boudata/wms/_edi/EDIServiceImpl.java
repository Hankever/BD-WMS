/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._edi;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity._Customer;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.inbound.AsnService;
import com.boudata.wms.outbound.OrderService;

@Service("EDIService")
public class EDIServiceImpl implements EDIService {
	
	Logger log = Logger.getLogger(this.getClass());

	@Autowired EDIDao ediDao;
	@Autowired ICommonDao commDao;
	@Autowired OrderHDao orderDao;
	@Autowired OrderService orderService;
	@Autowired AsnDao asnDao;
	@Autowired AsnService asnService;
	
	public Map<String, Object> receiveSKUs(List<Map<String, Object>> list) {
		for (Map<String, Object> dto : list) {
			String skuCode = EasyUtils.obj2String(dto.remove("code"));
			String skuName = EasyUtils.obj2String(dto.get("name"));
			String barcode = EasyUtils.obj2String(dto.get("barcode"));
			String owner = EasyUtils.obj2String(dto.remove("owner"));

			String domain = Environment.getDomainOrign();
			_Owner _owner  = ediDao.getOwner(owner, domain ) ;
			_Sku sku = ediDao.checkSku(domain, _owner.getId(), skuName, skuCode, barcode, dto);
			
			ediDao.update(sku);
		}
		
		return _Util.toMap(100, "success");
	}

	public Map<String, Object> receiveOrder(Map<String, ?> map) {
		String warehouse = (String) map.remove("warehouse");
		String owner = (String) map.remove("owner");
		String code = (String) map.remove("code");
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) map.get("items"); // 出库明细

		// 查询数据库里同单号是否已存在
		String domain = Environment.getDomainOrign();
		OrderH order = orderDao.getOrder(code);
		if ( order != null ) {
			if (!WMS.O_STATUS_01.equals(order.getStatus()) && !WMS.O_STATUS_02.equals(order.getStatus())) {
				return _Util.toMap(101, "出库单【" + code + "】已存在并已经开始作业，先通知仓库取消作业");
			}

			// 清除已有的明细数据
			List<?> list = orderDao.getOrderItems(order.getId());
			commDao.deleteAll(list);
 
			order.setQty(0D);
			order.setSkus(0);
			order.setMoney(0D);
		}
		else {
			order = new OrderH();
			order.setOrderno(code);
			order.setDomain(domain);
			order.setOwner( ediDao.getOwner(owner, domain) );
			order.setWarehouse( ediDao.getWarehouse(warehouse, domain) );
			commDao.createWithLog(order);
		}
		order.setStatus(WMS.O_STATUS_01);
		order.setOrderday((Date) EasyUtils.checkNull(DateUtil.parse((String) map.remove("orderday")), DateUtil.today()));
		BeanUtil.setDataToBean(order, map, true);
		commDao.update(order);
		
		List<_DTO> list = _DTO.parse(items);

		for (_DTO dto : list) {
			String skuCode = dto.code;
			String skuName = dto.name;
			String barcode = dto.barcode;

			// 导单同时新建商品
			_Sku sku = ediDao.checkSku(domain, order.getOwner().getId(), skuName, skuCode, barcode, null);

			OrderItem oitem = new OrderItem();
			oitem.setOrder(order);
			oitem.setSku(sku);
			oitem.setQty(dto.qty);
			oitem.setPrice(dto.price);
			oitem.setMoney(dto.money);
			oitem.copyLotAtt(dto);
			
			commDao.create(oitem);

			order.setSkus(order.getSkus() + 1);
			order.setQty(MathUtil.addDoubles(order.getQty(), oitem.getQty()));
			order.setMoney(MathUtil.addDoubles(order.getMoney(), oitem.getMoney()));
			order.items.add(oitem);
		}
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id", order.getId());
		data.put("createtime", order.getCreateTime());
		Map<String, Object> result = _Util.toMap(100, "success");
		result.put("data", data);
		return result;
	}

	public Map<String, Object> cancelOrder(String code, String reason) {
		OrderH order = orderDao.getOrder(code);
		if( order == null ) {
			return _Util.toMap(404, "没有找到该订单的出库单");
		}
		
		orderService.cancelOrder(order.getId(), reason);
		
		order.setTag("外部取消");
		orderDao.update(order);
		
		return _Util.toMap(100, "success");
	}

	public Map<String, Object> receiveAsn(Map<String, ?> map) {

		String warehouse = (String) map.remove("warehouse");
		String owner = (String) map.remove("owner");
		String code = (String) map.remove("code");
		
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> items = (List<Map<String, Object>>) map.get("items"); // 入库明细

		// 查询数据库里同单号是否已存在
		String domain = Environment.getDomainOrign();
		Asn asn = asnDao.getAsn(code);
		if ( asn != null ) {
			if( !WMS.ASN_STATUS_01.equals(asn.getStatus()) && !WMS.ASN_STATUS_02.equals(asn.getStatus()) ) {
				return _Util.toMap(101, "入库单【" + code + "】已存在并已经开始作业，先通知仓库取消作业");
			}

			// 清除已有的明细数据
			commDao.deleteAll( asnDao.getItems(asn.getId()) );
 
			asn.setQty(0D);
			asn.setSkus(0);
			asn.setMoney(0D);
		}
		else {
			asn = new Asn();
			asn.setAsnno(code);
			asn.setDomain(domain);
			asn.setOwner( ediDao.getOwner(owner, domain) );
			asn.setWarehouse( ediDao.getWarehouse(warehouse, domain) );
			commDao.createWithLog(asn);
		}
		asn.setAsnday((Date) EasyUtils.checkNull(DateUtil.parse((String) map.remove("asnday")), DateUtil.today()));
		BeanUtil.setDataToBean(asn, map, true);
		asn.setStatus(WMS.ASN_STATUS_01);
		commDao.update(asn);
		
		List<_DTO> list = _DTO.parse(items);

		for (_DTO dto : list) {
			String skuCode = dto.code;
			String skuName = dto.name;
			String barcode = dto.barcode;

			// 导单同时新建商品
			_Sku sku = ediDao.checkSku(domain, asn.getOwner().getId(), skuName, skuCode, barcode, null);

			AsnItem asnItem = new AsnItem();
			asnItem.setAsn(asn);
			asnItem.setSku(sku);
			asnItem.setQty(dto.qty);
			asnItem.setPrice(dto.price);
			asnItem.setMoney(dto.money);
			asnItem.copyLotAtt(dto);
			
			commDao.create(asnItem);

			asn.setSkus(asn.getSkus() + 1);
			asn.setQty(MathUtil.addDoubles(asn.getQty(), asnItem.getQty()));
			asn.setMoney(MathUtil.addDoubles(asn.getMoney(), asnItem.getMoney()));
			asn.items.add(asnItem);
		}
		
		Map<String, Object> data = new HashMap<String, Object>();
		data.put("id", asn.getId());
		Map<String, Object> result = _Util.toMap(100, "success");
		result.put("data", data);
		return result;
	}
	
	public Map<String, Object> cancelAsn(String code, String reason) {
		Asn asn = asnDao.getAsn(code);
		asnService.cancelAsn(asn.getId(), reason);
		
		asn.setTag("外部取消");
		asnDao.update(asn);
		
		return _Util.toMap(100, "success");
	}

	public Map<String, Object> receiveCustomers(List<Map<String, Object>> list) {
		for (Map<String, Object> dto : list) {
			String code = (String) dto.remove("code");
			
			String domain = Environment.getDomainOrign();
			_Customer customer = ediDao.checkCustomer(domain, code, dto);

			ediDao.update( customer );
		}
		
		return _Util.toMap(100, "success");
	}

}
