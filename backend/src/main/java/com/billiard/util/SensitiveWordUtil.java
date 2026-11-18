package com.billiard.util;

import java.util.*;

/**
 * 敏感词检测工具类，采用DFA（确定有穷自动机）算法实现高效匹配
 * <p>
 * 思路：将敏感词库构建成一棵前缀树（字典树），然后对目标文本逐字符在树上进行状态转移，
 * 命中结束节点即视为包含敏感词。匹配复杂度与文本长度线性相关，远优于正则逐词匹配。
 */
public class SensitiveWordUtil {

    /** 敏感词前缀树根节点 */
    private static Map<String, Object> sensitiveWordMap = new HashMap<>();

    /** 结束标识 */
    private static final String IS_END = "isEnd";

    /** 初始化敏感词树 */
    @SuppressWarnings("unchecked")
    public synchronized static void initKeyWord(Set<String> keyWordSet) {
        Map<String, Object> newMap = new HashMap<>(keyWordSet.size());
        for (String key : keyWordSet) {
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            Map<String, Object> curNode = newMap;
            for (int i = 0; i < key.length(); i++) {
                String ch = String.valueOf(key.charAt(i));
                Map<String, Object> next = (Map<String, Object>) curNode.get(ch);
                if (next == null) {
                    next = new HashMap<>();
                    curNode.put(ch, next);
                }
                curNode = next;
                // 最后一个字符标记结束
                if (i == key.length() - 1) {
                    curNode.put(IS_END, "1");
                }
            }
        }
        sensitiveWordMap = newMap;
    }

    /**
     * 检查文本是否包含敏感词
     *
     * @return 命中的第一个敏感词，未命中返回null
     */
    @SuppressWarnings("unchecked")
    public static String containsSensitiveWord(String text) {
        if (text == null || text.isEmpty() || sensitiveWordMap.isEmpty()) {
            return null;
        }
        for (int i = 0; i < text.length(); i++) {
            int matchLen = checkSensitiveWord(text, i);
            if (matchLen > 0) {
                return text.substring(i, i + matchLen);
            }
        }
        return null;
    }

    /**
     * 从指定位置开始匹配，返回命中敏感词长度，未命中返回0
     */
    @SuppressWarnings("unchecked")
    private static int checkSensitiveWord(String text, int beginIndex) {
        Map<String, Object> curNode = sensitiveWordMap;
        int matchLen = 0;
        boolean flag = false;
        for (int i = beginIndex; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            curNode = (Map<String, Object>) curNode.get(ch);
            if (curNode == null) {
                break;
            }
            matchLen++;
            if ("1".equals(curNode.get(IS_END))) {
                flag = true;
                // 不break，继续找更长匹配（如"傻"与"傻逼"），但记录已命中
            }
        }
        return flag ? matchLen : 0;
    }

    /**
     * 敏感词替换为 *
     */
    public static String replaceSensitiveWord(String text) {
        if (text == null || text.isEmpty() || sensitiveWordMap.isEmpty()) {
            return text;
        }
        String result = text;
        for (int i = 0; i < result.length(); i++) {
            int matchLen = checkSensitiveWord(result, i);
            if (matchLen > 0) {
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < matchLen; j++) {
                    sb.append("*");
                }
                result = result.substring(0, i) + sb.toString() + result.substring(i + matchLen);
                i += matchLen - 1;
            }
        }
        return result;
    }
}
