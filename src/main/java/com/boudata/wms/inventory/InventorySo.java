/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;

public class InventorySo extends MacrocodeQueryCondition {

	private String domain = Environment.getDomain();
	
	private Long warehouseId;
	private Long locationId;
	private Long skuId;
	private Long ownerId;
	private Long invId;
	
	private List<Object> locationCodes;

	private String warehouseCode;
	private String locationCode;
	private String loccode;
	private String zone;
	private String skuCode;
	private String barCode;
	private String skuName;
	private String skuBrand;
	private String category;
	private String brand;
	private String owner;
	private Double qtyfrom;
	private Double qtyto;
	private Double qtyLocked;

	private Date createdate1;
	private Date createdate2;
	private Date expiredate1;
	private Date expiredate2;

	/** 货物状态：良品、不良品、破损、过期 */
	private String invstatus;

	/** 备用批次属性01--04 */
	private String lotatt01;
	private String lotatt02;
	private String lotatt03;
	private String lotatt04;

	public Map<String, Object> getConditionMacrocodes() {

		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("${domain}", " and o.domain = :domain");
		
		map.put("${warehouseId}", " and o.wh.id = :warehouseId");
		map.put("${warehouseCode}", " and o.wh.code = :warehouseCode");

//		map.put("${locationId}", " and o.location.id = :locationId");
		map.put("${loccode}", " and o.location.code like :loccode");
		map.put("${locationCode}", " and o.location.code = :locationCode");
		map.put("${zone}", " and o.location.zone like :zone");

		map.put("${locationId}", " and :locationId in (o.location.id, o.location.parent.id) ");
		map.put("${locationCodes}", " and o.location.code in (:locationCodes)");
		
		map.put("${skuId}", " and o.sku.id = :skuId");
		map.put("${skuCode}", " and o.sku.code like :skuCode");
		map.put("${barCode}", " and :barCode in (o.sku.barcode, o.sku.barcode2, o.sku.code)");
		map.put("${skuName}", " and o.sku.name like :skuName");
		map.put("${skuBrand}", " and o.sku.brand like :skuBrand");
		map.put("${category}", " and o.sku.category like :category");
		map.put("${brand}", " and o.sku.brand like :brand");

		map.put("${ownerId}", " and o.owner.id = :ownerId");
		map.put("${owner}", " and o.owner.name = :owner");
		
		map.put("${invId}", " and o.id = :invId");
		map.put("${qtyfrom}", " and o.qty > :qtyfrom");
		map.put("${qtyto}", " and o.qty <= :qtyto");
		map.put("${qtyLocked}", " and o.qty_locked > :qtyLocked");

		map.put("${createdate1}", " and o.createdate >= :createdate1");
		map.put("${createdate2}", " and o.createdate <= :createdate2");
		map.put("${expiredate1}", " and o.expiredate >= :expiredate1");
		map.put("${expiredate2}", " and o.expiredate <= :expiredate2");
		map.put("${invstatus}", " and o.invstatus = :invstatus");
		map.put("${lotatt01}", " and o.lotatt01 like :lotatt01");
		map.put("${lotatt02}", " and o.lotatt02 = :lotatt02");
		map.put("${lotatt03}", " and o.lotatt03 like :lotatt03");
		map.put("${lotatt04}", " and o.lotatt04 like :lotatt04");

		return map;
	}

	public Long getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}

	public Long getLocationId() {
		return locationId;
	}

	public void setLocationId(Long locationId) {
		this.locationId = locationId;
	}

	public Long getSkuId() {
		return skuId;
	}

	public void setSkuId(Long skuId) {
		this.skuId = skuId;
	}

	public Long getInvId() {
		return invId;
	}

	public void setInvId(Long invId) {
		this.invId = invId;
	}

	public String getWarehouseCode() {
		return warehouseCode;
	}

	public void setWarehouseCode(String warehouseCode) {
		this.warehouseCode = warehouseCode;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public String getSkuCode() {
		return wrapLike(skuCode);
	}

	public void setSkuCode(String skuCode) {
		this.skuCode = skuCode;
	}
	
	public String getBarCode() {
		return barCode;
	}

	public void setBarCode(String barCode) {
		this.barCode = barCode;
	}
	
	public String getCategory() {
		return wrapLike(category);
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Double getQtyfrom() {
		return qtyfrom;
	}

	public void setQtyfrom(Double qtyfrom) {
		this.qtyfrom = qtyfrom;
	}

	public Double getQtyto() {
		return qtyto;
	}

	public void setQtyto(Double qtyto) {
		this.qtyto = qtyto;
	}

	public String getLotatt01() {
		return wrapLike(lotatt01);
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
		return wrapLike(lotatt03);
	}

	public void setLotatt03(String lotatt03) {
		this.lotatt03 = lotatt03;
	}

	public String getLotatt04() {
		return wrapLike(lotatt04);
	}

	public void setLotatt04(String lotatt04) {
		this.lotatt04 = lotatt04;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public String getSkuName() {
		return wrapLike(skuName);
	}

	public void setSkuName(String skuName) {
		this.skuName = skuName;
	}

	public String getSkuBrand() {
		return wrapLike(skuBrand);
	}

	public void setSkuBrand(String skuBrand) {
		this.skuBrand = skuBrand;
	}

	public Date getExpiredate1() {
		return expiredate1;
	}

	public void setExpiredate1(Date expiredate1) {
		this.expiredate1 = expiredate1;
	}

	public Date getExpiredate2() {
		return expiredate2;
	}

	public void setExpiredate2(Date expiredate2) {
		this.expiredate2 = expiredate2;
	}

	public Date getCreatedate1() {
		return createdate1;
	}

	public void setCreatedate1(Date createdate1) {
		this.createdate1 = createdate1;
	}

	public Date getCreatedate2() {
		return createdate2;
	}

	public void setCreatedate2(Date createdate2) {
		this.createdate2 = createdate2;
	}

	public String getInvstatus() {
		return invstatus;
	}

	public void setInvstatus(String invstatus) {
		this.invstatus = invstatus;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Double getQtyLocked() {
		return qtyLocked;
	}

	public void setQtyLocked(Double qtyLocked) {
		this.qtyLocked = qtyLocked;
	}

	public String getLoccode() {
		if( !EasyUtils.isNullOrEmpty(loccode) ) {
			loccode = loccode.trim() + "%";           
        }
		return loccode;
	}

	public void setLoccode(String loccode) {
		this.loccode = loccode;
	}

	public String getBrand() {
		return wrapLike(brand);
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public List<Object> getLocationCodes() {
		return locationCodes;
	}

	public void setLocationCodes(List<Object> locationCodes) {
		this.locationCodes = locationCodes;
	}
}
