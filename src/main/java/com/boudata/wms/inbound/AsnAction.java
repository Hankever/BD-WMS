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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.util.BeanUtil;
import com.boubei.tss.util.DateUtil;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.dao.InventoryDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.SkuDao;
import com.boudata.wms.dto._DTO;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.Inventory;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Sku;
import com.boudata.wms.entity._Warehouse;

@Controller("AsnAction")
@RequestMapping( {"/asn", "/wms/api"} )
public class AsnAction {
	
	protected Logger log = Logger.getLogger(this.getClass());
	
	@Autowired private AsnService asnService;
	@Autowired private AsnDao asnDao;
	@Autowired private SkuDao skuDao;
	@Autowired private InventoryDao invDao;
	@Autowired private OperationDao operationDao;
	
	/** 增删改通过录入表接口了 */
	
	/**
	 * 1、单号自动创建
	 * 2、指定既有单号为入库单号，此单号在数据库不存在
	 * 3、指定既有单号为入库单号，此单号在数据库已存在 && 状态 == 已完成（即多次用同一单号入库）
	 * 4、指定既有单号为入库单号，此单号在数据库已存在 && 状态 == 新建
	 */
	@RequestMapping(value = {"/createAndReceive", "/createAndInbound"}, method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> createAndInbound(HttpServletRequest request, Long whId, Long ownerId) {
		if( !WMS.no_asn_inbound() && WMS.isWorker() ) {
			throw new BusinessException(WMS.ASN_ERR_1);
		}
		
		Map<String, String> params = DMUtil.getRequestMap(request, false);
		
		Asn asn = null;
		String id = params.remove("id");
		String asnno = params.remove("asnno");
		if( !EasyUtils.isNullOrEmpty(id) ) {
			asn = asnDao.getEntity( EasyUtils.obj2Long(id) );
			asnno = asn.getAsnno();
		}
		
		List<AsnItem> asnItems = new ArrayList<AsnItem>();
		if( !EasyUtils.isNullOrEmpty(asnno) ) {
			asn = asnDao.getAsn(asnno);
			if( asn != null ) {
				String status = asn.getStatus();
				if( WMS.ASN_STATUS_01.equals(status) ) {
					if(EasyUtils.obj2Double(asn.getQty()) > 0) {
						throw new BusinessException( EX.parse( WMS.ASN_ERR_2, asnno, 1) );
					}
				} else if( !WMS.NO_ASN.equals(asn.getTag()) ) {
					throw new BusinessException( EX.parse( WMS.ASN_ERR_2, asnno, 2) );
				}
				
				asn.setStatus( (String) EasyUtils.checkTrue(WMS.ASN_STATUS_04.equals(status), WMS.ASN_STATUS_03, status) );
				asnItems = asnDao.getItems(asn.getId());
			}
		} 
		
		if( asn == null ) {
			_Owner owner = operationDao.getOwner(ownerId);
			asn = new Asn();
			asn.setAsnno((String) EasyUtils.checkNull(asnno, _Util.genDocNO(owner, "1", false) ));
			asn.setAsnday(DateUtil.today());
			asn.setWarehouse(new _Warehouse(whId));
			asn.setOwner( owner );
		}
		
		BeanUtil.setDataToBean(asn, params);
		asn.setTag(WMS.NO_ASN);

		Set<Long> skuIds = new HashSet<>();
		for(AsnItem asnItem : asnItems) {
			skuIds.add(asnItem.getSku().getId());
		}
		List<AsnItem> itemList = new ArrayList<AsnItem>();
		
		List<_DTO> list = _DTO.parse(params);
		Double qty_total = 0D, money = 0D;
		for (_DTO item : list) {
			Long skuId = item.sku_id;
			AsnItem ai = new AsnItem();
			_Sku sku = skuDao.getEntity(skuId);
			
			Double qty;
			String snList = item.snlist;
			if( !EasyUtils.isNullOrEmpty(snList) ) {
				List<String> sns = Arrays.asList(snList.trim().split(","));
				ai.snlist.addAll(sns);
				qty = sns.size() * 1.0;
			} 
			else {
				qty = item.qty;
			}
			
			if( qty == null || qty <= 0 ) continue;
			
			ai.setSku(sku);
			ai.setQty(qty);
			ai.setLoccode( params.get("toloccode") );
			ai.copyLotAtt(item);
			ai.setQty_this(qty); // 直接入库，订单量 = 入库量
			ai.setPrice( sku._price( item.price ) );
			ai.setMoney( item.money );

			itemList.add(ai);
			skuIds.add(skuId);

			qty_total = MathUtil.addDoubles(qty_total, ai.getQty());
			money =  MathUtil.addDoubles(money, ai.getMoney());
		}
		asn.setSkus( skuIds.size() );
		asn.setQty( MathUtil.addDoubles(asn.getQty(), qty_total) );
		asn.setMoney( MathUtil.addDoubles(asn.getMoney(), money) );

		asnService.createAndInbound(asn, itemList);
		
		Map<String, Object> result = _Util.toMap("200", "入库成功");
		result.put("asnno", asn.getAsnno());
		
		return result;
	}
	
	/**
	 * 直接在入库单页，填写收货库位及数量进行收货入库。
	 */
	@RequestMapping(value = "/inbound", method = RequestMethod.POST)
	@ResponseBody
	public void inboundInAsnHtml(Long id, String items) {
		List<AsnItem> itemList = json2AsnItems(items, null);		
		asnService.inbound(id, itemList);
	}
	
	private List<AsnItem> json2AsnItems(String items, String toloccode) {
		List<AsnItem> itemList = new ArrayList<AsnItem>();
		List<_DTO> list = _DTO.parse(items);
		for(_DTO item : list) {
			AsnItem ai = new AsnItem();
			ai.setId( item.id );
			ai.setLoccode( (String) EasyUtils.checkNull(toloccode, item.loccode) );
			
			Double qty_this;
			String snList = item.snlist;
			if( !EasyUtils.isNullOrEmpty(snList) ) {
				List<String> sns = Arrays.asList(snList.trim().split(","));
				ai.snlist.addAll(sns);
				qty_this = sns.size() * 1.0;
			} else {
				qty_this = item.qty_this;
			}
			ai.setQty_this( qty_this );
			
			if( WMS.checkin_with_lot() ) {
				ai.copyLotAtt(item);
			}
			
			itemList.add(ai);
		}
		return itemList;
	}
	
	/**
	 * 按单验货入库（PC）
	 * 
		A 拆开包装（箱或袋），取出跟箱单，按单上列明的货物明细逐件清点货物（核对数量、规格、品相）
		B 如果货物内包装颜色或尺码有明显差异，则拼装在收货容器中的货物要按“同款同色不同码”优先顺序集中存放
		C 每个容器放入的SKU的个数不超过3个
	 * 
	 * @param id
	 * @param items
	 * @param toloccode  收货容器
	 */
	@RequestMapping(value = "/ck_inbound", method = RequestMethod.POST)
	@ResponseBody
	public void asnCKInbound(Long id, String items, String toloccode) {
		List<AsnItem> itemList = json2AsnItems(items, toloccode);	
		asnService.asnCKInbound(id, itemList);
	}
	
	/**
	 * 按入库工单入库（RF）
	 */
	@RequestMapping(value = "/asnCheckin", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> asnCKInbound4rf(HttpServletRequest request, Long id, String toloccode, Long opId) {
		String items = DMUtil.getRequestMap(request, false).get("items");  // 批次信息里可能含有中文字符
		List<AsnItem> itemList = json2AsnItems(items, toloccode);	
		
		OperationH op = operationDao.getEntity(opId);
		operationDao.evict(op);
		
		asnService.asnCKInbound4rf(id, itemList, op);
		
		return _Util.toMap("200", "验货入库成功");
	}
	
	@RequestMapping(value = "/assignAsn", method = RequestMethod.POST)
	@ResponseBody
	public OperationH assign(Long id, String worker, String type) {
		type = (String) EasyUtils.checkNull(type, WMS.OP_IN);
		return asnService.assign(id, worker, type);
	}

	@RequestMapping(value = "/cancel_inbound", method = RequestMethod.POST)
	@ResponseBody
	public void cancelReceiveAsn(Long id) {
		asnService.cancelInbound(id);
	}
	
	@RequestMapping(value = "/cancel_asn", method = RequestMethod.POST)
	@ResponseBody
	public void cancelAsn(Long id, String remark) {
		asnService.cancelAsn(id, remark);
	}

	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteAsn(String id) {
		String[] ids = EasyUtils.filterEmptyItem(id).split(",");
		for(String _id : ids) {
			asnService.deleteAsn( Long.parseLong(_id) );
		}
	}
	
	@RequestMapping(value = "/close", method = RequestMethod.POST)
	@ResponseBody
	public void closeAsn(Long id) {
		asnService.closeAsn(id);
	}
	
	@RequestMapping(value = "/opQty")
	@ResponseBody
	public List<?> asnOpItems(String asnIds, String optype) {
		String hql = " select asnitem.asn.id, round(sum(qty), 2), max(asnitem.pstatus) from OperationItem "
				+ " where asnitem.asn.id in (" +asnIds+ ") and operation.optype.text = ? "
				+ " group by asnitem.asn.id";
		
		return operationDao.getEntities(hql, optype);
	}

	@RequestMapping(value = "/putawayInfo", method = RequestMethod.GET)
	@ResponseBody
	public List<?> putawayInfo(Long asnId) {
		
		List< Map<String, Object> > result = new ArrayList<>();
		String hql = " from OperationItem where asnitem = ? and operation.optype.text = ? order by id";
		
		List<AsnItem> items = asnDao.getItems(asnId);
		for(AsnItem ai : items) {
			List<OperationItem> sjItems = operationDao.getItems(hql, ai, WMS.OP_TYPE_SJ);   // 上架作业明细
			
			if( sjItems.isEmpty() ) {
				List<OperationItem> rkItems = operationDao.getItems(hql, ai, WMS.OP_TYPE_IN); // 入库作业明细
				for(OperationItem opi : rkItems) {
					Map<String, Object> row = BeanUtil.getProperties(ai);
					row.put("sku_id", ai.getSku().getId());
					row.put("inv_id", opi.getOpinv().getId());
					result.add(row);
				}
			} 
			else {
				for(OperationItem opi : sjItems) {
					Map<String, Object> row = BeanUtil.getProperties(ai);
					row.put("sku_id", ai.getSku().getId());
					row.put("qty_sj", opi.getQty());
					row.put("opitem_id", opi.getId());
					row.put("inv_id", opi.getOpinv().getId());
					row.put("toloccode", opi.getToloccode());
					
					result.add(row);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * 预上架：根据上架规则生成上架指导单，支持批量操作
	 */
	@RequestMapping(value = "/prePutaway", method = RequestMethod.GET)
	@ResponseBody
	public Map<String, Object> prePutaway(Long asnId) {
		if( asnId == null ) {
			return _Util.toMap("555", "asnId不能为空");
		}
		
		List<_Rule> rList = new ArrayList<_Rule>();
		List<OperationItem> sjItems = asnService.prePutaway(asnId, rList);
		
		Map<String, Object> result = new HashMap<>();
		result.put("items", sjItems);
		result.put("rules", rList);
		
		return result;
	}
	
	@RequestMapping(value = "/putaway", method = RequestMethod.POST)
	@ResponseBody
	public Map<String,Object> putaway(String items) {
		List<OperationItem> itemList = new ArrayList<OperationItem>();
		List<_DTO> list = _DTO.parse( items );
		
		Asn asn = null;
		for (_DTO item : list) {
			AsnItem aItem = asnDao.getAsnItem( item.asnitem_id );
			asn = aItem.getAsn();
			
			if(item.qty <= 0) continue;

			Inventory inv = invDao.getEntity(item.inv_id);
			OperationItem oi = new OperationItem();
			oi.setOwner(aItem.getAsn().getOwner());
			oi.setSkucode(aItem.getSku().getCode());
			oi.setQty( item.qty );
			
			oi.setLoccode( inv.getLocation().getCode() );
			oi.setToloccode( item.toloccode );
			oi.copyLotAtt( inv );
			oi.setOpinv( inv );
			oi.setAsnitem( aItem );
			
			itemList.add(oi);
		}
		
		OperationH op = asnService.putaway(asn, itemList);
		
		Map<String, Object> result = _Util.toMap("200", "上架成功");
		result.put("opId", op.getId());
		
		return result;
	}
	
	@RequestMapping(value = "/queryAsnItem", method = RequestMethod.GET)
	@ResponseBody
	public Map<Object, Object> queryAsnItem(String code){
		Asn asn = asnDao.getAsn(code);
		if(asn == null) {
			throw new BusinessException( EX.parse(WMS.ASN_ERR_12, code) );
		}
		
		Long asnId = asn.getId();
		List<AsnItem> items = asnDao.getItems( asnId );
		
		Map<Object, Object> map = new HashMap<Object, Object>();
		map.put( "asnId", asnId );
		map.put( "asn", asn );
		map.put( "items", items );
		map.put( asnId, items );
		return map;
	}
}