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

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms._Util;

/**
 * https://www.boudata.com/bd/wms/wms_api.html
 */
@Controller
@RequestMapping("/wms/edi")
public class EDI {
	
	protected Logger log = Logger.getLogger(this.getClass());

	@Autowired public EDIService ediService;
	
	//---------------------------------------------------------------- 外部数据流入WMS  --------------------------------------------------------
	
	/**
	 * 接收同步SKU信息
	 */
	@RequestMapping(value = "/sku", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> receiveSKUs(String params) {
		List<Map<String, Object>> list = _Util.json2List(params);
		return ediService.receiveSKUs(list);
	}
	
	/**
	 * 接收同步往来方（供货方、门店、客户等）信息
	 */
	@RequestMapping(value = "/customer", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> receiveCustomers(String params) {
		List<Map<String, Object>> list = _Util.json2List(params);
		return ediService.receiveCustomers(list);
	}
	
	/**
	 * 查询库存
	 * @param warehouse 仓库编码或名称
	 * @param skuList  barcode逗号分隔
	 * @return
	 */
	@RequestMapping(value = "/inv", method = RequestMethod.POST)
	@ResponseBody
	public List<?> queryInv(String warehouse, String owner, String skuList, String invStatus) {
		skuList = DMUtil.insertSingleQuotes(skuList);
		String sql = "select sku.code, sku.barcode, sku.name, round(sum(qty)) qty "
				+ "	from wms_inv inv, wms_sku sku, wms_warehouse wh, wms_owner ow "
				+ "	where inv.sku_id = sku.id and inv.wh_id = wh.id and inv.owner_id = ow.id "
				+ "   and inv.domain = ? "
				+ "   and ? in (wh.name, wh.code) "
				+ "   and ? in (ow.name, ow.code) "
				+ "   and (sku.code in (" +skuList+ ") or sku.barcode in (" +skuList+ ") )";
		if(!EasyUtils.isNullOrEmpty(invStatus)){
			sql += " and inv.invstatus = '" + invStatus + "'";
		}
		sql += " group by sku.code";
		return SQLExcutor.queryL(sql, Environment.getDomain(), warehouse, owner);
	}

	/**
	 * 接收出库通知单
	 * { code: 'xxx', warehouse: '仓库编码或名称'， owner: '货主编码或名称', orderday: '2020-02-02', d_receiver, d_mobile, d_addr, udfx, remark,
	 * 	 items: [
	 * 		{code: 'sxxx', qty: 100, lotattx: 'xxx', }
	 * 	 ]
	 * }
	 */
	@RequestMapping(value = "/order", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> receiveOrder(String params) {
		Map<String, ?> map = _Util.json2Map(params);
		return ediService.receiveOrder(map);
	}
	
	/**
	 * 接收外部取消出库指令
	 */
	@RequestMapping(value = "/order/cancel", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> cancelOrder(String code, String reason) {
		return ediService.cancelOrder(code, reason);
	}
	
	/**
	 * 接收入库通知单
	 */
	@RequestMapping(value = "/asn", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> receiveAsn(String params) {
		Map<String, ?> map = _Util.json2Map(params);
		return ediService.receiveAsn(map);
	}

	/**
	 * 接收外部取消入库指令
	 */
	@RequestMapping(value = "/asn/cancel", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> cancelAsn(String code, String reason) {
		return ediService.cancelAsn(code, reason);
	}
	
	//---------------------------------------------------------------- WMS向外推送数据  --------------------------------------------------------
	
	/**
	 * 1、反馈出库单作业结果：在 AfterOpEnvet 里配置
	 * 2、反馈入库单作业结果：在 AfterOpEnvet 里配置
	 * 3、推送出库单（取消）到TMS：  在 AfterOpEnvet 里配置 （ 推送TMS 写一个标准版， 看EDIKey里有没有Key，有的话推送）
	 * 4、反馈最新库存信息：  在 OwnerReportJob里触发，在owner上配置接收接口相关参数（url、key、字段格式等）
	 */
}
