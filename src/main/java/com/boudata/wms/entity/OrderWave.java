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

import com.boubei.tss.dm.record.ARecordTable;
import com.boudata.wms.WMS;

@Entity
@Table(name = "wms_order_wave", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_ORDER_WAVE_WH", columnNames = { "warehouse_id", "code" })
})
@SequenceGenerator(name = "order_wave_seq", sequenceName = "order_wave_seq")
public class OrderWave extends ARecordTable {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "order_wave_seq")
	private Long id;
	
	/** 仓库 */
	@ManyToOne
	private _Warehouse warehouse;
	
	/** 波次号 */
	private String code;
	
	/** 总订单数 */
	private Integer total;
	
	/** 创建、取消、分配完成、拣货中、拣货完成、关闭 */
    private String status;
    
    /** 按单分配、波次分配 */
    private String origin = WMS.W_ORIGIN_01;
    
    @Transient public List<_Rule> rules = new ArrayList<>();
    
    public boolean isWave() {
    	return WMS.W_ORIGIN_02.equals(origin);
    }
	
    public String toString() {
    	return "波次: " + code + ", " + warehouse.getName() + ", " + total + ", " + status;
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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public _Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(_Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}
}
