package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台：销售数据分析
 */
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    @Autowired
    private StatsService statsService;

    @GetMapping("/overview")
    public Result<?> overview() {
        return Result.success(statsService.overview());
    }

    @GetMapping("/salesRank")
    public Result<?> salesRank(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int size) {
        return Result.success(statsService.salesRankPage(page, size));
    }

    @GetMapping("/monthlyTrend")
    public Result<?> monthlyTrend() {
        return Result.success(statsService.monthlyTrend());
    }

    @GetMapping("/yearlySummary")
    public Result<?> yearlySummary() {
        return Result.success(statsService.yearlySummary());
    }

    @GetMapping("/countByType")
    public Result<?> countByType() {
        return Result.success(statsService.countByType());
    }

    @GetMapping("/countByStatus")
    public Result<?> countByStatus() {
        return Result.success(statsService.countByStatus());
    }
}
