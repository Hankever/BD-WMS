/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.service.IGroupService;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms.WmsProduct;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._SkuX;
import com.boudata.wms.entity._Warehouse;

@Service("BaseService")
public class BaseServiceImpl implements BaseService {
	
	@Autowired private SkuDao skuDao;
	@Autowired private ICommonService commservice;
	@Autowired private IGroupService groupService;
	
	public _Sku skux(String barcode, Long ownerId) {
		_Sku sku = skuDao.getSku(barcode, ownerId);
		
		@SuppressWarnings("unchecked")
		List<_SkuX> list = (List<_SkuX>) skuDao.getEntities(" from _SkuX where parent.id = ?", sku.getId());
		sku.skuxList.addAll(list) ;
		
		sku.package_qty = 0;
		for(_SkuX skux : sku.skuxList) {
			sku.package_qty += skux.getWeight(); 
		}
		
		return sku;
	}
	
	public List<_Sku> sku(String barcode, Long ownerId) {
		return skuDao.getSkus(barcode, ownerId);
	}
	
	public List<Group> _saveWarehouse(List<Map<String, Object>> json_list) {
		
		List<_Warehouse> whList = new ArrayList<_Warehouse>();
		
		for(Map<String, Object> item : json_list) {
			_Warehouse wh = new _Warehouse();
			Object _id = item.get("id");
			if( EasyUtils.isNullOrEmpty(_id) ) {
				BeanUtil.setDataToBean(wh, item);
				commservice.create(wh);
				
				// 默认建一个库位
				WmsProduct.createLocation("A1", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_IN), wh); // 库位
			} 
			else {
				Long id = EasyUtils.obj2Long(_id);
				wh = (_Warehouse) commservice.getEntity(_Warehouse.class, id);
				BeanUtil.setDataToBean(wh, item);
				commservice.update(wh);
			}
			whList.add(wh);
		}
		
		List<Group> groupList = new ArrayList<Group>();
		for(_Warehouse wh : whList) {
			Group group = (Group) this.syncWarehouseGroup(wh.getId(), "");
			groupList.add(group);
			Integer status = wh.getStatus();
			
			// 修改仓库组下用户状态（如仓库停用，则组织及人员也跟着停用）
			if( status == 0 ) {
				try {
					groupService.startOrStopGroup(group.getId(), ParamConstants.TRUE);
				} catch(Exception e) {}
			}
		}
		
		return groupList;
	} 

	public Object syncWarehouseGroup(Long whId, String roles) {
		_Warehouse w = (_Warehouse) commservice.getEntity(_Warehouse.class, whId);
		
		String whName = w.getName();
		String fromWh = whId.toString();
		roles = EasyUtils.obj2String(roles);
		
		String hql = " from Group where (fromGroupId = ? or name = ?) and domain = ?";
		List<?> list = commservice.getList(hql, fromWh, whName, Environment.getDomain());
		
		Group group;
		if( list.isEmpty() ) {
			// 创建一个新的仓库组，默认给以”作业人员“角色
			group = new Group();
			group.setName( whName );
			group.setParentId( Environment.getUserGroupId() );
			group.setGroupType( Group.MAIN_GROUP_TYPE );
			group.setFromGroupId( fromWh );
			groupService.createNewGroup(group, "", roles);
			
			// 创建几个默认库位
			WmsProduct.createLocation("001", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_IN), w);
			WmsProduct.createLocation("000", WMS.comboParam(WMS.LOC_TYPE, WMS.LOC_TYPE_OUT), w);
		} 
		else {
			// 修改仓库组名称
			group = (Group) list.get(0);
			group.setName( whName );
			group.setFromGroupId( fromWh );
			groupService.editExistGroup(group, "", roles);
		}
		
		// 刷新session，不然看不到新增的仓库
		HttpSession session = Context.getRequestContext().getSession();
		session.setAttribute("WD_IDS", session.getAttribute("WD_IDS") + "," + whId);
		
		return group;
	}
}
