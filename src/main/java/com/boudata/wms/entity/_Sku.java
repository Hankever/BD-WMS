/* ==================================================================   
 * Created [2019-07-12] by BD 
 * ==================================================================  
 * BD-WMS
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boudata.com, 2019-2029  
 * ================================================================== 
 */package com.boudata.wms.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.boubei.tss.dm.record.ARecordTable;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.MathUtil;

/**
 * 货品SKU实体定义
 * 
 * ALTER TABLE `wms_sku` ADD UNIQUE INDEX `barcode` (`domain` ASC, `barcode` ASC, `owner_id` ASC);
 * ALTER TABLE `wms_sku` ADD UNIQUE INDEX `barcode2` (`domain` ASC, `barcode2` ASC, `owner_id` ASC);
 */
@Entity
@Table(name = "wms_sku", uniqueConstraints = { 
        @UniqueConstraint(name = "MULTI_SKU_CODE_DOMAIN", columnNames = { "domain", "code" })
})
@SequenceGenerator(name = "sku_seq", sequenceName = "sku_seq")
public class _Sku extends ARecordTable {
	
	public _Sku() { }
	public _Sku(Long id) { this.id = id; }

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sku_seq")
	private Long id;

	/** 货品编号 */
	@Column(length = 128, nullable = false)
	private String code;

	@Column(length = 128, nullable = false)
	private String name;
	
	/** 货品条形码 */
	private String barcode;  // 新包装、内包装
	private String barcode2; // 老包装、外包装
	
	/** 货主 */
	@ManyToOne
	private _Owner owner;
	
	/** 仓库：货品只对指定仓库可见 */
	@ManyToOne
	private _Warehouse warehouse;
	
	/** 分类 */
	private String category; // 赠品：gift 
	
	/** 品牌/供应商 */
	private String brand;

	/** 货品描述 */
	private String remark;
	
	/** 进价 */
	private Double price0;
	/** 批价 */
	private Double price;
	/** 售价 */
	private Double price2;
	
	/** 货品状态：1|0|3, 上架|下架|停产 */
	private Integer status;

	/** 长、宽、高 */
	private Double len;
	private Double width;
	private Double height;
	
	/** 体积 */
	private Double cube;
	
	/** 重量 */
	private Double weight;
	
	private String  guige;     // 规格1：600ml、手机颜色（红、黑、白）
	private String  guige2;    // 规格2：手机内存（16G、32G、64G...）
	private String  uom;       // 包装：箱、瓶
	private Integer shelflife; // 保质期
	
	/** 安全库存量 */
	private Double safety_qty;
	
	/** ABC分类 */
	private String abc;
	
	/** 自定义属性：品牌、供应商、颜色、尺码等 */
	private String udf1;
	private String udf2;
	private String udf3;
	private String udf4;
	
	private Long product_id;    // 对应产品 
	private String lot_options; // 批次选项，eg: {"lotatt03": "白色 黑色 红色"， "lotatt04": "S M L XL XXL"} 、{"lotatt03": "大 中 小"}
	
	
	@Transient public boolean isNew = false;
	@Transient public List<_SkuX> skuxList = new ArrayList<_SkuX>();
	@Transient public Integer package_qty = 0;
	@Transient public Double inv_qty  = 0D; // 可售量（期货）
	@Transient public Double real_qty = 0D; // 库存量（现有）
	
	
	public Double _price(Object _price) {
		if( _price != null ) {
			return EasyUtils.obj2Double(_price);
		}
		return (Double) EasyUtils.checkNull(this.price2, this.price, this.price0);
	}

	public Serializable getPK() {
		return this.id;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getStatus() {
		return status;
	}
	public void setStatus(Integer status) {
		this.status = status;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public String getBarcode() {
		return barcode;
	}
	public void setBarcode(String barcode) {
		this.barcode = EasyUtils.isNullOrEmpty(barcode) ? null : barcode;
	}
	public Double getSafety_qty() {
		return safety_qty;
	}
	public void setSafety_qty(Double safety_qty) {
		this.safety_qty = safety_qty;
	}
	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	public Double getWeight() {
		return EasyUtils.obj2Double(weight);
	}
	public void setWeight(Double weight) {
		this.weight = weight;
	}
	public Double getCube() {
		if( EasyUtils.obj2Double(cube) <= 0) {
			// 此处不能对cube进行赋值
			return MathUtil.multiply( MathUtil.multiply(len, width), height) / 100 / 100 / 100;
		}
		return EasyUtils.obj2Double(cube);
	}
	public void setCube(Double cube) {
		this.cube = cube;
	}
	public Double getPrice0() {
		return price0;
	}
	public void setPrice0(Double price0) {
		this.price0 = price0;
	}
	public Double getPrice() {
		return price;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
	public Double getPrice2() {
		return price2;
	}
	public void setPrice2(Double price2) {
		this.price2 = price2;
	}
	public Double getWidth() {
		return width;
	}
	public void setWidth(Double width) {
		this.width = width;
	}
	public Double getHeight() {
		return height;
	}
	public void setHeight(Double height) {
		this.height = height;
	}
	public String getGuige() {
		return guige;
	}
	public void setGuige(String guige) {
		this.guige = guige;
	}
	public String getUom() {
		return uom;
	}
	public void setUom(String uom) {
		this.uom = uom;
	}
	public Integer getShelflife() {
		return shelflife;
	}
	public void setShelflife(Integer shelflife) {
		this.shelflife = shelflife;
	}
	public String getUdf1() {
		return udf1;
	}
	public void setUdf1(String udf1) {
		this.udf1 = udf1;
	}
	public String getUdf2() {
		return udf2;
	}
	public void setUdf2(String udf2) {
		this.udf2 = udf2;
	}
	public String getUdf3() {
		return udf3;
	}
	public void setUdf3(String udf3) {
		this.udf3 = udf3;
	}
	public String getUdf4() {
		return udf4;
	}
	public void setUdf4(String udf4) {
		this.udf4 = udf4;
	}
	public Double getLen() {
		return len;
	}
	public void setLen(Double len) {
		this.len = len;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getAbc() {
		return abc;
	}
	public void setAbc(String abc) {
		this.abc = abc;
	}
	public _Owner getOwner() {
		return owner;
	}
	public void setOwner(_Owner owner) {
		this.owner = owner;
	}
	public String getLot_options() {
		return lot_options;
	}
	public void setLot_options(String lot_options) {
		this.lot_options = lot_options;
	}
	public String getGuige2() {
		return guige2;
	}
	public void setGuige2(String guige2) {
		this.guige2 = guige2;
	}
	public Long getProduct_id() {
		return product_id;
	}
	public void setProduct_id(Long product_id) {
		this.product_id = product_id;
	}
	public _Warehouse getWarehouse() {
		return warehouse;
	}
	public void setWarehouse(_Warehouse warehouse) {
		this.warehouse = warehouse;
	}
	public String getBarcode2() {
		return barcode2;
	}
	public void setBarcode2(String barcode2) {
		this.barcode2 = barcode2;
	}
}
