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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import com.boubei.tss.framework.Global;
import com.boubei.tss.framework.exception.BusinessException;
import com.boubei.tss.framework.persistence.ICommonDao;
import com.boubei.tss.framework.persistence.IEntity;
import com.boubei.tss.modules.sn.SerialNOer;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.URLUtil;
import com.boudata.wms.entity._Owner;
import com.boudata.wms.entity._Rule;
import com.boudata.wms.entity._Warehouse;


public class _Util {
	
	static Logger log = Logger.getLogger(_Util.class);
	
	public static String ruleDef(List<_Rule> rList, String defaultDef, _Rule...rules) {
		for(_Rule rule : rules) {
			if(rule != null && !EasyUtils.isNullOrEmpty(rule.getContent())) {
				rList.add( rule );
				return rule.getContent();
			}
		}
		return defaultDef;
	}
	
	/**
	 * 根据作业单号反解出 单据号
	 */
	public static String getDocNo(String opno) {
		int index = opno.lastIndexOf("-");
		return index > 0 ? opno.substring(0, index) : opno;
	}
	
	/**
	 * 自定义出入库单号生成规则：默认 OyyMMdd000x，也可以 货主编码xxxx
	 * 
	 * @param owner
	 * @param type  A：入库 O：出库
	 * 		  1:PC 2:小程序/RF 3:Excel
	 */
	public static String genDocNO(_Owner owner, String type, boolean impcsv) {
		String code = owner.getCode().trim();
		if( code.startsWith("{") && code.endsWith("}") ) {
			code = code.substring(1, code.length() - 1);
			code = SerialNOer.get( type + code );
		}
		else {
			code = SerialNOer.get( type );
		}
		
		return (impcsv ? "3" : EasyUtils.checkTrue(URLUtil.isMobile(), "2", "1")) + code;
	}
	
	public static String genOpNO(String opType) {
		 return SerialNOer.get( opType + "xxxx" );
	}
	
	/**
	 * 出（入）库完成，关闭该出（入）库单相关的未完成的其它已领用工单
	 */
	public static void closeOpWhenFinished(String docNo, _Warehouse warehouse) {
		ICommonDao commDao = (ICommonDao) Global.getBean("CommonDao");
		String hql = "update OperationH set status = ? "
				+ "	where status in ('已领用', '新建', '部分完成') and opno like '" +docNo+ "-%' and warehouse = ? ";
		commDao.executeHQL(hql, WMS.OP_STATUS_00, warehouse);
	}
	
	public static Map<String, Object> toMap(Object code, String message) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("code", code);
		map.put("message", message);
		
		return map;
	}
	
	/**
	 * 前台传的json字符串 转成 list对象
	 */
	public static List<Map<String, Object>> json2List(String json) {
		try {
			return EasyUtils.json2List(json);
		} catch (Exception e) {
			throw new BusinessException("JSON数据格式有误，请检查！" + e.getMessage());
		}
	}
	
	/**
	 * 传入的json字符串 转成 map对象
	 */
	public static Map<String, ?> json2Map(String json) {
		if (EasyUtils.isNullOrEmpty(json)) {
			return null;
		}
		if (!json.trim().startsWith("{")) {
			log.error("非法的json串: " + json);
			throw new BusinessException("非法的json串！");
		}

		try {
			return EasyUtils.json2Map2(json);
		} catch (Exception e) {
			log.error("JSON数据格式有误：" + json);
			throw new BusinessException("JSON数据格式有误，请检查！" + e.getMessage());
		}
	}
	
    /** 获取对象列表的ID集合 */
    public static Set<Long> getIDs(Collection<? extends IEntity> entityList) {
        if(entityList == null) {
            return new HashSet<Long>();
        }
        
        Set<Long> entityIdList = new LinkedHashSet<Long>();
        for(IEntity entity : entityList) {
            Long entityID = (Long) entity.getPK();
            if(entityID != null) {
                entityIdList.add(entityID);
            }
        }
        return entityIdList;
    }
    
    /** 将字符串的XML数据转换为Document对象 */
    public static Document dataXml2Doc(String dataXml) {
        StringBuffer sb = new StringBuffer();
        if (!dataXml.startsWith("<?xml")) {
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }
        sb.append(dataXml);
        
        Document doc;
        try {
            doc = DocumentHelper.parseText(sb.toString());
        } catch (DocumentException e) {
            throw new RuntimeException("由dataXml生成doc出错：", e);
        }
        return doc;
    }

    /**
     * 从element中根据节点路径选择相应子节点列表。
     * @param element
     * @param xPath
     * @return
     */
    @SuppressWarnings("unchecked")
    public static List<Element> selectNodes(Element element, String xPath) {
        return element.selectNodes(xPath);
    }
    
    /**
     * 获取Dom节点的文本内容。
     * @param node
     * @return
     */
    public static String getNodeText(Node node) {
        return node == null ? null : node.getText().trim();
    }
	
    public static List<Long> ids2List(String ids) {
    	Object[] arr = ids.split(",");
    	return idArray2List(arr);
    }
	public static List<Long> idArray2List(Object...ids) {
		List<Long> list = new ArrayList<Long>();
		for(Object id : ids) {
			list.add( EasyUtils.obj2Long(id) );
		}
		return list;
	}
	
	public static Collection<Long> fixCollection(Collection<Long> c) {
		return c == null ? new ArrayList<Long>() : c;
	}
	
}
