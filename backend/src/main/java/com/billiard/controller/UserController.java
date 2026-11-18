package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.entity.User;
import com.billiard.service.UserService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 用户端：登录/注册/个人信息
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        return Result.success(userService.register(body.get("username"), body.get("password")));
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        return Result.success(userService.login(body.get("username"), body.get("password")));
    }

    @PostMapping("/wxLogin")
    public Result<?> wxLogin(@RequestBody Map<String, String> body) {
        return Result.success(userService.wxLogin(body.get("openid"), body.get("nickname"), body.get("avatar")));
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("token") String token) {
        userService.logout(token);
        return Result.success();
    }

    @GetMapping("/info")
    public Result<User> info() {
        return Result.success(userService.getById(UserContext.getCurrentUser()));
    }

    @PostMapping("/nickname")
    public Result<User> updateNickname(@RequestBody Map<String, String> body) {
        return Result.success(userService.updateNickname(UserContext.getCurrentUser(), body.get("nickname")));
    }

    /** 上传头像（文件上传方式） */
    @PostMapping("/avatar")
    public Result<?> updateAvatar(@RequestParam("file") MultipartFile file) {
        String avatarPath = userService.uploadAvatar(UserContext.getCurrentUser(), file);
        return Result.success(avatarPath);
    }

    /** 修改手机号 */
    @PostMapping("/phone")
    public Result<User> updatePhone(@RequestBody Map<String, String> body) {
        return Result.success(userService.updatePhone(UserContext.getCurrentUser(), body.get("phone")));
    }
}
