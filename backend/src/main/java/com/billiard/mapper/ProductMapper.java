package com.billiard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.billiard.entity.Product;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.Map;

public interface ProductMapper extends BaseMapper<Product> {

    /** 原子扣库存（防超卖）：stock>=qty 才扣，返回影响行数，0 表示库存不足 */
    @Update("UPDATE product SET stock = stock - #{qty} WHERE id = #{id} AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") int qty);

    /** 原子恢复库存（取消订单时） */
    @Update("UPDATE product SET stock = stock + #{qty} WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("qty") int qty);

    /** 订单完成时累加销量（真实销量仅统计已完成订单） */
    @Update("UPDATE product SET sales = sales + #{qty} WHERE id = #{id}")
    int addSales(@Param("id") Long id, @Param("qty") int qty);

    /** 已完成订单被改回非完成时扣减销量（销量不低于 0） */
    @Update("UPDATE product SET sales = GREATEST(0, sales - #{qty}) WHERE id = #{id}")
    int reduceSales(@Param("id") Long id, @Param("qty") int qty);

    /**
     * 商品列表分页查询（关联比价表，带分类/品牌/关键词/价格区间/排序）
     */
    IPage<Map<String, Object>> selectProductPage(IPage<Map<String, Object>> page,
                                                  @Param("categoryId") Long categoryId,
                                                  @Param("brandId") Long brandId,
                                                  @Param("keyword") String keyword,
                                                  @Param("cueType") String cueType,
                                                  @Param("hardness") String hardness,
                                                  @Param("minPrice") Double minPrice,
                                                  @Param("maxPrice") Double maxPrice,
                                                  @Param("sort") String sort,
                                                  @Param("baseUrl") String baseUrl);

    /**
     * 商品详情（含比价）
     */
    Map<String, Object> selectDetailById(@Param("id") Long id, @Param("baseUrl") String baseUrl);

    /**
     * 热门器材（按销量排序）
     */
    List<Map<String, Object>> selectHotProducts(@Param("limit") int limit, @Param("baseUrl") String baseUrl);

    /**
     * 比价专区列表
     */
    IPage<Map<String, Object>> selectComparePage(IPage<Map<String, Object>> page,
                                                  @Param("categoryId") Long categoryId,
                                                  @Param("minPrice") Double minPrice,
                                                  @Param("maxPrice") Double maxPrice,
                                                  @Param("sort") String sort,
                                                  @Param("baseUrl") String baseUrl);

    /**
     * 销量排行榜
     */
    List<Map<String, Object>> selectSalesRank(@Param("limit") int limit, @Param("baseUrl") String baseUrl);

    /** 统计某品牌下的商品数量（用于删除品牌前的引用检查） */
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM product WHERE brand_id = #{brandId} AND status != -1")
    int countByBrandId(@Param("brandId") Long brandId);
}
