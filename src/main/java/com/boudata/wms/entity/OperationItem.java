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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;

/**
 * 库内作业明细。
 * 
 * 1、 if locCode != toLocCode then 移库
 * 2、 if locCode == toLocCode && toQty != null && toQty != qty  then 盘点调整
 * 3、 if locCode == 收货区 && qty > 0 then 入库
 * 3、 if locCode == 出货区 && qty > 0 then 出库
 * 
 * 注：操作人记录在操作日志OperationLog上，操作日志相当于是执行明细
 */
@Entity
@Table(name = "wms_operation_item")
@SequenceGenerator(name = "operation_item_seq", sequenceName = "operation_item_seq")
@JsonIgnoreProperties(value={"pk", "operation"})
public class OperationItem extends AbstractLotAtt {
	
	public OperationItem() { }
	
	public OperationItem(Inventory inv, Double qty) {
		this.setOpinv(inv);
		
		this.setOwner(inv.getOwner());
		this.setSkucode(inv.getSku().getCode());
		this.setLoccode(inv.getLocation().getCode());
		this.copyLotAtt(inv);
		
		this.setQty(qty);
	}
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "operation_item_seq")
	private Long id;
	
	/**
	 * 作业单
	 */
	@ManyToOne
	private OperationH operation;
	
	/**
	 * 对应的订单明细（出库作业）
	 */
	@ManyToOne
	private OrderItem orderitem;
	
	/**
	 * 对应的到货单明细（入库作业）
	 */
	@ManyToOne
	private AsnItem asnitem;
 
	@ManyToOne
	private _Owner owner;
	
	@Column(nullable=false)
	private String skucode;
	
	@Column(nullable=false)
	private String loccode;    // 库位
	private String toloccode; // 目的库位（移库）
	
	@Column(nullable=false)
	private Double qty;   // 数量
	private Double toqty; // 到数量（盘点用）
	private Double qty_checked; // 验货量（PC验货没有这个值，移动端拣货验货用到）
	private Double qty_old;    // 原分配的拣货量
	
	@ManyToOne 
	private Inventory opinv;
	
	@ManyToOne 
	private Inventory toinv;
	
	/** 按工单明细行分配执行人 */
    private String worker; 
    
	private String status;

	private String udf1;
    private String udf2;  // 出入库单号
    private String udf3;  // 作业单号
    
    public static OperationItem copy(OperationItem other, OperationH op) {
    	OperationItem ck_opi = new OperationItem();
		BeanUtil.copy(ck_opi, other);
		ck_opi.setId(null);
		ck_opi.setOperation(op);
		
		return ck_opi;
    }
    
    public String toString() {
    	return id + ":【" + (operation == null ? "没关联作业单" : operation.getOpno())
    			+ ", " + owner + ", " + skucode 
    			+ ", " + loccode + (toloccode == null ? "" : "-->" +toloccode)
    			+ ", " + qty + "-->" + EasyUtils.obj2String(toqty) 
    			+ ", " + super.toString() + "】";
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

	public OperationH getOperation() {
		return operation;
	}

	public void setOperation(OperationH operation) {
		this.operation = operation;
	}

	public String getSkucode() {
		return skucode;
	}

	public void setSkucode(String skuCode) {
		this.skucode = skuCode;
	}

	public String getLoccode() {
		return loccode;
	}

	public void setLoccode(String locCode) {
		this.loccode = locCode;
	}

	public String getToloccode() {
		return toloccode;
	}

	public void setToloccode(String toLocCode) {
		this.toloccode = toLocCode;
	}

	public Double getQty() {
		return qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public Double getQty_checked() {
		return qty_checked;
	}

	public void setQty_checked(Double qty_checked) {
		this.qty_checked = qty_checked;
	}

	public Double getQty_old() {
		return qty_old;
	}

	public void setQty_old(Double qty_old) {
		this.qty_old = qty_old;
	}

	public Double getToqty() {
		return toqty;
	}

	public void setToqty(Double toQty) {
		this.toqty = toQty;
	}

	public _Owner getOwner() {
		return owner;
	}

	public void setOwner(_Owner owner) {
		this.owner = owner;
	}

	public OrderItem getOrderitem() {
		return orderitem;
	}

	public void setOrderitem(OrderItem orderItem) {
		this.orderitem = orderItem;
		if(orderitem != null && orderitem.getOrder() != null) {
			this.setUdf2( orderitem.getOrderno() );
		}
	}

	public AsnItem getAsnitem() {
		return asnitem;
	}

	public void setAsnitem(AsnItem asnItem) {
		this.asnitem = asnItem;
		if( asnItem != null && asnItem.getAsn() != null ) {
			this.setUdf2( asnItem.getAsn().getAsnno() );
		}
	}

	public Inventory getOpinv() {
		return opinv;
	}

	public void setOpinv(Inventory opInv) {
		this.opinv = opInv;
	}

	public Inventory getToinv() {
		return toinv;
	}

	public void setToinv(Inventory toInv) {
		this.toinv = toInv;
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

	public String getWorker() {
		return worker;
	}

	public void setWorker(String worker) {
		this.worker = worker;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
