package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.billiard.entity.UserAddress;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务（全平台共用：普通商品/定制/改装）
 */
@Service
public class AddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    public List<UserAddress> list(Long userId) {
        return userAddressMapper.selectList(new QueryWrapper<UserAddress>()
                .eq("user_id", userId).orderByDesc("is_default").orderByDesc("id"));
    }

    public UserAddress getDefault(Long userId) {
        return userAddressMapper.selectOne(new QueryWrapper<UserAddress>()
                .eq("user_id", userId).eq("is_default", 1));
    }

    public UserAddress getById(Long id, Long userId) {
        UserAddress addr = userAddressMapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        return addr;
    }

    @Transactional
    public UserAddress add(UserAddress addr) {
        if (addr.getIsDefault() != null && addr.getIsDefault() == 1) {
            clearDefault(addr.getUserId());
        }
        addr.setId(null);
        userAddressMapper.insert(addr);
        return addr;
    }

    @Transactional
    public UserAddress update(UserAddress addr, Long userId) {
        UserAddress exist = getById(addr.getId(), userId);
        if (addr.getIsDefault() != null && addr.getIsDefault() == 1) {
            clearDefault(userId);
        }
        addr.setUserId(userId);
        userAddressMapper.updateById(addr);
        return exist;
    }

    @Transactional
    public void delete(Long id, Long userId) {
        getById(id, userId);
        userAddressMapper.deleteById(id);
    }

    @Transactional
    public void setDefault(Long id, Long userId) {
        getById(id, userId);
        clearDefault(userId);
        userAddressMapper.update(null, new UpdateWrapper<UserAddress>()
                .eq("id", id).set("is_default", 1));
    }

    private void clearDefault(Long userId) {
        userAddressMapper.update(null, new UpdateWrapper<UserAddress>()
                .eq("user_id", userId).eq("is_default", 1).set("is_default", 0));
    }
}
