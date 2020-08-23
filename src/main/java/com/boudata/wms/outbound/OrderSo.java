/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.boubei.tss.framework.persistence.pagequery.MacrocodeQueryCondition;

public class OrderSo extends MacrocodeQueryCondition {

	private Long   warehouseId;
    private String warehouseCode;
    private String orderno;
    private Long id;
    
    private Date orderdayFrom;
    private Date orderdayTo;
    
    private Long ownerId;
    private String creator;
    
    /** 订单状态 */
    private String status;
    
    private String domain;

    public Map<String, Object> getConditionMacrocodes() {
    	
        Map<String, Object> map = new HashMap<String, Object>() ;
        map.put("${warehouseId}", " and o.warehouse.id = :warehouseId");
        map.put("${warehouseCode}", " and o.warehouse.code = :warehouseCode");
       
        map.put("${id}", " and o.id = :id");
        map.put("${orderno}", " and o.orderno = :orderno");
        map.put("${creator}", " and o.creator = :creator");
        map.put("${ownerId}", " and o.owner.id = :ownerId");
        map.put("${status}", " and o.status = :status");
        map.put("${domain}", " and o.domain = :domain");
        
        map.put("${orderdayFrom}", " and o.orderday >= :orderdayFrom");
        map.put("${orderdayTo}",   " and o.orderday <= :orderdayTo");

        return map;
    }

	public Long getWarehouseId() {
		return warehouseId;
	}

	public void setWarehouseId(Long warehouseId) {
		this.warehouseId = warehouseId;
	}

	public String getWarehouseCode() {
		return warehouseCode;
	}

	public void setWarehouseCode(String warehouseCode) {
		this.warehouseCode = warehouseCode;
	}

	public String getOrderno() {
		return orderno;
	}

	public void setOrderno(String orderno) {
		this.orderno = orderno;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getOrderdayFrom() {
		return orderdayFrom;
	}

	public void setOrderdayFrom(Date orderdayFrom) {
		this.orderdayFrom = orderdayFrom;
	}

	public Date getOrderdayTo() {
		return orderdayTo;
	}

	public void setOrderdayTo(Date orderdayTo) {
		this.orderdayTo = orderdayTo;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
}
