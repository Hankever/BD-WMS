/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.List;
import java.util.Map;

import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.InvCheck;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;

/**
 * 出入库是现有单子后有库存， 库内操作是是依据既有库存操作（即先有库存后有单子）
 */
public interface InventoryService {
	
	/**
	 * 生成移库的作业单并执行移库（扫描移出库位，SKU，移入库位）
	 * 注：需通过移动库位（小推车等）中转，每次移出移入都应该扫描移动库位。
	 * 移出生成一张移库作业单，移入再单独生成一张作业单。如此，一条库存移动到多个库位的情形就比较好解决了。
	 */
	OperationH move(Long whId, String loccode, String toloccode, List<_DTO> list);
	
	void containerMove(Long whId, String loccode, String toloccode, String type, List<_DTO> mvItems);
	
	OperationH execOperations(OperationH op, List<OperationItem> items);
	
	OperationH execOperations(Long opId);

	OperationH closeOp(Long opId);

	List<OperationLog> operationLog(Long opId);

	List<InventoryLog> inventoryLog(Long opId);

	Map<String,Object> search(InventorySo ic, boolean with_skux);
	
	OperationH invCheckCommit(Long whId, List<OperationItem> items, String remark);
	
	OperationH createInvCheck(InvCheck invCheck, Long whId, List<OperationItem> items, int round);
	
	void saveInvCheckData(List<_DTO> list, Long invCheckId, int round);

	Map<String, Object> adjustInvByCheckResult(Long invCheckId, Long opId, String itemIds, String type);
	
	/**
	 * 强制释放冻结库存
	 */
	void unlock(Long id, Double qty_unlock);
	
	/**
	 * 更改作业明细（PKD等）对应的库存
	 */
	void changeOPIInv(Long opiId, Long toInvId);

	/**
	 * 取消盘点单
	 */
	void cancelInvCheck(Long id);

	/**
	 * 关闭盘点单
	 */
	void closeInvCheck(Long id);
}
