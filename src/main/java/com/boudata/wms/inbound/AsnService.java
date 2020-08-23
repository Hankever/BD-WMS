/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inbound;

import java.util.List;

import com.boubei.tss.modules.log.Logable;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity._Rule;

public interface AsnService {
	
	Asn getAsn(Long id);
	
	@Logable(operateObject="入库单", operateInfo="新建：${returnVal} ")
	Asn createAsn(Asn asn, List<AsnItem> items);

	@Logable(operateObject="入库单", operateInfo="删除：${args[0]} : ${returnVal} ")
	Asn deleteAsn(Long id);
	
	/**
	 * 小程序 入库
	 * 
	 * @param asn
	 * @param items
	 */
	void createAndInbound(Asn asn, List<AsnItem> items);
	
	/**
	 * 入库：直接依据Asn单进行入库。
	 */
	void inbound(Long asnId, List<AsnItem> itemList);
	
	/**
	 * 网页 验货入库
	 * 
	 * @param items
	 * @param toloccode
	 */
	void asnCKInbound(Long id, List<AsnItem> items);
	
	/**
	 * 小程序 作业单 验货入库
	 * 
	 * @param id
	 * @param items
	 * @param op
	 */
	void asnCKInbound4rf(Long id, List<AsnItem> items, OperationH op);

	/**
	 * 网页 入库单指派入库作业人（入库员、卸货员）
	 * 
	 * @param asnId
	 * @param worker
	 */
	@Logable(operateObject="入库单", operateInfo="指派入库工单 ${returnVal} ")
	OperationH assign(Long asnId, String worker, String type);
	
	/**
	 * 取消入库：通过操作一个调整单，将已经增加库存数量清零。
	 */
	void cancelInbound(Long asnId);
	
	/**
	 * 取消：入库单据取消，将新建的作业单删除。
	 */
	void cancelAsn(Long asnId, String reason);

	@Logable(operateObject="入库单", operateInfo="关闭：${returnVal} ")
	Asn closeAsn(Long asnId);
	
	/**
	 * 预上架：根据上架规则生成上架指导单，支持批量操作
	 */
	List<OperationItem> prePutaway(Long asnId, List<_Rule> rList);
	
	/**
	 * 保存上架单且执行上架操作
	 */
	OperationH putaway(Asn asn, List<OperationItem> items);

}
