/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */
/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.param.ParamConstants;

/**
 * 往来对象表：供货方|收货方|承运商|往来方
 */
@Entity
@Table(name = "wmsx_customer")
@SequenceGenerator(name = "customer_seq", sequenceName = "customer_seq")
public class _Customer extends ARecordTable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "customer_seq")
	private Long id;

	@Column(length = 128, nullable = false)
	private String name;
	private String code; // 编码

	@Column(length = 128, nullable = false)
	private String type; // 类型： 供货方|收货方|承运商|往来方
	
	@ManyToOne
	private _Owner owner; // 账户
	
	@ManyToOne
	private _Warehouse warehouse; // 绑定仓库，为空则不绑定任何仓库
	
	private String tax_no; // 税号
	private String title;  // 发票抬头
	
	private String contact;   // 联系人
	private String mobile;    // 联系电话
	private String addr;      // 详细地址
	private String province;  // 省
	private String city;      // 市
	private String district;  // 区
	
	private String remark;
	
	/** 状态：启用/停用 */
	private Integer status = ParamConstants.TRUE;

	public Serializable getPK() {
		return this.getId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public _Owner getOwner() {
		return owner;
	}

	public void setOwner(_Owner owner) {
		this.owner = owner;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getTax_no() {
		return tax_no;
	}

	public void setTax_no(String tax_no) {
		this.tax_no = tax_no;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getAddr() {
		return addr;
	}

	public void setAddr(String addr) {
		this.addr = addr;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getDistrict() {
		return district;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public _Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(_Warehouse warehouse) {
		this.warehouse = warehouse;
	}
}
