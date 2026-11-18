package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.common.Constants;
import com.billiard.entity.User;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.UserMapper;
import com.billiard.util.MD5Util;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务：账号密码登录 + 微信授权登录，Token存Redis
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StringRedisTemplate redis;
    @Autowired
    private SensitiveWordService sensitiveWordService;

    @Value("${upload.dir:./uploads/billiard_img}")
    private String uploadDir;
    
    private static final Random RANDOM = new Random();
    // 头像文件名列表（11个）
    private static final String[] AVATARS = {
        "billiard_img/avatars/1.jpg",
        "billiard_img/avatars/2.jpg",
        "billiard_img/avatars/3.jpg",
        "billiard_img/avatars/4.jpg",
        "billiard_img/avatars/5.jpg",
        "billiard_img/avatars/6.jpg",
        "billiard_img/avatars/7.jpg",
        "billiard_img/avatars/8.jpg",
        "billiard_img/avatars/9.jpg",
        "billiard_img/avatars/10.jpg",
        "billiard_img/avatars/11.jpg"
    };
    
    /** 获取随机头像 */
    private String getRandomAvatar() {
        return AVATARS[RANDOM.nextInt(AVATARS.length)];
    }

    /** 账号密码注册 */
    public Map<String, Object> register(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new BusinessException("用户名或密码不能为空");
        }
        User exist = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(MD5Util.encryptPassword(password));
        u.setNickname(username);
        u.setAvatar(getRandomAvatar());
        u.setGender(0);
        u.setStatus(1);
        userMapper.insert(u);
        return login(username, password);
    }

    /** 账号密码登录（兼容老 MD5，校验成功自动升级为 BCrypt） */
    public Map<String, Object> login(String username, String password) {
        User u = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        MD5Util.VerifyResult vr = u != null
                ? MD5Util.verifyPassword(password, u.getPassword())
                : new MD5Util.VerifyResult(false, false);
        if (!vr.matched) {
            throw new BusinessException("用户名或密码错误");
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (vr.needUpgrade) {
            u.setPassword(MD5Util.encryptPassword(password));
            userMapper.updateById(u);
        }
        return buildLoginResult(u);
    }

    /** 微信一键授权登录（自动注册/同步头像昵称） */
    public Map<String, Object> wxLogin(String openid, String nickname, String avatar) {
        if (openid == null || openid.isEmpty()) {
            throw new BusinessException("openid不能为空");
        }
        User u = userMapper.selectOne(new QueryWrapper<User>().eq("openid", openid));
        if (u == null) {
            u = new User();
            u.setOpenid(openid);
            u.setNickname(nickname != null ? nickname : "微信用户");
            u.setAvatar(avatar != null ? avatar : getRandomAvatar());
            u.setGender(0);
            u.setStatus(1);
            userMapper.insert(u);
        } else {
            // 同步最新头像昵称
            boolean dirty = false;
            if (nickname != null && !nickname.isEmpty() && !nickname.equals(u.getNickname())) {
                u.setNickname(nickname);
                dirty = true;
            }
            if (avatar != null && !avatar.isEmpty() && !avatar.equals(u.getAvatar())) {
                u.setAvatar(avatar);
                dirty = true;
            }
            if (dirty) {
                userMapper.updateById(u);
            }
        }
        if (u.getStatus() != null && u.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        return buildLoginResult(u);
    }

    private Map<String, Object> buildLoginResult(User u) {
        String token = IdUtil.fastSimpleUUID();
        redis.opsForValue().set(Constants.REDIS_USER_TOKEN + token, u.getId().toString(),
                Constants.TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", u.getId());
        result.put("nickname", u.getNickname());
        result.put("avatar", u.getAvatar());
        return result;
    }

    public void logout(String token) {
        if (token != null) {
            redis.delete(Constants.REDIS_USER_TOKEN + token);
        }
    }

    public User getById(Long id) {
        return userMapper.selectById(id);
    }

    /** 修改昵称（含敏感词校验） */
    public User updateNickname(Long userId, String nickname) {
        String bad = sensitiveWordService.check(nickname);
        if (bad != null) {
            throw new BusinessException("昵称含违规词：" + bad);
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setNickname(nickname);
        userMapper.updateById(u);
        return u;
    }

    public void updateAvatar(Long userId, String avatar) {
        User u = userMapper.selectById(userId);
        if (u != null) {
            u.setAvatar(avatar);
            userMapper.updateById(u);
        }
    }

    /** 允许上传的图片扩展名白名单（与 UploadController 保持一致） */
    private static final java.util.Set<String> ALLOWED_EXT = new java.util.HashSet<>(
            java.util.Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"));

    /** 上传头像文件并更新用户头像 */
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择头像文件");
        }
        // 校验文件类型：扩展名白名单 + MIME 必须是 image/*
        String originalFilename = file.getOriginalFilename();
        String ext = extractExt(originalFilename);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg/jpeg/png/gif/webp/bmp 图片格式");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("文件类型无效");
        }
        try {
            // 保存到 {项目根}/uploads/billiard_img/avatars/，不会被 mvn clean 删除
            File rootDir = new File(uploadDir);
            File avatarDir = new File(rootDir.getAbsolutePath() + File.separator + "avatars");
            if (!avatarDir.exists()) {
                avatarDir.mkdirs();
            }
            // 生成唯一文件名
            String newFilename = "u_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            // 保存文件
            File dest = new File(avatarDir.getAbsolutePath() + File.separator + newFilename);
            file.transferTo(dest);
            // 更新用户头像路径
            String avatarPath = "billiard_img/avatars/" + newFilename;
            updateAvatar(userId, avatarPath);
            return avatarPath;
        } catch (IOException e) {
            throw new BusinessException("头像上传失败：" + e.getMessage());
        }
    }

    /** 安全提取扩展名（小写，含.；无扩展名返回空串） */
    private static String extractExt(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf(".");
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx).toLowerCase();
    }

    /** 修改手机号 */
    public User updatePhone(Long userId, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            throw new BusinessException("手机号不能为空");
        }
        // 简单校验手机号格式
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException("手机号格式不正确");
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        u.setPhone(phone);
        userMapper.updateById(u);
        return u;
    }
}
