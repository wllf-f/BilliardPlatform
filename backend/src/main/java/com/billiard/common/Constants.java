package com.billiard.common;

/**
 * 系统常量
 */
public class Constants {
    /** Redis中用户登录Token前缀 */
    public static final String REDIS_USER_TOKEN = "billiard:user:token:";
    /** Redis中管理员登录Token前缀 */
    public static final String REDIS_ADMIN_TOKEN = "billiard:admin:token:";
    /** 首页热门器材缓存Key */
    public static final String REDIS_HOT_PRODUCTS = "billiard:hot:products";
    /** 社区帖子浏览量计数Hash Key */
    public static final String REDIS_POST_VIEWS = "billiard:post:views";
    /** 系统配置缓存Hash Key */
    public static final String REDIS_SYS_CONFIG = "billiard:sys:config";
    /** 比价数据缓存Key前缀 */
    public static final String REDIS_PRICE_COMPARE = "billiard:price:compare:";

    /** Token过期时间(小时) */
    public static final int TOKEN_EXPIRE_HOURS = 72;
    /** 管理员Token过期时间(小时) */
    public static final int ADMIN_TOKEN_EXPIRE_HOURS = 168;

    /** 订单类型 */
    public static final int ORDER_TYPE_NORMAL = 1;
    public static final int ORDER_TYPE_CUSTOM = 2;
    public static final int ORDER_TYPE_TIP_MODIFY = 3;

    /** 订单状态 */
    public static final int ORDER_UNPAID = 0;
    public static final int ORDER_UNSHIP = 1;
    public static final int ORDER_UNRECEIVE = 2;
    public static final int ORDER_FINISH = 3;
    public static final int ORDER_CANCEL = 4;

    /** 帖子状态 */
    public static final int POST_AUDIT = 0;
    public static final int POST_PUBLISH = 1;
    public static final int POST_REJECT = 2;
    public static final int POST_HIDDEN = 3; // 仅自己可见
    public static final int POST_DELETED = 4; // 已删除（用户软删除，保留数据避免评论/点赞变孤儿）

    /** 订单超时配置（单位：分钟） */
    /** 未付款订单自动取消时长（分钟） */
    public static final int ORDER_PAY_TIMEOUT_MINUTES = 15;
    /** 发货后自动确认收货超时（分钟）：7 天 = 10080 分钟 */
    public static final int ORDER_RECEIVE_TIMEOUT_MINUTES = 10080;

    /** 系统配置项Key */
    public static final String CFG_AUDIT_MODE = "audit_mode";
    public static final String CFG_TIP_ADDRESS = "tip_modify_address";
    public static final String CFG_IMAGE_BASE = "image_base_url";
    public static final String CFG_CUSTOM_PRICE = "custom_cue_price";
    public static final String CFG_TIP_FEE = "tip_modify_fee";
    public static final String CFG_TIP_HANDFEE = "tip_modify_handfee";
    public static final String CFG_CUSTOMER_SERVICE = "customer_service";
    public static final String CFG_ABOUT_US = "about_us";
}
