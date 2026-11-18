package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.service.CategoryBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户端：分类与品牌
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryBrandService categoryBrandService;

    @GetMapping("/list")
    public Result<?> list() {
        return Result.success(categoryBrandService.listCategory());
    }

    @GetMapping("/brand")
    public Result<?> brand(@RequestParam(required = false) Long categoryId) {
        return Result.success(categoryBrandService.listBrand(categoryId));
    }
}
