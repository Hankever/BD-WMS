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
import java.util.ArrayList;
import java.util.List;

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
 * 入库单明细
 */
@Entity
@Table(name = "wms_asn_item")
@SequenceGenerator(name = "asn_item_seq", sequenceName = "asn_item_seq")
@JsonIgnoreProperties(value={"pk", "asn"})
public class AsnItem extends AbstractLotAtt {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "asn_item_seq")
	private Long id;
	
	@ManyToOne
	@JoinColumn(nullable = false)
	private Asn asn;
	
	@ManyToOne
	@JoinColumn(nullable = false)
	private _Sku sku;
	
	private Double price;      // 实际进价
	private Double money = 0D; // 金额
	
	@Column(nullable = false)
	private Double qty;
	
	private Double qty_actual = 0D; // 实收数量
	
	@Transient
	private Double qty_this = 0D;   // 本次入库量
	
	private String loccode; // 入库库位（可以直接入到存储位）
	
	private String pstatus; // 上架状态
	
	@Transient
	public List<String> snlist = new ArrayList<String>();
	
    public String toString() {
    	return "Asn明细：" + asn.getAsnno() + ", " + sku.getName() + ", " + qty + ", " + qty_actual + ", " + super.toString();
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

	public Asn getAsn() {
		return asn;
	}

	public void setAsn(Asn asn) {
		this.asn = asn;
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

	public Double getQty_actual() {
		return qty_actual;
	}

	public void setQty_actual(Double qty_actual) {
		this.qty_actual = qty_actual;
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

	public Double getQty_this() {
		return qty_this;
	}

	public void setQty_this(Double qty_this) {
		this.qty_this = qty_this;
	}

	public String getLoccode() {
		return loccode;
	}

	public void setLoccode(String loccode) {
		this.loccode = loccode;
	}

	public String getPstatus() {
		return pstatus;
	}

	public void setPstatus(String pstatus) {
		this.pstatus = pstatus;
	}
	
}
