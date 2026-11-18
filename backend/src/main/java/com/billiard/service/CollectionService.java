package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.billiard.entity.Collection;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.CollectionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 收藏服务
 */
@Service
public class CollectionService {

    @Autowired
    private CollectionMapper collectionMapper;
    @Autowired
    private ProductService productService;

    public IPage<Map<String, Object>> myCollections(int page, int size, Long userId) {
        Page<Map<String, Object>> p = new Page<>(page, size);
        return collectionMapper.selectCollectionPage(p, userId, productService.getBaseUrl());
    }

    /** 收藏/取消收藏（toggle） */
    public boolean toggle(Long userId, Long productId) {
        Collection exist = collectionMapper.selectOne(new QueryWrapper<Collection>()
                .eq("user_id", userId).eq("product_id", productId));
        if (exist != null) {
            collectionMapper.deleteById(exist.getId());
            return false; // 已取消
        }
        Collection c = new Collection();
        c.setUserId(userId);
        c.setProductId(productId);
        collectionMapper.insert(c);
        return true; // 已收藏
    }

    public boolean isCollected(Long userId, Long productId) {
        return collectionMapper.selectCount(new QueryWrapper<Collection>()
                .eq("user_id", userId).eq("product_id", productId)) > 0;
    }
}
