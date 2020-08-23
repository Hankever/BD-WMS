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
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;

/**
 * 库位定义
 */
@Entity
@Table(name = "wms_location", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_LOC_CODE_WH", columnNames = { "warehouse_id", "code" })
})
@SequenceGenerator(name = "location_seq", sequenceName = "location_seq")
public class _Location extends ARecordTable {
	
	public _Location() { }
	public _Location(Long id) { this.id = id; }
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "location_seq")
	private Long id;
	
	/** 所属仓库 */
	@ManyToOne
	private _Warehouse warehouse;
 
	/** 容器当前所在的上架库位 */
	@ManyToOne
	private _Location parent;
	
	/** 库位类型（用途）*/
	@ManyToOne
    private Param type;
	
	@Column(length = 128, nullable = false)
	private String name;

	/** 库位编号 */
	@Column(length = 128, nullable = false)
	private String code;
	private String rack; // 货架
	private String zone; // 库区
	
	/** 库位高 */
	private Double height = 0d;

	/** 库位长 */
	private Double len = 0d;

	/** 库位宽 */
	private Double width = 0d;
	
	/** 最大容量（EA） */
	private Integer capacity;

	/** 该库位是否被锁定 */
	private Integer holding = ParamConstants.FALSE;

	/** 该库位是否正在被盘点 */
	private Integer checking = ParamConstants.FALSE;
	
	/** 库位描述 */
	private String remark;
	
	/** 仓库状态：启用/停用 */
	private Integer status = ParamConstants.TRUE;
	
	public boolean isFrozen() {
		return _isChecking() || _isHolding();
	}
	public boolean _isChecking() {
		return ParamConstants.TRUE.equals(checking); // 盘点中
	}
	public boolean _isHolding() {
		return ParamConstants.TRUE.equals(holding);  // 锁定中
	}
	
	public boolean isContainer() {
		return  WMS.LOC_TYPE_MV.equals( type.getText() );
	}
	
	@Transient public Double qty = 0D; // 库存量
	@Transient public int loc1 = 0;    // 作业量
	@Transient public int loc2 = 0;    // 完成量
	
	public String toString() {
    	return this.getCode() + ", capacity = " + this.capacity + ", qty = " + qty;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public _Warehouse getWarehouse() {
		return warehouse;
	}

	public void setWarehouse(_Warehouse warehouse) {
		this.warehouse = warehouse;
	}

	public Double getHeight() {
		return height;
	}

	public void setHeight(Double height) {
		this.height = height;
	}

	public Double getLen() {
		return len;
	}

	public void setLen(Double len) {
		this.len = len;
	}

	public Double getWidth() {
		return width;
	}

	public void setWidth(Double width) {
		this.width = width;
	}

	public Param getType() {
		return type;
	}

	public void setType(Param type) {
		this.type = type;
	}

	public Integer getHolding() {
		return holding;
	}

	public void setHolding(Integer holding) {
		this.holding = holding;
	}

	public Integer getChecking() {
		return checking;
	}

	public void setChecking(Integer checking) {
		this.checking = checking;
	}

	public String getZone() {
		return zone;
	}

	public void setZone(String zone) {
		this.zone = zone;
	}

	public String getRack() {
		return rack;
	}

	public void setRack(String rack) {
		this.rack = rack;
	}
	
	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}
	
	public Integer getCapacity() {
		return EasyUtils.obj2Int( capacity ) == 0 ? 999999 : capacity;
	}
	
	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}
	public _Location getParent() {
		return parent;
	}
	public void setParent(_Location parent) {
		this.parent = parent;
	}
}
