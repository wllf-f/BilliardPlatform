package com.billiard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.billiard.entity.Orders;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface OrdersMapper extends BaseMapper<Orders> {

    /** 总销售额（排除未支付和已取消/退款订单） */
    Double totalSales();

    /** 有效订单数（排除未支付和已取消/退款订单） */
    Long validOrderCount();

    /** 本月销售额 */
    Double thisMonthSales();

    /** 月度销售趋势（近12个月，按月汇总销售额与订单量） */
    List<Map<String, Object>> monthlyTrend();

    /** 年度销售汇总（按年） */
    List<Map<String, Object>> yearlySummary();

    /** 各订单状态分布 */
    List<Map<String, Object>> countByStatus();

    /** 商品销量排行榜总数（仅有效订单，按商品去重） */
    Long salesRankCount();

    /** 商品销量排行榜分页（仅有效订单明细聚合，按销售额从大到小排序） */
    List<Map<String, Object>> salesRankPage(@Param("offset") int offset, @Param("size") int size, @Param("baseUrl") String baseUrl);

    /** 各订单类型统计（仅有效订单） */
    List<Map<String, Object>> countByType();
}
