/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */
package com.boudata.wms.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * 货主实体定义
 */
@Entity
@Table(name = "wms_owner", uniqueConstraints = {
		@UniqueConstraint(name = "MULTI_OWNER_DOMAIN", columnNames = { "domain", "name" }) 
})
@SequenceGenerator(name = "owner_seq", sequenceName = "owner_seq")
@JsonIgnoreProperties(value={"pk", "pickupr", "waver", "putawayr"})
public class _Owner extends AbstractBO {

	public _Owner() { }
	public _Owner(Long id) {
		this.id = id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "owner_seq")
	private Long id;
	
	/** 绑定仓库，为空则不绑定任何仓库 */
	@ManyToOne
	private _Warehouse warehouse;

	private String address;
	private String mobile;
	private String email;
	private String contact; // 联系人
	private String remark;
 
	/** 对接TMS：uName uToken */
	private String tms_key;

	public String toString() {
		return this.getName();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public Serializable getPK() {
		return this.getId();
	}

	public _Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(_Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getTms_key() {
		return tms_key;
	}

	public void setTms_key(String tms_key) {
		this.tms_key = tms_key;
	}
}
