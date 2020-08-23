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
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.util.DateUtil;

/**
 * 作业日志（作为作业类消息反馈的依据）
 * 
 * 一个作业单可能被多个人一起完成作业，每个人作业一次都将生成一条作业日志。
 */
@Entity
@Table(name = "wms_operation_log")
@SequenceGenerator(name = "operation_log_seq", sequenceName = "operation_log_seq")
public class OperationLog implements IEntity {

	public OperationLog() {
		this.setId(null);
		this.setCreateTime(new Date());
		this.setOperator( Environment.getUserId() );
		this.setOperatorName( Environment.getUserName() );
		this.setOrigin(Environment.getOrigin());
    }
 
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "operation_log_seq")
    private Long id;
    
    /** 作业单 */
    @ManyToOne
    private OperationH operation;
    
    /**
     * 作业类型
     */
    @ManyToOne
    private Param opType;
    
    /** 作业人、作业时间、作业终端 */
    private Long   operator;
    private String operatorName;
    private Date   operateTime;
    private String origin;
    
    private Date   createTime;
    private String remark;

	public String toString() {
    	String type = opType.getText();
		return "作业日志" + id + "：" + type + ", " + operation + ", " + DateUtil.formatCare2Second(operateTime) ;
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

	public Long getOperator() {
		return operator;
	}

	public void setOperator(Long operator) {
		this.operator = operator;
	}

	public Date getOperateTime() {
		return operateTime;
	}

	public void setOperateTime(Date operateTime) {
		this.operateTime = operateTime;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Param getOpType() {
		return opType;
	}

	public void setOpType(Param opType) {
		this.opType = opType;
	}
    
    public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}
