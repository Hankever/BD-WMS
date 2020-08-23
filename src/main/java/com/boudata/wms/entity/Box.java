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
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.sn.SerialNOer;

/**
 * 出库封箱实体
 */
@Entity
@Table(name = "wms_box", uniqueConstraints = { @UniqueConstraint(name = "MULTI_boxNO_DOMAIN", columnNames = { "domain", "boxno" }) })
@SequenceGenerator(name = "box_seq", sequenceName = "box_seq")
public class Box extends ARecordTable {
	
	public static String TYPE_1 = "整箱";
	public static String TYPE_2 = "拼箱";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "box_seq")
	private Long id;

	/** box编号 */
	@Column(length = 128, nullable = false)
	private String boxno;

	@ManyToOne
	private OrderH order;
	
	private String type;  // 整箱 | 拼箱， 只有一个明细，且数量 = 库存装箱量 ==> 整箱， 否则为拼箱

	/** 总体积 */
	private Double cube = 0d;

	/** 总重 */
	private Double weight = 0d;

	private Double  qty  = 0D; // 数量
	private Integer skus = 0;  // 品项数
	private Double money = 0D; // 金额

	private Date boxdate; // 封箱日期
	private String boxer; // 封箱人
	
	private String pallet;     // 托盘编码
	private Date   palletTime; // 组托时间
	private String palleter;   // 组托人
	private Date   outTime;    // 出库时间
	private String sender;     // 出库人

	private String udf1;
	private String remark; // 备注

	@Transient
	public List<BoxItem> items = new ArrayList<BoxItem>();

	public String toString() {
		return "box: " + boxno + ", " + qty + ", " + weight + ", " + cube;
	}

	public String _genBoxNo() {
		return SerialNOer.get(this.order.getOrderno() + "-xxxx");
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

	public String getBoxno() {
		return boxno;
	}

	public void setBoxno(String boxno) {
		this.boxno = boxno;
	}

	public OrderH getOrder() {
		return order;
	}

	public void setOrder(OrderH order) {
		this.order = order;
	}

	public Double getCube() {
		return cube;
	}

	public void setCube(Double cube) {
		this.cube = cube;
	}

	public Double getWeight() {
		return weight;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getQty() {
		return qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public Integer getSkus() {
		return skus;
	}

	public void setSkus(Integer skus) {
		this.skus = skus;
	}

	public Double getMoney() {
		return money;
	}

	public void setMoney(Double money) {
		this.money = money;
	}

	public Date getBoxdate() {
		return boxdate;
	}

	public void setBoxdate(Date boxdate) {
		this.boxdate = boxdate;
	}

	public String getBoxer() {
		return boxer;
	}

	public void setBoxer(String boxer) {
		this.boxer = boxer;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPallet() {
		return pallet;
	}

	public void setPallet(String pallet) {
		this.pallet = pallet;
	}

	public Date getOutTime() {
		return outTime;
	}

	public void setOutTime(Date outTime) {
		this.outTime = outTime;
	}

	public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public Date getPalletTime() {
		return palletTime;
	}

	public void setPalletTime(Date palletTime) {
		this.palletTime = palletTime;
	}

	public String getPalleter() {
		return palleter;
	}

	public void setPalleter(String palleter) {
		this.palleter = palleter;
	}

	public String getUdf1() {
		return udf1;
	}

	public void setUdf1(String udf1) {
		this.udf1 = udf1;
	}
}
