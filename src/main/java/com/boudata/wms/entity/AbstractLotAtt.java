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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.MappedSuperclass;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.dto._DTO;

/**
 * 批次属性抽象类。
 * 
 * 批次属性支持自定义：是否显示、批次属性自定义名称等
 */
@MappedSuperclass
public abstract class AbstractLotAtt extends ARecordTable {
	
	/** 生产日期 */
    protected Date createdate;
    
    /** 过期日期 */
    protected Date expiredate;

    /** 货物状态：良品、不良品、破损、过期 */
    protected String invstatus;

    /** 备用批次属性01--04 */
    protected String lotatt01;
    protected String lotatt02;
    protected String lotatt03;
    protected String lotatt04;
    
    protected abstract Double getQty();
    
    public Double getQty_uom() {
    	if( lotatt02 != null && EasyUtils.isDigit( lotatt02 ) ) {
    		int uom = EasyUtils.obj2Int( lotatt02 );
    		if( uom > 1) {
    			try {
    				return Math.floor(this.getQty() * 10 / uom) / 10d;
    			} catch( Exception e ) {
    			}
    		}
    	}
    	return null;
    }
    
    public String toString() {
    	String lot = getLot();
		return EasyUtils.isNullOrEmpty(lot) ? "" : "批次【" + lot + "】";
    }
    
    public String getLot() {
    	List<String> lots = new ArrayList<>();
    	if( !EasyUtils.isNullOrEmpty(lotatt01) ) {
    		lots.add(lotatt01);
    	}
    	if( !EasyUtils.isNullOrEmpty(lotatt02) ) {
    		lots.add(lotatt02);
    	}
    	if( !EasyUtils.isNullOrEmpty(lotatt03) ) {
    		lots.add(lotatt03);
    	}
    	if( !EasyUtils.isNullOrEmpty(lotatt04) ) {
    		lots.add(lotatt04);
    	}
    	if( !EasyUtils.isNullOrEmpty(invstatus) ) {
    		lots.add(invstatus);
    	}
    	if( !EasyUtils.isNullOrEmpty(createdate) ) {
    		lots.add( DateUtil.format(createdate) );
    	}
    	if( !EasyUtils.isNullOrEmpty(expiredate) ) {
    		lots.add( DateUtil.format(expiredate) );
    	}
    	return EasyUtils.list2Str(lots);
    }
    
    public void copyLotAtt(AbstractLotAtt from) {
    	this.setLotatt01(from.getLotatt01());
        this.setLotatt02(from.getLotatt02());
        this.setLotatt03(from.getLotatt03());
        this.setLotatt04(from.getLotatt04());
        this.setCreatedate(from.getCreatedate());
        this.setExpiredate(from.getExpiredate());
        this.setInvstatus(from.getInvstatus());
    }
    
    public void copyLotAtt(InventoryTemp from) {
    	this.setLotatt01(from.getLotatt01());
        this.setLotatt02(from.getLotatt02());
        this.setLotatt03(from.getLotatt03());
        this.setLotatt04(from.getLotatt04());
        this.setCreatedate(from.getCreatedate());
        this.setExpiredate(from.getExpiredate());
        this.setInvstatus(from.getInvstatus());
    }
    
    public void copyLotAtt(_DTO from) {
    	this.setLotatt01(from.getLotatt01());
        this.setLotatt02(from.getLotatt02());
        this.setLotatt03(from.getLotatt03());
        this.setLotatt04(from.getLotatt04());
        this.setCreatedate(from.getCreatedate());
        this.setExpiredate(from.getExpiredate());
        this.setInvstatus(from.getInvstatus());
    }
    
    public void copyLotAtt( Map<String, String> map ) {
    	this.setLotatt01( map.get("lotatt01") );
		this.setLotatt02( map.get("lotatt02") );
		this.setLotatt03( map.get("lotatt03") );
		this.setLotatt04( map.get("lotatt04") );
		this.setInvstatus( map.get("invstatus") );
		this.setCreatedate( DateUtil.parse(map.get("createdate")) );
		this.setExpiredate( DateUtil.parse(map.get("expiredate")) );
    }
    
    /** 比较两个对象的批次是否严格匹配。*/
    public boolean compareLotAtt(AbstractLotAtt target) {
        return compare(this.createdate, target.createdate) 
        		&& compare(this.expiredate, target.expiredate) 
        		&& compare(this.invstatus, target.invstatus)
        		&& compare(this.lotatt01, target.lotatt01)
        		&& compare(this.lotatt02, target.lotatt02)
        		&& compare(this.lotatt03, target.lotatt03)
                && compare(this.lotatt04, target.lotatt04);
    }
    
    private static boolean compare(Object before, Object after) {        
        return EasyUtils.obj2String(before).equals( EasyUtils.obj2String(after) );
    }

	public Date getCreatedate() {
		return createdate;
	}

	public void setCreatedate(Date createdDate) {
		this.createdate = createdDate;
	}

	public Date getExpiredate() {
		return expiredate;
	}

	public void setExpiredate(Date expiredDate) {
		this.expiredate = expiredDate;
	}

	public String getInvstatus() {
		return invstatus;
	}

	public void setInvstatus(String invstatus) {
		this.invstatus = EasyUtils.isNullOrEmpty(invstatus) ? null : invstatus;
	}

	public String getLotatt01() {
		return lotatt01;
	}

	public void setLotatt01(String lotatt01) {
		this.lotatt01 = EasyUtils.isNullOrEmpty(lotatt01) ? null : lotatt01;
	}

	public String getLotatt02() {
		return lotatt02;
	}

	public void setLotatt02(String lotatt02) {
		this.lotatt02 = EasyUtils.isNullOrEmpty(lotatt02) ? null : lotatt02;
	}

	public String getLotatt03() {
		return lotatt03;
	}

	public void setLotatt03(String lotatt03) {
		this.lotatt03 = EasyUtils.isNullOrEmpty(lotatt03) ? null : lotatt03;
	}

	public String getLotatt04() {
		return lotatt04;
	}

	public void setLotatt04(String lotatt04) {
		this.lotatt04 = EasyUtils.isNullOrEmpty(lotatt04) ? null : lotatt04;
	}
}
