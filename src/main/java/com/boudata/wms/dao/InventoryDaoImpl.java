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
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.boubei.tss.framework.persistence.BaseDao;
import com.boubei.tss.framework.persistence.pagequery.PageInfo;
import com.boubei.tss.framework.persistence.pagequery.PaginationQueryByHQL;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryTemp;
import com.boudata.wms.entity._Location;
import com.boudata.wms.inventory.InventorySo;

@SuppressWarnings("unchecked")
@Repository("InventoryDao")
public class InventoryDaoImpl extends BaseDao<Inventory> implements InventoryDao {
	
	@Autowired LocationDao locDao;

	public InventoryDaoImpl() {
		super(Inventory.class);
	}
	
	public PageInfo search(InventorySo so) {
		if( WMS.isOWner() ) {
			so.setOwnerId( (Long) Environment.getInSession("OWNER_ID") );
		}
		
		Long warehouseId = so.getWarehouseId();
		
		// 传入的参数是 locationCode、loccode、zone，先找出 库位及子库位， 把满足条件locIDs里拼好传入 做 in 查询
		String locationCode = so.getLocationCode(), loccode = so.getLoccode(), zone = so.getZone();
		if(locationCode != null) {
			try {
				_Location location = locDao.getLoc(warehouseId, locationCode);
				so.setLocationId( location.getId() );
				so.setLocationCode(null);
			} 
			catch(Exception e) { }
		}
		if( zone != null ) {
			List<_Location> locs = locDao.queryLocsAndContainers("from _Location where warehouse.id = ? and zone = ? ", warehouseId, zone);
			if( locs.size() > 0 ) {
				so.setLocationCodes(  EasyUtils.objAttr2List(locs, "code")  );
				so.setZone(null);
			}
		}
		if( loccode != null ) {
			List<_Location> locs = locDao.queryLocsAndContainers("from _Location where warehouse.id = ? and code like ? ", warehouseId, loccode+"%");
			if( locs.size() > 0 ) {
				so.setLocationCodes(  EasyUtils.objAttr2List(locs, "code")  );
				so.setLoccode(null);
			}
		}
		
        String hql = " from Inventory o where 1=1 " + so.toConditionString();
        so.getOrderByFields().add("o.owner, o.sku, o.location, o.qty");
        so.getPage().setPageSize(so.getPage().getPageSize());
        
        PaginationQueryByHQL pageQuery = new PaginationQueryByHQL(em, hql, so);
        PageInfo page = pageQuery.getResultList();
        page.setItems(page.getItems());
        return page;
    }
    
	
	/*********************************** 批量找库存 start **********************************/
    
    public List<Inventory> searchInvsByIDs(Set<Long> idList) {
        /* 每次插入临时表前先删除临时表中的数据（一般不会有，除非一个事务里多次插入，比如测试时拣货 --> 取消 --> 再拣货）*/
        super.insertIds2TempTable(idList);
        String hql = "select o from Inventory o, Temp t where o.id = t.id and t.thread=?";

        return (List<Inventory>) getEntities(hql, Environment.threadID());
    }
    
    /**
     * 根据 InventoryTemp 里的维度信息，批量得去查找 库存。避免在循环里单个查找库存LLU。 
     * 注：本方法是严格按照批次的, 可用于创建库存前，查找同维度库存是否已存在。
     * 
     * 查找 库存 时需要带上仓库维度，维护时，可能库位从一个仓库变到另一个仓库，而此库位在原仓库已存在库存；
     * 
     * @param list
     * @return
     */
    public List<Object[]> searchInvs(List<InventoryTemp> list) {
        log.debug( Environment.threadID() + ": searchInvs start, condition size = " + list.size());
        
        /* 每次插入临时表前先删除临时表中的数据（一般不会有，除非一个事务里多次插入，比如测试时拣货 --> 取消 --> 再拣货）*/
        deleteAll( getEntities("from InventoryTemp where thread=?", Environment.threadID()) );
        for (InventoryTemp temp : list) {
            em().persist(temp);
        }
        
        String hql = "select distinct o, t.id from Inventory o, InventoryTemp t " +
                " where t.thread = ?" +
                " and o.wh.id    	= t.whId " +
                " and o.owner.id    = t.ownerId " +
                " and o.location.id = t.locationId" +
                " and o.sku.id      = t.skuId " +
                " and ((t.invstatus is null and o.invstatus is null) or t.invstatus = o.invstatus) " +
                " and ((t.lotatt01 is null and o.lotatt01 is null)  or t.lotatt01 = o.lotatt01) " +
                " and ((t.lotatt02 is null and o.lotatt02 is null)  or t.lotatt02 = o.lotatt02) " +
                " and ((t.lotatt03 is null and o.lotatt03 is null)  or t.lotatt03 = o.lotatt03) " +
                " and ((t.lotatt04 is null and o.lotatt04 is null)  or t.lotatt04 = o.lotatt04) " +
                " and ((t.createdate is null and o.createdate is null)  or t.createdate = o.createdate) " +
                " and ((t.expiredate is null and o.expiredate is null)  or t.expiredate = o.expiredate) " +
                " order by o.id desc ";
        
        List<Object[]> resultList = (List<Object[]>) getEntities(hql, Environment.threadID());
        
        log.debug( Thread.currentThread().getId() + ": searchInvs end, return size = " + list.size());
        return resultList;
    }

    /**
     * 根据soi列表取预拣货库存列表，用于预拣货调用。
     * 注：本方法和以前的严格批次方法的区别在于：
     *    上面的方法严格按照批次，如果soi某批次属性为空，则查询到的LLU的对应该批次属性也要求为空；
     *    本方法不同，如果soi某批次属性为空，则查询到的LLU的对应该批次属性可以不为空，即查询条件里忽略该批次属性（=null）的条件。                      
     *    
     * @return
     */
	public List<Object[]> searchInvsIgnoreLot(List<InventoryTemp> list) {
    	
        log.debug( Environment.threadID() + ": searchInvsIgnoreLot start, condition size = " + list.size());
        
        /* 每次插入临时表前先删除临时表中的数据（一般不会有，除非一个事务里多次插入，比如测试时拣货 --> 取消 --> 再拣货）*/
        deleteAll( getEntities("from InventoryTemp where thread=?", Environment.threadID()) );
        for (InventoryTemp temp : list) {
            em().persist(temp);
        }
        em().flush();
        
        String hql = "select distinct o, t.id from Inventory o, InventoryTemp t " +
                " where t.thread = ? " +
                " and o.wh.id    	= t.whId " +
                " and o.owner.id    = t.ownerId " +
                " and o.location.id = t.locationId" +
                " and o.sku.id      = t.skuId " +
                " and (t.invstatus  is null or o.invstatus = t.invstatus)" +
                " and (t.createdate is null or o.createdate = t.createdate)" +
                " and (t.expiredate is null or o.expiredate = t.expiredate)" +
                " and (t.lotatt01 is null or o.lotatt01 = t.lotatt01)" +
                " and (t.lotatt02 is null or o.lotatt02 = t.lotatt02)" +
                " and (t.lotatt03 is null or o.lotatt03 = t.lotatt03)" +
                " and (t.lotatt04 is null or o.lotatt04 = t.lotatt04)" +
                " order by o.id desc ";
                
        List<Object[]> resultList = (List<Object[]>) getEntities(hql, Environment.threadID());
        
        log.debug( Thread.currentThread().getId() + ": searchInvsIgnoreLot end, return size = " + resultList.size());
        return resultList;
    }
	
	public List<Inventory> getInvs(String hql, Object...params) {
    	return (List<Inventory>) getEntities(hql, params);
    }
}
