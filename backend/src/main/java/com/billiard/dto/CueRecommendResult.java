package com.billiard.dto;

import lombok.Data;

/**
 * 球杆推荐结果DTO
 */
@Data
public class CueRecommendResult {
    private Long productId;         // 商品ID
    private String productName;     // 商品名称
    private String model;           // 型号
    private String mainImg;         // 主图URL
    private String rodType;         // 杆型（值取自 cue_type：小头杆/大头杆）
    private String hardness;        // 硬度
    private String material;        // 材质
    private Double price;           // 价格
    private int matchLevel;         // 匹配等级: 1=完全匹配, 2=放宽硬度, 3=仅价格区间, 4=不限价格兜底
    private String matchLabel;      // 匹配等级中文标签
    private Integer matchScore;    // 匹配分数(0-100)，前端可展示
    private String reason;          // 推荐理由（模板拼接）
}
