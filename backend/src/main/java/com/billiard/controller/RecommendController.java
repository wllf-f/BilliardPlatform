package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.dto.CueRecommendRequest;
import com.billiard.service.RecommendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端：个性化器材适配推荐
 * 接口：
 *   GET  /api/recommend/list  — 旧版评分推荐（兼容保留）
 *   POST /api/recommend/cue   — 离线规则引擎推荐（球杆专用）
 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    /**
     * 球杆离线规则引擎推荐（新接口）
     * @param req 问卷参数：playType/level/powerType/likeType/minPrice/maxPrice（预算区间，比对 product_price.platform_price）
     * @return 推荐结果：2主推+1备选=3条
     */
    @PostMapping("/cue")
    public Result<?> recommendCue(@RequestBody CueRecommendRequest req) {
        return Result.success(recommendService.recommendByRuleEngine(req));
    }

    /**
     * 旧版推荐接口（兼容保留）
     * @deprecated 建议使用 POST /api/recommend/cue
     */
    @Deprecated
    @GetMapping("/list")
    public Result<?> list(@RequestParam String type,
                          @RequestParam(required = false) String cueType,
                          @RequestParam(required = false) String hardness,
                          @RequestParam(required = false) Double minTip,
                          @RequestParam(required = false) Double maxTip,
                          @RequestParam(required = false) Double minWeight,
                          @RequestParam(required = false) Double maxWeight,
                          @RequestParam(required = false) Double minPrice,
                          @RequestParam(required = false) Double maxPrice) {
        return Result.success(recommendService.recommend(type, cueType, hardness, minTip, maxTip,
                minWeight, maxWeight, minPrice, maxPrice));
    }
}
