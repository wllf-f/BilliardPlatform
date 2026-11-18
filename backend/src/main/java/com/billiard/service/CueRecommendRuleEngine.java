package com.billiard.service;

import com.billiard.common.CueRecommendConstants;
import com.billiard.dto.CueRecommendRequest;
import com.billiard.dto.CueRecommendResult;
import com.billiard.entity.Product;
import com.billiard.entity.ProductPrice;
import com.billiard.mapper.ProductMapper;
import com.billiard.mapper.ProductPriceMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 台球杆离线规则推荐引擎（打分制）
 * 纯本地代码运行，不依赖任何AI接口或外部服务
 *
 * 核心算法：
 *   1. 预算硬过滤：先从 product_price 表查出预算区间内的球杆ID
 *      - 如果预算区间内没有任何球杆 → 直接返回空列表（前端显示"暂未找到匹配"）
 *   2. 打分制排序：对候选球杆按4个维度打分
 *      - 杆型匹配(40分) + 硬度匹配(30分) + 打法标签(15分) + 发力标签(15分)
 *      - 硬度相邻（偏软↔均衡、均衡↔偏硬）给15分，完全匹配30分
 *   3. 取前3条：按总分降序取前3，不再有兜底随机
 *
 * 说明：杆型复用 cue_type 字段，价位复用 product_price 表，无冗余字段
 */
@Slf4j
@Component
public class CueRecommendRuleEngine {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPriceMapper productPriceMapper;

    @Value("${image.base-url}")
    private String baseUrl;

    /**
     * 核心推荐方法（打分制）
     * @param req 问卷参数（含预算区间 minPrice/maxPrice）
     * @return 推荐结果列表（最多3条）；预算区间内无球杆时返回空列表
     */
    public List<CueRecommendResult> recommend(CueRecommendRequest req) {
        log.info("===== 开始推荐(打分制): playType={}, level={}, powerType={}, likeType={}, minPrice={}, maxPrice={} =====",
                req.getPlayType(), req.getLevel(), req.getPowerType(), req.getLikeType(), req.getMinPrice(), req.getMaxPrice());

        // ===== 第1步：预算硬过滤 =====
        // 从 product_price 表查出预算区间内的所有商品ID
        QueryWrapper<ProductPrice> pqw = new QueryWrapper<>();
        if (req.getMinPrice() != null) pqw.ge("platform_price", req.getMinPrice());
        if (req.getMaxPrice() != null) pqw.le("platform_price", req.getMaxPrice());
        List<ProductPrice> prices = productPriceMapper.selectList(pqw);

        if (prices.isEmpty()) {
            log.info("预算区间内无任何商品，返回空列表（前端显示暂未找到匹配）");
            return new ArrayList<>();
        }
        List<Long> budgetIds = prices.stream().map(ProductPrice::getProductId).distinct().collect(Collectors.toList());
        log.info("预算区间内候选商品数: {}", budgetIds.size());

        // ===== 第2步：查出这些商品的完整信息（只查球杆类）=====
        QueryWrapper<Product> qw = new QueryWrapper<>();
        qw.eq("status", 1);
        qw.isNotNull("cue_type");
        qw.in("id", budgetIds);
        List<Product> candidates = productMapper.selectList(qw);
        log.info("候选球杆数(球杆类): {}", candidates.size());

        if (candidates.isEmpty()) {
            log.info("预算区间内无球杆类商品，返回空列表");
            return new ArrayList<>();
        }

        // ===== 第3步：计算每个商品的匹配分 =====
        List<String> targetCueTypes = mapCueTypes(req.getPlayType());
        String targetHardness = mapHardness(req.getPowerType(), req.getLikeType(), req.getLevel());
        String targetFitPlay = mapFitPlay(req.getPlayType());
        String targetFitPower = req.getPowerType();
        log.info("目标标签: cueTypes={}, hardness={}, fitPlay={}, fitPower={}",
                targetCueTypes, targetHardness, targetFitPlay, targetFitPower);

        // 价格Map（productId → platformPrice），用于展示
        Map<Long, ProductPrice> priceMap = prices.stream()
                .collect(Collectors.toMap(ProductPrice::getProductId, p -> p, (a, b) -> a));

        // 打分并排序
        List<ScoredProduct> scoredList = new ArrayList<>();
        for (Product p : candidates) {
            int score = calcMatchScore(p, targetCueTypes, targetHardness, targetFitPlay, targetFitPower);
            scoredList.add(new ScoredProduct(p, score));
        }
        scoredList.sort((a, b) -> b.score - a.score);  // 降序

        log.info("打分结果前5名:");
        for (int i = 0; i < Math.min(5, scoredList.size()); i++) {
            ScoredProduct sp = scoredList.get(i);
            log.info("  #{}: id={}, name={}, score={}", i+1, sp.product.getId(), sp.product.getName(), sp.score);
        }

        // ===== 第4步：取前3条，构建结果 =====
        List<CueRecommendResult> results = new ArrayList<>();
        for (int i = 0; i < Math.min(3, scoredList.size()); i++) {
            ScoredProduct sp = scoredList.get(i);
            Product p = sp.product;
            ProductPrice pp = priceMap.get(p.getId());
            int matchLevel = mapScoreToLevel(sp.score);
            results.add(buildResult(p, pp, targetCueTypes, targetHardness, matchLevel, sp.score, req));
        }

        log.info("===== 最终推荐: {} 条 =====", results.size());
        return results;
    }

    // ========== 打分核心 ==========
    // 总分100：杆型(40) + 硬度(30) + 打法标签(15) + 发力标签(15)
    private int calcMatchScore(Product p, List<String> targetCueTypes, String targetHardness,
                                String targetFitPlay, String targetFitPower) {
        int score = 0;
        // 1. 杆型匹配 40分
        if (p.getCueType() != null && targetCueTypes.contains(p.getCueType())) {
            score += 40;
        }
        // 2. 硬度匹配 30分（完全匹配30，相邻15，不匹配0）
        if (p.getHardness() != null && targetHardness != null) {
            if (p.getHardness().equals(targetHardness)) {
                score += 30;
            } else if (isAdjacentHardness(p.getHardness(), targetHardness)) {
                score += 15;
            }
        }
        // 3. 打法标签 15分
        if (targetFitPlay != null && targetFitPlay.equals(p.getFitPlay())) {
            score += 15;
        }
        // 4. 发力标签 15分
        if (targetFitPower != null && targetFitPower.equals(p.getFitPower())) {
            score += 15;
        }
        return score;
    }

    // 硬度相邻判定：偏软↔均衡、均衡↔偏硬 相邻；偏软↔偏硬 不相邻
    private boolean isAdjacentHardness(String a, String b) {
        if (a == null || b == null || a.equals(b)) return false;
        if (("偏软".equals(a) && "均衡".equals(b)) || ("均衡".equals(a) && "偏软".equals(b))) return true;
        if (("偏硬".equals(a) && "均衡".equals(b)) || ("均衡".equals(a) && "偏硬".equals(b))) return true;
        return false;
    }

    // 分数→匹配等级
    // 85-100: 完全匹配(1), 60-84: 推荐选择(2), 40-59: 预算优选(3), <40: 参考选品(4)
    private int mapScoreToLevel(int score) {
        if (score >= 85) return CueRecommendConstants.MATCH_FULL;
        if (score >= 60) return CueRecommendConstants.MATCH_RELAXED;
        if (score >= 40) return CueRecommendConstants.MATCH_BUDGET_ONLY;
        return CueRecommendConstants.MATCH_RANDOM;
    }

    // 玩法→杆型映射
    private List<String> mapCueTypes(String playType) {
        List<String> cueTypes = new ArrayList<>();
        if (playType == null) {
            cueTypes.add(CueRecommendConstants.ROD_SMALL);
            return cueTypes;
        }
        switch (playType) {
            case CueRecommendConstants.PLAY_OFFENSE:
                cueTypes.add(CueRecommendConstants.ROD_LARGE);
                break;
            case CueRecommendConstants.PLAY_DEFENSE:
            case CueRecommendConstants.PLAY_SNOOKER:
            case CueRecommendConstants.PLAY_BALANCE:
            default:
                cueTypes.add(CueRecommendConstants.ROD_SMALL);
                break;
        }
        return cueTypes;
    }

    // 玩法→fit_play 标签映射
    private String mapFitPlay(String playType) {
        if (playType == null) return CueRecommendConstants.PLAY_BALANCE;
        switch (playType) {
            case CueRecommendConstants.PLAY_DEFENSE:
            case CueRecommendConstants.PLAY_SNOOKER:
                return CueRecommendConstants.PLAY_DEFENSE;
            case CueRecommendConstants.PLAY_OFFENSE:
                return CueRecommendConstants.PLAY_OFFENSE;
            default:
                return CueRecommendConstants.PLAY_BALANCE;
        }
    }

    // 发力+偏好+球龄→硬度映射（保留原有逻辑，likeType 高区分度）
    private String mapHardness(String powerType, String likeType, String level) {
        // 准度优先 → 倾向偏硬
        if (CueRecommendConstants.LIKE_ACCURACY.equals(likeType)) {
            if (CueRecommendConstants.POWER_STRONG.equals(powerType)) return CueRecommendConstants.HARD_HARD;
            if (CueRecommendConstants.POWER_LIGHT.equals(powerType)
                    && CueRecommendConstants.LEVEL_BEGINNER.equals(level)) return CueRecommendConstants.HARD_MEDIUM;
            if (CueRecommendConstants.POWER_LIGHT.equals(powerType)) return CueRecommendConstants.HARD_SOFT;
            return CueRecommendConstants.HARD_HARD;
        }
        // 走位优先 → 倾向偏软/均衡
        if (CueRecommendConstants.LIKE_POSITION.equals(likeType)) {
            if (CueRecommendConstants.POWER_STRONG.equals(powerType)) return CueRecommendConstants.HARD_MEDIUM;
            if (CueRecommendConstants.POWER_LIGHT.equals(powerType)) return CueRecommendConstants.HARD_SOFT;
            if (CueRecommendConstants.LEVEL_ADVANCED.equals(level)) return CueRecommendConstants.HARD_MEDIUM;
            return CueRecommendConstants.HARD_SOFT;
        }
        // 攻守均衡 → 倾向均衡
        if (CueRecommendConstants.LIKE_BALANCE.equals(likeType)) {
            if (CueRecommendConstants.POWER_STRONG.equals(powerType)
                    && CueRecommendConstants.LEVEL_ADVANCED.equals(level)) return CueRecommendConstants.HARD_HARD;
            if (CueRecommendConstants.POWER_LIGHT.equals(powerType)
                    && CueRecommendConstants.LEVEL_BEGINNER.equals(level)) return CueRecommendConstants.HARD_SOFT;
            return CueRecommendConstants.HARD_MEDIUM;
        }
        // 默认
        if (CueRecommendConstants.POWER_STRONG.equals(powerType)) return CueRecommendConstants.HARD_HARD;
        if (CueRecommendConstants.POWER_LIGHT.equals(powerType)) return CueRecommendConstants.HARD_SOFT;
        return CueRecommendConstants.HARD_MEDIUM;
    }

    // ========== 构建单条推荐结果 ==========
    private CueRecommendResult buildResult(Product p, ProductPrice pp, List<String> targetCueTypes,
                                            String targetHardness, int matchLevel, int score, CueRecommendRequest req) {
        CueRecommendResult r = new CueRecommendResult();
        r.setProductId(p.getId());
        r.setProductName(p.getName());
        r.setModel(p.getModel());
        r.setRodType(p.getCueType() != null ? p.getCueType()
                : (targetCueTypes.isEmpty() ? null : targetCueTypes.get(0)));
        r.setHardness(p.getHardness());
        r.setMaterial(p.getMaterial());
        r.setMatchLevel(matchLevel);
        r.setMatchLabel(CueRecommendConstants.getMatchLabel(matchLevel));
        r.setMatchScore(score);  // 新增：匹配分数，前端可展示
        if (pp != null && pp.getPlatformPrice() != null) {
            r.setPrice(pp.getPlatformPrice().doubleValue());
            r.setMainImg(baseUrl + p.getImgDir() + p.getMainImg());
        }
        r.setReason(buildReason(p, pp, targetCueTypes, targetHardness, matchLevel, score, req));
        return r;
    }

    // ========== 推荐理由模板拼接 ==========
    private String buildReason(Product p, ProductPrice pp, List<String> targetCueTypes,
                                String targetHardness, int matchLevel, int score, CueRecommendRequest req) {
        String cueType = p.getCueType() != null ? p.getCueType()
                : (targetCueTypes.isEmpty() ? "球杆" : targetCueTypes.get(0));
        String hardness = p.getHardness() != null ? p.getHardness()
                : (targetHardness != null ? targetHardness : "适中");
        String material = p.getMaterial() != null ? p.getMaterial() : CueRecommendConstants.MAT_WOOD;

        StringBuilder reason = new StringBuilder();
        // 匹配等级标签
        if (matchLevel == CueRecommendConstants.MATCH_FULL) {
            reason.append("【完全匹配】");
        } else if (matchLevel == CueRecommendConstants.MATCH_RELAXED) {
            reason.append("【推荐选择】");
        } else if (matchLevel == CueRecommendConstants.MATCH_BUDGET_ONLY) {
            reason.append("【预算优选】");
        } else {
            reason.append("【参考选品】");
        }
        reason.append("根据您");
        reason.append(req.getPlayType() != null ? req.getPlayType() : CueRecommendConstants.PLAY_BALANCE);
        reason.append("的打法偏好");
        if (req.getLikeType() != null && !req.getLikeType().isEmpty()) {
            reason.append("（").append(req.getLikeType()).append("）");
        }
        reason.append("，");

        if (matchLevel == CueRecommendConstants.MATCH_FULL) {
            reason.append("为您完全匹配推荐");
        } else if (matchLevel == CueRecommendConstants.MATCH_RELAXED) {
            reason.append("为您推荐");
        } else {
            reason.append("为您精选");
        }

        reason.append(cueType).append("（").append(hardness).append("），");
        reason.append("该球杆采用").append(material).append("制作，");

        if (p.getFitPower() != null) {
            reason.append("适合").append(p.getFitPower()).append("的发力方式，");
        }
        if (p.getFitPlay() != null) {
            reason.append("适配").append(p.getFitPlay()).append("，");
        }
        // 匹配分展示
        reason.append("综合匹配度").append(score).append("分，");
        if (pp != null && pp.getPlatformPrice() != null) {
            reason.append("平台价").append(String.format("%.0f", pp.getPlatformPrice().doubleValue())).append("元，符合您的预算区间。");
        } else {
            reason.append("供您参考选择。");
        }
        return reason.toString();
    }

    // ========== 打分内部类 ==========
    private static class ScoredProduct {
        Product product;
        int score;
        ScoredProduct(Product product, int score) {
            this.product = product;
            this.score = score;
        }
    }
}
