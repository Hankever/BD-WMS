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
import com.boubei.tss.util.DateUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;

/**
 * 入库单（到货通知单）
 */
@Entity
@Table(name = "wms_asn", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_ASNNO_DOMAIN", columnNames = { "domain", "asnno" })
})
@SequenceGenerator(name = "asn_seq", sequenceName = "asn_seq")
public class Asn extends ARecordTable {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "asn_seq")
	private Long id;
	
	 /** 货主 */
    @ManyToOne
    private _Owner owner;
    
	/** 仓库 */
	@ManyToOne
	private _Warehouse warehouse;
 
    /** ASN编号 */
	@Column(length = 128, nullable = false)
    private String asnno;
    
    private Date asnday = DateUtil.today() ; // 下单日期
    
    private String type;
    private String typecode;
    
    private String supplier; // 供（退）货方（WMS）

    /** 总体积 */
    private Double cube = 0d;

    /** 总重 */
    private Double weight = 0d;
    
    private Double  qty = 0D;    // 数量
    private Integer skus = 0;    // 品项数
    private Double  money = 0D;  // 金额
    
    private String status;      // see CX.ASN_STATUS
    private Date inbound_date;  // 入库日期
    private String tag;         // 用于入库方式、确认已反馈（ERP）等标记
    private String origin;      // 来源（ERP名称、详情等）
    
    @ManyToOne
    private _Customer payer;   // 付款方（FMS）
    private Long fms_order_id; // 由付款单 生成的收款单（资金管理）
    private Long order_id;     // 由销售单 生成的收款单（卜数记账）
    
    private String udf1;
    private String udf2;
    private String udf3;
    private String udf4;
    private String udf5;
    private String udf6;
    private String udf7;
    private String udf8;
    
    private String worker;   // 入库员
    private String unloader; // 卸货员
    
    private String remark;
    
    @Transient
    public List<AsnItem> items = new ArrayList<AsnItem>();
    
    public String toString() {
    	return "Asn单: " + asnno + ", " + warehouse.getName() + ", " + owner + ", " + qty + ", " + status;
    }
    
    public String genOpNo(String opType) {
    	String tl = this.asnno + "-" + opType + "xxxx";
		return SerialNOer.get(tl).replace("-"+opType+"00", "-"+opType).replace("-"+opType+"0", "-"+opType);
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

	public _Owner getOwner() {
		return owner;
	}

	public void setOwner(_Owner owner) {
		this.owner = owner;
	}

	public String getAsnno() {
		return asnno;
	}

	public void setAsnno(String asnno) {
		this.asnno = asnno;
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

	public Double getMoney() {
		return money;
	}

	public void setMoney(Double money) {
		this.money = money;
	}

	public Integer getSkus() {
		return skus;
	}

	public void setSkus(Integer skus) {
		this.skus = skus;
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
		if( WMS.ASN_STATUS_04.equals(status) ) {
			_Util.closeOpWhenFinished(this.asnno, this.warehouse);
		}
		this.status = status;
	}

	public Date getAsnday() {
		return asnday;
	}

	public void setAsnday(Date asnday) {
		this.asnday = asnday;
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

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getSupplier() {
		return supplier;
	}

	public void setSupplier(String supplier) {
		this.supplier = supplier;
	}

	public _Customer getPayer() {
		return payer;
	}

	public void setPayer(_Customer payer) {
		this.payer = payer;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public Long getFms_order_id() {
		return fms_order_id;
	}

	public void setFms_order_id(Long fms_order_id) {
		this.fms_order_id = fms_order_id;
	}

	public Long getOrder_id() {
		return order_id;
	}

	public void setOrder_id(Long order_id) {
		this.order_id = order_id;
	}

	public String getWorker() {
		return worker;
	}

	public void setWorker(String worker) {
		this.worker = worker;
	}

	public Date getInbound_date() {
		return inbound_date;
	}

	public void setInbound_date(Date inboundDate) {
		this.inbound_date = inboundDate;
	}

	public String getUnloader() {
		return unloader;
	}

	public void setUnloader(String unloader) {
		this.unloader = unloader;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getUdf8() {
		return udf8;
	}

	public void setUdf8(String udf8) {
		this.udf8 = udf8;
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
}
