/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dao;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boubei.tss.EX;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms.entity._Location;

@SuppressWarnings("unchecked")
@Repository("LocationDao")
public class LocationDaoImpl extends BaseDao<_Location> implements LocationDao {

	public LocationDaoImpl() {
		super(_Location.class);
	}

	public _Location getLoc(Long whId, String code, boolean exDisabed) {
		String hql = "from _Location where warehouse.id = ? and code = ? ";
		List<?> list = getEntities(hql, whId, code);
		if( list.isEmpty() ) {
			throw new BusinessException( EX.parse(WMS.LOC_ERR_1, code) );
		}

		_Location loc = (_Location) list.get(0);
		if( exDisabed && ParamConstants.FALSE.equals(loc.getStatus())) {
			throw new BusinessException( EX.parse(WMS.LOC_ERR_2, code) );
		}
		return loc;
	}
	
	public _Location getLoc(Long whId, String code) {
		return getLoc(whId, code, false);
	}

	public String getInLoc(Long whId) {
		String hql = "select code from _Location where warehouse.id = ? and type.text = ? and (status = 1 or status is null) order by id asc";
		List<?> list = getEntities(hql, whId, WMS.LOC_TYPE_IN);
		if( list.isEmpty() ) {
			throw new BusinessException( WMS.LOC_ERR_3 );
		}

		return (String) list.get(0);
	}

	public String getOutLoc(Long whId) {
		String hql = "select code from _Location where warehouse.id = ? and type.text = ? and (status = 1 or status is null)  order by id asc ";
		List<?> list = getEntities(hql, whId, WMS.LOC_TYPE_OUT);
		if( list.isEmpty() ) {
			throw new BusinessException( WMS.LOC_ERR_4 );
		}

		return (String) list.get(0);
	}

	
	public List<_Location> getCCLocs(Long whId) {
		String hql = "from _Location where warehouse.id = ? and type.text = ? and (status = 1 or status is null)";
		return queryLocs(hql, whId, WMS.LOC_TYPE_CC);
	}
	
	public List<_Location> queryLocs(String hql, Object...params) {
		return (List<_Location>) getEntities(hql, params);
	}
	
	public List<_Location> queryLocsAndContainers(String hql, Object...params) {
		List<_Location> locs = this.queryLocs(hql, params);
		if( locs.size() > 0 ) {
			String hql2 = "from _Location where parent.id in (:locIDs) ";
			locs.addAll( (List<_Location>) getEntities(hql2, new String[] {"locIDs"}, new Object[] { EasyUtils.objAttr2List(locs, "id") }) );
		}
		return locs;
	}
	
	public void restartLoc(Long whId, String loccode) {
		List<_Location> list = queryLocs("from _Location where warehouse.id = ? and code = ? and status = 0", whId, loccode);
		if(list.size() > 0) {
			_Location loc = list.get(0);
			loc.setStatus(ParamConstants.TRUE); // 如果指定的是一个停用的容器号，则重新启用此容器号
			update(loc);
		}
	}
}
