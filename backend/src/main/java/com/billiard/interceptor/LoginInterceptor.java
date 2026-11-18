package com.billiard.interceptor;

import com.billiard.common.Constants;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 用户登录拦截器：校验Header中的token是否存在于Redis
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            return false;
        }
        String key = Constants.REDIS_USER_TOKEN + token;
        String userId = redis.opsForValue().get(key);
        if (userId == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"msg\":\"登录已过期，请重新登录\"}");
            return false;
        }
        // 续期
        redis.expire(key, Constants.TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        UserContext.setCurrentUser(Long.valueOf(userId));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clearUser();
    }
}
