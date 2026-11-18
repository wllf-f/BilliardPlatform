package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.billiard.common.Constants;
import com.billiard.entity.*;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 订单服务：统一管理普通商品订单、仁杆定制订单、皮头改装订单
 * 状态流转：0未付款 → 1待发货 → 2待收货 → 3已完成 / 4已取消
 */
@Service
public class OrderService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 仁杆定制默认图片（使用用户提供的图片） */
    private static final String DEFAULT_IMG_CUSTOM = "service/custom.png";
    /** 皮头改装默认图片 */
    private static final String DEFAULT_IMG_TIP_MODIFY = "service/tip-modify.png";
    /** 通用占位图（兜底用） */
    private static final String DEFAULT_IMG_PLACEHOLDER = "service/tip-modify.png";

    @Autowired
    private OrdersMapper ordersMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private CustomOrderMapper customOrderMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductPriceMapper productPriceMapper;
    @Autowired
    private UserAddressMapper userAddressMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ConfigService configService;

    @Value("${image.base-url}")
    private String baseUrl;

    /** 我的订单列表（按状态筛选） */
    public IPage<Map<String, Object>> myOrders(int page, int size, Long userId, Integer status, Integer orderType) {
        Page<Map<String, Object>> p = new Page<>(page, size);
        QueryWrapper<Orders> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        if (status != null) {
            qw.eq("status", status);
        }
        if (orderType != null) {
            qw.eq("order_type", orderType);
        }
        qw.orderByDesc("create_time");
        IPage<Orders> orderPage = ordersMapper.selectPage(new Page<>(page, size), qw);
        // 组装详情
        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setTotal(orderPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (Orders o : orderPage.getRecords()) {
            Map<String, Object> m = orderToMap(o);
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", o.getId()));
            m.put("items", buildOrderItems(items, o.getOrderType()));
            // 定制订单附带表单
            if (o.getOrderType() == Constants.ORDER_TYPE_CUSTOM) {
                CustomOrder co = customOrderMapper.selectOne(new QueryWrapper<CustomOrder>().eq("order_id", o.getId()));
                m.put("customForm", co);
            }
            records.add(m);
        }
        result.setRecords(records);
        return result;
    }

    /** 全部订单（管理后台） */
    public IPage<Map<String, Object>> allOrders(int page, int size, Integer status, Integer orderType, String orderNo) {
        QueryWrapper<Orders> qw = new QueryWrapper<>();
        if (status != null) {
            qw.eq("status", status);
        }
        if (orderType != null) {
            qw.eq("order_type", orderType);
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            qw.like("order_no", orderNo);
        }
        qw.orderByDesc("create_time");
        IPage<Orders> orderPage = ordersMapper.selectPage(new Page<>(page, size), qw);
        Page<Map<String, Object>> result = new Page<>(page, size);
        result.setTotal(orderPage.getTotal());
        List<Map<String, Object>> records = new ArrayList<>();
        for (Orders o : orderPage.getRecords()) {
            Map<String, Object> m = orderToMap(o);
            // 查询用户信息（异常保护，避免用户表查不到时中断列表）
            try {
                if (o.getUserId() != null) {
                    User u = userMapper.selectById(o.getUserId());
                    if (u != null) {
                        m.put("nickname", u.getNickname());
                        m.put("username", u.getUsername());
                    }
                }
            } catch (Exception e) {
                // 用户信息查询失败不影响订单数据返回
            }
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", o.getId()));
            m.put("items", buildOrderItems(items, o.getOrderType()));
            if (o.getOrderType() == Constants.ORDER_TYPE_CUSTOM) {
                CustomOrder co = customOrderMapper.selectOne(new QueryWrapper<CustomOrder>().eq("order_id", o.getId()));
                m.put("customForm", co);
            }
            records.add(m);
        }
        result.setRecords(records);
        return result;
    }

    /** 构建订单商品列表，拼接完整图片URL。orderType 用于空图片时兜底默认图 */
    private List<Map<String, Object>> buildOrderItems(List<OrderItem> items, Integer orderType) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (OrderItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("productId", item.getProductId());
            m.put("productName", item.getProductName());
            m.put("price", item.getPrice());
            m.put("quantity", item.getQuantity());
            m.put("subtotal", item.getSubtotal());
            String imgVal = item.getImgUrl();
            // 空图片时按订单类型给默认图
            if (imgVal == null || imgVal.isEmpty()) {
                imgVal = defaultImgByType(orderType);
            }
            if (imgVal != null && !imgVal.isEmpty()) {
                String imgUrl = imgVal;
                if (!imgUrl.startsWith("http")) {
                    imgUrl = baseUrl + imgUrl;
                }
                try {
                    java.net.URI uri = new java.net.URI(imgUrl);
                    imgUrl = uri.toASCIIString();
                } catch (Exception e) {}
                m.put("imgUrl", imgUrl);
            } else {
                m.put("imgUrl", "");
            }
            result.add(m);
        }
        return result;
    }

    /** 按订单类型返回默认图片路径 */
    private String defaultImgByType(Integer orderType) {
        if (orderType == null) return DEFAULT_IMG_PLACEHOLDER;
        switch (orderType) {
            case Constants.ORDER_TYPE_CUSTOM: return DEFAULT_IMG_CUSTOM;
            case Constants.ORDER_TYPE_TIP_MODIFY: return DEFAULT_IMG_TIP_MODIFY;
            default: return DEFAULT_IMG_PLACEHOLDER;
        }
    }

    /** 普通商品下单 */
    @Transactional
    public Orders createNormalOrder(Long userId, Long productId, Integer quantity, Long addressId, String remark) {
        Product p = productMapper.selectById(productId);
        if (p == null || p.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
        if (p.getStock() != null && p.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }
        ProductPrice price = productPriceMapper.selectOne(new QueryWrapper<ProductPrice>().eq("product_id", productId));
        if (price == null) {
            throw new BusinessException("商品价格异常");
        }
        UserAddress addr = userAddressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }
        BigDecimal total = price.getPlatformPrice().multiply(new BigDecimal(quantity));
        Orders order = buildOrder(userId, Constants.ORDER_TYPE_NORMAL, total, addr, remark);
        ordersMapper.insert(order);
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(productId);
        item.setProductName(p.getName());
        item.setImgUrl(p.getImgDir() + p.getMainImg());
        item.setPrice(price.getPlatformPrice());
        item.setQuantity(quantity);
        item.setSubtotal(total);
        orderItemMapper.insert(item);
        // 原子扣库存（防超卖）：WHERE stock>=qty 才扣，返回0表示库存不足；销量在订单完成时才计入
        int rows = productMapper.deductStock(productId, quantity);
        if (rows == 0) {
            throw new BusinessException("库存不足，下单失败");
        }
        return order;
    }

    /** 仁杆定制下单（固定金额，从系统配置读取） */
    @Transactional
    public Orders createCustomOrder(Long userId, CustomOrder form, Long addressId) {
        UserAddress addr = userAddressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }
        BigDecimal price = new BigDecimal(configService.get(Constants.CFG_CUSTOM_PRICE));
        Orders order = buildOrder(userId, Constants.ORDER_TYPE_CUSTOM, price, addr, "仁杆定制订单");
        ordersMapper.insert(order);
        form.setOrderId(order.getId());
        customOrderMapper.insert(form);
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductName("仁杆定制 - " + (form.getPlayType() != null ? form.getPlayType() : "中式八球"));
        item.setImgUrl(DEFAULT_IMG_CUSTOM);
        item.setPrice(price);
        item.setQuantity(1);
        item.setSubtotal(price);
        orderItemMapper.insert(item);
        return order;
    }

    /** 皮头改装 - 自备皮头（固定服务费+运费） */
    @Transactional
    public Orders createTipModifyOwnOrder(Long userId, Long addressId, String remark) {
        UserAddress addr = userAddressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }
        BigDecimal fee = new BigDecimal(configService.get(Constants.CFG_TIP_FEE));
        BigDecimal handFee = new BigDecimal(configService.get(Constants.CFG_TIP_HANDFEE));
        BigDecimal total = fee.add(handFee);
        Orders order = buildOrder(userId, Constants.ORDER_TYPE_TIP_MODIFY, total, addr, "自备皮头更换 " + (remark != null ? remark : ""));
        ordersMapper.insert(order);
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductName("皮头改装服务（自备皮头）");
        item.setImgUrl(DEFAULT_IMG_TIP_MODIFY);
        item.setPrice(total);
        item.setQuantity(1);
        item.setSubtotal(total);
        orderItemMapper.insert(item);
        return order;
    }

    /** 皮头改装 - 购买平台皮头（皮头售价+手工费） */
    @Transactional
    public Orders createTipModifyBuyOrder(Long userId, Long productId, Long addressId, String remark) {
        Product p = productMapper.selectById(productId);
        if (p == null) {
            throw new BusinessException("皮头商品不存在");
        }
        if (p.getStatus() == 0) {
            throw new BusinessException("皮头商品已下架");
        }
        if (p.getStock() != null && p.getStock() < 1) {
            throw new BusinessException("皮头库存不足");
        }
        ProductPrice price = productPriceMapper.selectOne(new QueryWrapper<ProductPrice>().eq("product_id", productId));
        if (price == null) {
            throw new BusinessException("商品价格异常");
        }
        UserAddress addr = userAddressMapper.selectById(addressId);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }
        BigDecimal handFee = new BigDecimal(configService.get(Constants.CFG_TIP_HANDFEE));
        BigDecimal total = price.getPlatformPrice().add(handFee);
        Orders order = buildOrder(userId, Constants.ORDER_TYPE_TIP_MODIFY, total, addr, "寄换皮头 " + (remark != null ? remark : ""));
        ordersMapper.insert(order);
        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(productId);
        item.setProductName(p.getName() + "(寄换皮头)");
        item.setImgUrl(p.getImgDir() + p.getMainImg());
        item.setPrice(total);
        item.setQuantity(1);
        item.setSubtotal(total);
        orderItemMapper.insert(item);
        // 原子扣库存（皮头也是商品，用户下单即扣减，取消订单时 restoreStock 会还原库存；销量在订单完成时才计入）
        int rows = productMapper.deductStock(productId, 1);
        if (rows == 0) {
            throw new BusinessException("皮头库存不足，下单失败");
        }
        return order;
    }

    /** 模拟支付：未付款 → 待发货 */
    @Transactional
    public void pay(Long orderId, Long userId) {
        Orders o = getOrder(orderId, userId);
        if (o.getStatus() != Constants.ORDER_UNPAID) {
            throw new BusinessException("订单状态不允许支付");
        }
        o.setStatus(Constants.ORDER_UNSHIP);
        o.setPayTime(LocalDateTime.now());
        ordersMapper.updateById(o);
    }

    /** 取消订单 */
    @Transactional
    public void cancel(Long orderId, Long userId) {
        Orders o = getOrder(orderId, userId);
        if (o.getStatus() != Constants.ORDER_UNPAID) {
            throw new BusinessException("仅未付款订单可取消");
        }
        o.setStatus(Constants.ORDER_CANCEL);
        ordersMapper.updateById(o);
        restoreStock(o.getId(), o.getOrderType());
    }

    /** 自动取消超时订单（未付款超时自动取消） */
    @Transactional
    public int autoCancelTimeoutOrders() {
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(Constants.ORDER_PAY_TIMEOUT_MINUTES);
        QueryWrapper<Orders> qw = new QueryWrapper<>();
        qw.eq("status", Constants.ORDER_UNPAID);
        qw.lt("create_time", timeoutTime);
        List<Orders> timeoutOrders = ordersMapper.selectList(qw);
        int count = 0;
        for (Orders o : timeoutOrders) {
            o.setStatus(Constants.ORDER_CANCEL);
            ordersMapper.updateById(o);
            restoreStock(o.getId(), o.getOrderType());
            count++;
        }
        if (count > 0) {
            System.out.println("定时任务：自动取消超时订单 " + count + " 笔");
        }
        return count;
    }

    /** 自动确认收货（发货后超时自动收货；测试用2分钟，生产环境改为7天） */
    @Transactional
    public int autoCompleteTimeoutOrders() {
        LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(Constants.ORDER_RECEIVE_TIMEOUT_MINUTES);
        QueryWrapper<Orders> qw = new QueryWrapper<>();
        qw.eq("status", Constants.ORDER_UNRECEIVE);
        qw.lt("ship_time", timeoutTime);
        List<Orders> timeoutOrders = ordersMapper.selectList(qw);
        int count = 0;
        for (Orders o : timeoutOrders) {
            o.setStatus(Constants.ORDER_FINISH);
            o.setFinishTime(LocalDateTime.now());
            ordersMapper.updateById(o);
            // 订单完成，累加真实销量
            addSales(o.getId(), o.getOrderType());
            count++;
        }
        if (count > 0) {
            System.out.println("定时任务：自动确认收货订单 " + count + " 笔");
        }
        return count;
    }

    /** 恢复库存（原子操作，取消订单时调用；销量在订单完成时才计入，故取消无需减销量） */
    private void restoreStock(Long orderId, Integer orderType) {
        if (orderType == Constants.ORDER_TYPE_NORMAL || orderType == Constants.ORDER_TYPE_TIP_MODIFY) {
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", orderId));
            for (OrderItem item : items) {
                if (item.getProductId() != null) {
                    // 原子恢复库存，无需先查再改
                    productMapper.restoreStock(item.getProductId(), item.getQuantity());
                }
            }
        }
    }

    /** 订单完成时累加销量（仅普通/皮头改装等含商品ID的订单项） */
    private void addSales(Long orderId, Integer orderType) {
        if (orderType == Constants.ORDER_TYPE_NORMAL || orderType == Constants.ORDER_TYPE_TIP_MODIFY) {
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", orderId));
            for (OrderItem item : items) {
                if (item.getProductId() != null) {
                    productMapper.addSales(item.getProductId(), item.getQuantity());
                }
            }
        }
    }

    /** 已完成订单被改回非完成时扣减销量（销量不低于 0） */
    private void reduceSales(Long orderId, Integer orderType) {
        if (orderType == Constants.ORDER_TYPE_NORMAL || orderType == Constants.ORDER_TYPE_TIP_MODIFY) {
            List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", orderId));
            for (OrderItem item : items) {
                if (item.getProductId() != null) {
                    productMapper.reduceSales(item.getProductId(), item.getQuantity());
                }
            }
        }
    }

    /** 确认收货：待收货 → 已完成 */
    @Transactional
    public void confirm(Long orderId, Long userId) {
        Orders o = getOrder(orderId, userId);
        if (o.getStatus() != Constants.ORDER_UNRECEIVE) {
            throw new BusinessException("订单状态不允许确认收货");
        }
        o.setStatus(Constants.ORDER_FINISH);
        o.setFinishTime(LocalDateTime.now());
        ordersMapper.updateById(o);
        // 订单完成，累加真实销量
        addSales(o.getId(), o.getOrderType());
    }

    /** 管理员修改订单状态 */
    @Transactional
    public void updateStatus(Long orderId, Integer status) {
        Orders o = ordersMapper.selectById(orderId);
        if (o == null) {
            throw new BusinessException("订单不存在");
        }
        Integer oldStatus = o.getStatus();
        // 状态未变化时直接返回，避免重复取消导致重复恢复库存
        if (oldStatus != null && oldStatus.equals(status)) {
            return;
        }
        o.setStatus(status);
        if (status == Constants.ORDER_UNSHIP && o.getPayTime() == null) {
            o.setPayTime(LocalDateTime.now());
        }
        if (status == Constants.ORDER_UNRECEIVE && o.getShipTime() == null) {
            o.setShipTime(LocalDateTime.now());
        }
        if (status == Constants.ORDER_FINISH && o.getFinishTime() == null) {
            o.setFinishTime(LocalDateTime.now());
        }
        ordersMapper.updateById(o);
        // 管理员取消订单：如果之前不是取消状态，恢复库存（销量只在订单完成时计入，取消无需减销量）
        if (status == Constants.ORDER_CANCEL && oldStatus != Constants.ORDER_CANCEL) {
            restoreStock(o.getId(), o.getOrderType());
        }
        // 状态从非完成 → 完成：累加真实销量
        if (status == Constants.ORDER_FINISH && (oldStatus == null || oldStatus != Constants.ORDER_FINISH)) {
            addSales(o.getId(), o.getOrderType());
        }
        // 状态从完成 → 非完成（如取消、改回待收货）：扣减销量
        if (oldStatus != null && oldStatus == Constants.ORDER_FINISH && status != Constants.ORDER_FINISH) {
            reduceSales(o.getId(), o.getOrderType());
        }
    }

    /** 管理员发货：填写物流信息，状态从待发货变为待收货；也支持已发货后修改物流单号 */
    @Transactional
    public void ship(Long orderId, String expressCompany, String expressNo) {
        Orders o = ordersMapper.selectById(orderId);
        if (o == null) {
            throw new BusinessException("订单不存在");
        }
        if (o.getStatus() != Constants.ORDER_UNSHIP && o.getStatus() != Constants.ORDER_UNRECEIVE) {
            throw new BusinessException("订单状态不允许发货/修改物流");
        }
        // 如果是首次发货（未发货→待收货）则设置发货时间并改状态
        if (o.getStatus() == Constants.ORDER_UNSHIP) {
            o.setStatus(Constants.ORDER_UNRECEIVE);
            o.setShipTime(LocalDateTime.now());
        }
        // 无论首次发货还是修改，都更新物流公司+单号
        o.setExpressCompany(expressCompany);
        o.setExpressNo(expressNo);
        ordersMapper.updateById(o);
    }

    public Orders getById(Long id) {
        return ordersMapper.selectById(id);
    }

    public Orders getOrder(Long id, Long userId) {
        Orders o = ordersMapper.selectById(id);
        if (o == null || (userId != null && !o.getUserId().equals(userId))) {
            throw new BusinessException("订单不存在");
        }
        return o;
    }

    /** 删除订单（仅已取消订单可删除） */
    @Transactional
    public void delete(Long orderId, Long userId) {
        Orders o = getOrder(orderId, userId);
        if (o.getStatus() != Constants.ORDER_CANCEL) {
            throw new BusinessException("仅已取消订单可删除");
        }
        ordersMapper.deleteById(orderId);
    }

    /** 退款：待发货非定制款可直接退款，其他联系客服 */
    @Transactional
    public void refund(Long orderId, Long userId) {
        Orders o = getOrder(orderId, userId);
        if (o.getStatus() == Constants.ORDER_UNSHIP && o.getOrderType() == Constants.ORDER_TYPE_NORMAL) {
            o.setStatus(Constants.ORDER_CANCEL);
            ordersMapper.updateById(o);
            restoreStock(o.getId(), o.getOrderType());
        } else {
            throw new BusinessException("该订单请联系客服微信申请退款");
        }
    }

    /** 订单详情（含商品、地址、定制表单、用户信息） */
    public Map<String, Object> detail(Long id, Long userId) {
        Orders o = getOrder(id, userId);
        Map<String, Object> m = orderToMap(o);
        // 查询用户信息补充昵称/姓名（异常保护）
        try {
            if (o.getUserId() != null) {
                User u = userMapper.selectById(o.getUserId());
                if (u != null) {
                    m.put("username", u.getUsername());
                    m.put("nickname", u.getNickname());
                    m.put("userAvatar", u.getAvatar());
                }
            }
        } catch (Exception e) {
            // 用户信息查询失败不影响订单详情返回
        }
        List<OrderItem> items = orderItemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", o.getId()));
        m.put("items", buildOrderItems(items, o.getOrderType()));
        if (o.getOrderType() == Constants.ORDER_TYPE_CUSTOM) {
            CustomOrder co = customOrderMapper.selectOne(new QueryWrapper<CustomOrder>().eq("order_id", o.getId()));
            m.put("customForm", co);
        }
        return m;
    }

    private Orders buildOrder(Long userId, int orderType, BigDecimal total, UserAddress addr, String remark) {
        Orders o = new Orders();
        o.setOrderNo(generateOrderNo());
        o.setUserId(userId);
        o.setOrderType(orderType);
        o.setTotalAmount(total);
        o.setStatus(Constants.ORDER_UNPAID);
        o.setConsignee(addr.getConsignee());
        o.setPhone(addr.getPhone());
        o.setAddress(buildFullAddress(addr));
        o.setRemark(remark);
        return o;
    }

    private String buildFullAddress(UserAddress addr) {
        StringBuilder sb = new StringBuilder();
        if (addr.getProvince() != null) sb.append(addr.getProvince());
        if (addr.getCity() != null) sb.append(addr.getCity());
        if (addr.getDistrict() != null) sb.append(addr.getDistrict());
        if (addr.getDetail() != null) sb.append(addr.getDetail());
        return sb.toString();
    }

    private String generateOrderNo() {
        // 修复：原代码 "BL" + millis + rand.nextInt(9000) + 1000 因字符串拼接左结合，
        // 导致 rand+1000 变成 rand 后再拼 "1000"，订单号格式错乱。
        // 改用 ThreadLocalRandom（线程安全）+ String.format 保证 4 位补零。
        return "BL" + System.currentTimeMillis()
                + String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000));
    }

    private Map<String, Object> orderToMap(Orders o) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", o.getId());
        m.put("orderNo", o.getOrderNo());
        m.put("userId", o.getUserId());
        m.put("orderType", o.getOrderType());
        m.put("totalAmount", o.getTotalAmount());
        m.put("status", o.getStatus());
        m.put("consignee", o.getConsignee());
        m.put("phone", o.getPhone());
        m.put("address", o.getAddress());
        m.put("remark", o.getRemark());
        m.put("expressCompany", o.getExpressCompany());
        m.put("expressNo", o.getExpressNo());
        m.put("createTime", o.getCreateTime() != null ? o.getCreateTime().format(DTF) : null);
        m.put("payTime", o.getPayTime() != null ? o.getPayTime().format(DTF) : null);
        m.put("shipTime", o.getShipTime() != null ? o.getShipTime().format(DTF) : null);
        m.put("finishTime", o.getFinishTime() != null ? o.getFinishTime().format(DTF) : null);
        m.put("completeTime", o.getFinishTime() != null ? o.getFinishTime().format(DTF) : null);
        // 超时配置（分钟），前端用于计算倒计时
        m.put("payTimeoutMinutes", Constants.ORDER_PAY_TIMEOUT_MINUTES);
        m.put("receiveTimeoutMinutes", Constants.ORDER_RECEIVE_TIMEOUT_MINUTES);
        return m;
    }
}
