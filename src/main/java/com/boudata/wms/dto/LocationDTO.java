/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.dto;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.boubei.tss.util.EasyUtils;
import com.boudata.wms.entity._Location;

/**
 * 库位树（库区、货架、库位），用于监控库存分布、盘点进度等
 */
public class LocationDTO {

	public Long id;
	public String code;
	public String name;
	public String pcode;

	public Integer loc1 = 0; // 作业量
	public Integer loc2 = 0; // 完成量
	
	public String toString() {
		return pcode + "/" + code;
	}
	
	public int hashCode() {
		return this.code.hashCode();
	}
	
	public boolean equals(Object other) {
		if(other == null) return false;
		
		return this.getClass().equals(other.getClass()) 
				&& this.hashCode() == other.hashCode();
	}

	public LocationDTO(_Location loc) {
		this.id = loc.getId();
		this.code = loc.getCode();
		this.name = loc.getName();
		this.pcode = (String) EasyUtils.checkNull(loc.getRack(), loc.getZone());
		
		this.loc1 = loc.loc1;
		this.loc2 = loc.loc2;
	}

	public LocationDTO(String code) {
		this.code = code;
		this.name = code;
	}

	public Set<LocationDTO> children = new LinkedHashSet<LocationDTO>(); // 子节点

	public static Collection<LocationDTO> buildLocTree(Collection<_Location> locs) {

		LocationDTO root = new LocationDTO("wh");
		Map<String, LocationDTO> map1 = new HashMap<String, LocationDTO>();
		Map<String, LocationDTO> map2 = new HashMap<String, LocationDTO>();

		for (_Location loc : locs) {
			LocationDTO _loc = new LocationDTO(loc);
			
			String zone = loc.getZone();
			if (EasyUtils.isNullOrEmpty(zone)) {
				String rank = loc.getRack();
				if (EasyUtils.isNullOrEmpty(rank)) {
					root.children.add(_loc);
				} else {
					_loc.pcode = rank;
					LocationDTO _rank = map2.get(rank);
					if (_rank == null) {
						map2.put(rank, _rank = new LocationDTO(rank));
					}
					_rank.loc1 = _rank.loc1 + loc.loc1;
					_rank.loc2 = _rank.loc2 + loc.loc2;
					_rank.children.add(_loc);
					root.children.add(_rank);
				}
			} else {
				_loc.pcode = zone;
				LocationDTO _zone = map1.get(zone);
				if (_zone == null) {
					map1.put(zone, _zone = new LocationDTO(zone));
				}
				_zone.loc1 = _zone.loc1 + loc.loc1;
				_zone.loc2 = _zone.loc2 + loc.loc2;
				root.children.add(_zone);

				String rank = loc.getRack();
				if (EasyUtils.isNullOrEmpty(rank)) {
					_zone.children.add(_loc);
				} else {
					_loc.pcode = _loc.pcode + "/" + rank;
					LocationDTO _rank = map2.get(rank);
					if (_rank == null) {
						map2.put(rank, _rank = new LocationDTO(rank));
					}
					_rank.loc1 = _rank.loc1 + loc.loc1;
					_rank.loc2 = _rank.loc2 + loc.loc2;
					_rank.children.add(_loc);
					_zone.children.add(_rank);
				}
			}
		}

		return root.children;
	}
}
