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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.boubei.tss.framework.persistence.IEntity;

/**
 * 订单明细——INV
 */
@Entity
@Table(name = "wms_inv_soi")
@SequenceGenerator(name = "inv_soi_sequence", sequenceName = "inv_soi_sequence", initialValue = 1, allocationSize = 10)
public class InvSoi implements IEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "inv_soi_sequence")
	private Long id;
	private Long thread; // 当前线程ID，用以多线程场景
	private Integer candidateIndex;
	
	@Column(nullable=false)
	private Long soi_id;
	
	@Column(nullable=false)
	private Long inv_id;
	
	private String locType;
	
	private Double invQty; 
	private Double soiQty;
	
	private Date invCreatedate;
	private Date invExpiredate;
	private String invStatus;
	private String invLotAtt01;
	private String invLotAtt02;
	private String invLotAtt03;
	private String invLotAtt04;
	
	private Date soiCreatedate;
	private Date soiExpiredate;
	private String soiStatus;
	private String soiLotAtt01;
	private String soiLotAtt02;
	private String soiLotAtt03;
	private String soiLotAtt04;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getThread() {
		return thread;
	}
	public void setThread(Long thread) {
		this.thread = thread;
	}
	public Long getSoi_id() {
		return soi_id;
	}
	public void setSoi_id(Long soi_id) {
		this.soi_id = soi_id;
	}
	public Long getInv_id() {
		return inv_id;
	}
	public void setInv_id(Long inv_id) {
		this.inv_id = inv_id;
	}
	public String getLocType() {
		return locType;
	}
	public void setLocType(String locType) {
		this.locType = locType;
	}
	public Double getInvQty() {
		return invQty;
	}
	public void setInvQty(Double invQty) {
		this.invQty = invQty;
	}
	public Double getSoiQty() {
		return soiQty;
	}
	public void setSoiQty(Double soiQty) {
		this.soiQty = soiQty;
	}
	public Date getInvCreatedate() {
		return invCreatedate;
	}
	public void setInvCreatedate(Date invCreatedate) {
		this.invCreatedate = invCreatedate;
	}
	public Date getInvExpiredate() {
		return invExpiredate;
	}
	public void setInvExpiredate(Date invExpiredate) {
		this.invExpiredate = invExpiredate;
	}
	public String getInvStatus() {
		return invStatus;
	}
	public void setInvStatus(String invStatus) {
		this.invStatus = invStatus;
	}
	public String getInvLotAtt01() {
		return invLotAtt01;
	}
	public void setInvLotAtt01(String invLotAtt01) {
		this.invLotAtt01 = invLotAtt01;
	}
	public String getInvLotAtt02() {
		return invLotAtt02;
	}
	public void setInvLotAtt02(String invLotAtt02) {
		this.invLotAtt02 = invLotAtt02;
	}
	public String getInvLotAtt03() {
		return invLotAtt03;
	}
	public void setInvLotAtt03(String invLotAtt03) {
		this.invLotAtt03 = invLotAtt03;
	}
	public String getInvLotAtt04() {
		return invLotAtt04;
	}
	public void setInvLotAtt04(String invLotAtt04) {
		this.invLotAtt04 = invLotAtt04;
	}
	public Date getSoiCreatedate() {
		return soiCreatedate;
	}
	public void setSoiCreatedate(Date soiCreatedate) {
		this.soiCreatedate = soiCreatedate;
	}
	public Date getSoiExpiredate() {
		return soiExpiredate;
	}
	public void setSoiExpiredate(Date soiExpiredate) {
		this.soiExpiredate = soiExpiredate;
	}
	public String getSoiStatus() {
		return soiStatus;
	}
	public void setSoiStatus(String soiStatus) {
		this.soiStatus = soiStatus;
	}
	public String getSoiLotAtt01() {
		return soiLotAtt01;
	}
	public void setSoiLotAtt01(String soiLotAtt01) {
		this.soiLotAtt01 = soiLotAtt01;
	}
	public String getSoiLotAtt02() {
		return soiLotAtt02;
	}
	public void setSoiLotAtt02(String soiLotAtt02) {
		this.soiLotAtt02 = soiLotAtt02;
	}
	public String getSoiLotAtt03() {
		return soiLotAtt03;
	}
	public void setSoiLotAtt03(String soiLotAtt03) {
		this.soiLotAtt03 = soiLotAtt03;
	}
	public String getSoiLotAtt04() {
		return soiLotAtt04;
	}
	public void setSoiLotAtt04(String soiLotAtt04) {
		this.soiLotAtt04 = soiLotAtt04;
	}
	
	public Serializable getPK() {
		return this.getId();
	}
	public Integer getCandidateIndex() {
		return candidateIndex;
	}
	public void setCandidateIndex(Integer candidateIndex) {
		this.candidateIndex = candidateIndex;
	}
}