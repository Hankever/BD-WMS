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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.um.dao.IGroupDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.um.entity.User;
import com.boubei.tss.um.helper.dto.OperatorDTO;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.util.StringUtil;
import com.boudata.wms.dao.BaseService;
import com.boudata.wms.dao.LocationDao;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Sku;

@Controller
@RequestMapping("/wms/api")
public class WmsAPI {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired private ICommonService commservice;
	@Autowired private ILoginService loginService;
	@Autowired private BaseService baseService;
	@Autowired private IGroupDao groupDao;
	@Autowired private LocationDao locDao;
	
	@RequestMapping(value = "/coder", method = RequestMethod.GET)
	@ResponseBody
	public String genAsnNO(Long ownerId, String type) {
		_Owner owner = (_Owner) commservice.getEntity(_Owner.class, ownerId);
		return  _Util.genDocNO(owner, type, false);
	}
	
	@RequestMapping(value = "/skux", method = RequestMethod.GET)
	@ResponseBody
	public _Sku skux(String barcode, Long ownerId) {
		return baseService.skux(barcode, ownerId);
	}
	
	@RequestMapping(value = "/sku", method = RequestMethod.GET)
	@ResponseBody
	public List<_Sku> sku(String barcode, Long ownerId) {
		return baseService.sku(barcode, ownerId);
	}
	
	@RequestMapping(value = "/locs", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, _Location> locs(String codes, Long warehouse_id) {
		List<_Location> locs = locDao.queryLocs("from _Location where code in (" +DMUtil.insertSingleQuotes(codes)+ ") and warehouse.id = " + warehouse_id);
		Map<String, _Location> result = new HashMap<>();
		for(_Location loc : locs) {
			result.put(loc.getCode(), loc);
		}
		return result;
	}
	
	/*************************************************************************** 个人中心相关服务 *****************************************************************************/
	
	@RequestMapping(value = "/saveWarehouse", method = RequestMethod.POST)
	@ResponseBody
	public List<Group> saveWarehouse(HttpServletRequest request) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		List<Map<String, Object>> json_list = _Util.json2List(params.get("items"));
		
		return baseService._saveWarehouse(json_list);
	} 
	
	/**
	 * 创建仓库的同时自动在 用户组织里 创建一个同名的用户组；状态不同步
	 */
	@RequestMapping(value = "/syncWhGroup", method = RequestMethod.POST)
	@ResponseBody
	public Object syncWarehouseGroup(Long whId, String roles) {
		return baseService.syncWarehouseGroup(whId, roles);
	}
	
	@RequestMapping(value = "/getUserByGroup")
	@ResponseBody
	public Map<String, Object> getUserByGroup(HttpServletRequest request) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		
		String group = params.get("groupName");
		String domain = Environment.getDomain();
		
		@SuppressWarnings("unchecked")
		List<Long> groups = (List<Long>) commservice.getList("select id from Group where name = ? and domain = ?", group, domain);
		if(groups.size() == 0) {
			log.error(domain + "域下用户组" + group + "不存在。" + params);
			throw new BusinessException(group + "不存在");
		}
		
		List<OperatorDTO> role_wh_users  = loginService.getUsersByRole(WMS.ROLE_CG, domain);
		List<OperatorDTO> role_fin_users = loginService.getUsersByRole(WMS.ROLE_FIN, domain);
		Long groupId = groups.get(0);
		List<User> users = groupDao.getUsersByGroupId(groupId);  // groupDao不过滤权限，groupService会
		for(User user : users) {
			for(OperatorDTO role_user : role_wh_users) {
				if(user.getLoginName().equals(role_user.getLoginName())) {
					user.setRoleNames(WMS.ROLE_CG);
				}
			}
			for(OperatorDTO role_user : role_fin_users) {
				if(user.getLoginName().equals(role_user.getLoginName())) {
					user.setRoleNames( user.getRoleNames() + ',' + WMS.ROLE_FIN );
				}
			}
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("data", users);
		map.put("groupId", groupId);
		
		return map;
	}
	
	/**
	 * 获取自己所在组下的用户及父组的用户
	 */
	@RequestMapping(value = "/getBranchUsers")
	@ResponseBody
	public List<User> getBranchUsers(String role) {
		Long groupId = Environment.getUserGroupId();
		Long parentId = groupDao.getEntity(groupId).getParentId();
		
		List<User> users = groupDao.getUsersByGroupId(parentId);
		users.addAll( groupDao.getUsersByGroupIdDeeply(groupId) );
		if( role == null ) {
			return users;
		}
		 
		String[] roles = StringUtil.split(role);
		List<User> temp = new ArrayList<>();
		for( String _role : roles ) {
			List<OperatorDTO> rUsers  = loginService.getUsersByRole(_role, Environment.getDomain());
			for(User user : users) {
				for(OperatorDTO role_user : rUsers) {
					if(user.getId().equals(role_user.getId())) {
						temp.add(user);
					}
				}
			}
		}
		
		return temp;
	}
}
