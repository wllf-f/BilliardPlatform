-- =====================================================
-- 球杆商品全量打标脚本（一次性数据初始化）
-- 给所有 cue_type 非空的球杆商品补全推荐标签字段
-- 杆型复用 cue_type、价位复用 product_price 表，本脚本仅维护 fit_play/fit_power/hardness/material
-- =====================================================

-- 1. fit_play 适配打法打标
--    大头杆→追分走位；小头杆偏硬→准度防守；小头杆偏软/均衡→均衡打法
UPDATE product SET fit_play = CASE
  WHEN cue_type = '大头杆' THEN '追分走位'
  WHEN cue_type = '小头杆' AND hardness = '偏硬' THEN '准度防守'
  WHEN cue_type = '小头杆' THEN '均衡打法'
  ELSE '均衡打法'
END
WHERE cue_type IS NOT NULL AND cue_type != '';

-- 2. fit_power 适配发力打标（按 hardness 映射）
UPDATE product SET fit_power = CASE
  WHEN hardness = '偏硬' THEN '大力发力'
  WHEN hardness = '偏软' THEN '轻推发力'
  WHEN hardness = '均衡' THEN '均匀发力'
  ELSE '均匀发力'
END
WHERE cue_type IS NOT NULL AND cue_type != '';

-- 3. material 材质规范化（推荐文案用，统一为枚举值）
--    含"碳/科技" → 科技前支；含"木" → 实木
UPDATE product SET material = '科技前支'
WHERE cue_type IS NOT NULL AND cue_type != ''
  AND (material LIKE '%碳%' OR material LIKE '%科技%');
UPDATE product SET material = '实木'
WHERE cue_type IS NOT NULL AND cue_type != ''
  AND material LIKE '%木%';

-- 4. 验证打标结果
SELECT cue_type, hardness, COUNT(*) AS cnt FROM product
WHERE cue_type IS NOT NULL AND cue_type != '' GROUP BY cue_type, hardness ORDER BY cue_type, hardness;
