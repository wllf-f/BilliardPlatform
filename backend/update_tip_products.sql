-- =====================================================
-- 皮头数据更新脚本：统一硬度值 + 添加10款新皮头
-- =====================================================

-- 1. 更新现有皮头的硬度值，统一为偏软/均衡/偏硬
UPDATE product SET hardness = '偏硬' WHERE id = 207; -- 3.0T皮头 (原中硬)
UPDATE product SET hardness = '均衡' WHERE id = 208; -- 卡姆KAMUI皮头 (原中)
UPDATE product SET hardness = '偏软' WHERE id = 209; -- 猛犸象Pro皮头 (原中软)
UPDATE product SET hardness = '偏软' WHERE id = 210; -- 胜利者皮头 (原中软)
UPDATE product SET hardness = '偏软' WHERE id = 211; -- 超级斩皮头 (原中软)
UPDATE product SET hardness = '均衡' WHERE id = 212; -- 闯牌皮头 (原中)
UPDATE product SET hardness = '均衡' WHERE id = 213; -- 鹿头多层皮头 (原中)

-- 2. 添加10款新皮头商品
-- 偏软类
INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (214,7,37,'皮头 蓝钻多层皮头','蓝钻多层','07_配套耗材配件/皮头/蓝钻多层皮头/','pic1.jpg',6,NULL,'偏软','10.5',NULL,'猪皮多层',NULL,'中式八球','{"硬度": "偏软", "直径": "10.5mm", "材质": "猪皮多层", "层数": 11, "适用杆型": "小头杆", "适用场景": "中式八球"}','蓝钻多层皮头，多层压制工艺，弹性好，控球精准。',50,30,1,214);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (214,168.00,148.00,135.00);

INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (215,7,37,'皮头 威利FURY皮头','FURY皮头','07_配套耗材配件/皮头/威利FURY皮头/','pic1.jpg',6,NULL,'偏软','13.0',NULL,'牛皮多层',NULL,'九球','{"硬度": "偏软", "直径": "13.0mm", "材质": "牛皮多层", "层数": 9, "适用杆型": "大头杆", "适用场景": "九球"}','威利FURY皮头，专为九球设计，吃球深，旋转强。',40,25,1,215);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (215,128.00,113.00,103.00);

INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (216,7,37,'皮头 奥秘OMIN皮头','OMIN皮头','07_配套耗材配件/皮头/奥秘OMIN皮头/','pic1.jpg',5,NULL,'偏软','11.5',NULL,'鹿皮',NULL,'斯诺克','{"硬度": "偏软", "直径": "11.5mm", "材质": "鹿皮", "层数": 10, "适用杆型": "小头杆", "适用场景": "斯诺克"}','奥秘OMIN皮头，细腻材质，适合精细控球。',35,20,1,216);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (216,98.00,86.00,78.00);

-- 均衡类
INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (217,7,37,'皮头 皮尔力PERI皮头','PERI皮头','07_配套耗材配件/皮头/皮尔力PERI皮头/','pic1.jpg',8,NULL,'均衡','12.0',NULL,'牛皮多层',NULL,'中式八球','{"硬度": "均衡", "直径": "12.0mm", "材质": "牛皮多层", "层数": 10, "适用杆型": "小头杆", "适用场景": "中式八球"}','皮尔力PERI皮头，均衡硬度，适合各种打法。',60,45,1,217);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (217,148.00,130.00,118.00);

INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (218,7,37,'皮头 美洲豹Predator皮头','Predator皮头','07_配套耗材配件/皮头/美洲豹Predator皮头/','pic1.jpg',10,NULL,'均衡','13.0',NULL,'猪皮多层',NULL,'九球','{"硬度": "均衡", "直径": "13.0mm", "材质": "猪皮多层", "层数": 11, "适用杆型": "大头杆", "适用场景": "九球"}','美洲豹Predator皮头，职业选手首选，打感扎实。',55,50,1,218);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (218,380.00,335.00,305.00);

INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (219,7,37,'皮头 世纪冠军皮头','世纪冠军','07_配套耗材配件/皮头/世纪冠军皮头/','pic1.jpg',6,NULL,'均衡','10.0',NULL,'牛皮单层',NULL,'斯诺克','{"硬度": "均衡", "直径": "10.0mm", "材质": "牛皮单层", "层数": 12, "适用杆型": "小头杆", "适用场景": "斯诺克"}','世纪冠军皮头，斯诺克专业级皮头。',40,35,1,219);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (219,188.00,165.00,150.00);

INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (220,7,37,'皮头 龙古尼Longoni皮头','Longoni皮头','07_配套耗材配件/皮头/龙古尼Longoni皮头/','pic1.jpg',8,NULL,'均衡','12.4',NULL,'鹿皮',NULL,'中式八球','{"硬度": "均衡", "直径": "12.4mm", "材质": "鹿皮", "层数": 11, "适用杆型": "小头杆", "适用场景": "中式八球"}','龙古尼Longoni皮头，意大利工艺，控球细腻。',30,22,1,220);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (220,268.00,235.00,214.00);

-- 偏硬类
INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (221,7,37,'皮头 卡姆KAMUI黑皮头','KAMUI黑皮头','07_配套耗材配件/皮头/卡姆KAMUI黑皮头/','pic1.jpg',10,NULL,'偏硬','12.0',NULL,'牛皮多层',NULL,'中式八球','{"硬度": "偏硬", "直径": "12.0mm", "材质": "牛皮多层", "层数": 12, "适用杆型": "小头杆", "适用场景": "中式八球"}','卡姆KAMUI黑皮头，硬弹反馈，精准打点。',45,38,1,221);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (221,288.00,253.00,230.00);

INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (222,7,37,'皮头 三角Triangle皮头','Triangle皮头','07_配套耗材配件/皮头/三角Triangle皮头/','pic1.jpg',6,NULL,'偏硬','13.0',NULL,'牛皮单层',NULL,'九球','{"硬度": "偏硬", "直径": "13.0mm", "材质": "牛皮单层", "层数": 14, "适用杆型": "大头杆", "适用场景": "九球"}','三角Triangle皮头，美式经典，耐用性强。',60,42,1,222);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (222,198.00,174.00,158.00);

INSERT INTO product(id,category_id,brand_id,name,model,img_dir,main_img,pic_count,cue_type,hardness,tip_diameter,weight,material,spec,applicable_scene,params,description,stock,sales,status,sort) 
VALUES (223,7,37,'皮头 蒂卡姆TIGA皮头','TIGA皮头','07_配套耗材配件/皮头/蒂卡姆TIGA皮头/','pic1.jpg',6,NULL,'偏硬','10.5',NULL,'牛皮多层',NULL,'斯诺克','{"硬度": "偏硬", "直径": "10.5mm", "材质": "牛皮多层", "层数": 13, "适用杆型": "小头杆", "适用场景": "斯诺克"}','蒂卡姆TIGA皮头，斯诺克硬皮头，击打干脆。',35,18,1,223);
INSERT INTO product_price(product_id,official_price,pdd_price,platform_price) VALUES (223,238.00,209.00,190.00);

-- 验证皮头硬度分布
SELECT hardness, COUNT(*) as count FROM product WHERE category_id = 7 AND brand_id = 37 AND status = 1 GROUP BY hardness;
