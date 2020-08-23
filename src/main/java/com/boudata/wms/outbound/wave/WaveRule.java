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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.boubei.tss.util.EasyUtils;
import com.boudata.wms._Util;
import com.boudata.wms.entity.OperationItem;
import com.boudata.wms.entity.OrderItem;


public class WaveRule {
	
	static final Log logger = LogFactory.getLog(WaveRule.class);
    
    public static final String ALL_OIS = "ALL.OIs";
    public static final String ALL_PKDS = "ALL.PKDs";

    /**
     * 每个子波次的OH个数限制（用于集货墙，格子个数有限），超出的循环处理，即每 maxOHNum 建一个子波次
     */
    int maxOHsPerSW = 0;
    
    /**
     * 规则文件里定义的属性值 id / value
     */
    Map<String, String> properties = new HashMap<String, String>();
    
    /**
     * 按顺序存放规则文件里定义的各个step配置
     */
    Map<String, Step> stepsMap = new LinkedHashMap<String, Step>();
    
    /**
     * 按顺序存放规则文件里定义的各个subwave配置
     */
    Map<String, Subwave> subWavesMap = new LinkedHashMap<String, Subwave>();
    
    /**
     * step 对应 OIs / remainOIs / remainOHs
     */
    Map<String, Set<Long>> step_ois = new HashMap<String, Set<Long>>();
    
    /**
     * subwave 对应 PKDs
     */
    Map<String, Collection<Long>> subwave_pkds = new HashMap<String, Collection<Long>>();
    
    Map<Long, OperationItem> persitedPKDs = new HashMap<Long, OperationItem>();
    
    /**
     * 把当前Step执行后未完成分配的订单明细记录下来，以便后续step读取。
     * @param step
     * @param notEnoughOis
     */
    void recordStepResult(Step step, List<OrderItem> notEnoughOis) {
        Set<Long> notEnoughOhIDs = new LinkedHashSet<Long>();
        for(OrderItem oi : notEnoughOis) {
            Long ohID = oi.getOrder().getId();
            notEnoughOhIDs.add(ohID);
        }
        
        String stepName = step.getName().trim();
        step_ois.put(stepName + ".remainOIs", _Util.getIDs(notEnoughOis)); // 未完成的订单明细
        step_ois.put(stepName + ".remainOHs", notEnoughOhIDs); // 未完成的订单
    }
    
    /**
     * 根据Key读取PKDs列表，可能是step生成的PKDs，也可能是Subwave选中的PKDs。
     * @param key
     * @return
     */
    @SuppressWarnings("unchecked")
	Collection<Long> getPkdIDsByKey(String key) {
        key = key.trim();
        if( stepsMap.containsKey(key) ) { // 如果是step开头的，说明是其他step的PKD列表
            Step step = stepsMap.get(key);
            return _Util.getIDs( step.pkds ); // pkd.id为null的，是被剔除掉得PKD，没有被持久化。getIDs()方法里会过滤掉这部分PKD
        }
        else { // 如果是 .PKDs结尾的，说明是其他子波次的PKDId列表
            Collection<Long> sw_pkds = subwave_pkds.get(key);
            return (Collection<Long>) EasyUtils.checkNull(sw_pkds, new ArrayList<Long>());
        }
    }
 
    /**
     * 波次分配具体执行步骤
     */
    static class Step {
        String index;
        String rule;  // 拣货分配规则
        String sql;
        String partAllocate; // 是否允许部分分配，Y/N， 默认为Y
        List<String> insertTemps = new ArrayList<String>();
        List<String> includes = new ArrayList<String>();
        List<String> excludes = new ArrayList<String>();
        
        Set<Long> oiIDs = new LinkedHashSet<Long>(); // 满足本step条件的所有OiIDs
        List<OperationItem> pkds = new ArrayList<OperationItem>();
        
        public boolean canPartAllocate() {
            return !("N".equals(partAllocate)); // 空的时候为Y
        }
        
        public String getName() {
            return "step" + index;
        }
    }
    
    /**
     * include，exclude的是即可以是step的PKDs，也可以是subwave的PKDIds
     */
    static class Subwave {
        String name;
        String sql;
        List<String> includes = new ArrayList<String>(); 
        List<String> excludes = new ArrayList<String>();  
        
        /**
         * 子波次的OH个数限制（用于集货墙，格子个数有限），超出的循环处理，即每 maxOHNum 建一个子波次. 
         */
        int maxOHNum = 0;
        
        public int getMaxOHNum() {
            return  (int) EasyUtils.checkTrue(maxOHNum <= 0, 99999999, maxOHNum) ;
        }
    }
}
