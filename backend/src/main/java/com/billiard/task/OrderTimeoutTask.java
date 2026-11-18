package com.billiard.task;

import com.billiard.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单超时处理定时任务
 * 每分钟扫描一次，自动取消超时未付款订单（15分钟）
 * 每分钟扫描一次，自动确认收货发货后超过2分钟的订单（测试用）
 */
@Component
public class OrderTimeoutTask {

    @Autowired
    private OrderService orderService;

    /**
     * 每分钟执行一次，处理超时订单（未付款15分钟）
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkTimeoutOrders() {
        try {
            int count = orderService.autoCancelTimeoutOrders();
            if (count > 0) {
                System.out.println("===== 订单超时处理完成：自动取消 " + count + " 笔订单 =====");
            }
        } catch (Exception e) {
            System.err.println("订单超时处理异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 每分钟执行一次，处理自动确认收货（发货后超过2分钟，测试用）
     */
    @Scheduled(cron = "30 * * * * ?")
    public void checkAutoCompleteOrders() {
        try {
            int count = orderService.autoCompleteTimeoutOrders();
            if (count > 0) {
                System.out.println("===== 自动确认收货处理完成：" + count + " 笔订单 =====");
            }
        } catch (Exception e) {
            System.err.println("自动确认收货处理异常：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
