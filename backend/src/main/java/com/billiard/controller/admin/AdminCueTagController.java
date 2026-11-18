package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.dto.TagGenerateResult;
import com.billiard.service.CueTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台：球杆批量标签生成工具（数据初始化专用）
 * ⚠️ 此模块为独立后台接口，与前端问卷推荐功能完全解耦
 * 答辩完成后可直接注释/删除此类，推荐引擎不受任何影响
 */
@RestController
@RequestMapping("/admin/cue-tag")
public class AdminCueTagController {

    @Autowired
    private CueTagService cueTagService;

    /**
     * 触发全量批量打标
     * 读取数据库内所有球杆商品，根据型号/品牌/价格自动打上6个枚举标签
     * 处理过程：分批加载（每批100条）→ 自动判断 → 增量更新
     */
    @PostMapping("/generate")
    public Result<?> generate() {
        TagGenerateResult result = cueTagService.generateTags();
        return Result.success(result);
    }

    /**
     * 查询打标结果统计（不触发新的打标，仅返回上次执行的统计信息）
     * 注：如需重新打标，请调用 POST /admin/cue-tag/generate
     */
    @GetMapping("/status")
    public Result<?> status() {
        TagGenerateResult result = cueTagService.getLastResult();
        return Result.success(result);
    }
}
