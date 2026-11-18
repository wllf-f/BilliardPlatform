package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台：系统配置管理
 */
@RestController
@RequestMapping("/admin/config")
public class AdminConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/list")
    public Result<?> list() {
        return Result.success(configService.getAll());
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody Map<String, String> body) {
        configService.update(body.get("key"), body.get("value"));
        return Result.success();
    }
}
