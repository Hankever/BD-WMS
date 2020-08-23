/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.boubei.tss.util.BeanUtil;
import com.boudata.wms._Util;
import com.boudata.wms.entity.AbstractLotAtt;

/**
 * 接收批量操作参数
 */
public class _DTO {
	
	public Long id;
	public Long opi_id;
	public Long inv_id;
	public Long location_id;
	public Long sku_id;
	public Long owner_id;
	public Long asnitem_id;
	public Long item_id;
	
	public String owner;
	public String warehouse;
	
	public String items;
	public String snlist;
	
	public String skucode;
	public String code;
	public String name;
	public String barcode;
	
	public String loccode;
	public String toloccode;
	
	public Double qty;
	public Double toqty;
	public Double qty_this;
	public Double qty_actual;
	public Double price;
	public Double money;
	
	public Double service_fee;
	
	public Date createdate;
	public Date expiredate;
	public String invstatus;
	public String lotatt01;
	public String lotatt02;
	public String lotatt03;
	public String lotatt04;
	
	public static List<_DTO> parse(Map<?, ?> map) {
		return parse( (String) map.get("items") );
	}
	public static List<_DTO> parse(String items) {
		List<Map<String, Object>> list = _Util.json2List(items);
		return parse(list);
	}
	public static List<_DTO> parse(List<Map<String, Object>> list) {
		List<_DTO> temp = new ArrayList<>();
		
		for(Map<String, ?> map : list) {
			_DTO dto = new _DTO();
			BeanUtil.setDataToBean(dto, map);
			temp.add(dto);
		}
		
		return temp;
	}
	
	public void copyLotatt(AbstractLotAtt from) {
		this.setLotatt01(from.getLotatt01());
        this.setLotatt02(from.getLotatt02());
        this.setLotatt03(from.getLotatt03());
        this.setLotatt04(from.getLotatt04());
        this.setCreatedate(from.getCreatedate());
        this.setExpiredate(from.getExpiredate());
        this.setInvstatus(from.getInvstatus());
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getOpi_id() {
		return opi_id;
	}
	public void setOpi_id(Long opi_id) {
		this.opi_id = opi_id;
	}
	public Long getInv_id() {
		return inv_id;
	}
	public void setInv_id(Long inv_id) {
		this.inv_id = inv_id;
	}
	public Long getSku_id() {
		return sku_id;
	}
	public void setSku_id(Long sku_id) {
		this.sku_id = sku_id;
	}
	public Long getOwner_id() {
		return owner_id;
	}
	public void setOwner_id(Long owner_id) {
		this.owner_id = owner_id;
	}
	public Long getAsnitem_id() {
		return asnitem_id;
	}
	public void setAsnitem_id(Long asnitem_id) {
		this.asnitem_id = asnitem_id;
	}
	public String getItems() {
		return items;
	}
	public void setItems(String items) {
		this.items = items;
	}
	public String getSnlist() {
		return snlist;
	}
	public void setSnlist(String snlist) {
		this.snlist = snlist;
	}
	public String getSkucode() {
		return skucode;
	}
	public void setSkucode(String skucode) {
		this.skucode = skucode;
	}
	public String getLoccode() {
		return loccode;
	}
	public void setLoccode(String loccode) {
		this.loccode = loccode;
	}
	public String getToloccode() {
		return toloccode;
	}
	public void setToloccode(String toloccode) {
		this.toloccode = toloccode;
	}
	public Double getQty() {
		return qty;
	}
	public void setQty(Double qty) {
		this.qty = qty;
	}
	public Double getToqty() {
		return toqty;
	}
	public void setToqty(Double toqty) {
		this.toqty = toqty;
	}
	public Double getQty_this() {
		return qty_this;
	}
	public void setQty_this(Double qty_this) {
		this.qty_this = qty_this;
	}
	public Double getQty_actual() {
		return qty_actual;
	}
	public void setQty_actual(Double qty_actual) {
		this.qty_actual = qty_actual;
	}
	public Double getPrice() {
		return price;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
	public Double getMoney() {
		return money;
	}
	public void setMoney(Double money) {
		this.money = money;
	}
	public Double getService_fee() {
		return service_fee;
	}
	public void setService_fee(Double service_fee) {
		this.service_fee = service_fee;
	}
	public Date getCreatedate() {
		return createdate;
	}
	public void setCreatedate(Date createdate) {
		this.createdate = createdate;
	}
	public Date getExpiredate() {
		return expiredate;
	}
	public void setExpiredate(Date expiredate) {
		this.expiredate = expiredate;
	}
	public String getInvstatus() {
		return invstatus;
	}
	public void setInvstatus(String invstatus) {
		this.invstatus = invstatus;
	}
	public String getLotatt01() {
		return lotatt01;
	}
	public void setLotatt01(String lotatt01) {
		this.lotatt01 = lotatt01;
	}
	public String getLotatt02() {
		return lotatt02;
	}
	public void setLotatt02(String lotatt02) {
		this.lotatt02 = lotatt02;
	}
	public String getLotatt03() {
		return lotatt03;
	}
	public void setLotatt03(String lotatt03) {
		this.lotatt03 = lotatt03;
	}
	public String getLotatt04() {
		return lotatt04;
	}
	public void setLotatt04(String lotatt04) {
		this.lotatt04 = lotatt04;
	}
	public Long getLocation_id() {
		return location_id;
	}
	public void setLocation_id(Long location_id) {
		this.location_id = location_id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getBarcode() {
		return barcode;
	}
	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(String warehouse) {
		this.warehouse = warehouse;
	}
	public Long getItem_id() {
		return item_id;
	}
	public void setItem_id(Long item_id) {
		this.item_id = item_id;
	}
}
