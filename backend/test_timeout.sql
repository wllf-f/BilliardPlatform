-- 测试超时订单取消功能
-- 1. 查看当前未付款订单
SELECT id, order_no, status, create_time FROM orders WHERE status = 0 LIMIT 10;

-- 2. 将某个未付款订单的创建时间修改为24小时前进行测试
-- UPDATE orders SET create_time = DATE_SUB(NOW(), INTERVAL 25 HOUR) WHERE id = xxx AND status = 0;

-- 3. 查看修改后的订单
-- SELECT id, order_no, status, create_time, DATE_SUB(NOW(), INTERVAL 24 HOUR) as timeout_time FROM orders WHERE id = xxx;
