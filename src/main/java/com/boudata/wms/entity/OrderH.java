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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.DateUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;

/**
 * 出库单
 */
@Entity
@Table(name = "wms_order", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_ORDERNO_DOMAIN", columnNames = { "domain", "orderno" })
})
@SequenceGenerator(name = "order_seq", sequenceName = "order_seq")
@JsonIgnoreProperties(value={"pk", "wave"})
public class OrderH extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "order_seq")
	private Long id;
	
    /** 订单编号 */
    private String orderno;
    
    private Date orderday = DateUtil.today() ;

	/** 出库仓 */
	@ManyToOne
	private _Warehouse warehouse;
    
    /** 货主 */
	@ManyToOne
    private _Owner owner;
	
	@ManyToOne
	private OrderWave wave;  // 作业波次
	
	/**
	 * 出库单类型：普通出库、生产领料出库、成品出库、损耗出库、报废出库...
	 */
	private String type;
	private String typecode;
	private String send_type; // 发货方式
	
    /** 收件人 */    
    private String d_receiver;
    private String d_mobile;
    private String d_addr; // 收件地址
    private String d_province;
    private String d_city;
    private String d_district;
    private String d_street;
    
    private Double  qty = 0D;    // 数量
    private Integer skus = 0;    // 品项数
    private Double  money = 0D;  // 金额
    
    private String status;      // see CX.ORDER_STATUS
    private Date outbound_date; // 出库日期
    private String tag;         // 用于打库存不足、确认已反馈（ERP）等标记
    private String origin;      // 来源（ERP名称、详情等）
    
	@ManyToOne
    private _Customer payee;
	private Double service_fee;
    private Long fms_asn_id;  // 由收款单 生成的付款单（资金管理）
    private Long asn_id;      // 由入库单 生成的付款单（卜数记账）
	
    private String udf1;
    private String udf2;
    private String udf3;
    private String udf4; 
    private String udf5;
    private String udf6;
    private String udf7;
    private String udf8;
    
    private String worker;
    
    private String remark;
    
    private String tms_code;  // 对接TMS 返回的单号
    private String tms_error; // 对接TMS 返回的异常
    
    @Transient
    public List<OrderItem> items = new ArrayList<OrderItem>();
    
    public String toString() {
    	return "订单: " + orderno + ", " + warehouse.getName() + ", " + owner.getName() + ", " + qty + ", " + status;
    }
    
    public String genOpNo(String opType) {
    	String tl = this.orderno + "-" + opType + "xxxx";
		return SerialNOer.get(tl).replace("-"+opType+"00", "-"+opType).replace("-"+opType+"0", "-"+opType);
    }
    
    public boolean isShort() {
    	 return WMS.INV_SHORT.equals( getTag() );
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

	public _Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(_Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public Double getMoney() {
		return money;
	}

	public void setMoney(Double money) {
		this.money = money;
	}

	public _Owner getOwner() {
		return owner;
	}

	public void setOwner(_Owner owner) {
		this.owner = owner;
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

	public String getUdf4() {
		return udf4;
	}

	public void setUdf4(String udf4) {
		this.udf4 = udf4;
	}

	public String getOrderno() {
		return orderno;
	}

	public void setOrderno(String orderno) {
		this.orderno = orderno;
	}

	public Date getOrderday() {
		return orderday;
	}

	public void setOrderday(Date orderday) {
		this.orderday = orderday;
	}

	public Double getQty() {
		return qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		// 出（入）库完成，取消该出（入）库单相关的未完成的其它已领用工单
		if( WMS.O_STATUS_06.equals(status) ) {
			_Util.closeOpWhenFinished(this.orderno, this.warehouse);
		}
		this.status = status;
	}

	public Integer getSkus() {
		return skus;
	}

	public void setSkus(Integer skus) {
		this.skus = skus;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public _Customer getPayee() {
		return payee;
	}

	public void setPayee(_Customer payee) {
		this.payee = payee;
	}

	public Double getService_fee() {
		return service_fee;
	}

	public void setService_fee(Double service_fee) {
		this.service_fee = service_fee;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public Long getFms_asn_id() {
		return fms_asn_id;
	}

	public void setFms_asn_id(Long fms_asn_id) {
		this.fms_asn_id = fms_asn_id;
	}

	public Long getAsn_id() {
		return asn_id;
	}

	public void setAsn_id(Long asn_id) {
		this.asn_id = asn_id;
	}

	public String getD_receiver() {
		return d_receiver;
	}

	public void setD_receiver(String d_receiver) {
		this.d_receiver = d_receiver;
	}

	public String getD_mobile() {
		return d_mobile;
	}

	public void setD_mobile(String d_mobile) {
		this.d_mobile = d_mobile;
	}

	public String getD_addr() {
		return d_addr;
	}

	public void setD_addr(String d_addr) {
		this.d_addr = d_addr;
	}

	public String getD_province() {
		return d_province;
	}

	public void setD_province(String d_province) {
		this.d_province = d_province;
	}

	public String getD_city() {
		return d_city;
	}

	public void setD_city(String d_city) {
		this.d_city = d_city;
	}

	public String getD_district() {
		return d_district;
	}

	public void setD_district(String d_district) {
		this.d_district = d_district;
	}

	public String getD_street() {
		return d_street;
	}

	public void setD_street(String d_street) {
		this.d_street = d_street;
	}

	public OrderWave getWave() {
		return wave;
	}

	public void setWave(OrderWave wave) {
		this.wave = wave;
	}

	public String getWorker() {
		return worker;
	}

	public void setWorker(String worker) {
		this.worker = worker;
	}

	public Date getOutbound_date() {
		return outbound_date;
	}

	public void setOutbound_date(Date outboundDate) {
		this.outbound_date = outboundDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTypecode() {
		return typecode;
	}

	public void setTypecode(String typecode) {
		this.typecode = typecode;
	}

	public String getSend_type() {
		return send_type;
	}

	public void setSend_type(String send_type) {
		this.send_type = send_type;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getUdf5() {
		return udf5;
	}

	public void setUdf5(String udf5) {
		this.udf5 = udf5;
	}

	public String getUdf6() {
		return udf6;
	}

	public void setUdf6(String udf6) {
		this.udf6 = udf6;
	}

	public String getUdf7() {
		return udf7;
	}

	public void setUdf7(String udf7) {
		this.udf7 = udf7;
	}

	public String getUdf8() {
		return udf8;
	}

	public void setUdf8(String udf8) {
		this.udf8 = udf8;
	}

	public String getTms_code() {
		return tms_code;
	}

	public void setTms_code(String tms_code) {
		this.tms_code = tms_code;
	}

	public String getTms_error() {
		return tms_error;
	}

	public void setTms_error(String tms_error) {
		this.tms_error = tms_error;
	}
}
