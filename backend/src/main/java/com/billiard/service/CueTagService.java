package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.common.CueRecommendConstants;
import com.billiard.dto.TagGenerateResult;
import com.billiard.entity.Product;
import com.billiard.entity.ProductPrice;
import com.billiard.mapper.ProductMapper;
import com.billiard.mapper.ProductPriceMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量球杆标签生成服务（数据初始化专用，与推荐引擎完全解耦）
 * 用途：读取数据库内所有球杆商品，根据型号/品牌/价格自动打上6个枚举标签
 * 注：答辩完成后可注释/删除此类，推荐引擎不受影响
 */
@Service
public class CueTagService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPriceMapper productPriceMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int BATCH_SIZE = 100;

    /** 缓存最近一次打标结果，供 /status 接口查询 */
    private volatile TagGenerateResult lastResult;

    /**
     * 全量批量打标
     * 按每页100条分批处理，避免全表加载
     */
    @Transactional
    public TagGenerateResult generateTags() {
        TagGenerateResult result = new TagGenerateResult();
        int total = 0;
        int updated = 0;
        int skipped = 0;

        // 先统计总数
        Long totalCount = productMapper.selectCount(
                new QueryWrapper<Product>().isNotNull("cue_type"));
        int totalPages = (int) Math.ceil((double) totalCount / BATCH_SIZE);

        // 分批处理
        for (int page = 0; page < totalPages; page++) {
            QueryWrapper<Product> qw = new QueryWrapper<>();
            qw.isNotNull("cue_type");
            qw.last("LIMIT " + BATCH_SIZE + " OFFSET " + (page * BATCH_SIZE));
            List<Product> batch = productMapper.selectList(qw);

            for (Product p : batch) {
                total++;
                Map<String, Object> tags = determineTags(p);

                // 判断是否需要更新
                boolean needUpdate = false;
                Product update = new Product();
                update.setId(p.getId());

                String fitPlay = (String) tags.get("fitPlay");
                String fitPower = (String) tags.get("fitPower");
                String hardness = (String) tags.get("hardness");
                String material = (String) tags.get("material");

                if (fitPlay != null && !fitPlay.equals(p.getFitPlay())) {
                    update.setFitPlay(fitPlay);
                    needUpdate = true;
                }
                if (fitPower != null && !fitPower.equals(p.getFitPower())) {
                    update.setFitPower(fitPower);
                    needUpdate = true;
                }
                // hardness 和 material 只在原有值为空时才覆盖（已有的人工设置不强制覆盖）
                if (hardness != null && !hardness.isEmpty() && (p.getHardness() == null || p.getHardness().isEmpty())) {
                    update.setHardness(hardness);
                    needUpdate = true;
                }
                if (material != null && !material.isEmpty() && (p.getMaterial() == null || p.getMaterial().isEmpty())) {
                    update.setMaterial(material);
                    needUpdate = true;
                }

                if (needUpdate) {
                    productMapper.updateById(update);
                    updated++;
                } else {
                    skipped++;
                }
            }
        }

        result.setTotal(total);
        result.setUpdated(updated);
        result.setSkipped(skipped);
        result.setMessage(String.format("扫描%d条球杆商品，成功打标%d条，跳过%d条", total, updated, skipped));
        this.lastResult = result;
        return result;
    }

    /**
     * 获取最近一次打标结果
     */
    public TagGenerateResult getLastResult() {
        return lastResult;
    }

    /**
     * 根据商品信息自动判断6个标签
     * 规则：内置判断，不调AI
     */
    private Map<String, Object> determineTags(Product p) {
        Map<String, Object> tags = new HashMap<>();
        String name = p.getName() != null ? p.getName() : "";
        String model = p.getModel() != null ? p.getModel() : "";
        String cueType = p.getCueType() != null ? p.getCueType() : "";

        // 1. fit_play / fit_power 优先从params JSON读取
        String fitPlay = null;
        String fitPower = null;
        if (p.getParams() != null) {
            try {
                JsonNode json = objectMapper.readTree(p.getParams());
                if (json.has("fitPlay")) fitPlay = json.get("fitPlay").asText();
                if (json.has("fitPower")) fitPower = json.get("fitPower").asText();
                if (json.has("适用打法") && (fitPlay == null || fitPlay.isEmpty())) {
                    String scene = json.get("适用打法").asText();
                    fitPlay = mapSceneToPlay(scene);
                }
            } catch (Exception ignored) {}
        }
        // 如果params里没有，从商品名/描述判断
        if (fitPlay == null) fitPlay = determineFitPlay(name, p.getApplicableScene());
        if (fitPower == null) fitPower = determineFitPower(name, p.getDescription());
        tags.put("fitPlay", fitPlay);
        tags.put("fitPower", fitPower);

        // 4. hardness / material 优先从params读取，否则自动判断
        String hardness = null;
        String material = null;
        if (p.getParams() != null) {
            try {
                JsonNode json = objectMapper.readTree(p.getParams());
                if (json.has("硬度")) hardness = json.get("硬度").asText();
                if (json.has("hardness")) hardness = json.get("hardness").asText();
                if (json.has("材质")) material = json.get("材质").asText();
                if (json.has("material")) material = json.get("material").asText();
            } catch (Exception ignored) {}
        }
        if (hardness == null || hardness.isEmpty()) {
            hardness = determineHardness(name, p.getDescription(), p.getHardness());
        }
        if (material == null || material.isEmpty()) {
            material = determineMaterial(name, p.getDescription(), p.getMaterial());
        }
        tags.put("hardness", hardness);
        tags.put("material", material);

        return tags;
    }

    // ========== 标签判断规则 ==========

    /**
     * 适配打法判断
     */
    private String determineFitPlay(String name, String scene) {
        // 从适用场景判断
        if (scene != null) {
            if (scene.contains("中式八球") || scene.contains("防守"))
                return CueRecommendConstants.PLAY_DEFENSE;
            if (scene.contains("九球") || scene.contains("追分"))
                return CueRecommendConstants.PLAY_OFFENSE;
        }
        // 从商品名判断
        if (name.contains("进攻") || name.contains("追分"))
            return CueRecommendConstants.PLAY_OFFENSE;
        if (name.contains("防守") || name.contains("稳定"))
            return CueRecommendConstants.PLAY_DEFENSE;
        return CueRecommendConstants.PLAY_BALANCE; // 默认均衡
    }

    /**
     * 适配发力判断
     */
    private String determineFitPower(String name, String desc) {
        String text = (name != null ? name : "") + (desc != null ? desc : "");
        if (text.contains("大力") || text.contains("爆发") || text.contains("硬"))
            return CueRecommendConstants.POWER_STRONG;
        if (text.contains("轻推") || text.contains("控制") || text.contains("控球"))
            return CueRecommendConstants.POWER_LIGHT;
        return CueRecommendConstants.POWER_MEDIUM; // 默认均匀
    }

    /**
     * 从适用场景映射打法
     */
    private String mapSceneToPlay(String scene) {
        if (scene == null) return null;
        if (scene.contains("中式八球")) return CueRecommendConstants.PLAY_DEFENSE;
        if (scene.contains("九球")) return CueRecommendConstants.PLAY_OFFENSE;
        if (scene.contains("斯诺克")) return CueRecommendConstants.PLAY_DEFENSE;
        return null;
    }

    /**
     * 硬度自动判断（基于商品名、描述、现有值）
     */
    private String determineHardness(String name, String desc, String currentHardness) {
        // 如果已有有效值，保留
        if (currentHardness != null && !currentHardness.isEmpty()) {
            if (CueRecommendConstants.HARD_SOFT.equals(currentHardness)
                    || CueRecommendConstants.HARD_MEDIUM.equals(currentHardness)
                    || CueRecommendConstants.HARD_HARD.equals(currentHardness)) {
                return currentHardness;
            }
        }
        String text = (name != null ? name : "") + " " + (desc != null ? desc : "");
        if (text.contains("偏硬") || text.contains("硬") || text.contains("H"))
            return CueRecommendConstants.HARD_HARD;
        if (text.contains("偏软") || text.contains("软") || text.contains("S"))
            return CueRecommendConstants.HARD_SOFT;
        if (text.contains("中性") || text.contains("中") || text.contains("M"))
            return CueRecommendConstants.HARD_MEDIUM;
        return null; // 信息不足，不强行生成
    }

    /**
     * 材质自动判断（基于商品名、描述、现有值）
     */
    private String determineMaterial(String name, String desc, String currentMaterial) {
        if (currentMaterial != null && !currentMaterial.isEmpty()) {
            if (CueRecommendConstants.MAT_WOOD.equals(currentMaterial)
                    || CueRecommendConstants.MAT_TECH.equals(currentMaterial)) {
                return currentMaterial;
            }
        }
        String text = (name != null ? name : "") + " " + (desc != null ? desc : "");
        if (text.contains("科技") || text.contains("碳纤维") || text.contains("碳素") || text.contains("carbon"))
            return CueRecommendConstants.MAT_TECH;
        if (text.contains("实木") || text.contains("木材") || text.contains("白蜡"))
            return CueRecommendConstants.MAT_WOOD;
        return null; // 信息不足，不强行生成
    }
}
