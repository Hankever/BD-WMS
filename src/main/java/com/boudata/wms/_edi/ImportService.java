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

import com.boubei.tss.modules.progress.Progress;

public interface ImportService {
	
	String importAsn(Long warehouse_id, Long owner_id, List<Map<String, String>> rows, Progress progress);
	
	String importOrder(Long warehouse_id, Long owner_id, List<Map<String, String>> rows, Progress progress);

}
