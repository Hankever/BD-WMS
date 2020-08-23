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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.boudata.wms.entity.Inventory;

/**
 * 专门定制一个用以存放Inv的Map。
 * 要求能存放被多次修改的同一inv的不同阶段的值，第一个为刚从DB取出时的库存值，最后一个为最后一次被修改后的库存值。
 * 用于记录库存的修改日志，如果一条库存在一个操作被多次修改到，则相应产生多条库存交易日志
 * （如果只记一条日志，无法应对不同单据对同一库存做修改的情况）。
 * 
 * 同时内部还维护inv和单据号的对应关系，以处理批量操作时涉及多个单据的情况下库存交易日志记录时能获取到。
 * 
 * 注：存入的未每次存入本Map的都是最近一次修改后的inv的clone对象。
 */
public class InvMap implements Map<Long, Inventory> {
    
    Map<Long, List<Inventory>> invsMap = new LinkedHashMap<Long, List<Inventory>>();
    
    Map<Long, Inventory> invPoMap = new HashMap<Long, Inventory>();
    
    boolean needPersist = true;
 
    public void setNeedPersist(boolean needPersist) {
        this.needPersist = needPersist;
    }
 
    /**
     * 返回同一条库存被多次修改情况下，每次修改后的记录列表
     * @param key
     * @return
     */
    public List<Inventory> getList(Long invID) {
        return invsMap.get(invID);
    }
    
    public Inventory put(Inventory inv, String opNo) {
    	// 将单据号放到inv里暂存。注：这里无法用 inv - opNo这样的Map的记录，无法处理一inv对应多单据的情况，将会相互覆盖掉
        inv.opNo = opNo; 
        return put(inv);
    }
    
    public Inventory put(Inventory inv) {
        return inv == null ? null : this.put(inv.getId(), inv);
    }
    
    public Inventory put(Long invID, Inventory inv) {
    	
        // 存放着被多次修改的同一inv的不同阶段的值
        List<Inventory> list = (List<Inventory>) invsMap.get(invID); 
        if(list == null) {
            invsMap.put(invID, list = new ArrayList<Inventory>());
        }
        
        // 存入的未每次存入本Map的都是最近一次修改后的inv的clone对象（clone出来的不会影响到 Session 里的PO）
        list.add(inv.clone()); 
        
        if( needPersist ) {
            // 将和session关联的PO inv存入 invPoMap，同inv多次修改的话会被覆盖，存的将是最后修改的inv PO
            invPoMap.put(invID, inv);
        }
        
        return inv;
    }

    public void clear() {
        invsMap.clear();
    }

    public boolean containsKey(Object invID) {
        return invsMap.containsKey(invID);
    }

    public boolean containsValue(Object value) {
        return invsMap.containsValue(value);
    }

    public Set<java.util.Map.Entry<Long, Inventory>> entrySet() {
        return invPoMap.entrySet();
    }

    /**  取最后一次修改后的inv对象 */
    public Inventory get(Object invID) {
        return invPoMap.get(invID);
    }
    
    /** 取历史inv列表里的最原始值 */
    public Inventory getOriginalinv(Object invID) {
      List<Inventory> valueList = invsMap.get(invID);
      Inventory firstinv = valueList != null && !valueList.isEmpty() ? valueList.get(0) : null;
      return firstinv;
    }
    
    /** 取历史inv列表里的最终值 ( = invPoMap.get(invID) )*/
    public Inventory getTerminalinv(Object invID) {
      List<Inventory> valueList = invsMap.get(invID);
      Inventory lastinv = valueList != null && !valueList.isEmpty() ? valueList.get(valueList.size() - 1) : null;
      return lastinv;
    }

    public boolean isEmpty() {
        return invsMap.isEmpty();
    }

    public Set<Long> keySet() {
        return invsMap.keySet();
    }

    public void putAll(Map<? extends Long, ? extends Inventory> m) {
        for(Long invID : m.keySet()) {
            this.put(invID, m.get(invID));
        }
    }

    public Inventory remove(Object invID) {
    	invPoMap.remove(invID);
        List<Inventory> valueList = invsMap.remove(invID);
        return valueList != null && !valueList.isEmpty() ? valueList.get(valueList.size() - 1) : null;
    }

    public int size() {
        return invsMap.size();
    }

    /* 返回各个inv最后一次修改的列表，用于批量的update库存 */
    public Collection<Inventory> values() {
        return invPoMap.values();
    }
    
    /**
     * 返回所有inv每次修改过的历史记录。本方法暂时无用，可用于批量创建单据日志（目前单据日志每次不会创建很多，暂时没批量处理）。
     */
    public Collection<Inventory> historyOfinvs() {
      Collection<Inventory> c = new ArrayList<Inventory>();
      for(Long invID : this.keySet()) {
          c.add(this.get(invID));
      }
      return c;
  }
}
