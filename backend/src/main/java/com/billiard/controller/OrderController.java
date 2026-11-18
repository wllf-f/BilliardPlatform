package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.entity.Orders;
import com.billiard.service.OrderService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端：普通商品订单
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /** 下单（立即购买） */
    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, Object> body) {
        if (body.get("productId") == null) {
            return Result.error("商品ID不能为空");
        }
        if (body.get("addressId") == null) {
            return Result.error("收货地址不能为空");
        }
        Long productId;
        Long addressId;
        try {
            productId = Long.valueOf(body.get("productId").toString());
            addressId = Long.valueOf(body.get("addressId").toString());
        } catch (NumberFormatException e) {
            return Result.error("参数格式错误");
        }
        Integer quantity = body.get("quantity") != null ? Integer.valueOf(body.get("quantity").toString()) : 1;
        if (quantity == null || quantity < 1) quantity = 1;
        String remark = body.get("remark") != null ? body.get("remark").toString() : null;
        return Result.success(orderService.createNormalOrder(UserContext.getCurrentUser(),
                productId, quantity, addressId, remark));
    }

    /** 我的订单 */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(required = false) Integer orderType) {
        return Result.success(orderService.myOrders(page, size, UserContext.getCurrentUser(), status, orderType));
    }

    /** 模拟支付 */
    @PostMapping("/pay/{id}")
    public Result<?> pay(@PathVariable Long id) {
        orderService.pay(id, UserContext.getCurrentUser());
        return Result.success();
    }

    /** 取消订单 */
    @PostMapping("/cancel/{id}")
    public Result<?> cancel(@PathVariable Long id) {
        orderService.cancel(id, UserContext.getCurrentUser());
        return Result.success();
    }

    /** 确认收货 */
    @PostMapping("/confirm/{id}")
    public Result<?> confirm(@PathVariable Long id) {
        orderService.confirm(id, UserContext.getCurrentUser());
        return Result.success();
    }

    /** 订单详情 */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(orderService.detail(id, UserContext.getCurrentUser()));
    }

    /** 删除订单（仅已取消订单可删除） */
    @PostMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        orderService.delete(id, UserContext.getCurrentUser());
        return Result.success();
    }

    /** 退款（待发货非定制款可直接退款） */
    @PostMapping("/refund/{id}")
    public Result<?> refund(@PathVariable Long id) {
        orderService.refund(id, UserContext.getCurrentUser());
        return Result.success();
    }
}
