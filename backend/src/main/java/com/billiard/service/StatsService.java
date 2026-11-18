package com.billiard.service;

import com.billiard.mapper.OrdersMapper;
import com.billiard.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 销售数据分析服务
 */
@Service
public class StatsService {

    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private UserMapper userMapper;

    @Value("${image.base-url}")
    private String baseUrl;

    /** 概览统计：总销售额、有效订单量、本月销售额、用户总量 */
    public Map<String, Object> overview() {
        Map<String, Object> m = new HashMap<>();
        Double totalSales = ordersMapper.totalSales();
        m.put("totalSales", totalSales != null ? totalSales : 0);
        m.put("orderCount", ordersMapper.validOrderCount());
        Double thisMonth = ordersMapper.thisMonthSales();
        m.put("thisMonthSales", thisMonth != null ? thisMonth : 0);
        m.put("userCount", userMapper.selectCount(null));
        return m;
    }

    /** 商品销量排行榜分页（从订单真实聚合，按销售额从大到小） */
    public Map<String, Object> salesRankPage(int page, int size) {
        int offset = (page - 1) * size;
        Map<String, Object> result = new HashMap<>();
        result.put("total", ordersMapper.salesRankCount());
        result.put("list", ordersMapper.salesRankPage(offset, size, baseUrl));
        return result;
    }

    /** 月度销售趋势（近12个月） */
    public List<Map<String, Object>> monthlyTrend() {
        return ordersMapper.monthlyTrend();
    }

    /** 年度销售汇总 */
    public List<Map<String, Object>> yearlySummary() {
        return ordersMapper.yearlySummary();
    }

    /** 各订单类型统计 */
    public List<Map<String, Object>> countByType() {
        return ordersMapper.countByType();
    }

    /** 各订单状态分布 */
    public List<Map<String, Object>> countByStatus() {
        return ordersMapper.countByStatus();
    }
}
