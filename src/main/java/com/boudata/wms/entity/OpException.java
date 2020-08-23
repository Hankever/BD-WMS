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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;

/**
 * 作业异常登记
 */
@Entity
@Table(name = "wms_op_exception")
@SequenceGenerator(name = "op_exception_seq", sequenceName = "op_exception_seq")
public class OpException extends ARecordTable {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "op_exception_seq")
	private Long id;

	public Serializable getPK() {
		return this.id;
	}

	/** 所属作业单 */
	@ManyToOne
	private OperationH operation;

	/** 所属作业单明细 */
	@ManyToOne
	private OperationItem opItem;

	private String type;    // 异常类型：库存异常、设备异常...
	private String content; // 反馈内容
	private String remark;  // 备注

	private String status; // 状态 新建、关闭
	private String result; // 处理结果

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

	public OperationItem getOpItem() {
		return opItem;
	}

	public void setOpItem(OperationItem opItem) {
		this.opItem = opItem;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}
}
