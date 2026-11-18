package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.entity.Orders;
import com.billiard.service.OrderService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端：皮头改装更换
 */
@RestController
@RequestMapping("/api/tipModify")
public class TipModifyController {

    @Autowired
    private OrderService orderService;

    /** 自备皮头更换（基础服务费） */
    @PostMapping("/own")
    public Result<Orders> own(@RequestBody Map<String, Object> body) {
        Long addressId = parseId(body.get("addressId"), "请选择收货地址");
        if (addressId == null) {
            return Result.error("收货地址参数错误");
        }
        String remark = body.get("remark") != null ? body.get("remark").toString() : null;
        return Result.success(orderService.createTipModifyOwnOrder(UserContext.getCurrentUser(), addressId, remark));
    }

    /** 购买平台皮头更换（皮头售价+手工费） */
    @PostMapping("/buy")
    public Result<Orders> buy(@RequestBody Map<String, Object> body) {
        Long productId = parseId(body.get("productId"), "请选择皮头商品");
        if (productId == null) {
            return Result.error("皮头商品参数错误");
        }
        Long addressId = parseId(body.get("addressId"), "请选择收货地址");
        if (addressId == null) {
            return Result.error("收货地址参数错误");
        }
        String remark = body.get("remark") != null ? body.get("remark").toString() : null;
        return Result.success(orderService.createTipModifyBuyOrder(UserContext.getCurrentUser(), productId, addressId, remark));
    }

    /** 安全解析 ID，空值/非数字返回 null */
    private static Long parseId(Object v, String emptyMsg) {
        if (v == null || v.toString().trim().isEmpty()) {
            throw new com.billiard.exception.BusinessException(emptyMsg);
        }
        try {
            return Long.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
