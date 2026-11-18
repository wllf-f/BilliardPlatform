package com.billiard.controller;

import com.billiard.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 * 上传文件统一保存到项目根目录下的 uploads/billiard_img/，不会被 mvn clean 删除
 */
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    @Value("${upload.dir:./uploads/billiard_img}")
    private String uploadDir;

    /** 允许上传的图片扩展名白名单 */
    private static final java.util.Set<String> ALLOWED_EXT = new java.util.HashSet<>(
            java.util.Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"));

    @PostMapping("/image")
    public Result<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        // 校验文件类型：扩展名白名单 + MIME 必须是 image/*
        String originalFilename = file.getOriginalFilename();
        String ext = extractExt(originalFilename);
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error("仅支持 jpg/jpeg/png/gif/webp/bmp 图片格式");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("文件类型无效");
        }

        try {
            // 根目录：{项目根}/uploads/billiard_img
            File rootDir = new File(uploadDir);
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }

            // 按日期创建目录，避免文件夹过大
            String dateDir = new SimpleDateFormat("yyyyMM").format(new Date());
            File dir = new File(rootDir.getAbsolutePath() + File.separator + "post" + File.separator + dateDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名（保留原扩展名）
            String newFilename = UUID.randomUUID().toString() + ext;

            // 保存文件
            File dest = new File(dir.getAbsolutePath() + File.separator + newFilename);
            file.transferTo(dest);

            // 返回相对路径（和 /billiard_img/** 静态资源映射配合）
            Map<String, String> res = new HashMap<>();
            res.put("url", "billiard_img/post/" + dateDir + "/" + newFilename);
            return Result.success(res);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }

    /** 安全提取扩展名（小写，含.；无扩展名返回空串） */
    private static String extractExt(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf(".");
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx).toLowerCase();
    }
}
