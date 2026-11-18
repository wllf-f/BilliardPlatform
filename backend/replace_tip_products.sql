-- 替换不好找图片的品牌
UPDATE product SET 
  name = '飞刃皮头',
  model = '飞刃',
  img_dir = '07_配套耗材配件/皮头/飞刃皮头/',
  description = '飞刃皮头，斯诺克专业级皮头，打感稳定。'
WHERE id = 219;

UPDATE product SET 
  name = '名将皮头',
  model = '名将',
  img_dir = '07_配套耗材配件/皮头/名将皮头/',
  description = '名将皮头，硬弹反馈，精准打点。'
WHERE id = 223;
