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
import javax.persistence.Transient;

import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import com.boubei.tss.util.MathUtil;

/**
 * 库存记录，含以下维度：
 * 1、仓库
 * 2、货主
 * 3、货品
 * 4、库位
 * 5、批次（包装、货品状态、生产日期、过期日期、批次号等）
 */
@Entity
@Table(name = "wms_inv")
@SequenceGenerator(name = "inv_seq", sequenceName = "inv_seq")
@JsonIgnoreProperties(value={"pk", "operation", "asnItem"})
public class Inventory extends AbstractLotAtt implements Cloneable {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "inv_seq")
	private Long id;

	/** 货品 */
	@ManyToOne
	private _Sku sku;
	
	/** 货主 */
	@ManyToOne
	private _Owner owner;

	/** 库位 */
	@ManyToOne
	private _Location location;
	
	/** 仓库 */
	@ManyToOne
	private _Warehouse wh;
	
	/** 库存量 */
	private Double qty = 0D;

	/** 锁定量 */
	private Double qty_locked = 0D;
	
	//------------------------ @Transient ----------------------------------------------------------
	
    /** 操作本库存的单据号，用以记录库存流水日志。*/
    @Transient
    public OperationH operation;
    
    /** 刚入库库存对应的入库单明细 */
    @Transient
    public AsnItem asnItem;
    
    /** 建议拣货量，在根据拣货规则对库存进行排序过滤时使用。*/
    @Transient
    public double qty_advice = 0d;

	/** 当前改变本库存数量的作业单号 */
	@Transient
	public String opNo;
	
	
	public Inventory() {
	}
 
    public String toString() {
    	return "inv_" + id + "[" + sku.getName() + ", " + super.toString() 
    			+ " " + location.getCode() + ", " + owner + "], " + qty + ", " + qty_locked + (getLocation().isFrozen() ? ", 冻结" : "");
    }
    
    public Inventory clone() {
    	Inventory inv = null;
        try {
        	inv = (Inventory) super.clone();
        } 
        catch (Exception e) {
        }
        
        return mockClone(inv);
    }
    
    public Inventory mockClone(Inventory inv) {
    	if(inv != null) return inv;
    	
        inv = new Inventory();
        try {
            PropertyUtils.copyProperties(inv, this);
        } catch (Exception e1) { }
        
        return inv;
    }
    
    /**
     * 比较两条库存是否所有维度值都一样，是的话返回True。
     * 用于批量新建LLU时，防止创建出所有维度一样但不是同一条记录的多条LLU记录。
     * 比如：拣货时，有多个PKD拣的是同一库存，出货库位也一致，这时新建一条拣货到LLU即够了。
     * 
     * @param other
     * @return
     */
    public boolean compare(Inventory other) {
        if( this.id != null && this.id.equals(other.id) ) return true;
        
        if(!this.owner.equals(other.owner)) return false;
        if(!this.sku.equals(other.sku)) return false;
        if(!this.location.equals(other.location)) return false;
        
        if(!this.compareLotAtt(other)) return false;
        
        return true;
    }
    
    public Double getQty_avaiable() {
    	return MathUtil.subDoubles(this.qty, this.qty_locked);
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

	public _Sku getSku() {
		return sku;
	}

	public void setSku(_Sku sku) {
		this.sku = sku;
	}

	public _Owner getOwner() {
		return owner;
	}

	public void setOwner(_Owner owner) {
		this.owner = owner;
	}
 
	public _Location getLocation() {
		return location;
	}

	public void setLocation(_Location location) {
		this.location = location;
	}

	public Double getQty() {
		return qty;
	}

	public void setQty(Double qty) {
		this.qty = qty;
	}

	public Double getQty_locked() {
		return qty_locked;
	}

	public void setQty_locked(Double qty_locked) {
		this.qty_locked = Math.max(0, qty_locked) ;
	}

	public _Warehouse getWh() {
		return wh;
	}

	public void setWh(_Warehouse wh) {
		this.wh = wh;
	}
}
