package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.dto.CueRecommendRequest;
import com.billiard.dto.CueRecommendResult;
import com.billiard.entity.Product;
import com.billiard.entity.ProductPrice;
import com.billiard.mapper.ProductMapper;
import com.billiard.mapper.ProductPriceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 个性化器材适配推荐服务
 * 支持两种推荐模式：
 *   1. 原有评分算法（GET /api/recommend/list）— 兼容保留
 *   2. 离线规则引擎（POST /api/recommend/cue）— 球杆专用推荐
 */
@Service
public class RecommendService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPriceMapper productPriceMapper;
    @Autowired
    private CueRecommendRuleEngine cueRecommendRuleEngine;

    @Value("${image.base-url}")
    private String baseUrl;

    /**
     * 球杆离线规则引擎推荐（新接口）
     * 纯本地代码运行，不依赖AI
     */
    public List<CueRecommendResult> recommendByRuleEngine(CueRecommendRequest req) {
        return cueRecommendRuleEngine.recommend(req);
    }

    /**
     * 推荐器材（旧接口，兼容保留）
     * @param type 类型：cue球杆 / tip皮头
     */
    public List<Map<String, Object>> recommend(String type, String cueType, String hardness,
                                                Double minTip, Double maxTip,
                                                Double minWeight, Double maxWeight,
                                                Double minPrice, Double maxPrice) {
        QueryWrapper<Product> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        if ("cue".equals(type)) {
            if (cueType != null && !cueType.isEmpty()) {
                qw.eq("cue_type", cueType);
            }
        } else if ("tip".equals(type)) {
            // 皮头归类在耗材分类下，通过 hardness 字段筛选
            qw.isNotNull("hardness");
            qw.isNull("cue_type");
        }
        List<Product> products = productMapper.selectList(qw);

        List<Map<String, Object>> scored = new ArrayList<>();
        for (Product p : products) {
            int score = calcScore(type, p, cueType, hardness, minTip, maxTip, minWeight, maxWeight);
            if (score <= 0) continue;
            ProductPrice pp = productPriceMapper.selectOne(new QueryWrapper<ProductPrice>().eq("product_id", p.getId()));
            if (pp == null) continue;
            BigDecimal price = pp.getPlatformPrice();
            if (minPrice != null && price.doubleValue() < minPrice) continue;
            if (maxPrice != null && price.doubleValue() > maxPrice) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("model", p.getModel());
            // 返回相对路径，由前端 encodeImageUrl 自动补全 baseUrl 和中文编码
            m.put("mainImg", "billiard_img/" + p.getImgDir() + p.getMainImg());
            m.put("cueType", p.getCueType());
            m.put("hardness", p.getHardness());
            m.put("tipDiameter", p.getTipDiameter());
            m.put("weight", p.getWeight());
            m.put("platformPrice", price);
            m.put("matchScore", score);
            // 皮头推荐：附加匹配等级和理由，提升展示效果
            if ("tip".equals(type)) {
                m.put("matchLabel", score >= 90 ? "完全匹配" : (score >= 70 ? "推荐选择" : "参考选品"));
                m.put("reason", buildTipReason(p, hardness, score));
            }
            scored.add(m);
        }
        scored.sort((a, b) -> ((Integer) b.get("matchScore")).compareTo((Integer) a.get("matchScore")));
        if (scored.size() > 20) {
            return scored.subList(0, 20);
        }
        return scored;
    }

    /** 皮头推荐理由构建 */
    private String buildTipReason(Product p, String targetHardness, int score) {
        StringBuilder sb = new StringBuilder();
        if (score >= 90) {
            sb.append("【完全匹配】");
        } else if (score >= 70) {
            sb.append("【推荐选择】");
        } else {
            sb.append("【参考选品】");
        }
        sb.append("硬度").append(p.getHardness() != null ? p.getHardness() : "适中");
        if (p.getTipDiameter() != null) {
            sb.append("，直径").append(p.getTipDiameter()).append("mm");
        }
        if (p.getMaterial() != null) {
            sb.append("，").append(p.getMaterial());
        }
        sb.append("，综合匹配度").append(score).append("%");
        return sb.toString();
    }

    /** 计算匹配度评分（0-100） */
    private int calcScore(String type, Product p, String cueType, String hardness,
                          Double minTip, Double maxTip, Double minWeight, Double maxWeight) {
        int score = 0;
        int totalWeight = 0;

        if ("cue".equals(type)) {
            // 球杆类型匹配（权重30）
            if (cueType != null && !cueType.isEmpty()) {
                totalWeight += 30;
                if (cueType.equals(p.getCueType())) {
                    score += 30;
                }
            }

            // 硬度匹配（权重30）
            if (hardness != null && !hardness.isEmpty()) {
                totalWeight += 30;
                if (hardness.equals(p.getHardness())) {
                    score += 30;
                } else {
                    // 部分匹配：偏硬/偏软与均衡
                    if ("均衡".equals(hardness) || "均衡".equals(p.getHardness())) {
                        score += 10;
                    }
                }
            }

            // 重量区间匹配（权重20）
            if (minWeight != null && maxWeight != null && minWeight > 0 && maxWeight > 0) {
                totalWeight += 20;
                if (p.getWeight() != null) {
                    double w = p.getWeight().doubleValue();
                    if (w >= minWeight && w <= maxWeight) {
                        score += 20;
                    } else {
                        // 接近区间给予部分分数
                        double diff = Math.min(Math.abs(w - minWeight), Math.abs(w - maxWeight));
                        if (diff <= 0.5) score += 10;
                        else if (diff <= 1.0) score += 5;
                    }
                }
            }

            // 先角直径匹配（权重20）
            if (minTip != null && maxTip != null && minTip > 0 && maxTip > 0) {
                totalWeight += 20;
                if (p.getTipDiameter() != null) {
                    double d = p.getTipDiameter().doubleValue();
                    if (d >= minTip && d <= maxTip) {
                        score += 20;
                    } else {
                        double diff = Math.min(Math.abs(d - minTip), Math.abs(d - maxTip));
                        if (diff <= 0.5) score += 10;
                    }
                }
            }
        } else if ("tip".equals(type)) {
            // 皮头推荐：硬过滤 + 直径打分
            // 1. 硬度硬过滤：不完全匹配直接返回 0 分（不显示）
            if (hardness != null && !hardness.isEmpty()) {
                String normalizedHardness = normalizeTipHardness(p.getHardness());
                if (!hardness.equals(normalizedHardness)) {
                    return 0;  // 硬度不匹配，直接过滤
                }
                score += 50;  // 硬度完全匹配得 50 分
                totalWeight += 50;
            }

            // 2. 直径区间匹配（权重50）— 通过前端传入的区间判断杆型适配
            if (minTip != null && maxTip != null && minTip > 0 && maxTip > 0) {
                totalWeight += 50;
                if (p.getTipDiameter() != null) {
                    double d = p.getTipDiameter().doubleValue();
                    if (d >= minTip && d <= maxTip) {
                        score += 50;  // 在区间内
                    } else {
                        // 接近区间给予部分分数
                        double diff = Math.min(Math.abs(d - minTip), Math.abs(d - maxTip));
                        if (diff <= 0.5) score += 25;
                        else if (diff <= 1.0) score += 10;
                    }
                }
            }
        }

        // 计算百分比
        if (totalWeight == 0) return 0;
        return (int) Math.round((score * 100.0) / totalWeight);
    }

    /**
     * 将皮头商品的原始硬度值归一化到用户选项（偏软/均衡/偏硬）
     * 皮头实际硬度值：超软/软/中软/中/中硬/硬/超硬 等
     */
    private String normalizeTipHardness(String raw) {
        if (raw == null || raw.isEmpty()) return "均衡";
        // 归一化映射表
        switch (raw) {
            case "超软":
            case "软":
            case "中软":
            case "偏软":
                return "偏软";
            case "中":
            case "均衡":
            case "中性":
                return "均衡";
            case "中硬":
            case "硬":
            case "超硬":
            case "偏硬":
                return "偏硬";
            default:
                return "均衡";
        }
    }

    /** 判断是否为相邻硬度（偏软↔均衡、均衡↔偏硬 视为相邻） */
    private boolean isAdjacentHardness(String h1, String h2) {
        if (h1 == null || h2 == null) return false;
        String[] order = {"偏软", "均衡", "偏硬"};
        int i1 = -1, i2 = -1;
        for (int i = 0; i < order.length; i++) {
            if (order[i].equals(h1)) i1 = i;
            if (order[i].equals(h2)) i2 = i;
        }
        return i1 >= 0 && i2 >= 0 && Math.abs(i1 - i2) == 1;
    }
}
