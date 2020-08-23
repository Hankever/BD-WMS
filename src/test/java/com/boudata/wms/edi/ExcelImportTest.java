/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.edi;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boubei.tss.EX;
import com.boubei.tss.dm.DMConstants;
import com.boubei.tss.dm.record.Record;
import com.boubei.tss.dm.record.RecordService;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.modules.progress.Progress;
import com.boubei.tss.um.UMConstants;
import com.boubei.tss.util.FileHelper;
import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.WMS;
import com.boudata.wms._edi.ImportAsn;
import com.boudata.wms._edi.ImportOrder;
import com.boudata.wms._edi.ImportService;

public class ExcelImportTest extends AbstractTest4WMS {
	
	static String UPLOAD_PATH = FileHelper.ioTmpDir() + "/upload/";
	
	@Autowired RecordService recordService;
	@Autowired ImportService importService;
	
	@Test
	public void testImportAsn() {
		login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
		String tbl_def = "[ " +
		  "{'label':'入库单号','code':'asnno'}," +
		  "{'label':'下单日期','code':'asnday','type':'date'}," +
		  "{'label':'入库类型','code':'type'}," +
		  "{'label':'货品','code':'skuname','nullable':'false'}," +
		  "{'label':'货品编码','code':'skucode','nullable':'false'}," +
		  "{'label':'订单数量','code':'qty','nullable':'false','type':'number'}," +
		  "{'label':'单价','code':'price','type':'number'}," +
		  "{'label':'金额','code':'money','type':'number'}," +
		  "{'label':'批号','code':'lotatt01'}," +
		  "{'label':'装箱量','code':'lotatt02'}," +
		  "{'label':'生产日期','code':'createdate','type':'date'}," +
		  "{'label':'过期日期','code':'expiredate','type':'date'}," +
		  "{'label':'入库库位','code':'loccode'}," +
		  "{'label':'备注','code':'remark'}," +
		  "{'label':'自定义1','code':'udf1'}," +
		  "{'label':'自定义2','code':'udf2'}," +
		  "{'label':'自定义3','code':'udf3'}," +
		  "{'label':'自定义4','code':'udf4'}" +
		"]";
		
		Record record = new Record();
		record.setName("wms_asn_import");
		record.setTable("wms_asn_import");
		record.setType(1);
		record.setParentId(0L);
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setDefine(tbl_def);
		record.setBatchImp(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		Long recordId = record.getId();
		
		login(super.pm);
		String data = "入库单号,下单日期,入库类型,*货品*,*货品编码*,*订单数量*,单价,金额,批号,装箱量,生产日期,过期日期,入库库位,备注,自定义1\n"
				+ "1900000984,2019/9/7,销售出库,散大核桃,10002,1,20,20,101,1,,,,test,1\n"
				+ "1900000984,2019/9/7,销售出库,牛肉礼盒6,5007,5,85,425,101,1,,,,test,2\n"
				+ "1900000985,2019/9/7,销售出库,毛巾,6010,2,8,16,101,1,,,,test,3\n";
		
		String filepath = UPLOAD_PATH + "2.csv";
		FileHelper.writeFile(new File(filepath), data); 
		
		ImportAsn im = new ImportAsn();
		request.addParameter("recordId", recordId.toString());
		
		try {
			im.processUploadFile(request, filepath, "2.csv");
			Assert.fail();
		} 
	    catch (Exception e) { 
	    	// 仓库不能为空
	    }
		
		request.addParameter("warehouse", W1.getId().toString());
		
		try {
			im.processUploadFile(request, filepath, "2.csv");
			Assert.fail();
		} 
	    catch (Exception e) { 
	    	// 货主不能为空
	    }
		
		try {
			request.addParameter("owner", OW1.getId().toString());
			im.processUploadFile(request, filepath, "2.csv");
			im.processUploadFile(request, filepath, "2.csv");
		} 
	    catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.fail( e.getMessage() );
		}
		
		// 将单据状态设置为作业中
		commonDao.executeHQL("update Asn set status = '已完成' where asnno = ? and domain = ?", "1900000984", domain);
		try {
			im.processUploadFile(request, filepath, "2.csv");
			Assert.fail();
		} 
	    catch (Exception e) { 
	    	Assert.assertTrue( e.getMessage().indexOf("已存在并已经开始作业") > 0 );
	    }
	}
	
	@Test
	public void testImportOrder() {
		
		login(UMConstants.ADMIN_USER_ID, UMConstants.ADMIN_USER);
		String tbl_def = "[ " +
		  "{'label':'出库单号','code':'orderno'}," +
		  "{'label':'下单日期','code':'orderday','type':'date'}," +
		  "{'label':'出库类型','code':'type'}," +
		  "{'label':'货品','code':'skuname','nullable':'false'}," +
		  "{'label':'货品编码','code':'skucode','nullable':'false'}," +
		  "{'label':'订单数量','code':'qty','nullable':'false','type':'number'}," +
		  "{'label':'单价','code':'price','type':'number'}," +
		  "{'label':'金额','code':'money','type':'number'}," +
		  "{'label':'批号','code':'lotatt01'}," +
		  "{'label':'装箱量','code':'lotatt02'}," +
		  "{'label':'收件人','code':'d_receiver'}," +
		  "{'label':'收件电话','code':'d_mobile'}," +
		  "{'label':'收件地址','code':'d_addr'}," +
		  "{'label':'备注','code':'remark'}," +
		  "{'label':'自定义1','code':'udf1'}," +
		  "{'label':'自定义2','code':'udf2'}," +
		  "{'label':'自定义3','code':'udf3'}," +
		  "{'label':'自定义4','code':'udf4'}" +
		"]";
		
		Record record = new Record();
		record.setName("wms_order_import");
		record.setTable("wms_order_import");
		record.setType(1);
		record.setParentId(0L);
		record.setDatasource(DMConstants.LOCAL_CONN_POOL);
		record.setDefine(tbl_def);
		record.setBatchImp(ParamConstants.TRUE);
		
		recordService.createRecord(record);
		Long recordId = record.getId();
		
		login(super.pm);
		String data = "出库单号,下单日期,出库类型,*货品*,*货品编码*,*订单数量*,单价,金额,批号,装箱量,收件人,收件电话,收件地址,备注,自定义1\n"
				+ "1900000984,2019/9/7,销售出库,散大核桃,10002,1,20,20,101,1,张三,13588833833,xxx号,test,1\n"
				+ "1900000984,2019/9/7,销售出库,牛肉礼盒6,5007,5,85,425,101,1,张三,13588833833,xxx号,test,2\n"
				+ "1900000985,2019/9/7,销售出库,毛巾,6010,2,8,16,101,1,张三,13588833833,xxx号,test,3\n"
				+ ",2011/9/7,销售出库,毛巾,6010,2,8,16,101,1,张三,13588833833,xxx1号,test,4\n"
				+ ",2011/9/7,销售出库,毛巾,6010,2,8,16,101,1,张三,13588833833,xxx1号,test,5\n"
				+ ",2011/9/7,销售出库,毛巾,6010,2,8,16,101,1,李四,13588833834,xxx2号,test,6\n"
				+ ",2011/9/7,销售出库,毛巾,6010,2,8,16,101,1,,,,test,7\n";
		
		String filepath = UPLOAD_PATH + "2.csv";
		FileHelper.writeFile(new File(filepath), data); 
		
		ImportOrder im = new ImportOrder();
		request.addParameter("recordId", recordId.toString());
		
		try {
			im.processUploadFile(request, filepath, "2.csv");
			Assert.fail();
		} 
	    catch (Exception e) { 
	    	// 仓库不能为空
	    }
		
		request.addParameter("warehouse", W1.getId().toString());
		
		try {
			im.processUploadFile(request, filepath, "2.csv");
			Assert.fail();
		} 
	    catch (Exception e) { 
	    	// 货主不能为空
	    }
		
		try {
			request.addParameter("owner", OW1.getId().toString());
			im.processUploadFile(request, filepath, "2.csv");
			Assert.assertEquals(5, commonDao.getEntities("from OrderH").size());
			
			im.processUploadFile(request, filepath, "2.csv");
			Assert.assertEquals(8, commonDao.getEntities("from OrderH").size());
		} 
	    catch (Exception e) {
			log.error(e.getMessage(), e);
			Assert.fail( e.getMessage() );
		}
		
		
		// 将单据状态设置为作业中
		commonDao.executeHQL("update OrderH set status = '已分配' where orderno = ? and domain = ?", "1900000984", domain);
		try {
			im.processUploadFile(request, filepath, "2.csv");
			Assert.fail();
		} 
	    catch (Exception e) { 
	    	Assert.assertTrue( e.getMessage().indexOf("已存在并已经开始作业") > 0 );
	    }
	}
	
	@Test
	public void testOther() {
		List<Map<String, String>> rows = new ArrayList<>();
		
		Map<String, String> item = new HashMap<String, String>();
		item.put("asnno", "001");
		rows.add(item);
		
		String result = importService.importAsn(W1.getId(), OW1.getId(), rows , new Progress(1)); // 导入一个只有单号的空单
		Assert.assertTrue( result.indexOf("共导入明细0行，合计订单1个") > 0 );
		
		
		rows = new ArrayList<>();
		Map<String, String> item1 = new HashMap<String, String>();
		item1.put("asnno", "001");
		rows.add(item1);
		
		Map<String, String> item2 = new HashMap<String, String>();
		item2.put("skucode", "S111");
		rows.add(item2);
		try {
			importService.importAsn(W1.getId(), OW1.getId(), rows , new Progress(1));
			Assert.fail();
		} 
	    catch (Exception e) { 
	    	Assert.assertEquals( EX.parse(WMS.EDI_ERR_1, "[2]") , e.getMessage());
	    }
	}
}
