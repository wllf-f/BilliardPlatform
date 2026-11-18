package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.billiard.entity.Brand;
import com.billiard.entity.Category;
import com.billiard.entity.Product;
import com.billiard.entity.ProductPrice;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.BrandMapper;
import com.billiard.mapper.CategoryMapper;
import com.billiard.mapper.ProductMapper;
import com.billiard.mapper.ProductPriceMapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台商品管理服务：CRUD、上下架、批量改分类/价格
 */
@Service
public class AdminProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPriceMapper productPriceMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private BrandMapper brandMapper;
    @Autowired
    private ProductService productService;

    /** 商品分页（附带三类比价） */
    public Page<Map<String, Object>> page(int page, int size, String keyword, Long categoryId, Integer status) {
        QueryWrapper<Product> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            // 用 and 包裹 OR，避免与后续 category/status 条件产生优先级问题
            qw.and(w -> w.like("name", keyword).or().like("model", keyword));
        }
        if (categoryId != null) {
            qw.eq("category_id", categoryId);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("id");
        Page<Product> productPage = productMapper.selectPage(new Page<>(page, size), qw);
        // 转为Map列表并附加价格
        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setTotal(productPage.getTotal());
        java.util.List<Map<String, Object>> records = new java.util.ArrayList<>();
        for (Product p : productPage.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("categoryId", p.getCategoryId());
            m.put("brandId", p.getBrandId());
            m.put("name", p.getName());
            m.put("model", p.getModel());
            m.put("imgDir", p.getImgDir());
            m.put("mainImg", p.getMainImg());
            m.put("picCount", p.getPicCount());
            m.put("cueType", p.getCueType());
            m.put("hardness", p.getHardness());
            m.put("tipDiameter", p.getTipDiameter());
            m.put("weight", p.getWeight());
            m.put("material", p.getMaterial());
            m.put("spec", p.getSpec());
            m.put("applicableScene", p.getApplicableScene());
            m.put("params", p.getParams());
            m.put("description", p.getDescription());
            m.put("stock", p.getStock());
            m.put("sales", p.getSales());
            m.put("status", p.getStatus());
            m.put("sort", p.getSort());
            m.put("createTime", p.getCreateTime());
            // 球杆推荐引擎标签字段（杆型复用 cueType、价位复用 product_price 表）
            m.put("fitPlay", p.getFitPlay());
            m.put("fitPower", p.getFitPower());
            // 附带价格
            ProductPrice pp = productPriceMapper.selectOne(new QueryWrapper<ProductPrice>().eq("product_id", p.getId()));
            if (pp != null) {
                m.put("officialPrice", pp.getOfficialPrice());
                m.put("pddPrice", pp.getPddPrice());
                m.put("platformPrice", pp.getPlatformPrice());
            }
            records.add(m);
        }
        result.setRecords(records);
        return result;
    }

    /** 商品详情（含价格） */
    public Map<String, Object> getById(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) return null;
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("categoryId", p.getCategoryId());
        m.put("brandId", p.getBrandId());
        m.put("name", p.getName());
        m.put("model", p.getModel());
        m.put("imgDir", p.getImgDir());
        m.put("mainImg", p.getMainImg());
        m.put("picCount", p.getPicCount());
        m.put("cueType", p.getCueType());
        m.put("hardness", p.getHardness());
        m.put("tipDiameter", p.getTipDiameter());
        m.put("weight", p.getWeight());
        m.put("material", p.getMaterial());
        m.put("spec", p.getSpec());
        m.put("applicableScene", p.getApplicableScene());
        m.put("params", p.getParams());
        m.put("description", p.getDescription());
        m.put("stock", p.getStock());
        m.put("sales", p.getSales());
        m.put("status", p.getStatus());
        m.put("sort", p.getSort());
        // 球杆推荐引擎标签字段（杆型复用 cueType、价位复用 product_price 表）
        m.put("fitPlay", p.getFitPlay());
        m.put("fitPower", p.getFitPower());
        ProductPrice pp = productPriceMapper.selectOne(new QueryWrapper<ProductPrice>().eq("product_id", id));
        if (pp != null) {
            m.put("officialPrice", pp.getOfficialPrice());
            m.put("pddPrice", pp.getPddPrice());
            m.put("platformPrice", pp.getPlatformPrice());
        }
        return m;
    }

    @Transactional
    public void add(Product product, BigDecimal officialPrice, BigDecimal pddPrice, BigDecimal platformPrice) {
        if (product.getName() == null || product.getImgDir() == null) {
            throw new BusinessException("商品名和图片目录不能为空");
        }
        if (product.getMainImg() == null) {
            product.setMainImg("pic1.jpg");
        }
        if (product.getPicCount() == null) {
            product.setPicCount(1);
        }
        if (product.getStock() == null) {
            product.setStock(0);
        }
        if (product.getStatus() == null) {
            product.setStatus(1);
        }
        if (product.getBrandId() == null) {
            product.setBrandId(0L);
        }
        if (product.getSort() == null) {
            product.setSort(0);
        }
        if (product.getSales() == null) {
            product.setSales(0);
        }
        // 球杆标签（cueType/fitPlay/fitPower）仅对球杆类商品有效，其他分类一律清空
        if (!isCueCategory(product.getCategoryId())) {
            product.setCueType(null);
            product.setFitPlay(null);
            product.setFitPower(null);
        }
        productMapper.insert(product);
        ProductPrice pp = new ProductPrice();
        pp.setProductId(product.getId());
        pp.setOfficialPrice(officialPrice);
        pp.setPddPrice(pddPrice);
        pp.setPlatformPrice(platformPrice);
        productPriceMapper.insert(pp);
        productService.clearHotCache();
    }

    @Transactional
    public void update(Product product, BigDecimal officialPrice, BigDecimal pddPrice, BigDecimal platformPrice) {
        if (product.getId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        if (product.getBrandId() == null) {
            product.setBrandId(0L);
        }
        // 球杆标签（cueType/fitPlay/fitPower）仅对球杆类商品有效，其他分类一律清空（updateById 忽略 null 字段，需显式置空）
        // 仅当显式传了 categoryId 时才处理标签，避免编辑商品未带分类字段时误清空标签
        if (product.getCategoryId() != null && !isCueCategory(product.getCategoryId())) {
            product.setCueType(null);
            product.setFitPlay(null);
            product.setFitPower(null);
            productMapper.update(null, new UpdateWrapper<Product>()
                    .eq("id", product.getId())
                    .set("cue_type", null)
                    .set("fit_play", null)
                    .set("fit_power", null));
        }
        productMapper.updateById(product);
        if (officialPrice != null || pddPrice != null || platformPrice != null) {
            ProductPrice pp = productPriceMapper.selectOne(new QueryWrapper<ProductPrice>().eq("product_id", product.getId()));
            if (pp == null) {
                pp = new ProductPrice();
                pp.setProductId(product.getId());
                pp.setOfficialPrice(officialPrice);
                pp.setPddPrice(pddPrice);
                pp.setPlatformPrice(platformPrice);
                productPriceMapper.insert(pp);
            } else {
                if (officialPrice != null) pp.setOfficialPrice(officialPrice);
                if (pddPrice != null) pp.setPddPrice(pddPrice);
                if (platformPrice != null) pp.setPlatformPrice(platformPrice);
                productPriceMapper.updateById(pp);
            }
        }
        productService.clearHotCache();
    }

    @Transactional
    public void updateStatus(Long id, Integer status) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new BusinessException("商品不存在");
        p.setStatus(status);
        productMapper.updateById(p);
        productService.clearHotCache();
    }

    @Transactional
    public void delete(Long id) {
        productMapper.deleteById(id);
        productPriceMapper.delete(new QueryWrapper<ProductPrice>().eq("product_id", id));
        productService.clearHotCache();
    }

    /** 批量修改分类 */
    @Transactional
    public void batchUpdateCategory(List<Long> ids, Long categoryId) {
        boolean isCue = isCueCategory(categoryId);
        for (Long id : ids) {
            Product p = new Product();
            p.setId(id);
            p.setCategoryId(categoryId);
            productMapper.updateById(p);
            // 改到非球杆分类时，清空球杆标签；改到球杆分类时保持原标签（前端表单可重新设置）
            if (!isCue) {
                productMapper.update(null, new UpdateWrapper<Product>()
                        .eq("id", id)
                        .set("cue_type", null)
                        .set("fit_play", null)
                        .set("fit_power", null));
            }
        }
        productService.clearHotCache();
    }

    /** 判断分类是否为球杆类（小头杆/大头杆） */
    private boolean isCueCategory(Long categoryId) {
        if (categoryId == null) return false;
        Category c = categoryMapper.selectById(categoryId);
        if (c == null || c.getName() == null) return false;
        String name = c.getName();
        return name.contains("小头杆") || name.contains("大头杆");
    }

    /** 批量调整价格（按比例） */
    @Transactional
    public void batchAdjustPrice(List<Long> ids, Double rate) {
        for (Long id : ids) {
            ProductPrice pp = productPriceMapper.selectOne(new QueryWrapper<ProductPrice>().eq("product_id", id));
            if (pp != null) {
                BigDecimal r = new BigDecimal(rate);
                // 价格字段可能为 null（部分价格未录入），需判空避免 NPE 导致整个批量事务回滚
                pp.setOfficialPrice(pp.getOfficialPrice() == null ? null : pp.getOfficialPrice().multiply(r).setScale(2, BigDecimal.ROUND_HALF_UP));
                pp.setPddPrice(pp.getPddPrice() == null ? null : pp.getPddPrice().multiply(r).setScale(2, BigDecimal.ROUND_HALF_UP));
                pp.setPlatformPrice(pp.getPlatformPrice() == null ? null : pp.getPlatformPrice().multiply(r).setScale(2, BigDecimal.ROUND_HALF_UP));
                productPriceMapper.updateById(pp);
            }
        }
    }

    /** 分类管理 */
    public List<Category> listCategory() {
        return categoryMapper.selectList(new QueryWrapper<Category>().orderByAsc("sort").orderByAsc("id"));
    }

    public List<Brand> listBrand(Long categoryId) {
        QueryWrapper<Brand> qw = new QueryWrapper<Brand>().orderByAsc("sort").orderByAsc("id");
        if (categoryId != null) qw.eq("category_id", categoryId);
        return brandMapper.selectList(qw);
    }

    @Transactional
    public void saveCategory(Category c) {
        if (c.getId() == null) {
            categoryMapper.insert(c);
        } else {
            categoryMapper.updateById(c);
        }
    }

    @Transactional
    public void deleteCategory(Long id) {
        categoryMapper.deleteById(id);
    }

    @Transactional
    public void saveBrand(Brand b) {
        if (b.getId() == null) {
            brandMapper.insert(b);
        } else {
            brandMapper.updateById(b);
        }
    }

    @Transactional
    public void deleteBrand(Long id) {
        int count = productMapper.countByBrandId(id);
        if (count > 0) {
            // 有商品引用该品牌，先将这些商品的 brandId 置为 0
            productMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Product>()
                .eq("brand_id", id).set("brand_id", 0));
        }
        brandMapper.deleteById(id);
    }
}
