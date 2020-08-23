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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.boubei.tss.util.MathUtil;

/**
 * 出库单明细
 */
@Entity
@Table(name = "wms_order_item")
@SequenceGenerator(name = "order_item_seq", sequenceName = "order_item_seq")
@JsonIgnoreProperties(value={"pk", "order"})
public class OrderItem extends AbstractLotAtt {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "order_item_seq")

	private Long id;
	
	@ManyToOne
	private OrderH order;  // 关联的订单头，如果为空，则为购物车状态
	
	@ManyToOne
	@JoinColumn(nullable = false)
	private _Sku sku;
	
	@Column(nullable = false)
	private Double qty;
	
	@Transient
	private Double qty_allocated = 0D; // 已分配数量
	
	private Double qty_checked = 0D; // 实际验货数量
	
	private Double qty_send = 0D; // 实发数量
	
	@Transient
	public Double qty_this = 0D;  // 本次出库量（小程序工单出库用到）
	
	/**
	 * 出库库存、直接出库用
	 */
	private Long inv_id;
	
	@Transient 
	public String out_loc; // 指定出库库位
	
	private Double  price;      // 实际售价
	private Double  money = 0D; // 金额
	
	private String remark;
    
    public String toString() {
    	return id + "：" + sku.getName() + ", " + qty + ", " + qty_send + ", " + super.toString();
    }
	
	public Serializable getPK() {
		return this.id;
	}
	
	public String getOrderno() {
		return this.getOrder().getOrderno();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public OrderH getOrder() {
		return order;
	}

	public void setOrder(OrderH order) {
		this.order = order;
	}

	public Double getQty() {
		return qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public _Sku getSku() {
		return sku;
	}

	public void setSku(_Sku sku) {
		this.sku = sku;
	}

	public Double getQty_send() {
		return qty_send;
	}

	public void setQty_send(Double qty_send) {
		this.qty_send = qty_send;
	}

	public Double getQty_allocated() {
		return qty_allocated;
	}

	public void setQty_allocated(Double qty_allocated) {
		this.qty_allocated = qty_allocated;
	}

	public Double getMoney() {
		return money;
	}

	public void setMoney(Double money) {
		if( money == null || money == 0D ) {
			money = MathUtil.multiply(this.price, this.qty);
		}
		this.money = money;
	}

	public Double getPrice() {
		return price;
	}

	public void setPrice(Double price) {
		this.price = price;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Double getQty_checked() {
		return qty_checked;
	}

	public void setQty_checked(Double qty_checked) {
		this.qty_checked = qty_checked;
	}

	public Long getInv_id() {
		return inv_id;
	}

	public void setInv_id(Long inv_id) {
		this.inv_id = inv_id;
	}
}
