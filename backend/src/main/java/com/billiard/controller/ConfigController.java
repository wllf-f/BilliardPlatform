package com.billiard.controller;

import com.billiard.common.Constants;
import com.billiard.common.Result;
import com.billiard.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户端：公开配置查询（客服、关于我们、皮头改装地址等）
 */
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/get/{key}")
    public Result<?> get(@PathVariable String key) {
        Map<String, Object> m = new HashMap<>();
        m.put("key", key);
        m.put("value", configService.get(key));
        return Result.success(m);
    }

    /** 批量获取常用配置 */
    @GetMapping("/common")
    public Result<?> common() {
        Map<String, Object> m = new HashMap<>();
        m.put("customerService", configService.get(Constants.CFG_CUSTOMER_SERVICE));
        m.put("aboutUs", configService.get(Constants.CFG_ABOUT_US));
        m.put("tipModifyAddress", configService.get(Constants.CFG_TIP_ADDRESS));
        m.put("customCuePrice", configService.get(Constants.CFG_CUSTOM_PRICE));
        m.put("tipModifyFee", configService.get(Constants.CFG_TIP_FEE));
        m.put("tipModifyHandfee", configService.get(Constants.CFG_TIP_HANDFEE));
        return Result.success(m);
    }
}
