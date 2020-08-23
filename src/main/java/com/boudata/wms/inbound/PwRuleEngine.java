/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inbound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.boubei.tss.cache.extension.CacheHelper;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.persistence.Temp;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Warehouse;

public class PwRuleEngine {
	
	private AsnDao asnDao;
	
	public PwRuleEngine(AsnDao asnDao) {
		this.asnDao = asnDao;
	}
	
	public List<OperationItem> excute(Asn asn, List<_Rule> rList) {
		Long asnId = asn.getId();
		_Warehouse wh = asn.getWarehouse();
		
		// 取出ASN明细对应的入库作业明细
		List<OperationItem> items   = asnDao.getOpItems(asnId, WMS.OP_TYPE_IN);
		List<OperationItem> sjItems = asnDao.getOpItems(asnId, WMS.OP_TYPE_SJ);
		
		// 先剔除已上架的
		for(OperationItem item : sjItems) {
			AsnItem ai = item.getAsnitem();
			ai.setQty_this( MathUtil.addDoubles(ai.getQty_this(), item.getQty()) );
		}
		
		List<Inventory> invs = new ArrayList<>();
		for(OperationItem item : items) {
			Inventory opInv = item.getOpinv();
			AsnItem ai = item.getAsnitem();
			
			opInv.asnItem = ai;
			opInv.qty_advice = MathUtil.addDoubles(opInv.qty_advice, MathUtil.subDoubles( ai.getQty_actual(), ai.getQty_this() )  );
			invs.add( opInv );
		}
		
		// 根据上架规则生成上架指导单
		_Owner owner = asn.getOwner();
		String[] ruleScripts = _Util.ruleDef(rList, _Rule.DEFAULT_PW_RULE, owner.getPutawayr(), wh.getPutawayr()).split(";");
		
		Map<String, Object> context = new HashMap<>();
		context.put("whID", wh.getId());
		context.put("owID", owner.getId());
		
		// 规则可以是若干个规则组合：用逗号分隔，或者直接引用已有的单条规则
		List<?> rules = asnDao.getEntities("from _Rule where domain = ? and status = 1", Environment.getDomain());
		for( Object obj : rules ) {
			_Rule rule = (_Rule) obj;
			context.put(rule.getCode(), rule.getContent());
		}
		context.put("emptyLocFirst", _Rule.DEFAULT_PW_RULE);
		
		Map<Long, _Location> locMap = new HashMap<>();
		
		CacheHelper.getNoDeadCache().flush();
		// 一行行去找，上架记录数不大，无需批量
		L: for(Inventory inv : invs) {
			// 一次上架多个 入库明细时，第一个明细用掉A库位 80%，第二个入库明细不知道；需要把 locMap 的 loc_id, qty 写入临时表，用以规则语句进行排序
			List<Temp> list = new ArrayList<>();
			for( Long loc_id : locMap.keySet() ) {
				Temp t = new Temp();
				t.setId(loc_id);
				t.setUdf1( EasyUtils.obj2Double(locMap.get(loc_id).qty).toString() );
				list.add(t);
			}
			asnDao.insert2TempTable(list);
			
			context.put("skuID", inv.getSku().getId());
			context.put("invID", inv.getId());
			
			for( String ruleScript : ruleScripts ) {
				String sql = DMUtil.fmParse(ruleScript, context);
				if( EasyUtils.isNullOrEmpty(sql) ) continue;
				
				/* 规则返回：loc_id, qty */
				@SuppressWarnings("unchecked")
				List<Object[]> result = (List<Object[]>) asnDao.getEntitiesByNativeSql(sql);
				for( Object[] row : result ) {
					
					if( inv.qty_advice == 0 ) continue L;
					
					Long loc_id = EasyUtils.obj2Long(row[0]);
					_Location l;
					if( locMap.containsKey( loc_id ) ) {
						l = locMap.get(loc_id);
					}
					else {
						locMap.put(loc_id, l = (_Location) asnDao.getEntity( _Location.class, loc_id ));
						l.qty = EasyUtils.obj2Double(row[1]);
					}
					
					double remain = l.getCapacity() - l.qty;
					if(remain <= 0) continue;
					
					Double thisTimeQty = remain < inv.qty_advice ? remain : inv.qty_advice;
					inv.qty_advice = MathUtil.subDoubles(inv.qty_advice, thisTimeQty);
					l.qty += thisTimeQty;
					
					OperationItem opItem = new OperationItem(inv, thisTimeQty);
					opItem.setToloccode( l.getCode() );  // 上架库位
					opItem.setAsnitem( inv.asnItem );
					opItem.compareLotAtt( inv );
					
					sjItems.add(opItem);
				}
			}
		}
		
		return sjItems;
	}

}
