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
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.boubei.tss.dm.dml.SqlConfig;
import com.boubei.tss.dm.record.ARecordTable;

/**
 * 作业规则定义
 */
@Entity
@Table(name = "wms_rule", uniqueConstraints = { @UniqueConstraint(columnNames = { "domain", "type", "code" }) })
@SequenceGenerator(name = "rule_seq", sequenceName = "rule_seq", initialValue = 1, allocationSize = 10)
public class _Rule extends ARecordTable {
	
	public static String DEFAULT_PK_RULE = "10 from 库存表 order by 生产日期 升序 into 最终结果集;";
	public static String DEFAULT_WV_RULE = "<rule>" +
											"	<step index=\"1\"><include>ALL.OIs</include></step>" +
											"	<subwave name=\"S\"><include>ALL.PKDs</include></subwave>" +
											"</rule>";
	public static String DEFAULT_PW_RULE = SqlConfig.getScript("emptyLocFirst");
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "rule_seq")
    private Long id;

    /**
     * 规则类型：
     * 1、拣货规则（PICKUP_RULE）
     * 2、波次规则（WAVE_RULE）
     * 3、上架规则（PUTAWAY_RULE）
     */
    private String type;
    
    /** 规则代码 */
    @Column(length = 128, nullable = false)
    private String code;
    
    /** 规则名称 */
    @Column(length = 128, nullable = false)
    private String name;
    
    /** 规则的内容 */
    @Lob
    @Column(nullable = false)
    private String content;
    
    /** 规则的备注 */
    private String remark;
    
	/** 状态：启用/停用 */
	private Integer status;

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
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

	public String getType() {
		return type;
	}

	public void setType(String ruleType) {
		this.type = ruleType;
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
}
