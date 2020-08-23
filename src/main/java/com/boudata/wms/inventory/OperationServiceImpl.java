/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.inventory;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.WMS;
import com.boudata.wms._Util;
import com.boudata.wms.dao.AsnDao;
import com.boudata.wms.dao.OperationDao;
import com.boudata.wms.dao.OrderHDao;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.OpException;
import com.boudata.wms.entity.OperationH;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderH;
import com.boudata.wms.entity.OrderWave;
import com.boudata.wms.entity._Warehouse;

@Service("OperationService")
public class OperationServiceImpl implements OperationService {
	
	protected Logger log = Logger.getLogger(this.getClass());

	@Autowired private AsnDao asnDao;
	@Autowired private OrderHDao orderDao;
	@Autowired private OperationDao operationDao;
	

	public Map<String, Object> assignOp(Long whId, String opno, String worker) {
		OperationH op = operationDao.getOperation(whId, opno);
		String opStatus = op.getStatus();
		if("已完成,部分完成,关闭".indexOf(opStatus) >= 0) {
			return _Util.toMap("-1", "工单【" + opno + "】状态为【" +opStatus+ "】，无法领用");
		}
		
		op.setWorker(worker);
		op.setStatus( WMS.OP_STATUS_05 );
		op.fixOpType();
		operationDao.update(op);

		return _Util.toMap("200", "领用成功");
	}
	
	public Map<String, Object> takeJob(Long whId, String opno) {
		OperationH op = operationDao.getOperation(whId, opno);
		String op_status = op.getStatus();
		if("新建,拒绝".indexOf(op_status) < 0) {
			return _Util.toMap("-1", "工单【" + opno + "】状态为【" +op_status+ "】，无法领用");
		}
		
		String worker = Environment.getUserCode();
		op.setWorker(worker);
		op.setStatus( WMS.OP_STATUS_05 );
		op.fixOpType();
		
		OrderWave wave = op.getWave();
		if( wave == null ) {
			String code = _Util.getDocNo(opno);
			if( opno.contains( "-" + WMS.OP_IN ) ) {
				Asn asn = asnDao.getAsn(code);
				asn.setWorker(worker);
			} 
			else if ( opno.contains( "-" + WMS.OP_OUT ) ) {
				OrderH order = orderDao.getOrder(code);
				order.setWorker(worker);
			}
		} 
		else {
			List<OrderH> orders = orderDao.getOrderByWave(wave.getId());
			for( OrderH order : orders ) {
				order.setWorker(worker);
			}
		}
		operationDao.update(op);
		
		Map<String, Object> map = _Util.toMap("200", "领用成功");
		map.put("op", op);
		return map;
	}
	
	public Map<String, Object> takeIOJob(Long whId, String code, String type) {
		String worker = Environment.getUserCode();
		String opno, op_type, op_type_name;
		Double qty = 0D;
		Integer skus = 0;
		
		if("asn".equals(type)) {
			Asn asn = asnDao.getAsn(code);
			asn.setWorker(worker);
			
			op_type = WMS.OP_IN;
			op_type_name = WMS.OP_TYPE_IN;
			opno = asn.genOpNo(op_type);
			
			qty = asn.getQty();
			skus = asn.getSkus();
		} 
		else {
			OrderH order = orderDao.getOrder(code);
			order.setWorker(worker);
			
			op_type = WMS.OP_OUT;
			op_type_name = WMS.OP_TYPE_OUT;
			opno = order.genOpNo( op_type );
			
			qty = order.getQty();
			skus = order.getSkus();
		}
		
		OperationH op = operationDao.getOperation(code, op_type);
		if(op == null) {
			op = new OperationH();
			op.setQty(qty);
			op.setSkus(skus);
			op.setOpno( opno );
			op.setOptype( WMS.opType( op_type_name ) );
			op.setWarehouse( new _Warehouse(whId));
			operationDao.create(op);
		} 
		
		op.setStatus( WMS.OP_STATUS_05 ); 
		op.setWorker( worker );
		operationDao.update(op);
		
		Map<String, Object> map = _Util.toMap("200", "自主领用成功");
		map.put("op", op);
		return map;
	}
	
	// 作业单异常反馈
	public OpException opException(String pkdIds, String content, String remark, String type) {
		List<OperationItem> pkds = operationDao.getItems("from OperationItem where id in (" + EasyUtils.checkNull(pkdIds, -999) + ")");
		OpException ope = null;
		if( pkds.isEmpty() ) {
			ope = new OpException();
			setOpException(content, remark, type, ope);
			return ope;
		}
		
		for(OperationItem pkd : pkds) {
			pkd.setStatus("异常");
			orderDao.update(pkd);
			
			ope = new OpException();
			ope.setOpItem(pkd);
			setOpException(content, remark, type, ope);
		}
		
		return ope;
	}
	
	public void closeException(Long pkdId, Long opeId, String result) {
		if(pkdId != null) {
			OperationItem pkd = operationDao.getOpItem(pkdId);
			pkd.setStatus("");
			orderDao.update(pkd);
		}
		
		OpException ope = (OpException) operationDao.getEntity(OpException.class, opeId);
		ope.setResult(result);
		ope.setStatus(WMS.OpExc_STATUS_02);
		orderDao.update(ope);
	}
	
	public void opCheckException(OperationH op, String content, String remark) {
		OpException ope = new OpException();
		ope.setOperation(op);
		setOpException(content, remark, WMS.OpExc_TYPE_04, ope);
		
		op.setUdf1(content);
		orderDao.update(op);
	}
	
	private void setOpException(String content, String remark, String type, OpException ope) {
		ope.setType(type);
		ope.setContent(content);
		ope.setRemark(remark);
		ope.setStatus(WMS.OpExc_STATUS_01);
		orderDao.createObject(ope);
	}
	
	public void acceptConfirm(Long opId, String status, String remark) {
		OperationH op = operationDao.getEntity(opId);
		
		if( WMS.OP_STATUS_06.equals(status) ) {
			OpException ope = new OpException();
			ope.setOperation(op);
			setOpException(remark, "", WMS.OpExc_TYPE_03, ope);
		}
		else {
			op.setWorker(Environment.getUserCode());
		}
		
		op.setStatus(status);
		op.setUdf1(remark);
		orderDao.update(op);
	}

	public void approveOp(String approver, String type, String codeOrId) {
		OperationH op = null;
		if("asn".equals(type)) {
			op = operationDao.getOperation(codeOrId, WMS.OP_IN);
		} else if("order".equals(type)) {
			op = operationDao.getOperation(codeOrId, WMS.OP_OUT);
		} else {
			Long opId = EasyUtils.str2Long(codeOrId);
			op = operationDao.getEntity(opId);
		}
		
		op.setApprover(approver);
		operationDao.update(op);
	}
}
