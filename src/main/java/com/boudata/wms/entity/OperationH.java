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
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;

/**
 * 仓库作业，主要围绕库存。
 * 
 * 包括：入库、上架、移库（理货）、补货、分配、拣货、验货、出库、调整、冻结等。
 * 
 * 准确的作业日志，有助于保证库存的准确性和实时性；相比作业日志，各类指导单更适合只做指导，不能作为实际依据。
 */
@Entity
@Table(name = "wms_operation", uniqueConstraints = { @UniqueConstraint(columnNames = { "warehouse_id", "opNo" }) })
@SequenceGenerator(name = "operation_seq", sequenceName = "operation_seq")
@JsonIgnoreProperties(value={"move", "adjust"})
public class OperationH extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "operation_seq")
	private Long id;
	
	/** 仓库 */
	@ManyToOne
	private _Warehouse warehouse;
    
    /** 作业单号 */
    private String opno;
    
    /** 分配执行人 */
    private String worker; 
    
    /** 指定审核人 */
    private String approver; 

	/** 
     * 操作类型，see CX.OP_TYPE
     */
    @ManyToOne
    private Param optype;
    
    /**
     * 操作单状态，see CX.OP_STATUS
     */
    private String status;
    
    @ManyToOne
    private OrderWave wave; // 所属作业波次
    
    private Double qty;
    private Integer skus;
    
	private String udf1; // 拒绝接单 原因
    private String udf2;
    private String udf3;
    
    @Transient
    private String errorMsg;
    
    @Transient 
    public List<OperationItem> items = new ArrayList<OperationItem>();
    
    public boolean lockInv() {
    	String opTypeV = optype.getValue();
    	return WMS.OP_TYPE_FP.equals(opTypeV) || WMS.OP_TYPE_DJ.equals(opTypeV);
    }
    public boolean unLockInv() {
    	List<String> jhTypes = new ArrayList<String>();
    	jhTypes.add( WMS.OP_TYPE_JH );
    	jhTypes.add( WMS.OP_TYPE_BCJH );
    	
    	return jhTypes.contains( optype.getValue() );
    }
    
    public boolean isMove() {
    	List<String> moveTypes = new ArrayList<String>();
    	moveTypes.add( WMS.OP_TYPE_MV );
    	moveTypes.add( WMS.OP_TYPE_SJ );
    	moveTypes.add( WMS.OP_TYPE_BH );
    	moveTypes.add( WMS.OP_TYPE_JH );
    	moveTypes.add( WMS.OP_TYPE_BCJH );
    	
    	return moveTypes.contains( optype.getValue() );
    }
    
    public boolean isAdjust() {
    	String opTypeV = optype.getValue();
    	return WMS.OP_TYPE_TZ.equals(opTypeV) || WMS.OP_TYPE_PD.equals(opTypeV);
    }
    
    // 分配变为拣货
    public void fixOpType() {
    	if( WMS.opType( WMS.OP_TYPE_FP ).equals( this.optype ) ) {
			this.setOptype( WMS.opType( WMS.OP_TYPE_JH ) );
		}
    }
    
    public String toString() {
    	return "作业单" + id + "：" + warehouse.getName() + ", " + optype.getText() + ", " + opno + ", " + status 
    			+ ", " + EasyUtils.obj2String(udf1) + ", " + EasyUtils.obj2String(udf2) + ", " + EasyUtils.obj2String(udf3);
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

	public Param getOptype() {
		return optype;
	}

	public void setOptype(Param opType) {
		this.optype = opType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getOpno() {
		return opno;
	}

	public void setOpno(String opNo) {
		this.opno = opNo;
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
	
	public String getApprover() {
		return approver;
	}
	public void setApprover(String approver) {
		this.approver = approver;
	}
	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
		this.updateTime = (Date) EasyUtils.checkNull(this.updateTime, createTime);
	}
}
