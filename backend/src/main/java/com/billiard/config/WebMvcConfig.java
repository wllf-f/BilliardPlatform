package com.billiard.config;

import com.billiard.interceptor.AdminInterceptor;
import com.billiard.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Web MVC配置：静态资源映射 + 拦截器注册
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;
    @Autowired
    private AdminInterceptor adminInterceptor;

    @Value("${upload.dir:./uploads/billiard_img}")
    private String uploadDir;

    /**
     * 静态资源映射：
     * 1. classpath:/static/billiard_img/ — 打包时内置的原始图片（商品图、初始头像等）
     * 2. file:uploads/billiard_img/ — 用户上传的图片（帖子图、新头像等），不会被 mvn clean 删除
     * 二者都映射到同一个 /billiard_img/** 路径，Spring 会按顺序查找
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把上传目录转为绝对路径，保证 jar 包运行和 IDE 运行都能正确找到
        String uploadAbsPath = new File(uploadDir).getAbsolutePath() + File.separator;
        registry.addResourceHandler("/billiard_img/**")
                .addResourceLocations(
                        "file:" + uploadAbsPath,
                        "classpath:/static/billiard_img/"
                );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户端需登录接口
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns(
                        "/api/order/**",
                        "/api/collection/**",
                        "/api/address/**",
                        "/api/custom/**",
                        "/api/tipModify/**",
                        "/api/user/info",
                        "/api/user/avatar",
                        "/api/user/phone",
                        "/api/user/nickname",
                        "/api/post/mine",
                        "/api/post/create",
                        "/api/post/like/**",
                        "/api/post/delete/**",
                        "/api/post/toggle-hidden/**",
                        "/api/comment/create",
                        "/api/upload/**"
                );
        // 管理端：拦截 /admin/**（含 HTML 页面），登录页和静态资源除外
        // 页面访问通过 Cookie 中的 adminToken 校验，未登录重定向到登录页；AJAX 走 token header
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns(
                        // 登录接口
                        "/admin/login",
                        // 登录页（其余 HTML 页面由后端拦截器校验，未登录重定向到登录页）
                        "/admin/login.html",
                        // 静态资源（CSS/JS/图片）
                        "/admin/css/**",
                        "/admin/js/**",
                        "/admin/images/**",
                        "/admin/fonts/**"
                );
    }
}
