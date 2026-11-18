package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台：订单管理
 */
@RestController
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String orderType,
                          @RequestParam(required = false) String orderNo) {
        // 用 String 接收前端下拉框的空字符串，手动转为 Integer，避免 Spring 转换空串报 NumberFormatException
        Integer statusInt = toIntOrNull(status);
        Integer orderTypeInt = toIntOrNull(orderType);
        return Result.success(orderService.allOrders(page, size, statusInt, orderTypeInt, orderNo));
    }

    /** 安全把字符串转 Integer；空字符串/null/非数字一律返回 null */
    private Integer toIntOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id, null));
    }

    @PostMapping("/status")
    public Result<?> updateStatus(@RequestBody Map<String, Object> body) {
        Long id = toLongOrNull(body.get("id"));
        Integer status = toIntOrNull(body.get("status") == null ? null : body.get("status").toString());
        if (id == null || status == null) {
            return Result.error("参数错误");
        }
        orderService.updateStatus(id, status);
        return Result.success();
    }

    @PostMapping("/ship")
    public Result<?> ship(@RequestBody Map<String, Object> body) {
        Long id = toLongOrNull(body.get("id"));
        if (id == null) {
            return Result.error("参数错误");
        }
        String expressCompany = body.get("expressCompany") != null ? body.get("expressCompany").toString().trim() : "";
        String expressNo = body.get("expressNo") != null ? body.get("expressNo").toString().trim() : "";
        if (expressCompany.isEmpty() || expressNo.isEmpty()) {
            return Result.error("请填写快递公司和快递单号");
        }
        orderService.ship(id, expressCompany, expressNo);
        return Result.success();
    }

    /** 安全把对象转 Long；空/null/非数字一律返回 null */
    private Long toLongOrNull(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 手动触发超时订单取消（用于测试） */
    @PostMapping("/cancel-timeout")
    public Result<?> cancelTimeout() {
        int count = orderService.autoCancelTimeoutOrders();
        return Result.success("已取消 " + count + " 笔超时订单");
    }
}
