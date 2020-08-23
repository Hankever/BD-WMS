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

public interface EDIService {
	
	Map<String, Object> receiveOrder(Map<String, ?> map);

	Map<String, Object> receiveSKUs(List<Map<String, Object>> list);

	Map<String, Object> receiveAsn(Map<String, ?> map);

	Map<String, Object> cancelOrder(String code, String reason);

	Map<String, Object> cancelAsn(String code, String reason);

	Map<String, Object> receiveCustomers(List<Map<String, Object>> list);

}
