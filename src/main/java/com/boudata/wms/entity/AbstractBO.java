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

import javax.persistence.Column;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.modules.param.ParamConstants;

/**
 * 业务基础对象基类
 */
@MappedSuperclass
public abstract class AbstractBO extends ARecordTable {
	
	@Column(length = 128, nullable = false)
	protected String code;

	@Column(length = 128, nullable = false)
	protected String name;

	@ManyToOne
	protected _Rule pickupr;
	
	@ManyToOne
	protected _Rule waver;
	
	@ManyToOne
	protected _Rule putawayr;
	
	/** 仓库状态：启用/停用 */
	protected Integer status = ParamConstants.TRUE;
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public _Rule getPickupr() {
		return pickupr;
	}

	public void setPickupr(_Rule pickupr) {
		this.pickupr = pickupr;
	}

	public _Rule getWaver() {
		return waver;
	}

	public void setWaver(_Rule waver) {
		this.waver = waver;
	}

	public _Rule getPutawayr() {
		return putawayr;
	}

	public void setPutawayr(_Rule putawayr) {
		this.putawayr = putawayr;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
}
