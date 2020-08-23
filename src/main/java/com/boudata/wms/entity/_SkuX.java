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

import com.boubei.tss.dm.record.ARecordTable;

/**
 * SKU扩展：组合套餐配置、位置绑定、特殊作业规则等
 */
@Entity
@Table(name = "wms_skux", uniqueConstraints = { 
        @UniqueConstraint(name = "SINGLE_SKU_PARENT", columnNames = { "parent_id", "sku_id" })
})
@SequenceGenerator(name = "skux_seq", sequenceName = "skux_seq")
@JsonIgnoreProperties(value={"pk", "parent"})
public class _SkuX extends ARecordTable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "skux_seq")
	private Long id;

	@ManyToOne
	private _Sku sku;

	@ManyToOne
	private _Sku parent; // 相同SKU组成的不同比例【大SKU】，每个大SKU需要单独定义

	private Integer weight; // 权重

	/** 专属库位 */
	@ManyToOne
	private _Location private_loc;
	
	/** 专属库位安全库存量 */
	private Double safety_qty;
	
	private String remark;

	public Serializable getPK() {
		return this.id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public _Sku getSku() {
		return sku;
	}

	public void setSku(_Sku sku) {
		this.sku = sku;
	}

	public _Sku getParent() {
		return parent;
	}

	public void setParent(_Sku parent) {
		this.parent = parent;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(Integer weight) {
		this.weight = weight;
	}

	public _Location getPrivate_loc() {
		return private_loc;
	}

	public void setPrivate_loc(_Location private_loc) {
		this.private_loc = private_loc;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Double getSafety_qty() {
		return safety_qty;
	}

	public void setSafety_qty(Double safety_qty) {
		this.safety_qty = safety_qty;
	}
}
