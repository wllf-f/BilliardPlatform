package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.entity.Brand;
import com.billiard.entity.Category;
import com.billiard.mapper.BrandMapper;
import com.billiard.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类与品牌服务
 */
@Service
public class CategoryBrandService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private BrandMapper brandMapper;

    public List<Category> listCategory() {
        List<Category> list = categoryMapper.selectList(new QueryWrapper<Category>()
                .eq("status", 1).orderByAsc("sort").orderByAsc("id"));
        for (Category c : list) {
            if (c.getName() != null) {
                c.setName(removePrefix(c.getName()));
            }
        }
        return list;
    }

    private String removePrefix(String name) {
        if (name.matches("^\\d{2}_.*")) {
            return name.substring(3);
        }
        return name;
    }

    public List<Brand> listBrand(Long categoryId) {
        QueryWrapper<Brand> qw = new QueryWrapper<Brand>().orderByAsc("sort").orderByAsc("id");
        if (categoryId != null) {
            qw.eq("category_id", categoryId);
        }
        return brandMapper.selectList(qw);
    }
}
