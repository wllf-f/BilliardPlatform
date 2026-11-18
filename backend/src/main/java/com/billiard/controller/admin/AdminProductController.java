package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.entity.Brand;
import com.billiard.entity.Category;
import com.billiard.entity.Product;
import com.billiard.service.AdminProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 管理后台：商品管理
 */
@RestController
@RequestMapping("/admin/product")
public class AdminProductController {

    @Autowired
    private AdminProductService adminProductService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String categoryId,
                          @RequestParam(required = false) String status) {
        // 用 String 接收前端下拉框的空字符串，手动转为数字，避免 Spring 转换空串报 NumberFormatException
        Long categoryIdLong = toLongOrNull(categoryId);
        Integer statusInt = toIntOrNull(status);
        return Result.success(adminProductService.page(page, size, keyword, categoryIdLong, statusInt));
    }

    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id) {
        return Result.success(adminProductService.getById(id));
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Map<String, Object> body) {
        Product p = new Product();
        fillProduct(p, body);
        BigDecimal official = toBigDecimalOrNull(body.get("officialPrice"));
        BigDecimal pdd = toBigDecimalOrNull(body.get("pddPrice"));
        BigDecimal platform = toBigDecimalOrNull(body.get("platformPrice"));
        adminProductService.add(p, official, pdd, platform);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody Map<String, Object> body) {
        Product p = new Product();
        Long id = toLongOrNull(body.get("id") == null ? null : body.get("id").toString());
        if (id == null) {
            return Result.error("商品ID不能为空");
        }
        p.setId(id);
        fillProduct(p, body);
        BigDecimal official = toBigDecimalOrNull(body.get("officialPrice"));
        BigDecimal pdd = toBigDecimalOrNull(body.get("pddPrice"));
        BigDecimal platform = toBigDecimalOrNull(body.get("platformPrice"));
        adminProductService.update(p, official, pdd, platform);
        return Result.success();
    }

    @PostMapping("/status/{id}/{status}")
    public Result<?> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        adminProductService.updateStatus(id, status);
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        adminProductService.delete(id);
        return Result.success();
    }

    @PostMapping("/batchCategory")
    public Result<?> batchCategory(@RequestBody Map<String, Object> body) {
        List<Long> ids = parseIds(body.get("ids"));
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要操作的商品");
        }
        Long categoryId = toLongOrNull(body.get("categoryId") == null ? null : body.get("categoryId").toString());
        if (categoryId == null) {
            return Result.error("请选择分类");
        }
        adminProductService.batchUpdateCategory(ids, categoryId);
        return Result.success();
    }

    @PostMapping("/batchPrice")
    public Result<?> batchPrice(@RequestBody Map<String, Object> body) {
        List<Long> ids = parseIds(body.get("ids"));
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要操作的商品");
        }
        Double rate = null;
        Object rateVal = body.get("rate");
        if (rateVal != null) {
            try { rate = Double.valueOf(rateVal.toString().trim()); } catch (NumberFormatException e) { rate = null; }
        }
        if (rate == null) {
            return Result.error("请输入有效的价格倍率");
        }
        adminProductService.batchAdjustPrice(ids, rate);
        return Result.success();
    }

    @GetMapping("/category")
    public Result<?> category() {
        return Result.success(adminProductService.listCategory());
    }

    @GetMapping("/brand")
    public Result<?> brand(@RequestParam(required = false) Long categoryId) {
        return Result.success(adminProductService.listBrand(categoryId));
    }

    @PostMapping("/category/save")
    public Result<?> saveCategory(@RequestBody Category c) {
        adminProductService.saveCategory(c);
        return Result.success();
    }

    @PostMapping("/category/delete/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        adminProductService.deleteCategory(id);
        return Result.success();
    }

    @PostMapping("/brand/save")
    public Result<?> saveBrand(@RequestBody Brand b) {
        adminProductService.saveBrand(b);
        return Result.success();
    }

    @PostMapping("/brand/delete/{id}")
    public Result<?> deleteBrand(@PathVariable Long id) {
        adminProductService.deleteBrand(id);
        return Result.success();
    }

    private void fillProduct(Product p, Map<String, Object> body) {
        // 数字类型字段全部用安全转换，避免空字符串/非数字导致 NumberFormatException
        p.setCategoryId(toLongOrNull(body.get("categoryId") == null ? null : body.get("categoryId").toString()));
        p.setBrandId(toLongOrNull(body.get("brandId") == null ? null : body.get("brandId").toString()));
        if (body.get("name") != null) p.setName(body.get("name").toString());
        if (body.get("model") != null) p.setModel(body.get("model").toString());
        if (body.get("imgDir") != null) p.setImgDir(body.get("imgDir").toString());
        if (body.get("mainImg") != null) p.setMainImg(body.get("mainImg").toString());
        p.setPicCount(toIntOrNull(body.get("picCount") == null ? null : body.get("picCount").toString()));
        if (body.get("cueType") != null && !body.get("cueType").toString().isEmpty()) p.setCueType(body.get("cueType").toString());
        if (body.get("hardness") != null && !body.get("hardness").toString().isEmpty()) p.setHardness(body.get("hardness").toString());
        p.setTipDiameter(toBigDecimalOrNull(body.get("tipDiameter")));
        p.setWeight(toBigDecimalOrNull(body.get("weight")));
        if (body.get("material") != null) p.setMaterial(body.get("material").toString());
        if (body.get("spec") != null) p.setSpec(body.get("spec").toString());
        if (body.get("applicableScene") != null) p.setApplicableScene(body.get("applicableScene").toString());
        if (body.get("params") != null) p.setParams(body.get("params").toString());
        if (body.get("description") != null) p.setDescription(body.get("description").toString());
        p.setStock(toIntOrNull(body.get("stock") == null ? null : body.get("stock").toString()));
        p.setStatus(toIntOrNull(body.get("status") == null ? null : body.get("status").toString()));
        p.setSort(toIntOrNull(body.get("sort") == null ? null : body.get("sort").toString()));
        // 球杆推荐引擎标签字段（杆型复用 cueType、价位复用 product_price 表）
        if (body.get("fitPlay") != null) p.setFitPlay(body.get("fitPlay").toString());
        if (body.get("fitPower") != null) p.setFitPower(body.get("fitPower").toString());
    }

    /** 安全地把前端传入的值转 BigDecimal；空字符串/null/非数字一律返回 null */
    private BigDecimal toBigDecimalOrNull(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 安全把字符串转 Long；空/null/非数字返回 null */
    private Long toLongOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Long.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** 安全把字符串转 Integer；空/null/非数字返回 null */
    private Integer toIntOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    /**
     * 安全地把前端传来的 ids（JSON 数组会被 Jackson 反序列化为 List<Integer>）转为 List<Long>，
     * 避免直接强转 List<Long> 后迭代时才抛 ClassCastException。
     */
    private List<Long> parseIds(Object v) {
        if (!(v instanceof List)) return null;
        List<Long> result = new java.util.ArrayList<>();
        for (Object o : (List<?>) v) {
            if (o == null) continue;
            try {
                result.add(Long.valueOf(o.toString().trim()));
            } catch (NumberFormatException e) {
                // 忽略非数字元素
            }
        }
        return result.isEmpty() ? null : result;
    }
}
