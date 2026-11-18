package com.billiard.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.common.Constants;
import com.billiard.entity.Admin;
import com.billiard.entity.User;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.AdminMapper;
import com.billiard.mapper.UserMapper;
import com.billiard.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理员服务：登录、用户管理
 */
@Service
public class AdminService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate redis;

    public Map<String, Object> login(String username, String password) {
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>().eq("username", username));
        MD5Util.VerifyResult vr = admin != null
                ? MD5Util.verifyPassword(password, admin.getPassword())
                : new MD5Util.VerifyResult(false, false);
        if (!vr.matched) {
            throw new BusinessException("用户名或密码错误");
        }
        if (admin.getStatus() != null && admin.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        // 老 MD5 哈希自动升级为 BCrypt，下一次登录直接走 BCrypt
        if (vr.needUpgrade) {
            admin.setPassword(MD5Util.encryptPassword(password));
            adminMapper.updateById(admin);
        }
        String token = IdUtil.fastSimpleUUID();
        redis.opsForValue().set(Constants.REDIS_ADMIN_TOKEN + token, admin.getId().toString(),
                Constants.ADMIN_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("adminId", admin.getId());
        result.put("username", admin.getUsername());
        result.put("realName", admin.getRealName());
        result.put("avatar", admin.getAvatar());
        return result;
    }

    public void logout(String token) {
        if (token != null) {
            redis.delete(Constants.REDIS_ADMIN_TOKEN + token);
        }
    }

    public Admin getById(Long id) {
        return adminMapper.selectById(id);
    }

    /** 用户列表分页（显式排除 password 字段，避免哈希泄露） */
    public Map<String, Object> userPage(int page, int size, String keyword) {
        QueryWrapper<User> qw = new QueryWrapper<>();
        // 只查需要的字段，排除 password
        qw.select("id", "username", "nickname", "avatar", "phone", "openid", "gender", "status", "create_time", "update_time");
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("nickname", keyword).or().like("username", keyword).or().like("phone", keyword);
        }
        qw.orderByDesc("create_time");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.metadata.IPage<User> result = userMapper.selectPage(p, qw);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("records", result.getRecords());
        return map;
    }

    public void toggleUserStatus(Long userId, Integer status) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setStatus(status);
        userMapper.updateById(u);
    }

    /** 重置用户密码为默认值 123456（使用 BCrypt 加密） */
    public void resetUserPassword(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setPassword(MD5Util.encryptPassword("123456"));
        userMapper.updateById(u);
    }

    public List<User> allUsers() {
        // 排除 password 字段
        return userMapper.selectList(new QueryWrapper<User>()
                .select("id", "username", "nickname", "avatar", "phone", "openid", "gender", "status", "create_time", "update_time"));
    }

    // ==================== 管理员管理 ====================

    /** 管理员分页列表（显式排除 password 字段，避免哈希泄露） */
    public Map<String, Object> adminPage(int page, int size, String keyword) {
        QueryWrapper<Admin> qw = new QueryWrapper<>();
        qw.select("id", "username", "real_name", "avatar", "status", "create_time", "update_time");
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("username", keyword).or().like("real_name", keyword);
        }
        qw.orderByAsc("id");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Admin> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        com.baomidou.mybatisplus.core.metadata.IPage<Admin> result = adminMapper.selectPage(p, qw);
        Map<String, Object> map = new HashMap<>();
        map.put("total", result.getTotal());
        map.put("records", result.getRecords());
        return map;
    }

    /** 判断当前管理员是否为超级管理员（用户名 admin） */
    public boolean isSuperAdmin(Long adminId) {
        Admin a = adminMapper.selectById(adminId);
        return a != null && "admin".equals(a.getUsername());
    }

    /** 新增管理员（仅超级管理员可调用），密码使用 BCrypt 加密 */
    public void addAdmin(String username, String password, String realName) {
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("账号不能为空");
        }
        if (password == null || password.isEmpty()) {
            throw new BusinessException("密码不能为空");
        }
        String uname = username.trim();
        Long exists = adminMapper.selectCount(new QueryWrapper<Admin>().eq("username", uname));
        if (exists != null && exists > 0) {
            throw new BusinessException("账号已存在");
        }
        Admin a = new Admin();
        a.setUsername(uname);
        a.setPassword(MD5Util.encryptPassword(password));
        a.setRealName(realName == null || realName.trim().isEmpty() ? uname : realName.trim());
        a.setStatus(1);
        adminMapper.insert(a);
    }

    /** 禁用/启用管理员（仅超级管理员可调用） */
    public void changeAdminStatus(Long adminId, Integer status) {
        Admin a = adminMapper.selectById(adminId);
        if (a == null) {
            throw new BusinessException("管理员不存在");
        }
        if ("admin".equals(a.getUsername())) {
            throw new BusinessException("不能修改超级管理员状态");
        }
        a.setStatus(status);
        adminMapper.updateById(a);
    }
}
