package com.billiard.interceptor;

import com.billiard.common.Constants;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 管理员登录拦截器
 */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isEmpty()) {
            // HTML 页面导航请求不会携带自定义 header，从 Cookie 读取 adminToken
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie c : cookies) {
                    if ("adminToken".equals(c.getName())) {
                        token = c.getValue();
                        break;
                    }
                }
            }
        }
        // 是 HTML 页面请求（非 AJAX）且未登录 → 重定向到登录页
        boolean isPageRequest = uri.endsWith(".html") || uri.endsWith("/") || uri.matches(".*/admin/[^/.?]*$");
        if ((token == null || token.isEmpty()) && isPageRequest) {
            String ctx = request.getContextPath();
            response.sendRedirect(ctx + "/admin/login.html");
            return false;
        }
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return false;
        }
        String key = Constants.REDIS_ADMIN_TOKEN + token;
        String adminId = redis.opsForValue().get(key);
        if (adminId == null) {
            // token 过期时同样处理：页面请求重定向，AJAX 返回 JSON
            if (isPageRequest) {
                String ctx = request.getContextPath();
                response.sendRedirect(ctx + "/admin/login.html");
                return false;
            }
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"登录已过期，请重新登录\"}");
            return false;
        }
        redis.expire(key, Constants.ADMIN_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        UserContext.setCurrentAdmin(Long.valueOf(adminId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clearAdmin();
    }
}
