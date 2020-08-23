/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.dao.LocationDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dto.LocationDTO;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.InvCheck;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.InventoryLog;
import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OperationLog;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity._Location;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._SkuX;
import com.boudata.wms.entity._Warehouse;

@Controller("InventoryAction")
@RequestMapping( {"/inv", "/wms/api"}  )
public class InventoryAction {
	
	@Autowired private OperationService operationService;
	@Autowired private InventoryService invService;
	@Autowired private InventoryDao invDao;
	@Autowired private LocationDao locDao;
	@Autowired private OperationDao operationDao;
	
	/**
	 * TODO 中转容器整库移库
	 * 整库移库：如果移动的源库位是一个“中转容器”，则直接将此中转库位（可能是个托盘、纸箱...）和目标库位（存储位）建立父子关系（"中转容器"上，新增一个“上架库位”，以定位移动库位的位置）；
	 * 创建一个配置项: 是否按容器上架  否的话还是以常规整库移库处理；
	 * 修改库存查询接口，按库位查找，同时显示子库位的库存。
	 */
	@RequestMapping(value = "/move", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> move(HttpServletRequest request, Long whId) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		String type = params.get("type");
		String loccode = params.get("loccode");
		String toloccode = params.get("toloccode");
		if( loccode.equals(toloccode) ) {
			throw new BusinessException( WMS.OP_ERR_3 );
		}
		
		List<_DTO> list;
		if("整库移库".equals(type)) { // 如果是整库移库，则直接在后台取库存记录然后生成opItems
			list = new ArrayList<_DTO>();
			
			String hql = " from Inventory where location.warehouse.id = ? and location.code = ? and qty > 0";
			List<Inventory> invList = invDao.getInvs(hql, whId, loccode);
			for(Inventory inv : invList) {
				_DTO item =  new _DTO();
				item.qty = inv.getQty();
				item.skucode = inv.getSku().getCode();
				item.owner_id = inv.getOwner().getId();
				item.copyLotatt(inv);
				list.add(item);
			}
		} 
		else {
			list = _DTO.parse(params);
		}
		
		if(list.size() == 0) {
			return _Util.toMap("-200", "移库失败，起始库位【" + loccode + "】上没有库存记录" );
		}
		
		OperationH op = invService.move(whId, loccode, toloccode, list);
		Map<String, Object> result = _Util.toMap("200", "移库成功");
		result.put("opId", op.getId());
		
		return result;
	}
	
	@RequestMapping(value = "/containerMove", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> containerMove(HttpServletRequest request, Long whId) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		String type = params.get("type");
		String loccode = params.get("child_loc");
		String toloccode = params.get("toloccode");
		if( loccode.equals(toloccode) ) {
			throw new BusinessException( WMS.OP_ERR_11 );
		}
		
		List<_DTO> list = new ArrayList<_DTO>();
		String hql = " from Inventory where location.warehouse.id = ? and location.code = ? and qty > 0";
		List<Inventory> invList = invDao.getInvs(hql, whId, loccode);
		for(Inventory inv : invList) {
			_DTO item =  new _DTO();
			item.qty = inv.getQty();
			item.skucode = inv.getSku().getCode();
			item.owner_id = inv.getOwner().getId();
			item.copyLotatt(inv);
			list.add(item);
		}
		
		invService.containerMove(whId, loccode, toloccode, type, list);
	
		return _Util.toMap("200", type + "成功");
	}
	
	/**
	 * 无单盘点（小程序、RF）
	 */
	@RequestMapping(value = {"/invcheck", "/stocktake"}, method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> adjustInv4rf(HttpServletRequest request, Long whId) {
		Map<String, String> params = DMUtil.getRequestMap(request, false); 
		adjustInv(whId, params.get("items"), params.get("remark"));
		
		return _Util.toMap("200", "库存调整成功");
	}
	
	/**
	 * 无单盘点（PC）
	 */
	@RequestMapping(value = "/adjust", method = RequestMethod.POST)
	@ResponseBody
	public void adjustInv(Long whId, String items, String remark) {
		boolean modifyQty = false;
		List<_DTO> list = _DTO.parse(items);
		List<OperationItem> itemList = new ArrayList<>();
		for (_DTO item : list) {
			OperationItem opi;
			Long _inv_id = item.inv_id;
			Double toQty = item.qty_actual;
			
			if( _inv_id == null ) { // 新增的库存
				opi = new OperationItem();
				opi.copyLotAtt(item);
				Long owner_id = item.owner_id;
				if(owner_id == null) {
					throw new BusinessException( WMS.OP_ERR_4 );
				}
				opi.setOwner( new _Owner(owner_id) );
				opi.setSkucode( item.skucode );
				
				String loccode =  item.loccode;
				if( loccode == null ) {
					loccode = locDao.getEntity( item.location_id ).getCode();
				}
				opi.setLoccode(loccode);
				opi.setQty(0D);
			}
			else {
				Inventory inv = invDao.getEntity( _inv_id );
				Double qty = inv.getQty();
				if(toQty == null || toQty.doubleValue() == qty.doubleValue()) { // 数量不变，看批次有没有变
					OperationItem temp = new OperationItem(inv, 0D);
					temp.copyLotAtt(item);
					if( !temp.compareLotAtt(inv) ) { // 只改装批次
						temp.setOpinv(null);
						temp.setToqty(qty); // 数量加到新增批次的库存上
						itemList.add(temp);
						
						opi = new OperationItem(inv, qty);
						opi.setToqty(0D); // 原库存减为0
						itemList.add(opi);
					}
					continue;
				}
				
				opi = new OperationItem(inv, qty);
			}
			opi.setToqty(toQty);
			itemList.add(opi);
			modifyQty = true;
		}
		
		if( itemList.size() > 0 ) {
			/* 盘点审核开关打开时，关闭普通盘点的提交(抛出异常) , 包括库存加工也禁止 */
			if( modifyQty && WMS.inv_check_approve() ) {
				throw new BusinessException(WMS.INV_ERR_1);
			} 
			invService.invCheckCommit(whId, itemList, remark);
		}
	}
	
	/**
	 * 组合货品拆分
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = {"/skuxSplit"}, method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> skuxSplit(Long invId, Double qty_actual) {
		List<OperationItem> itemList = new ArrayList<OperationItem>();
		Inventory inv = invDao.getEntity(invId);
		
		OperationItem opi = setOpiByInv(inv);
		opi.setOpinv( inv );
		opi.setSkucode( inv.getSku().getCode() );
		opi.setQty( inv.getQty() );
		opi.setToqty( MathUtil.subDoubles(inv.getQty(), qty_actual) );
		itemList.add( opi );
		
		List<_SkuX> skuxs = (List<_SkuX>) invDao.getEntities(" from _SkuX where parent = ?", inv.getSku());
		for(_SkuX skux : skuxs) {
			OperationItem _opi = setOpiByInv(inv);
			_opi.setSkucode( skux.getSku().getCode() );
			_opi.setQty( 0D );
			_opi.setToqty( qty_actual * skux.getWeight() );
			_opi.setLotatt02("");
			itemList.add( _opi );
		}
		
		invService.invCheckCommit(inv.getWh().getId(), itemList, "拆组合");
		
		return _Util.toMap("200", "拆组合成功");
	}

	private OperationItem setOpiByInv(Inventory inv) {
		OperationItem opi = new OperationItem();
		opi.setOwner( inv.getOwner() );
		opi.setLoccode( inv.getLocation().getCode() );
		opi.copyLotAtt( inv );
		return opi;
	}
	
	/**
	 * $.post("/tss/inv/checkLocs", {whId:2, opNo:'PD001-TZ191203001'}, function(data) { console.log(data); });
	 */
	@RequestMapping(value = "checkLocs")
	@ResponseBody
	public Map<String, Object> getCheckLoc(Long whId, String opNo) {
		OperationH oph = operationDao.getOperation(whId, opNo);
		List<OperationItem> items = oph.items;
		
		Set<_Location> locs = new HashSet<>();
		for( OperationItem item : items ) {
			_Location loc = item.getOpinv().getLocation();
			locs.add( loc );
			loc.loc1 ++;
			if( item.getToqty() != null) {
				loc.loc2 ++;
			}
		}
		
		Collection<LocationDTO> locDTO = LocationDTO.buildLocTree(locs);
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("locData", locDTO);
		
		List<?> invChecks1 = invDao.getEntities(" from InvCheck where round1.id = ?", oph.getId());
		List<?> invChecks2 = invDao.getEntities(" from InvCheck where round2.id = ?", oph.getId());
		List<?> invChecks3 = invDao.getEntities(" from InvCheck where round3.id = ?", oph.getId());
		String status = WMS.INV_STATUS_04;
		if(invChecks1.size() > 0) {
			InvCheck invCheck = (InvCheck) invChecks1.get(0);
			setMap( map, 1, invCheck, !(invCheck.getRound2() == null && !status.equals(invCheck.getStatus())) );
		}
		if(invChecks2.size() > 0) {
			InvCheck invCheck = (InvCheck) invChecks2.get(0);
			setMap( map, 2, invCheck, !(invCheck.getRound3() == null && !status.equals(invCheck.getStatus())) );
		}
		if(invChecks3.size() > 0) {
			InvCheck invCheck = (InvCheck) invChecks3.get(0);
			setMap( map, 3, invCheck, status.equals(invCheck.getStatus()) );
		}
	
		return map;
	}

	private void setMap(Map<String, Object> map, Integer round, InvCheck invCheck, Boolean readOnly) {
		map.put("invCheck", invCheck);
		map.put("round", round);
		map.put("readOnly", readOnly);
	}
	
	/**
	 * 创建盘点单
	 * 
	 * @param whId
	 * @param ownerId
	 * @param code
	 * @param items
	 * @param type 0：暗盘， 1：明盘
	 * @param rounds 共几轮盘点
	 * @param round  当前盘点第几轮
	 */
	@RequestMapping(value = "/createInvCheck", method = RequestMethod.POST)
	@ResponseBody
	public InvCheck createInvCheck(Long whId, Long ownerId, String code, String items, int type, int rounds, int round) {
		List<_DTO> list = _DTO.parse(items);
		List<OperationItem> itemList = new ArrayList<OperationItem>();
		for (_DTO item : list) {
			Long _inv_id = item.inv_id;
			if( _inv_id == null ) continue; // 查询 组合货品内的子货品库存
			
			Inventory inv = invDao.getEntity( _inv_id );
			
			OperationItem oi = new OperationItem(inv, inv.getQty());
			itemList.add(oi);
		}
		
		InvCheck invCheck;
		if( code == null ) {
			invCheck = new InvCheck();
			invCheck.setCode( SerialNOer.get( WMS.OP_PD + "xxxx" ) );
			invCheck.setWarehouse( new _Warehouse(whId) );
			invCheck.setOwner(new _Owner(ownerId));
			invCheck.setType(type);
			invCheck.setRounds(rounds);
			invCheck.setStatus(WMS.INV_STATUS_01);
		} else {
			List<?> invChecks = invDao.getEntities( "from InvCheck where code = ? and warehouse.id = ?", code, whId );
			invCheck = (InvCheck) invChecks.get(0);
		}
		
		invService.createInvCheck(invCheck, whId, itemList, round);
		return invCheck;
	}
	
	@RequestMapping(value = "/cancelInvCheck/{id}", method = RequestMethod.POST)
	@ResponseBody
	public Object cancelInvCheck(@PathVariable Long id) {
		invService.cancelInvCheck(id);
		return _Util.toMap("200", "取消成功");
	}
	
	@RequestMapping(value = "/closeInvCheck/{id}", method = RequestMethod.POST)
	@ResponseBody
	public Object closeInvCheck(@PathVariable Long id) {
		invService.closeInvCheck(id);
		return _Util.toMap("200", "关闭成功");
	}
	
	@RequestMapping(value = "/submitInvCheck", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> recordCheckResult(Long invCheckId, int round, String items) {
		List<_DTO> list = _DTO.parse(items);
		invService.saveInvCheckData(list, invCheckId, round);
		
		return _Util.toMap("200", "保存成功");
	}
	
	@RequestMapping(value = "/saveInvCheck", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> adjustInvByCheckResult(Long invCheckId, Long opId, String itemIds, String type) {
		return invService.adjustInvByCheckResult(invCheckId, opId, itemIds, type);
	}
	
	@RequestMapping(value = "/unlock/{id}", method = RequestMethod.POST)
	@ResponseBody
	public void unlock(@PathVariable Long id, Double qty_unlock) {
		invService.unlock( id, qty_unlock );
	}
	
	@RequestMapping(value = "/opi/{opiId}/{toInvId}", method = RequestMethod.POST)
	@ResponseBody
	public void changeOPIInv(@PathVariable Long opiId, @PathVariable Long toInvId) {
		invService.changeOPIInv( opiId, toInvId );
	}

	@RequestMapping(value = "/op/close", method = RequestMethod.POST)
	@ResponseBody
	public void closeOp(Long id) {
		invService.closeOp( id );
	}
	
	@RequestMapping(value = "/op/log", method = RequestMethod.GET)
	@ResponseBody
	public List<OperationLog> queryOpLog(Long opId){
		return invService.operationLog(opId);
	}
	
	@RequestMapping(value = "/log", method = RequestMethod.GET)
	@ResponseBody
	public List<InventoryLog> queryInvLog(Long opId){
		return invService.inventoryLog(opId);
	}
	
	@RequestMapping(value = "/query")
	@ResponseBody
	public Map<String,Object> queryInventory(InventorySo invSo, int page, int rows) {
		invSo.getPage().setPageNum( Math.max(page, 1) );
		invSo.getPage().setPageSize( Math.max(rows, 100) );
		return invService.search(invSo, true);
	}
	
	@RequestMapping(value = "/queryInv", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> queryInv(HttpServletRequest request) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		int page = EasyUtils.obj2Int(EasyUtils.checkNull(params.get("page"), 1));
		int rows = EasyUtils.obj2Int(EasyUtils.checkNull(params.get("rows"), 100));
		String orderByFields = EasyUtils.checkNull(params.get("orderByFields"), "o.id desc").toString();
		boolean with_skux = !"false".equals( params.get("with_skux") );

		InventorySo is = new InventorySo();
		is.getPage().setPageNum(page);
		is.getPage().setPageSize(rows);
		is.getOrderByFields().add(orderByFields);
		
		BeanUtil.setDataToBean(is, params);

		return invService.search(is, with_skux);
	}
	
	@RequestMapping(value = "/queryOpi", method = RequestMethod.POST)
	@ResponseBody
	public OperationH queryOpi(Long whId, String opno) {
		OperationH oph = operationDao.getOperation(whId, opno);
		return queryOpi(oph.getId());
	}
	
	@RequestMapping(value = "/queryOpi2", method = RequestMethod.POST)
	@ResponseBody
	public OperationH queryOpi(Long opId) {
		OperationH oph = operationDao.getEntity(opId);
		oph.items.addAll( operationDao.getItems(opId) );
		return oph;
	}
	
	@RequestMapping(value = "/assignOp", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> assignOp(Long whId, String opno, String worker) {
		return operationService.assignOp(whId, opno, worker);
	}
	
	@RequestMapping(value = "/takeJob", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> takeJob(Long whId, String opno) {
		return operationService.takeJob(whId, opno);
	}
	
	@RequestMapping(value = "/takeJobByCode", method = RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> takeIOJob(Long whId, String code, String type) {
		return operationService.takeIOJob(whId, code, type);
	}
	
	@RequestMapping(value = "/opAcceptYN", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> opAcceptConfirm(HttpServletRequest request, Long opId) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		operationService.acceptConfirm(opId, params.get("status"), params.get("remark"));
		
		return _Util.toMap("200", "领用成功");
	}
	
	@RequestMapping(value = "/approveOp", method = RequestMethod.POST)
	@ResponseBody
	public void approveOp(HttpServletRequest request) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		String approver = params.get("approver");
		String type = params.get("type");
		String codeOrId = params.get("codeOrId");
		operationService.approveOp(approver, type, codeOrId);
	}
	
	@RequestMapping(value = "/opCheckException", method = RequestMethod.POST)
	@ResponseBody
	public void opCheckException(HttpServletRequest request, Long opId, Long asnId, Long orderId) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		
		OperationH op = null;
		if(opId != null) {
			op = operationDao.getEntity(opId);
		} 
		else if (asnId != null) {
			Asn asn = (Asn) operationDao.getEntity(Asn.class, asnId);
			op = operationDao.getOperation(asn.getAsnno(), WMS.OP_IN);
		}
		else if (orderId != null) {
			OrderH order = (OrderH) operationDao.getEntity(OrderH.class, orderId);
			op = operationDao.getOperation(order.getOrderno(), WMS.OP_YH);
		}
		
		if(op != null) {
			operationService.opCheckException(op, params.get("content"), params.get("remark"));
		}
	}
	
	@RequestMapping(value = "/opException", method = RequestMethod.POST)
	@ResponseBody
	public OpException opException(HttpServletRequest request, String pkdIds) {
		Map<String, String> m = DMUtil.getRequestMap(request, false);
		return operationService.opException(pkdIds, m.get("content"), m.get("remark"), m.get("type"));
	}
	
	@RequestMapping(value = "/closeException", method = RequestMethod.POST)
	@ResponseBody
	public void closeException(Long opeId, Long pkdId, HttpServletRequest request) {
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		operationService.closeException(pkdId, opeId, params.get("result"));
	}
}
