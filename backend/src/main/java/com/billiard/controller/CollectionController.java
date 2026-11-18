package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.service.CollectionService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户端：收藏
 */
@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    @Autowired
    private CollectionService collectionService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(collectionService.myCollections(page, size, UserContext.getCurrentUser()));
    }

    @PostMapping("/toggle/{productId}")
    public Result<?> toggle(@PathVariable Long productId) {
        boolean collected = collectionService.toggle(UserContext.getCurrentUser(), productId);
        Map<String, Object> m = new HashMap<>();
        m.put("collected", collected);
        return Result.success(m);
    }

    @GetMapping("/check/{productId}")
    public Result<?> check(@PathVariable Long productId) {
        boolean collected = collectionService.isCollected(UserContext.getCurrentUser(), productId);
        Map<String, Object> m = new HashMap<>();
        m.put("collected", collected);
        return Result.success(m);
    }
}
