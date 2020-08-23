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
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.boubei.tss.dm.dml.SQLExcutor;
import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonService;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.ILoginCustomizer;
import com.boubei.tss.framework.sso.SSOConstants;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.web.HttpClientUtil;
import com.boubei.tss.modules.cloud.entity.DomainInfo;
import com.boubei.tss.um.entity.Role;
import com.boubei.tss.um.service.ILoginService;
import com.boubei.tss.um.sso.FetchPermissionAfterLogin;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Warehouse;

/**
 * 登录后设置用户仓库权限及其它配置
 * 
 * 1、控制作业人员只能看到自己所属仓库的数据
 * 2、控制货主只能查询自己的订单、库存数据
 * 3、加载当前域的作业流程配置信息
 *
 */
@SuppressWarnings("unchecked")
public class WmsLoginCustomizer implements ILoginCustomizer {
	
	ILoginService loginService = (ILoginService) Global.getBean("LoginService");
	ICommonService commService = Global.getCommonService();
	
	void addRole2Session(HttpSession session, String roleName, List<String> roleNames) {
		
		if( roleNames.contains(roleName) ) return;
		
		List<?> list = commService.getList("from Role where name = ?", roleName );
		Role role = (Role) list.get(0);
		
		List<Long> roleIds = (List<Long>) session.getAttribute(SSOConstants.USER_RIGHTS_L);
		FetchPermissionAfterLogin.addRole2Session(session, Environment.getUserId(), role, roleIds, roleNames);
	}
	
	public void execute() {
		String domain = Environment.getDomain();
		List<Object> modules = (List<Object>) Environment.getInSession(SSOConstants.USER_MODULE_C);
		modules = (List<Object>) EasyUtils.checkNull(modules, new ArrayList<Object>());
		
		boolean isAllIN = modules.contains("ALL-IN");
		boolean isWMS   = modules.contains(WMS.MODULE_WMS);
		boolean hasWMS = isAllIN || isWMS;
		boolean onlyHasWMS = (isWMS && modules.size() == 1) ;
		
		// 默认授予所有仓库人员“作业人员”角色（customer的除外）
		HttpSession session = Context.getRequestContext().getSession();
		List<String> roleNames = (List<String>) session.getAttribute(SSOConstants.USER_ROLES_L);

		boolean isOwner = roleNames.contains(WMS.ROLE_OW);
		boolean userInCustomerDeep = Environment.inGroup("customer", true);
		boolean userInCustomerJust = Environment.inGroup("customer", false);
		
		// 自注册到各个域下的customer账号，无法直接登录
		if( userInCustomerJust && !isOwner && onlyHasWMS ) {
			throw new BusinessException(WMS.SYS_ERR_1);
		}
		
		if ( hasWMS && !userInCustomerDeep 
				&& !roleNames.contains( WMS.ROLE_OT )  
				&& !roleNames.contains( WMS.ROLE_SP ) ) {

			addRole2Session(session, WMS.ROLE_OP, roleNames); // 仓库人员默认授予 作业人员 角色
		}
		
		// 限制用户（仓管和作业人员）的仓库权限：报表等（新增仓库后要重新登录）
		String hql = "from _Warehouse where name in (" +Environment.getInSession(SSOConstants.SON_GROUP_TREE_)+ ")  and status=1 and domain=?";
		List<_Warehouse> whs = (List<_Warehouse>) commService.getList(hql, domain);
		
		session.setAttribute("WH_IDS", EasyUtils.checkTrue(whs.isEmpty(), "-999", EasyUtils.list2Str(EasyUtils.objAttr2List(whs, "id"))));
			
		/* 货主账号登录要自动带出货主记录信息，用以限制报表数据 
		 * 注：如果一个货主需要多个查询账号，可以在“用户管理” customer组下建多个同名（和货主同名）的账号 
		 */
		if ( isOwner ) {
			String sql = "from _Owner where (mobile = ? or name = ?) and domain = ? and status = 1 ";
			List<?> list = commService.getList(sql, Environment.getUserCode(), Environment.getUserName(), domain);
			if( list.size() > 0 ) {
				_Owner owner = (_Owner) list.get(0);
				session.setAttribute("OWNER_ID", owner.getId());
				
				_Warehouse bindWh = owner.getWarehouse();
				if( bindWh != null ) {
					session.setAttribute( "WH_IDS", bindWh.getId().toString() );
				}
				else {
					list = commService.getList("select id from _Warehouse where domain = ? and status = 1 ", domain);
					session.setAttribute( "WH_IDS", EasyUtils.list2Str(list));
				}
			}
		}
		
		// 加载域自定义配置，默认写到cookie
		loadDomainConfig(session, whs);
		
		// 判断是否多仓多货主
		int whNum = whs.size();
		HttpServletResponse response = Context.getResponse();
		HttpClientUtil.setCookie(response, "count_wh", whNum+"" );
		HttpClientUtil.setCookie(response, "count_ow", SQLExcutor.queryVL("select count(*) n from wms_owner where domain = ? and status=1", "n", domain)+"" );
		
		checkSubAuthorize(whNum);
	}

	private void loadDomainConfig(HttpSession session, List<_Warehouse> whs) {
		Object domainUdf1 = session.getAttribute("domain_udf1");   // 物流仓默认配置存放再udf1
		
		// 当用户只可见一个仓库，优先取仓库级的配置（即仓库级配置 只对 仓库下的用户有效，TODO fix：仓库A下有子仓库，则仓库A的配置无效）
		if( whs.size() == 1 ) { 
			_Warehouse onlyOneWh = (_Warehouse) whs.get(0);
			domainUdf1 = EasyUtils.checkNull( onlyOneWh.getConfig(), domainUdf1);
		}
		
		if( EasyUtils.isNullOrEmpty(domainUdf1) ) {
			try {
				// 默认取【云链科技（标准演示）】= 0L 的配置
				DomainInfo demoDomain = (DomainInfo) commService.getEntity(DomainInfo.class, 0L);
				domainUdf1 = demoDomain.getUdf1();
			} catch(Exception e) { }
		}
		
		parseConfigVal(session, domainUdf1);
		parseConfigVal(session, session.getAttribute("domain_udf2")); // 资金仓默认配置存放再udf2
	}

	private void parseConfigVal(HttpSession session, Object domainUdfx) {
		if( !EasyUtils.isNullOrEmpty(domainUdfx) ) {
			Map<String, ?> _config = _Util.json2Map(domainUdfx.toString());
			for (Entry<String, ?> entry : _config.entrySet()) {
				String key = entry.getKey();
				String value = EasyUtils.obj2String(entry.getValue());
				session.setAttribute(key, value);
				if(key.startsWith("js_")) {
					HttpClientUtil.setCookie(Context.getResponse(), key, value);
				}
			}
		}
	}
	
	/**
	 * 检查用户所在域购买的账号是否已到期
	 * TODO 如用户开启了多仓库，但策略数小于仓库数；发送提醒
	 */
	private void checkSubAuthorize(int whNum) {
		if( Environment.getDomainOrign() == null ) return;
		
		String sql = "select sa.id, sa.endDate from SubAuthorize sa, ModuleUser mu "
				+ " where sa.moduleId = mu.moduleId and sa.buyerId = mu.userId "
				+ "		and mu.domain = ? and sa.endDate > now()";
		List<?> list = commService.getList(sql, Environment.getDomainOrign());
		if( list.isEmpty() ) {
			if( Environment.isDomainAdmin() ) {
				// 发短信通知域管理员：账号已过期
			} else {
				throw new BusinessException("您所在域的购买账号已经过期，请联系管理员续费后再登录");
			}
		}
	}
}
