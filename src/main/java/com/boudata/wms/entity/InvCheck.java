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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.dm.record.ARecordTable;

/**
 * 库存盘点计划
 */
@Entity
@Table(name = "wms_inv_check")
@SequenceGenerator(name = "inv_check_seq", sequenceName = "inv_check_seq")
public class InvCheck extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "inv_check_seq")
	private Long id;
	
	/** 仓库 */
	@ManyToOne
	private _Warehouse warehouse;
	
	/** 货主 */
	@ManyToOne
	private _Owner owner;
	
	private String code;
	
	private Integer type;  // 1: 明盘 0：暗盘
	
	private Integer rounds; // 几轮盘点
	
	@ManyToOne
	private OperationH round1; // 第一轮：初盘
	
	@ManyToOne
	private OperationH round2; // 第二轮：复盘
	
	@ManyToOne
	private OperationH round3; // 第三轮：终盘
		
	private Integer locs;
	private Double  qtys;
	private Integer skus;
	
	private String status;

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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getRounds() {
		return rounds;
	}

	public void setRounds(Integer rounds) {
		this.rounds = rounds;
	}

	public OperationH getRound1() {
		return round1;
	}

	public void setRound1(OperationH round1) {
		this.round1 = round1;
	}

	public OperationH getRound2() {
		return round2;
	}

	public void setRound2(OperationH round2) {
		this.round2 = round2;
	}

	public OperationH getRound3() {
		return round3;
	}

	public void setRound3(OperationH round3) {
		this.round3 = round3;
	}

	public Integer getLocs() {
		return locs;
	}

	public void setLocs(Integer locs) {
		this.locs = locs;
	}

	public Double getQtys() {
		return qtys;
	}

	public void setQtys(Double qtys) {
		this.qtys = qtys;
	}

	public Integer getSkus() {
		return skus;
	}

	public void setSkus(Integer skus) {
		this.skus = skus;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
