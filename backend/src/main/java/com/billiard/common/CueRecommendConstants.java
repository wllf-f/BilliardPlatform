package com.billiard.common;

/**
 * 台球杆推荐引擎枚举常量
 * 所有枚举文本统一存放，禁止硬编码字符串
 */
public class CueRecommendConstants {

    // ========== 杆型枚举 ==========
    public static final String ROD_SMALL = "小头杆";
    public static final String ROD_LARGE = "大头杆";
    public static final String ROD_POTS = "波茨杆";
    public static final String ROD_SNOOKER = "斯诺克杆";

    // ========== 硬度枚举 ==========
    public static final String HARD_SOFT = "偏软";
    public static final String HARD_MEDIUM = "均衡";
    public static final String HARD_HARD = "偏硬";

    // ========== 材质枚举 ==========
    public static final String MAT_WOOD = "实木";
    public static final String MAT_TECH = "科技前支";

    // ========== 价位枚举 ==========
    public static final String PRICE_LOW = "0-300";
    public static final String PRICE_MID = "300-1000";
    public static final String PRICE_HIGH = "1000-5000";
    public static final String PRICE_LUXURY = "5000以上";

    // ========== 打法枚举 ==========
    public static final String PLAY_DEFENSE = "准度防守";
    public static final String PLAY_OFFENSE = "追分走位";
    public static final String PLAY_BALANCE = "均衡打法";
    public static final String PLAY_SNOOKER = "斯诺克玩法";

    // ========== 发力枚举 ==========
    public static final String POWER_LIGHT = "轻推发力";
    public static final String POWER_MEDIUM = "均匀发力";
    public static final String POWER_STRONG = "大力发力";

    // ========== 用户水平枚举 ==========
    public static final String LEVEL_BEGINNER = "新手";
    public static final String LEVEL_INTERMEDIATE = "中级";
    public static final String LEVEL_ADVANCED = "高手";

    // ========== 偏好倾向枚举 ==========
    public static final String LIKE_ACCURACY = "准度优先";
    public static final String LIKE_POSITION = "走位优先";
    public static final String LIKE_BALANCE = "攻守均衡";

    // ========== 匹配等级 ==========
    public static final int MATCH_FULL = 1;        // 完全匹配（杆型+硬度+预算）
    public static final int MATCH_RELAXED = 2;     // 放宽硬度（杆型+预算）
    public static final int MATCH_BUDGET_ONLY = 3; // 仅预算匹配
    public static final int MATCH_RANDOM = 4;      // 最终兜底（不限价位随机，保证必有结果）

    // ========== 匹配等级中文标签（用于前端展示区分度）==========
    public static String getMatchLabel(int level) {
        switch (level) {
            case MATCH_FULL: return "完全匹配";
            case MATCH_RELAXED: return "放宽硬度";
            case MATCH_BUDGET_ONLY: return "仅价格匹配";
            case MATCH_RANDOM: return "兜底推荐";
            default: return "推荐结果";
        }
    }
}
