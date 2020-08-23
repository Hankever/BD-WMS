/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.entity;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Environment;

/**
 * 库存临时表 本临时表包含了库存的各个维度字段，用于批量查询库存使用。
 */
@Entity
@Table(name = "wms_temp_inv")
@SequenceGenerator(name = "inv_temp_sequence", sequenceName = "inv_temp_sequence", initialValue = 1, allocationSize = 10)
public class InventoryTemp implements IEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "inv_temp_sequence")
	private Long pK;

	private Long id;
	private Long thread; // 当前线程ID，用以多线程场景

	private Long whId;
	private Long ownerId;
	private Long locationId;
	private Long skuId;

	private String locationCode;
	private String skuCode;

	/** 生产日期 */
	protected Date createdate;

	/** 过期日期 */
	protected Date expiredate;

	/** 货物状态：良品、不良品、破损、过期 */
	protected String invstatus;

	/** 备用批次属性01--04 */
	protected String lotatt01;
	protected String lotatt02;
	protected String lotatt03;
	protected String lotatt04;

	private String udf1;
	private String udf2;
	private String udf3;

	@Transient
	public OperationItem opItem;

	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	public InventoryTemp() {
		this.setThread(Environment.threadID());
	}

	public InventoryTemp(OperationH op, OperationItem item) {
		this();

		this.setId(item.getId());
		this.setOpItem(item);

		this.whId = op.getWarehouse().getId();
		this.ownerId = item.getOwner().getId();
		this.skuCode = item.getSkucode();
		this.locationCode = item.getLoccode();

		copyLotatt(item);
	}
	
	public void copyLotatt(AbstractLotAtt item) {
		this.setLotatt01(item.getLotatt01());
		this.setLotatt02(item.getLotatt02());
		this.setLotatt03(item.getLotatt03());
		this.setLotatt04(item.getLotatt04());

		this.setCreatedate(item.getCreatedate());
		this.setExpiredate(item.getExpiredate());
		this.setInvstatus(item.getInvstatus());
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public String getSkuCode() {
		return skuCode;
	}

	public void setSkuCode(String skuCode) {
		this.skuCode = skuCode;
	}

	public String getUdf1() {
		return udf1;
	}

	public void setUdf1(String udf1) {
		this.udf1 = udf1;
	}

	public String getUdf2() {
		return udf2;
	}

	public void setUdf2(String udf2) {
		this.udf2 = udf2;
	}

	public String getUdf3() {
		return udf3;
	}

	public void setUdf3(String udf3) {
		this.udf3 = udf3;
	}

	public Long getPK() {
		return pK;
	}

	public void setPK(Long pk) {
		this.pK = pk;
	}

	public Long getThread() {
		return thread;
	}

	public void setThread(Long thread) {
		this.thread = thread;
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

	public OperationItem getOpItem() {
		return opItem;
	}

	public void setOpItem(OperationItem opItem) {
		this.opItem = opItem;
	}

	public Long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public Long getWhId() {
		return whId;
	}

	public void setWhId(Long whId) {
		this.whId = whId;
	}
}
