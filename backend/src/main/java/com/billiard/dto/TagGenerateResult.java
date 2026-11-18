package com.billiard.dto;

import lombok.Data;

/**
 * 标签生成结果DTO
 */
@Data
public class TagGenerateResult {
    private int total;          // 扫描商品总数
    private int updated;        // 成功打标数量
    private int skipped;        // 跳过数量（非球杆商品）
    private String message;     // 执行结果消息
}
