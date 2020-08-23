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
import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;

import com.boudata.wms._Util;
import com.boudata.wms.outbound.wave.WaveRule.Step;
import com.boudata.wms.outbound.wave.WaveRule.Subwave;

public class WaveRuleFactory {
	
	public static WaveRule init(String ruleContentXml) {
        WaveRule rule = new WaveRule();
        
        Document doc = _Util.dataXml2Doc(ruleContentXml);
        Element ruleRootNode = doc.getRootElement();
        
        // 常量
        List<Element> propertyNodes = _Util.selectNodes(ruleRootNode, "properties/property");
        for(Element propertyNode : propertyNodes) {
            String id = propertyNode.attributeValue("id");
            rule.properties.put("${" + id + "}", propertyNode.getText());
        }
        
        try {
            rule.maxOHsPerSW = Integer.parseInt(ruleRootNode.attributeValue("maxSONumPerSubwave"));
        } catch (Exception e) {
            // do nothing
        }
        
        List<Element> stepNodes = _Util.selectNodes(ruleRootNode, "step");
        for(Element stepNode : stepNodes) {
            Step step = new Step();
            step.index = stepNode.attributeValue("index");
            step.rule = stepNode.attributeValue("rule");
            step.sql = _Util.getNodeText(stepNode.element("sql"));
            step.partAllocate = stepNode.attributeValue("partAllocate");
            step.insertTemps.addAll(getValuesFromNode(stepNode, "insertTemp"));
            step.includes.addAll(getValuesFromNode(stepNode, "include"));
            step.excludes.addAll(getValuesFromNode(stepNode, "exclude"));
            
            rule.stepsMap.put(step.getName(), step);
        }
        
        List<Element> subWaveNodes = _Util.selectNodes(ruleRootNode, "subwave");
        for(Element subWaveNode : subWaveNodes) {
            Subwave subWave = new Subwave();
            
            subWave.name = subWaveNode.attributeValue("name");
            subWave.sql = _Util.getNodeText(subWaveNode.element("sql")); 
            subWave.includes.addAll(getValuesFromNode(subWaveNode, "include"));
            subWave.excludes.addAll(getValuesFromNode(subWaveNode, "exclude"));
            
            try {
                subWave.maxOHNum = Integer.parseInt(subWaveNode.attributeValue("maxSONum"));
            } catch (Exception e) {
                subWave.maxOHNum = rule.maxOHsPerSW; // 默认取rule根节点上的配置
            }

            rule.subWavesMap.put(subWave.name, subWave);
        }
        
        return rule;
    }
    
    private static List<String> getValuesFromNode(Element parentNode, String nodeName) {
        Element excludeNode = parentNode.element(nodeName);
        if(excludeNode != null) {
            return Arrays.asList(excludeNode.getText().split(","));
        }
        
        return new ArrayList<String>();
    }
}
