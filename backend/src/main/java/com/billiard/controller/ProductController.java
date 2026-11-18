package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端：商品列表/详情/热门/比价/销量榜
 */
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /** 商品列表 */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) Long categoryId,
                          @RequestParam(required = false) Long brandId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String cueType,
                          @RequestParam(required = false) String hardness,
                          @RequestParam(required = false) Double minPrice,
                          @RequestParam(required = false) Double maxPrice,
                          @RequestParam(required = false) String sort) {
        return Result.success(productService.productPage(page, size, categoryId, brandId,
                keyword, cueType, hardness, minPrice, maxPrice, sort));
    }

    /** 商品详情 */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    /** 首页热门器材 */
    @GetMapping("/hot")
    public Result<?> hot(@RequestParam(defaultValue = "8") int limit) {
        return Result.success(productService.hotProducts(limit));
    }

    /** 比价专区 */
    @GetMapping("/compare")
    public Result<?> compare(@RequestParam(defaultValue = "1") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) Long categoryId,
                             @RequestParam(required = false) Double minPrice,
                             @RequestParam(required = false) Double maxPrice,
                             @RequestParam(required = false) String sort) {
        return Result.success(productService.comparePage(page, size, categoryId, minPrice, maxPrice, sort));
    }

    /** 销量排行榜 */
    @GetMapping("/salesRank")
    public Result<?> salesRank(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(productService.salesRank(limit));
    }
}
