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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *	仓库实体定义
 */
@Entity
@Table(name = "wms_warehouse", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_WH_CODE_DOMAIN", columnNames = { "domain", "name" })
})
@SequenceGenerator(name = "warehouse_seq", sequenceName = "warehouse_seq")
@JsonIgnoreProperties(value={"pk", "pickupr", "waver", "putawayr"})
public class _Warehouse extends AbstractBO {
	
	public static int WH_TYPE_SKU = 1;
	public static int WH_TYPE_MONEY = 2;

	public _Warehouse() { }

	public _Warehouse(Long id) {
		this.id = id;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "warehouse_seq")
	private Long id;
	
	private String province;
	private String city;
	private String district;
	private String address; // 详细地址
	private String contact; // 联系人
	private String mobile;  // 联系人电话
	
	private String remark;
	
	private String pic;
	private String config; // 仓库级配置，优先于域级配置
	
	public Serializable getPK() {
		return this.id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getConfig() {
		return config;
	}

	public void setConfig(String config) {
		this.config = config;
	}

	public String getPic() {
		return pic;
	}

	public void setPic(String pic) {
		this.pic = pic;
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

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
}
