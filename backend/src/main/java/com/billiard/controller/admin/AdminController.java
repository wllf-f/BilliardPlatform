package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.service.AdminService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台：管理员登录、管理员管理
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        return Result.success(adminService.login(body.get("username"), body.get("password")));
    }

    @PostMapping("/logout")
    public Result<?> logout(@RequestHeader("token") String token) {
        adminService.logout(token);
        return Result.success();
    }

    /** 管理员分页列表 */
    @GetMapping("/admins")
    public Result<?> adminList(@RequestParam(defaultValue = "1") String page,
                               @RequestParam(defaultValue = "10") String size,
                               @RequestParam(required = false) String keyword) {
        int p = toIntOrNull(page, 1);
        int s = toIntOrNull(size, 10);
        return Result.success(adminService.adminPage(p, s, keyword));
    }

    /** 新增管理员（仅超级管理员 admin 可操作） */
    @PostMapping("/admins")
    public Result<?> addAdmin(@RequestBody Map<String, String> body) {
        if (!requireSuperAdmin()) {
            return Result.error("无权限：仅超级管理员可新增管理员");
        }
        adminService.addAdmin(body.get("username"), body.get("password"), body.get("realName"));
        return Result.success();
    }

    /** 禁用/启用管理员（仅超级管理员 admin 可操作） */
    @PostMapping("/adminStatus/{id}/{status}")
    public Result<?> changeAdminStatus(@PathVariable String id, @PathVariable String status) {
        if (!requireSuperAdmin()) {
            return Result.error("无权限：仅超级管理员可操作");
        }
        Long adminId = toLongOrNull(id);
        int st = toIntOrNull(status, -1);
        if (adminId == null || (st != 0 && st != 1)) {
            return Result.error("参数错误");
        }
        // 不允许禁用超级管理员自身
        if (Long.valueOf(1).equals(adminId)) {
            return Result.error("不能禁用超级管理员");
        }
        adminService.changeAdminStatus(adminId, st);
        return Result.success();
    }

    /** 校验当前登录管理员是否为超级管理员 */
    private boolean requireSuperAdmin() {
        Long currentAdminId = UserContext.getCurrentAdmin();
        return currentAdminId != null && adminService.isSuperAdmin(currentAdminId);
    }

    /** 安全转换 int，非法或空时返回默认值 */
    private int toIntOrNull(String val, int def) {
        if (val == null || val.trim().isEmpty()) return def;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** 安全转换 long，非法或空时返回 null */
    private Long toLongOrNull(String val) {
        if (val == null || val.trim().isEmpty()) return null;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
