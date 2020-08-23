/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.Map;

import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;

public interface OperationService {
	
	/**
	 * 指派工单
	 * 
	 * @param whId
	 * @param opno
	 * @param worker
	 * @return
	 */
	Map<String, Object> assignOp(Long whId, String opno, String worker);

	/**
	 * 自主扫描作业单号，领用作业单
	 * 
	 * @param whId
	 * @param opno
	 * @return
	 */
	Map<String, Object> takeJob(Long whId, String opno);
	
	/**
	 * 自主领用 新建状态的 入库单/出库单
	 * 
	 * @param whId
	 * @param code
	 * @param type
	 * @return
	 */
	Map<String, Object> takeIOJob(Long whId, String code, String type);
	
	/**
	 * 作业单异常反馈
	 */
	OpException opException(String pkdIds, String content, String remark, String type);
	
	/**
	 * 关闭异常反馈
	 */
	void closeException(Long pkdId, Long opeId, String result);
	
	/**
	 * 小程序 领用/拒绝 作业单
	 */
	void acceptConfirm(Long opId, String status, String remark);
	
	/**
	 * 验货入库 异常反馈
	 * 
	 * @param op
	 * @param content
	 * @param remark
	 */
	void opCheckException(OperationH op, String content, String remark);
	
	/**
	 * 无单入库/出库/移库 作业单 指派审核人
	 * 
	 * @param approver
	 * @param type
	 * @param value
	 */
	void approveOp(String approver, String type, String codeOrId);
}
