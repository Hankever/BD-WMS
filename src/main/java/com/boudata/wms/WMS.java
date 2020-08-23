/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.param.ParamManager;
import com.boubei.tss.modules.param.ParamService;

@Controller
@RequestMapping("/init_wms")
public class WMS {
	
	@Autowired private ParamService paramService;
	
	public static String W1(String domain) {
		return  domain + WmsProduct.DEFAULT_WH_C;
	}
	
	public static String MODULE_WMS = "WMS";
	
	public static String ROLE_MG = "仓储经理";
	public static String ROLE_OW = "货主";
	public static String ROLE_CG = "仓库主管";
	public static String ROLE_OP = "作业人员";
	public static String ROLE_OT = "其它人员";
	public static String ROLE_SP = "供货商";
	
	public static String ROLE_FIN    = "财务";
	public static String ROLE_SELLER = "商家";
	public static String ROLE_BUYER  = "下单客户";
	
	public static boolean isWorker() { // 判断是否只有作业人员权限
		return isRoleX(ROLE_OP) && !isManager();
	}
	public static boolean isOWner() { // 判断是否为货主
		return isRoleX(ROLE_OW);
	}
	public static boolean isManager() { // 判断是否为仓库管理员 或者 仓储经理
		return isRoleX(ROLE_CG, ROLE_MG);
	}
	public static boolean isSupplier() { // 判断是否为供货商
		return isRoleX(ROLE_SP);
	}
	public static boolean isSeller() {
		return isRoleX(ROLE_SELLER);
	}
	
	public static boolean isRoleX(String...roles) { // 判断是否拥有所列角色之一
		List<String> ownRoles = Environment.getOwnRoleNames();
		for( String role : roles ) {
			if( ownRoles.contains(role) ) {
				return true;
			}
		}
		return false;
	}
	public static void checkRoleX(String...roles) {
		if( !isRoleX(roles) ) {
			throw new BusinessException("操作失败了，权限不足");
		}
	}
	
	public static boolean no_asn_inbound() {
		return !"0".equals( Environment.getInSession("no_asn_inbound") );  // 禁止作业人员无单入库（默认允许）
	}
	public static boolean no_order_outbound() {
		return !"0".equals( Environment.getInSession("no_order_outbound") );  // 禁止作业人员无单出库（默认允许）
	}
	public static boolean auto_create_box() {
		return !"0".equals( Environment.getInSession("auto_create_box") );
	}
	public static boolean auto_check_outbound() {
		return  "1".equals( Environment.getInSession("auto_check_outbound") ); // 验货完成后自动出库
	}
	public static boolean outbound_confirm() {
		return  "1".equals( Environment.getInSession("outbound_confirm") );
	}
	public static boolean inv_check_approve() {
		 return "1".equals( Environment.getInSession("js_inv_check_approve") );
	}
	public static boolean negative_inv_qty() {
		return  "1".equals( Environment.getInSession("negative_inv_qty") );
	}
	public static boolean checkin_with_lot() {
		return  "1".equals( Environment.getInSession("js_checkin_with_lot") );
	}
	public static boolean can_beyond_asn() {
		return  "1".equals( Environment.getInSession("js_beyond_asn") );
	}
	
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public Object init() {
		Param group = ParamManager.addParamGroup(ParamConstants.DEFAULT_PARENT_ID, "WMS配置组");
		
        addComboParam(LOC_TYPE, "库位类型", _LOC_TYPES, group);
        addComboParam(ASN_STATUS, "ASN状态", _ASN_STATUS, group);
        addComboParam(O_STATUS, "订单状态", _O_STATUS, group);
        addComboParam(W_STATUS, "波次状态", _W_STATUS, group);
        addComboParam(OP_STATUS, "作业单状态", _OP_STATUS, group);
        addComboParam(OP_TYPE, "作业类型", _OP_TYPE, group);
        addComboParam(INV_STATUS, "盘点状态", _INV_STATUS, group);
        
		return new Object[] { "Success" };
	}
	
	public static String LOC_TYPE = "LocType";   // 库位用途
	public static String LOC_TYPE_IN  = "收货区";
	public static String LOC_TYPE_OUT = "出货区";
	public static String LOC_TYPE_PK  = "拣选区";
	public static String LOC_TYPE_CC  = "存储区";
	public static String LOC_TYPE_MV  = "中转容器";
	public static String LOC_TYPE_JG  = "加工区";
	public static String LOC_TYPE_OT  = "其它";
	public static String[] _LOC_TYPES = new String[] {LOC_TYPE_IN, LOC_TYPE_OUT, LOC_TYPE_PK, LOC_TYPE_CC, LOC_TYPE_MV, LOC_TYPE_JG, LOC_TYPE_OT};
	
	public static String ASN_STATUS = "AsnStatus";
	public static String ASN_STATUS_01 = "新建";
	public static String ASN_STATUS_02 = "取消";
	public static String ASN_STATUS_03 = "部分入库";
	public static String ASN_STATUS_04 = "已完成";
	public static String ASN_STATUS_05 = "入库取消";
	public static String ASN_STATUS_00 = "关闭";
	public static String[] _ASN_STATUS = new String[] { ASN_STATUS_01, ASN_STATUS_02, ASN_STATUS_03, ASN_STATUS_04, ASN_STATUS_05, ASN_STATUS_00};
	
	public static String O_STATUS = "OrderStatus";
	public static String O_STATUS_01 = "新建";
	public static String O_STATUS_02 = "取消";
	public static String O_STATUS_03 = "已分配";
	public static String O_STATUS_41 = "部分拣货";
	public static String O_STATUS_42 = "已拣货";
	public static String O_STATUS_05 = "已验货";
	public static String O_STATUS_09 = "部分验货";
	public static String O_STATUS_08 = "部分出库";
	public static String O_STATUS_06 = "已完成";
	public static String O_STATUS_07 = "出库取消";
	public static String O_STATUS_00 = "关闭";
	public static String[] _O_STATUS = new String[] {O_STATUS_01, O_STATUS_02, O_STATUS_03, O_STATUS_41, O_STATUS_42, O_STATUS_05, O_STATUS_09, O_STATUS_06, O_STATUS_07, O_STATUS_08, O_STATUS_00};
	
	public static String W_STATUS = "WaveStatus";
	public static String W_STATUS_01 = "新建";
	public static String W_STATUS_02 = "取消";
	public static String W_STATUS_03 = "已分配";
	public static String W_STATUS_04 = "已拣货";
	public static String W_STATUS_05 = "库存不足";
	public static String W_STATUS_00 = "关闭";
	public static String[] _W_STATUS = new String[] {W_STATUS_01, W_STATUS_02, W_STATUS_03, W_STATUS_04, W_STATUS_05, W_STATUS_00};
	
	public static String W_ORIGIN = "WaveOrigin";
	public static String W_ORIGIN_01 = "拣货分配";
	public static String W_ORIGIN_02 = "波次作业";
	public static String[] _W_ORIGIN = new String[] {W_ORIGIN_01, W_ORIGIN_02};
	
	public static String INV_STATUS = "InvStatus";
	public static String INV_STATUS_00 = "取消";
	public static String INV_STATUS_01 = "初盘中";
	public static String INV_STATUS_02 = "复盘中";
	public static String INV_STATUS_03 = "终盘中";
	public static String INV_STATUS_04 = "盘点完成";
	public static String INV_STATUS_05 = "关闭";
	public static String[] _INV_STATUS = new String[] {INV_STATUS_00, INV_STATUS_01, INV_STATUS_02, INV_STATUS_03, INV_STATUS_04, INV_STATUS_05};
	
	public static String OP_STATUS = "OpStatus";
	public static String OP_STATUS_01 = "新建";
	public static String OP_STATUS_02 = "取消";
	public static String OP_STATUS_03 = "部分完成";
	public static String OP_STATUS_04 = "已完成";
	public static String OP_STATUS_07 = "确认完成";
	public static String OP_STATUS_05 = "已领用";
	public static String OP_STATUS_06 = "拒绝";
	public static String OP_STATUS_00 = "关闭";
	public static String[] _OP_STATUS = new String[] {OP_STATUS_01, OP_STATUS_02, OP_STATUS_03, OP_STATUS_04, OP_STATUS_05, OP_STATUS_06, OP_STATUS_07, OP_STATUS_00};
	
	public static String OP_TYPE = "OpType";
	public static String OP_TYPE_ZX = "装卸";
	public static String OP_TYPE_IN = "入库";
	public static String OP_TYPE_SJ = "上架";
	public static String OP_TYPE_MV = "移库";
	public static String OP_TYPE_BH = "补货";
	public static String OP_TYPE_FP = "分配";
	public static String OP_TYPE_JH = "拣货";
	public static String OP_TYPE_BCJH = "波次拣货";
	public static String OP_TYPE_YH = "验货";
	public static String OP_TYPE_OUT= "出库";
	public static String OP_TYPE_PD = "盘点";
	public static String OP_TYPE_TZ = "调整";
	public static String OP_TYPE_DJ = "冻结";
	public static String OP_TYPE_RQSJ = "容器上架";
	public static String OP_TYPE_RQXJ = "容器下架";
	public static String[] _OP_TYPE = new String[] {OP_TYPE_ZX, OP_TYPE_IN, OP_TYPE_SJ, OP_TYPE_MV, OP_TYPE_BH,
		OP_TYPE_FP, OP_TYPE_JH, OP_TYPE_BCJH, OP_TYPE_YH, OP_TYPE_OUT, OP_TYPE_PD, OP_TYPE_TZ, OP_TYPE_DJ, 
		OP_TYPE_RQSJ, OP_TYPE_RQXJ };
	
	public static String OP_XH = "XH"; // 卸货
	public static String OP_ZH = "ZH"; // 装货 
	public static String OP_IN = "IN"; // 入库
	public static String OP_OUT= "OU"; // 出库  
	public static String OP_SJ = "SJ"; // 上架
	public static String OP_MV = "MV"; // 移库
	public static String OP_LH = "LH"; // 理货
	public static String OP_BH = "BH"; // 补货
	public static String OP_JH = "JH"; // 拣货
	public static String OP_YH = "YH"; // 验货
	public static String OP_PD = "PD"; // 盘点
	public static String OP_TZ = "TZ"; // 调整
	public static String OP_FP = "FP"; // 分配
	public static String OP_DJ = "DJ"; // 冻结
	public static String OP_CC = "CC"; // 取消
	public static String OP_RQSJ = "RQSJ"; // 容器上架
	public static String OP_RQXJ = "RQXJ"; // 容器下架
	
	public static String OpExc_TYPE = "OpExcType";
	public static String OpExc_TYPE_01 = "库存异常";
	public static String OpExc_TYPE_02 = "设备异常";
	public static String OpExc_TYPE_03 = "人员异常";
	public static String OpExc_TYPE_04 = "验货异常";
	public static String[] _OpExc_TYPE = new String[] {OpExc_TYPE_01, OpExc_TYPE_02, OpExc_TYPE_03, OpExc_TYPE_04};
	
	public static String OpExc_STATUS = "OpExcStatus";
	public static String OpExc_STATUS_01 = "新建";
	public static String OpExc_STATUS_02 = "关闭";
	public static String[] _OpExc_STATUS = new String[] {OpExc_STATUS_01, OpExc_STATUS_02};
	
	void addComboParam(String code, String name, String[] items, Param group) {
		Param cp = paramService.getParam(code);
		List<Param> list;
		
		if( cp != null) {
			list = paramService.getComboParam(code);
		}
		else {
			cp = ParamManager.addComboParam(group.getId(), code, name);
			list = new ArrayList<Param>();
		}
		
		L:for(String item : items) {
			String value = item;
			String text =  item;
			
			for(Param p : list) {
				if(p.getValue().equals(value)) {
					p.setText(text);
					paramService.saveParam(p);
					continue L;
				}
			}
			ParamManager.addParamItem(cp.getId(), value, text, ParamConstants.COMBO_PARAM_MODE);
		}
	}
	
	public static Param comboParam(String code, String item) {
		return ParamManager.getComboParamItem(code, item);
	}
	
	public static Param opType(String item) {
		return comboParam(WMS.OP_TYPE, item);
	}
	
	/** ------------------------------------------------  异常集 -----------------------------------------------*/

	public static String SYS_ERR_1 = "您是否是自主扫码注册登录？请联系管理员开通账号";
	
	public static String INV_SHORT = "库存不足";
	public static String NO_ASN = "无单入库";
	public static String NO_ORDER = "无单出库";
	
	public static String INV_ERR_1 = "禁止直接修改库存数量，请联系仓管创建盘点单进行盘点";
	public static String INV_ERR_2 = "批次属性【${x1}】值【${x1}】输入格式有误";
	public static String INV_ERR_3 = "批次属性【${x1}】值不能为空";
	
	public static String EDI_ERR_1 = "第${x1}行的单号为空，请补充";
	
	public static String FIN_ERR_1 = "【${x1}】账户余额不足，付款失败，<a href='#' onclick='addTab(\"资金账户\", \"/tss/pages/fms/account.html\")'>先去充值</a>";
	
	public static String ASN_ERR_1 = "禁止无单入库，请在工单中心执行按单入库";
	public static String ASN_ERR_2 = "入库单【${x1}】需通过指派工单完成入库.${x2}";
	public static String ASN_ERR_3 = "删除失败: 入库单【${x1}】当前状态为【${x2}】, 只有新建、取消状态的可以删除";
	public static String ASN_ERR_5 = "取消入库失败: 入库单【${x1}】已上架，先取消上架";
	public static String ASN_ERR_6 = "取消入库失败: 入库单【${x1}】当前状态为【${x2}】";
	public static String ASN_ERR_7 = "入库失败: 入库单【${x1}】当前状态为【${x2}】, 只有新建、部分入库、入库取消状态的可以入库";
	public static String ASN_ERR_8 = "入库库位不能为空，<a href='#' onclick='addTab(\"url\", \"库位\", \"/tss/modules/dm/recorder.html?rctable=wms_location\")'>现在去创建</a>";
	public static String ASN_ERR_9 = "序列号【${x1}】已存在，请勿重复扫描。";
	public static String ASN_ERR_10 = "取消失败: 入库单【${x1}】当前状态为【${x2}】, 只有新建状态的可以取消";
	public static String ASN_ERR_11 = "操作失败: 入库单属于货主【${x1}】，而货品【${x2}】属于货主【${x3}】";
	public static String ASN_ERR_12 = "入库单【${x1}】不存在";
	public static String ASN_ERR_13 = "【${x1}】实际入库量已经超过入库通知单数量，请检查核实";
	public static String ASN_ERR_14 = "卸货失败: 入库单【${x1}】当前状态为【${x2}】, 只有新建、部分卸货状态的可以卸货";
	public static String ASN_ERR_15 = "您是供货商角色，无法执行入库操作";
	
	public static String OP_ERR_1 = "作业明细为空，请检查当前作业数量是否已正确填写";
	public static String OP_ERR_2 = "作业单【${x1}】不存在";
	public static String OP_ERR_3 = "目的库位不能和起始库位相同";
	public static String OP_ERR_4 = "保存盘点结果失败: 新增的库存未填写货主，请填写后重新保存";
	public static String OP_ERR_5 = "关闭失败: 作业单【${x1}】当前状态为【${x2}】, 只有新建、取消、已完成状态的可以关闭";
	public static String OP_ERR_6 = "拣货已完成，无法再更换拣货库存";
	public static String OP_ERR_7 = "作业单为空";
	public static String OP_ERR_8 = "系统库存不足，请先检查库存：${x1}";
	public static String OP_ERR_9 = "部分库存锁定中，作业失败，请先检查库存：${x1}";
	public static String OP_ERR_10 = "库位【${x1}】被冻结，无法完成当前库存作业";
	public static String OP_ERR_11 = "容器编号不能和上架库位码相同";

	public static String O_ERR_1 = "禁止无单出库，请在工单中心执行按单出库";
	public static String O_ERR_2 = "${x1}能进行无单出库，请按单出库${x2}";
	public static String O_ERR_3 = "删除失败: 出库单【${x1}】当前状态为【${x2}】, 只有新建、取消状态的可以删除";
	public static String O_ERR_4 = "关闭失败: 出库单【${x1}】当前状态为【${x2}】, 只有新建、取消或完成状态的可以关闭";
	public static String O_ERR_6 = "取消出库失败: 出库单【${x1}】当前状态为【${x2}】";
	public static String O_ERR_7 = "出库失败: 出库单【${x1}】当前状态为【${x2}】, 只有新建、部分出库、出库取消状态的可以出库";
	public static String O_ERR_9 = "取消失败: 出库单【${x1}】已做过出库，请先取消出库";
	public static String O_ERR_10 = "取消失败: 出库单【${x1}】当前状态为【${x2}】";
	public static String O_ERR_11 = "操作失败: 出库单属于货主【${x1}】，而货品【${x2}】属于货主【${x3}】";
	public static String O_ERR_12 = "出库单【${x1}】不存在";
	public static String O_ERR_13 = "指派失败: 出库单【${x1}】当前状态为【${x2}】, 请刷新页面";
	public static String O_ERR_14 = "取消库存分配失败: 当前出库单状态为【${x1}】";
	public static String O_ERR_15 = "此为出库自动封箱，无法取消封箱";
	public static String O_ERR_16 = "拣货出库失败: 出库单【${x1}】状态为【${x2}】, 只有已验货状态的可以拣货出库";
	public static String O_ERR_17 = "出库量不能为负数";
	public static String O_ERR_18 = "出库失败: 交接确认后才能出库";
	public static String O_ERR_19 = "第 ${x1} 行明细【${x2}】请重新扫码选择库存";
	public static String O_ERR_20 = "【${x1}】实际出库量已经超过出库单数量，请检查核实";
	public static String O_ERR_21 = "波次作业分配的订单，不能单独取消分配";
	
	public static String P_ERR_1 = "出库单【${x1}】上有封箱未上托";
	public static String P_ERR_2 = "托盘【${x1}】不存在或已出库";
	
	public static String WAVE_ERR_1 = "订单【${x1}】被创建到波次【${x2}】中，请先从波次中剔除";
	
	public static String LOC_ERR_1 = "库位【${x1}】不存在，请刷新页面再试";
	public static String LOC_ERR_2 = "库位【${x1}】已停用";
	public static String LOC_ERR_3 = "请先在库位里维护一个类型为收货区的库位";
	public static String LOC_ERR_4 = "请先在库位里维护一个类型为出货区的库位";
	
	public static String OWNER_ERR_1 = "请先选择货主";
	
	public static String SKU_ERR_1 = "找不到条（编）码为【${x1}】的货品信息，请先添加";
	public static String SKU_ERR_2 = "找不到条码为【${x1}】的货品信息，请先添加";
	public static String SKU_ERR_3 = "货主【${x1}】找不到条码为【${x2}】的货品信息，请先添加";
	
	public static String RULE_ERR_1 = "作业策略【${x1}】不存在或已停用";
}
