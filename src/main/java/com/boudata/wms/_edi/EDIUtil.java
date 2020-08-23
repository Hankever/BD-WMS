/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms._edi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.dm.ddl._Database;
import com.boubei.tss.util.EasyUtils;

public class EDIUtil {
	
	public static List<Map<String, String>> csv2Map(_Database _db, List<List<String>> rows, List<String> headers) {
		
		int total = rows.size();
		List<Map<String, String>> valuesMaps = new ArrayList<Map<String, String>>();
		
		for(int index = 1; index < total; index++) { // 第一行为表头，不要
			List<String> fieldVals = rows.get(index);
			Map<String, String> valuesMap = new HashMap<String, String>();
			for(int j = 0; j < fieldVals.size(); j++) {
				String value = fieldVals.get(j).trim();
				
				String filedLabel = headers.get(j);
				String fieldCode = _db.ncm.get(filedLabel);
				
				if( !EasyUtils.isNullOrEmpty(value) ) {
					valuesMap.put(fieldCode, value);
				}
	    	}
			
			valuesMaps.add(valuesMap);
		}
		
		return valuesMaps;
	}

}
