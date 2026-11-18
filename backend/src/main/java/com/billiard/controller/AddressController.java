package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.entity.UserAddress;
import com.billiard.service.AddressService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端：收货地址（全平台共用）
 */
@RestController
@RequestMapping("/api/address")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/list")
    public Result<List<UserAddress>> list() {
        return Result.success(addressService.list(UserContext.getCurrentUser()));
    }

    @GetMapping("/default")
    public Result<UserAddress> getDefault() {
        return Result.success(addressService.getDefault(UserContext.getCurrentUser()));
    }

    @PostMapping("/add")
    public Result<UserAddress> add(@RequestBody UserAddress addr) {
        addr.setUserId(UserContext.getCurrentUser());
        return Result.success(addressService.add(addr));
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody UserAddress addr) {
        addressService.update(addr, UserContext.getCurrentUser());
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        addressService.delete(id, UserContext.getCurrentUser());
        return Result.success();
    }

    @PostMapping("/setDefault/{id}")
    public Result<?> setDefault(@PathVariable Long id) {
        addressService.setDefault(id, UserContext.getCurrentUser());
        return Result.success();
    }
}
