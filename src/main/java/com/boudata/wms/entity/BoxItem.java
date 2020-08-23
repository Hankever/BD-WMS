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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * 封箱明细（一箱可对应多个明细）
 */
@Entity
@Table(name = "wms_box_item")
@SequenceGenerator(name = "box_item_seq", sequenceName = "box_item_seq")
@JsonIgnoreProperties(value={"pk", "box"})
public class BoxItem extends AbstractLotAtt {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "box_item_seq")
	private Long id;
	
	/**
	 * 封箱单
	 */
	@ManyToOne
	@JoinColumn(nullable = false)
	private Box box;
	
	/**
	 * 出库明细
	 */
	@ManyToOne
	@JoinColumn(nullable = false)
	private OrderItem orderitem;
	
	@ManyToOne
	@JoinColumn(nullable = false)
	private _Sku sku;
	
	@Column(nullable = false)
	private Double qty;
	
	 public String toString() {
		return sku.getName() + ", " + super.getLot();
    }
	
	public int hashCode() {
		return (box.getId() + sku.getId() + getLot()).hashCode();
	}

	public Serializable getPK() {
		return this.id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Box getBox() {
		return box;
	}

	public void setBox(Box box) {
		this.box = box;
	}

	public OrderItem getOrderitem() {
		return orderitem;
	}

	public void setOrderitem(OrderItem orderitem) {
		this.orderitem = orderitem;
	}

	public _Sku getSku() {
		return sku;
	}

	public void setSku(_Sku sku) {
		this.sku = sku;
	}

	public Double getQty() {
		return qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}
}
