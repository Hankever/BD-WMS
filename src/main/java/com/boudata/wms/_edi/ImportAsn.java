/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._edi;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.dm.record.file.ImportCSV;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.modules.progress.ProgressPool;
import com.boubei.tss.util.EasyUtils;

/**
 * 入库单号 下单日期 货品 货品编码 订单数量 单价 金额 批号 包装量 生产日期 过期日期 备注
 */
public class ImportAsn extends ImportCSV {
	
	ImportService impService = (ImportService) Global.getBean("ImportService");

	protected String import2db(_Database _db, HttpServletRequest request, 
			List<List<String>> rows, List<String> headers, List<String> originData,
			List<Integer> errLineIndexs, String fileName) {
		
		String warehouse = request.getParameter("warehouse");
		String owner = request.getParameter("owner");
		if( EasyUtils.isNullOrEmpty(warehouse) ) {
			throw new BusinessException("请先选择一个作业仓库");
		}
		if( EasyUtils.isNullOrEmpty(owner) ) {
			throw new BusinessException("请先选择本次导入订单所属货主");
		}
		
		Long warehouse_id = EasyUtils.obj2Long(warehouse);
		Long owner_id = EasyUtils.obj2Long(owner);
		
		Progress progress = ProgressPool.getSchedule( getPgCode(request, _db) );
		
		List<Map<String, String>> valuesMaps = EDIUtil.csv2Map(_db, rows, headers);
		String result = impService.importAsn(warehouse_id, owner_id, valuesMaps, progress);
		
		return result;
	}
}
