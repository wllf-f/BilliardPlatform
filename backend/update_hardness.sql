-- ================ 小头杆 (category_id=1) 硬度重新分配 ================
-- LP 天尊系列 - 偏硬
UPDATE product SET hardness = '偏硬' WHERE id IN (1,2);
-- LP 战神系列
UPDATE product SET hardness = '偏硬' WHERE id IN (4,5);
UPDATE product SET hardness = '偏软' WHERE id IN (6,7);
-- LP 梦杆/黑旋风 - 偏软
UPDATE product SET hardness = '偏软' WHERE id IN (8,9);
-- LP 尊者S - 均衡
UPDATE product SET hardness = '均衡' WHERE id = 3;

-- 健英系列
UPDATE product SET hardness = '偏硬' WHERE id IN (10,11,13,15,16,19);
UPDATE product SET hardness = '均衡' WHERE id IN (12,14,17,18);

-- 南匠系列
UPDATE product SET hardness = '偏硬' WHERE id IN (20,21,22);
UPDATE product SET hardness = '均衡' WHERE id = 23;
UPDATE product SET hardness = '偏软' WHERE id = 24;

-- 天工TG系列
UPDATE product SET hardness = '偏硬' WHERE id IN (25,26,27,28);
UPDATE product SET hardness = '均衡' WHERE id = 29;
UPDATE product SET hardness = '偏软' WHERE id = 30;

-- 奥斯本兹系列
UPDATE product SET hardness = '偏硬' WHERE id IN (31,32,33);
UPDATE product SET hardness = '均衡' WHERE id IN (34,35);
UPDATE product SET hardness = '偏软' WHERE id = 36;

-- 奥秘OMIN系列
UPDATE product SET hardness = '偏硬' WHERE id IN (37,38,39);
UPDATE product SET hardness = '均衡' WHERE id IN (40,41,42);
UPDATE product SET hardness = '偏软' WHERE id IN (43,44);

-- 波茨系列 - 偏硬
UPDATE product SET hardness = '偏硬' WHERE id IN (45,46,47,48);

-- 白沙BS系列
UPDATE product SET hardness = '偏硬' WHERE id IN (49,50);
UPDATE product SET hardness = '均衡' WHERE id IN (51,52);
UPDATE product SET hardness = '偏软' WHERE id IN (53,54);

-- 蓝图系列
UPDATE product SET hardness = '偏硬' WHERE id IN (55,56);
UPDATE product SET hardness = '均衡' WHERE id = 57;
UPDATE product SET hardness = '偏软' WHERE id = 58;

-- 野豹系列
UPDATE product SET hardness = '偏硬' WHERE id IN (59,60,61,62,63);
UPDATE product SET hardness = '均衡' WHERE id IN (64,65);
UPDATE product SET hardness = '偏软' WHERE id IN (66,67,68);

-- ================ 大头杆 (category_id=2) 硬度重新分配 ================
-- FURY威利系列
UPDATE product SET hardness = '偏硬' WHERE id IN (84,85,87);
UPDATE product SET hardness = '均衡' WHERE id IN (79,81,83,86,88);
UPDATE product SET hardness = '偏软' WHERE id IN (80,82);

-- 仁杆 - 偏硬
UPDATE product SET hardness = '偏硬' WHERE id = 89;

-- 奥斯本兹征途系列
UPDATE product SET hardness = '偏硬' WHERE id IN (91,92,93,94,98,100);
UPDATE product SET hardness = '均衡' WHERE id IN (90,95,99,101);
UPDATE product SET hardness = '偏软' WHERE id IN (96,97);

-- 奥秘OMIN大头系列
UPDATE product SET hardness = '偏硬' WHERE id = 103;
UPDATE product SET hardness = '均衡' WHERE id = 102;

-- 皮尔力PERI系列
UPDATE product SET hardness = '偏硬' WHERE id IN (104,105,109,113,116,117);
UPDATE product SET hardness = '均衡' WHERE id IN (106,107,108,110,111,114,115,118);
UPDATE product SET hardness = '偏软' WHERE id = 112;

SELECT '更新完成' as result;
