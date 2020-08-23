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
import java.util.List;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.boudata.wms.AbstractTest4WMS;
import com.boudata.wms.RuleType;
import com.boudata.wms.entity.Asn;
import com.boudata.wms.entity.AsnItem;
import com.boudata.wms.entity.OperationItem;

import junit.framework.Assert;

/**
 *  执行推荐规则引擎，为每条入库库存推荐一个或多个目标库位，及每个库位可以上架的量
 *  
 *  空库位优先
	寻找既有库存（同类产品合并、相邻）
	历史上架记录
	根据产品的ABC动性分配上架库位
	根据产品的属性(正常品、残损品)分配上架库位
	按照不同的包装(托盘、箱、件)分配不同的库位
	混批号、混产品限定（一品一位、一批号一位）
	同批号产品合并
	不同订单类型上架到不同库位（采购入库、退货入库）
*/
@SuppressWarnings("unchecked")
public class PutawayRuleTest extends AbstractTest4WMS {
	
	@Autowired AsnService asnService;
	@Autowired AsnAction asnAction;
	
	@Test
	public void test() {
		
		initRule();
		
		Asn asn = createAsn("ASN-001", OW1, W1, 5);

		List<AsnItem> items = (List<AsnItem>) commonDao.getEntities("from AsnItem where asn.id=?", asn.getId());
		for(AsnItem ai : items ) {
			ai.setQty_this( ai.getQty() );
		}
		asnService.inbound( asn.getId(), items );
		
		asnAction.prePutaway(null);
		
		List<OperationItem> preItems = (List<OperationItem>) asnAction.prePutaway(asn.getId()).get("items");
		
		List<JSONObject> list = new ArrayList<JSONObject>();
		Double qty = 0D;
		for(OperationItem pwd : preItems) {
			qty += pwd.getQty();
			log.debug(pwd);
			
			JSONObject obj = new JSONObject();
			obj.put("asnitem_id", pwd.getAsnitem().getId());
			obj.put("qty", "2");
			obj.put("toloccode", CC_LOC_1.getCode());
			obj.put("inv_id", pwd.getOpinv().getId());
			
			list.add( obj );
		}
		Assert.assertEquals(180D, qty); // 三个存储库位总容量只有 50 + 60 + 70 = 180
 
		asnAction.putaway(list.toString());
		
		preItems = (List<OperationItem>) asnAction.prePutaway(asn.getId()).get("items");
	}

	protected void initRule() {
		CC_LOC_1.setCapacity(50);
		CC_LOC_2.setCapacity(60);
		CC_LOC_3.setCapacity(70);
		log.debug( CC_LOC_1.toString() );
		
		// 空库位优先
    	String ruleTxt = "select l.id loc_id, inv.qty from wms_location l "
    			+ "left join  (select location_id, sum(qty) qty from wms_inv where wh_id = ${whID} group by location_id) inv on l.id = inv.location_id "
    			+ "left join component_param p on l.type_id = p.id "
    			+ "where l.status = 1 and l.warehouse_id = ${whID} and p.text in ('存储区')"
    			+ "order by qty asc, capacity desc; ;";
    	
        W1.setPutawayr( createRule("pw-rule1", ruleTxt, RuleType.PUTAWAY_RULE) );
    }
	
}
