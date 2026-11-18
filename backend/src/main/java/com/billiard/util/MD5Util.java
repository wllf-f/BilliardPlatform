package com.billiard.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具：从 MD5 升级为 BCrypt
 * - 新密码直接用 BCrypt 加密（自适应强度，抗彩虹表/暴力破解）
 * - 登录校验时做兼容：先试 BCrypt，失败再回退 MD5（老用户）；校验通过自动升级哈希
 * - md5() 方法保留，仅供非密码场景（如生成文件指纹、订单号辅助哈希）使用
 */
public class MD5Util {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    /** 非密码场景的 MD5（文件指纹等），密码禁止直接用此方法 */
    public static String md5(String text) {
        return cn.hutool.crypto.digest.DigestUtil.md5Hex(text);
    }

    /** 加密密码：统一用 BCrypt */
    public static String encryptPassword(String plainPassword) {
        return BCRYPT.encode(plainPassword);
    }

    /**
     * 校验明文与数据库密文是否匹配（兼容老 MD5 哈希）
     * @return Result 对象包含：是否匹配、是否需要升级哈希
     */
    public static VerifyResult verifyPassword(String plain, String encoded) {
        if (plain == null || encoded == null) {
            return new VerifyResult(false, false);
        }
        // 1) 优先 BCrypt：BCryptPasswordEncoder.matches 自带前缀判断，非 BCrypt 格式会直接返回 false
        try {
            if (BCRYPT.matches(plain, encoded)) {
                return new VerifyResult(true, false); // 已是 BCrypt，无需升级
            }
        } catch (Exception ignored) {
            // 非法格式回退 MD5
        }
        // 2) 回退 MD5（老用户）：匹配成功则标记需要升级
        if (md5(plain).equals(encoded)) {
            return new VerifyResult(true, true);
        }
        return new VerifyResult(false, false);
    }

    /** 旧方法兼容：保留签名，内部委托给 verifyPassword（仅返回是否匹配） */
    public static boolean verify(String plain, String md5OrBcrypt) {
        return verifyPassword(plain, md5OrBcrypt).matched;
    }

    /** 密码校验结果 */
    public static class VerifyResult {
        public final boolean matched;
        public final boolean needUpgrade; // true 表示当前是老 MD5，需升级为 BCrypt

        public VerifyResult(boolean matched, boolean needUpgrade) {
            this.matched = matched;
            this.needUpgrade = needUpgrade;
        }
    }
}
