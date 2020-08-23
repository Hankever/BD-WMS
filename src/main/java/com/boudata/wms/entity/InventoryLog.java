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
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;

/**
 * 库存流水（交易日志）
 */
@Entity
@Table(name = "wms_inventory_log")
@SequenceGenerator(name = "inventory_log_seq", sequenceName = "inventory_log_seq")
public class InventoryLog implements IEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "inventory_log_seq")
	private Long id;
	    
    /** 对应的单据日志 */
    @ManyToOne
    private OperationLog opLog; // 对应当前操作日志
    
    /******************************* LLU 维度属性直接关联到LLU获取 *********************************/
    /** 对应的LLU */
    @ManyToOne
    private Inventory inv;
    
    /*********************************** 以下为Inv 修改前属性值 ********************************************/

    private Double qty;          /** 库存量 */
	private Double qtyLocked;    /** 锁定量 */
    
    /*********************************** 以下为 Inv 修改后属性值 ********************************************/
	
	private Double toQty;          /** 库存量 */
	private Double toQtyLocked;    /** 锁定量 */
	
	private Boolean writeBack = false;  /** 是否已经冲账 */
	
	private Date createTime;
	
    public String toString() {
    	return "库存流水（" +opLog.getOpType().getText() + opLog.getId()+ "）" + inv + ".【 " + qty + "-->" + toQty + ", " + qtyLocked + "-->" + toQtyLocked + " 】  ";
    }
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public OperationLog getOpLog() {
		return opLog;
	}

	public void setOpLog(OperationLog opLog) {
		this.opLog = opLog;
	}

	public Inventory getInv() {
		return inv;
	}

	public void setInv(Inventory inv) {
		this.inv = inv;
	}

	public Double getQty() {
		return qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public Double getQtyLocked() {
		return qtyLocked;
	}

	public void setQtyLocked(Double qtyLocked) {
		this.qtyLocked = qtyLocked;
	}

	public Double getToQty() {
		return toQty;
	}

	public void setToQty(Double toQty) {
		this.toQty = toQty;
	}

	public Double getToQtyLocked() {
		return toQtyLocked;
	}

	public void setToQtyLocked(Double toQtyLocked) {
		this.toQtyLocked = toQtyLocked;
	}

	public Serializable getPK() {
		return this.id;
	}

	public Boolean getWriteBack() {
		return writeBack;
	}

	public void setWriteBack(Boolean writeBack) {
		this.writeBack = writeBack;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
}
