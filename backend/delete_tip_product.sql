-- 删除飞刃皮头商品
DELETE FROM product WHERE id = 219;
DELETE FROM product_price WHERE product_id = 219;

-- 验证剩余皮头商品
SELECT id, name, hardness, tip_diameter, pic_count FROM product WHERE category_id = 7 AND brand_id = 37 AND status = 1 ORDER BY id;
