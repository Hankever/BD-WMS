/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms;

import com.boubei.tss.framework.Global;
import com.boubei.tss.modules.cloud.entity.CloudOrder;
import com.boubei.tss.modules.cloud.pay.ModuleOrderHandler;
import com.boubei.tss.modules.param.Param;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Warehouse;

/**
 * 购买 WMS 模块成功后续操作: com.boudata.wms.WmsProduct
 * 
 * 1、生成一个仓库、一个货主（和公司名称同名）
 * 2、初始化一个收货区 + 一个出库区
 */
public class WmsProduct extends ModuleOrderHandler {

	public WmsProduct(CloudOrder co) {
		super(co);
	}
	
	public static String DEFAULT_WH_C = "-W1";
	public static String DEFAULT_WH_N = "一号仓";

	protected IGroupService groupService = (IGroupService) Global.getBean("GroupService");

	protected void init() {
		
		String creator = buyer.getLoginName();
		
		// 初次购买执行，续费或购买新账号无需执行
		if( dao.getEntities("from _Owner where domain = ? and ifnull(remark, '') != 'test'", domain).size() > 0) return;
		
		// 创建默认仓库
		_Warehouse warehouse = new _Warehouse();
		warehouse.setCode( domain + DEFAULT_WH_C );
		warehouse.setName( DEFAULT_WH_N );
		warehouse.setStatus( ParamConstants.TRUE );
		warehouse.setDomain(domain);
		warehouse.setCreator(creator);
		dao.createObject(warehouse);
		
		// 默认创建一个仓库用户组（和仓库同名）
    	Group whGroup = new Group();
    	whGroup.setName( warehouse.getName() );
    	whGroup.setGroupType(Group.MAIN_GROUP_TYPE);
    	whGroup.setParentId( buyer.getGroupId() );
    	whGroup.setFromGroupId(warehouse.getId().toString());
    	groupService.createNewGroup(whGroup, "", "");
		
    	// 创建货主
		_Owner owner = new _Owner();
		owner.setCode( domain + "F1" );
		owner.setName( (String) EasyUtils.checkNull(buyer.getUdf(), buyer.getUserName()) );
		owner.setStatus( ParamConstants.TRUE );
		owner.setDomain(domain);
		owner.setCreator(creator);
		owner.setRemark("系统默认创建");
		dao.createObject(owner);
		
		// 创建几个默认库位
        createLocation("001", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_IN), warehouse);
        createLocation("000", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OUT), warehouse);
	}
	
	public static void createLocation(String code, Param type, _Warehouse w) {
		_Location l = new _Location();
		l.setType(type);
		l.setCode(code);
		l.setName(code);
		l.setWarehouse(w);
		l.setStatus(ParamConstants.TRUE);
		l.setDomain(w.getDomain());
		l.setRemark("系统自动创建");
		
		Global.getCommonService().create(l);
	}
}
