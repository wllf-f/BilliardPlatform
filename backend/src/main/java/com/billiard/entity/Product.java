package com.billiard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private Long brandId;
    private String name;
    private String model;
    private String imgDir;
    private String mainImg;
    private Integer picCount;
    private String cueType;
    private String hardness;
    private BigDecimal tipDiameter;
    private BigDecimal weight;
    private String material;
    private String spec;
    private String applicableScene;
    private String params;
    private String description;
    private Integer stock;
    private Integer sales;
    private Integer status;
    private Integer sort;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 球杆推荐引擎标签字段（杆型复用 cue_type 字段，价位复用 product_price 表，无需冗余字段）
    private String fitPlay;      // 适配打法:准度防守/追分走位/均衡打法
    private String fitPower;     // 适配发力:轻推发力/均匀发力/大力发力
}
