package com.billiard.dto;

import lombok.Data;

/**
 * 球杆推荐请求DTO（问卷参数）
 */
@Data
public class CueRecommendRequest {
    private String playType;   // 打法偏好: 准度防守/追分走位/均衡打法/斯诺克玩法
    private String level;      // 球龄水平: 新手/中级/高手
    private String powerType;  // 发力方式: 轻推发力/均匀发力/大力发力
    private String likeType;   // 偏好倾向: 准度优先/走位优先/攻守均衡
    private Double minPrice;   // 预算区间下限（含），与 product_price.platform_price 比对
    private Double maxPrice;   // 预算区间上限（含）
}
