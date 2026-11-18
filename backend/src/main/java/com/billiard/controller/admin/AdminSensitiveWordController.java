package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.service.SensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台：违规词管理
 */
@RestController
@RequestMapping("/admin/sensitiveWord")
public class AdminSensitiveWordController {

    @Autowired
    private SensitiveWordService sensitiveWordService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String category) {
        return Result.success(sensitiveWordService.list(category));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Map<String, String> body) {
        sensitiveWordService.add(body.get("word"), body.get("category"));
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        return Result.success();
    }
}
