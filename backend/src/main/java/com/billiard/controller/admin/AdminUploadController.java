package com.billiard.controller.admin;

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
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 管理后台文件上传控制器
 *
 * 统一图片存储目录规划（项目根目录/uploads/billiard_img/，不会被 mvn clean 删除）：
 *   ├── post/{yyyyMM}/       帖子图片（用户端 /api/upload/image 上传）
 *   ├── avatars/             用户头像（/api/user/avatar 上传）
 *   └── product/{批次目录}/  商品图片（本控制器上传，按 pic1.jpg/pic2.jpg... 命名）
 *
 * 商品图按 picN.jpg 连续命名是为了兼容 ProductService.detail() 的详情图拼接逻辑：
 *   baseUrl + imgDir + "pic" + i + ".jpg"  (i = 1..picCount)
 */
@RestController
@RequestMapping("/admin/upload")
public class AdminUploadController {

    @Value("${upload.dir:./uploads/billiard_img}")
    private String uploadDir;

    /** 允许上传的图片扩展名白名单 */
    private static final java.util.Set<String> ALLOWED_EXT = new java.util.HashSet<>(
            java.util.Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"));

    /**
     * 商品图片批量上传
     * 接收多张图片，统一保存到 uploads/billiard_img/product/{批次目录}/ 下，
     * 按 pic1.jpg、pic2.jpg... 顺序命名，返回 imgDir / mainImg / picCount 供前端直接填充表单。
     *
     * @param files 图片文件数组（1~9张）
     * @return { imgDir, mainImg, picCount, urls }
     */
    @PostMapping("/product")
    public Result<?> uploadProduct(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error("请选择商品图片");
        }
        if (files.length > 9) {
            return Result.error("最多上传9张商品图片");
        }

        try {
            File rootDir = new File(uploadDir);
            if (!rootDir.exists()) {
                rootDir.mkdirs();
            }

            // 批次子目录：product/{yyyyMMddHHmmss}_{8位随机}/，保证同一商品的图在同一文件夹
            String batch = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                    + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            File dir = new File(rootDir.getAbsolutePath() + File.separator + "product" + File.separator + batch);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 逐个保存：pic1.jpg、pic2.jpg...
            // 统一用 .jpg 后缀：Spring 静态资源映射会根据文件实际内容设置 Content-Type，
            // 浏览器和小程序均能正常显示，且与后端 detailImgs 拼接逻辑保持一致。
            java.util.List<String> urls = new java.util.ArrayList<>();
            for (int i = 0; i < files.length; i++) {
                MultipartFile f = files[i];
                if (f == null || f.isEmpty()) continue;
                // 校验文件类型：扩展名白名单 + MIME 必须是 image/*
                String ofn = f.getOriginalFilename();
                String ext = ofn != null && ofn.contains(".") ? ofn.substring(ofn.lastIndexOf(".")).toLowerCase() : "";
                if (!ALLOWED_EXT.contains(ext)) {
                    throw new com.billiard.exception.BusinessException(
                            "第" + (i + 1) + "张图片格式不支持，仅支持 jpg/jpeg/png/gif/webp/bmp");
                }
                String ct = f.getContentType();
                if (ct == null || !ct.startsWith("image/")) {
                    throw new com.billiard.exception.BusinessException("第" + (i + 1) + "张文件类型无效");
                }
                String filename = "pic" + (i + 1) + ".jpg";
                File dest = new File(dir.getAbsolutePath() + File.separator + filename);
                f.transferTo(dest);
                urls.add("billiard_img/product/" + batch + "/" + filename);
            }

            if (urls.isEmpty()) {
                return Result.error("图片保存失败");
            }

            Map<String, Object> res = new HashMap<>();
            // imgDir 末尾带 /，与 imgUrl(imgDir, mainImg) 拼接逻辑一致
            res.put("imgDir", "product/" + batch + "/");
            res.put("mainImg", "pic1.jpg");
            res.put("picCount", urls.size());
            res.put("urls", urls);
            return Result.success(res);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("商品图片上传失败：" + e.getMessage());
        }
    }
}
