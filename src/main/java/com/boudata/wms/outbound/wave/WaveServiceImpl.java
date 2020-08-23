/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.outbound.wave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMUtil;
import com.boubei.tss.dm.ddl._Field;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderItem;
import com.boudata.wms.entity.OrderWave;

@Service("WaveService")
public class WaveServiceImpl implements WaveService {
	
	Logger log = Logger.getLogger(this.getClass());

	@Autowired private OrderHDao orderDao;
	@Autowired private OperationDao operationDao;
	@Autowired private InvOperation4Order invOperation;
    @Autowired private WaveRuleEngine ruleEngine;

    // 创建拣货作业子波次
	public OrderWave excutingWave(OrderWave wave, List<Long> orderIds) {

        Long waveId = wave.getId();
        if (waveId != null) { 
            wave = orderDao.getWave(waveId);  // 如果是已经保存过的wave，则直接取用
        }
        else {
            wave = createWave(wave, orderIds);
            waveId = wave.getId();
        }
        
        List<OrderH> orders = orderDao.getOrderByWave(waveId);
        if ( orders.isEmpty() ) {
            return wave;
        }

        orderIds = new ArrayList<Long>(_Util.getIDs(orders));
        return ruleEngine.excuteRule(wave, orderIds);
    }
    
	public OrderWave createWave(OrderWave wave, List<Long> orderIds) {
		wave.setStatus( WMS.W_STATUS_01 );
        orderDao.createObject(wave);
        
        int total = 0;
        List<OrderH> orderList = orderDao.getOrders(orderIds);
        for (OrderH order : orderList) {
            OrderWave oldWave = order.getWave();
			if (oldWave != null) {
				if( WMS.W_ORIGIN_02.equals(oldWave.getOrigin()) ) {
					throw new BusinessException( EX.parse(WMS.WAVE_ERR_1, order.getOrderno(), oldWave.getCode()) );
				}
				oldWave.setTotal(0);
				oldWave.setStatus(WMS.W_STATUS_02);
            }
            
            // 外部取消关闭的订单过滤掉
            if(WMS.O_STATUS_00.equals(order.getStatus())) {
            	continue;
            }

            total++;
            order.setWave(wave);
            orderDao.updateWithoutFlush(order);
        }
    
        wave.setTotal( total );
        orderDao.update(wave);
        
        return wave;
    }
	
	public OrderWave cancelWave(Long waveId) {
		OrderWave wave = orderDao.getWave(waveId);
		wave.setStatus(WMS.W_STATUS_02);
		orderDao.update(wave);
		
		Collection<Long> orderIds = new ArrayList<>();
		List<OrderH> orderList = orderDao.getOrderByWave(waveId);
		for (OrderH order : orderList) {
			order.setWave(null);
			order.setStatus( WMS.O_STATUS_01 );
			orderDao.update(order);
			orderIds.add(order.getId());
		}
		
		List<OrderItem> soiList = orderDao.getOrderItems(orderIds );
		for (OrderItem soi : soiList) {
			soi.setQty_allocated(0D);
			orderDao.update(soi);
		}
		
		// 释放占有的冻结库存
		List<OperationH> subwaves = operationDao.getSubwaves(wave.getId());
		for(OperationH sw : subwaves) {
			sw.setStatus(WMS.OP_STATUS_02);
			sw.setUdf1("分配取消");
			sw.setOpno( SerialNOer.get("CWxxxx") );
			sw.setWave(null);
			sw.setOptype( WMS.opType(WMS.OP_TYPE_FP) );
			
			List<OperationItem> pkds = operationDao.getItems(sw.getId());
			for(OperationItem pkd : pkds) {
				pkd.setOrderitem( null );
				pkd.setQty( pkd.getQty() * -1 );
			}
			invOperation.execOperations( sw, pkds );
		}
		
		return wave;
	}
	
	public List<OperationH> allocate(Long waveId) {
		OrderWave wave = orderDao.getWave(waveId);
		List<OrderH> orderList = orderDao.getOrderByWave(waveId);
		List<Long> orderIds = new ArrayList<>();
		for( OrderH order : orderList ) {
			orderIds.add( order.getId() );
		}
		
		wave = excutingWave(wave, orderIds);
		orderDao.flush();
		
		// 查询所得拣货作业单
		List<OperationH> subwaves = operationDao.getSubwaves(wave.getId());
		if( subwaves.isEmpty() ) {
			for( OrderH order : orderList ) {
				order.setWave(wave); // 波次分配时，没库存的订单从波次剔除了；此处重新绑定波次
			}
			
			OperationH sw = new OperationH();
			sw.setWarehouse(wave.getWarehouse());
			sw.setOptype( WMS.opType(WMS.OP_TYPE_FP) );
			sw.setErrorMsg("订单库存分配失败，库存可能不足，详情请查看订单明细备注");
			return Arrays.asList(sw);
		}
		
		// 剩下的都是库存满足的，清除其之前留下的“库存不足”标记
		orderList = orderDao.getOrderByWave(waveId);
		for(OrderH order : orderList) {
			if( order.isShort() ) {
				order.setTag( WMS.W_ORIGIN_02 );
				List<OrderItem> items = orderDao.getOrderItems( order.getId() );
				for(OrderItem item : items) {
					item.setRemark(null);
				}
			}
		}
		orderDao.flush();
		
		return subwaves;
	}
	
	public List<OrderH> cancelAllocate(Long waveId) {
		OrderWave wave = orderDao.getWave(waveId);
		wave.setStatus(WMS.W_STATUS_01);
		orderDao.update(wave);
		
		List<OrderH> orderList = orderDao.getOrderByWave(waveId);
		for(OrderH order : orderList) {
			String status = order.getStatus();
			if( !WMS.O_STATUS_03.equals( status ) && !WMS.O_STATUS_41.equals( status ) ) {
				throw new BusinessException( EX.parse(WMS.O_ERR_14, status) );
			}
			order.setStatus(WMS.O_STATUS_01);
			orderDao.update(order);
		}
		
		List<OperationH> subwaves = operationDao.getSubwaves(waveId);
		for(OperationH subwave : subwaves) {
			if(!WMS.OP_STATUS_02.equals(subwave.getStatus())) {
				String opno = subwave.getOpno() + "-FQ" + SerialNOer.get( _Field.SNO_xxxx );
				subwave.setOpno(opno);
				subwave.setStatus(WMS.OP_STATUS_02);
				subwave.setUdf1("分配取消");
				subwave.setWave(null);
			}
			
			List<OperationItem> pkds = operationDao.getItems(subwave.getId());
			List<OperationItem> unFinishedPkds = new ArrayList<>();
			for(OperationItem pkd : pkds) {
				if( !WMS.OP_STATUS_04.equals( pkd.getStatus() )) {
					pkd.setQty( pkd.getQty() * -1 );
					pkd.setStatus(WMS.OP_STATUS_02);
					unFinishedPkds.add(pkd);
				}
			}
			
			invOperation.execOperations(subwave, unFinishedPkds);
		}
		orderDao.flush();
		
		return orderList;
	}
	
	public void changeWave(Long waveId, List<Long> orderIds, String type) {
		OrderWave wave = orderDao.getWave(waveId);
		List<OrderH> orders = orderDao.getOrders(orderIds);
		for(OrderH order : orders) {
			order.setWave( "add".equals(type) ? wave : null );
		}
		
		wave.setTotal( orderDao.getOrderByWave(wave.getId()).size() );
		orderDao.flush();
	}
	
	public void assign(Long pkhId, List<OperationItem> pkds, String worker) {
		Set<OrderH> orders = new HashSet<>();
		for(OperationItem pkd : pkds) {
			pkd.setWorker(worker);
			orderDao.update(pkd);
			orders.add( pkd.getOrderitem().getOrder() );
		}
		
		for(OrderH order : orders) {
			order.setWorker(worker);
			orderDao.update(order);
		}
		
		List<OperationItem> all_pkds = operationDao.getItems(pkhId);
		Set<Object> wokerSet = new HashSet<Object>( EasyUtils.objAttr2List(all_pkds, "worker") ); // 拣货单可有多个拣货人，PKD分组分配
		String workers = EasyUtils.list2Str(wokerSet);
		
		OperationH pkh = operationDao.getEntity(pkhId);
		pkh.setOptype( WMS.opType(WMS.OP_TYPE_BCJH) );
		pkh.setStatus( WMS.OP_STATUS_05 );  // 已经存在的作业单会重置为“新建”状态
		pkh.setWorker( workers );
		orderDao.update(pkh);
	}

	@SuppressWarnings("unchecked")
	public List<OperationH> splitPKH(String ordernos, Long opId) {
		if( EasyUtils.isNullOrEmpty(ordernos) ) {
			throw new BusinessException("明细行为空，请选择要拆分的拣货明细行");
		}
		
		String hql = " from OperationItem where orderitem.order.orderno in ( " + DMUtil.insertSingleQuotes(ordernos) + ") and operation.id = ? and domain = ?";
		List<OperationItem> split_pkds = (List<OperationItem>) operationDao.getEntities(hql, opId, Environment.getDomain());
		if( split_pkds.size() == 0 ) {
			throw new BusinessException("明细行为空，请选择要拆分的拣货明细行");
		}
		Set<Long> _opItemIds = _Util.getIDs(split_pkds);
		
		// 重新设置 被拆分的作业单的 skus/qty
		List<OperationItem> all_pkds = operationDao.getItems(opId);
	
		Set<String> skuCodes = new HashSet<>();
		Set<String> split_skuCodes = new HashSet<>();
		Double qty = 0D, split_qty = 0D;
		for(OperationItem opi : all_pkds) {
			if( !_opItemIds.contains(opi.getId()) ) {
				skuCodes.add(opi.getSkucode());
				qty = MathUtil.addDoubles(qty, opi.getQty());
			} else {
				split_skuCodes.add(opi.getSkucode());
				split_qty = MathUtil.addDoubles(split_qty, opi.getQty());
			}
		}
		
		OperationH pkh = operationDao.getEntity(opId);
		pkh.setSkus( skuCodes.size() );
		pkh.setQty( qty );
		orderDao.update(pkh);
		
		// 将拆分出来的OperationItem 创建成一个新的作业单
		OperationH op = new OperationH();
		op.setWarehouse( pkh.getWarehouse() );
		op.setOpno( _Util.genOpNO(pkh.getOpno() + "-") );
	    op.setWorker( pkh.getWorker() );
	    op.setOptype( pkh.getOptype() );
	    op.setStatus( WMS.OP_STATUS_01 );
	    op.setWave( pkh.getWave() );
		op.setSkus( split_skuCodes.size() );
		op.setQty( split_qty );
		orderDao.createObject(op);
		
		for(OperationItem opi : split_pkds) {
			opi.setOperation(op);
			orderDao.update(opi);
		}
		
		List<OperationH> ops = new ArrayList<OperationH>();
		ops.add(pkh);
		ops.add(op);
		return ops;
	}
}
