package com.billiard.util;

/**
 * 当前登录用户上下文（ThreadLocal存储）
 */
public class UserContext {
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_ADMIN = new ThreadLocal<>();

    public static void setCurrentUser(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static void setCurrentAdmin(Long adminId) {
        CURRENT_ADMIN.set(adminId);
    }

    public static Long getCurrentAdmin() {
        return CURRENT_ADMIN.get();
    }

    public static void clearUser() {
        CURRENT_USER.remove();
    }

    public static void clearAdmin() {
        CURRENT_ADMIN.remove();
    }
}
