package com.billiard.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.billiard.common.Constants;
import com.billiard.entity.Product;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.ProductMapper;
import com.billiard.mapper.ProductPriceMapper;
import com.billiard.entity.ProductPrice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 商品服务：列表/详情/热门/比价/销量榜
 * 图片URL拼接：baseUrl + imgDir + picN.jpg
 */
@Service
public class ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPriceMapper productPriceMapper;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private ObjectMapper objectMapper;

    @Value("${image.base-url}")
    private String baseUrl;

    /** 商品列表分页 */
    public IPage<Map<String, Object>> productPage(int page, int size, Long categoryId, Long brandId,
                                                    String keyword, String cueType, String hardness,
                                                    Double minPrice, Double maxPrice, String sort) {
        Page<Map<String, Object>> p = new Page<>(page, size);
        return productMapper.selectProductPage(p, categoryId, brandId, keyword, cueType,
                hardness, minPrice, maxPrice, sort, baseUrl);
    }

    /** 商品详情（含详情轮播图数组） */
    public Map<String, Object> detail(Long id) {
        Map<String, Object> detail = productMapper.selectDetailById(id, baseUrl);
        if (detail == null) {
            throw new BusinessException("商品不存在");
        }
        // 构造详情图数组 pic1..picN
        int picCount = detail.get("picCount") != null ? ((Number) detail.get("picCount")).intValue() : 1;
        String imgDir = (String) detail.get("imgDir");
        List<String> detailImgs = new ArrayList<>();
        for (int i = 1; i <= picCount; i++) {
            detailImgs.add(baseUrl + imgDir + "pic" + i + ".jpg");
        }
        detail.put("detailImgs", detailImgs);
        // params JSON 字符串解析为 Map
        Object params = detail.get("params");
        if (params != null && params instanceof String && !((String) params).isEmpty()) {
            try {
                detail.put("params", objectMapper.readValue((String) params, Map.class));
            } catch (Exception e) {
                // 解析失败保留原始字符串
            }
        }
        return detail;
    }

    /** 首页热门器材（Redis缓存1小时） */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> hotProducts(int limit) {
        String key = Constants.REDIS_HOT_PRODUCTS + ":" + limit;
        String cache = redis.opsForValue().get(key);
        if (cache != null) {
            try {
                return objectMapper.readValue(cache, List.class);
            } catch (Exception ignored) {
            }
        }
        List<Map<String, Object>> list = productMapper.selectHotProducts(limit, baseUrl);
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(list), 1, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
        return list;
    }

    /** 比价专区列表（Redis缓存30分钟） */
    @SuppressWarnings("unchecked")
    public IPage<Map<String, Object>> comparePage(int page, int size, Long categoryId,
                                                   Double minPrice, Double maxPrice, String sort) {
        Page<Map<String, Object>> p = new Page<>(page, size);
        return productMapper.selectComparePage(p, categoryId, minPrice, maxPrice, sort, baseUrl);
    }

    /** 销量排行榜 */
    public List<Map<String, Object>> salesRank(int limit) {
        return productMapper.selectSalesRank(limit, baseUrl);
    }

    public Product getEntity(Long id) {
        return productMapper.selectById(id);
    }

    public ProductPrice getPrice(Long productId) {
        return productPriceMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ProductPrice>()
                .eq("product_id", productId));
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /** 清除热门商品缓存（用 SCAN 替代 keys，避免生产环境数据量大时阻塞 Redis） */
    public void clearHotCache() {
        redis.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(Constants.REDIS_HOT_PRODUCTS + ":*")
                            .count(100).build());
            while (cursor.hasNext()) {
                connection.del(cursor.next());
            }
            cursor.close();
            return null;
        });
    }
}
