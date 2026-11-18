package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台：用户管理
 */
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword) {
        return Result.success(adminService.userPage(page, size, keyword));
    }

    @PostMapping("/status/{id}/{status}")
    public Result<?> toggleStatus(@PathVariable Long id, @PathVariable Integer status) {
        adminService.toggleUserStatus(id, status);
        return Result.success();
    }

    /** 重置用户密码为 123456 */
    @PostMapping("/reset-password/{id}")
    public Result<?> resetPassword(@PathVariable Long id) {
        adminService.resetUserPassword(id);
        return Result.success("密码已重置为 123456");
    }
}
