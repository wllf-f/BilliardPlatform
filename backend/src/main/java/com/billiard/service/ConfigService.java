package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.common.Constants;
import com.billiard.entity.SysConfig;
import com.billiard.mapper.SysConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统配置服务：DB为持久源，Redis Hash缓存配置项（审核开关、邮寄地址等）
 */
@Service
public class ConfigService {

    @Autowired
    private SysConfigMapper sysConfigMapper;
    @Autowired
    private StringRedisTemplate redis;

    /** 启动时加载全部配置到Redis缓存 */
    @PostConstruct
    public void loadCache() {
        List<SysConfig> list = sysConfigMapper.selectList(null);
        Map<String, String> map = new HashMap<>();
        for (SysConfig c : list) {
            if (c.getConfigKey() != null && c.getConfigValue() != null) {
                map.put(c.getConfigKey(), c.getConfigValue());
            }
        }
        if (!map.isEmpty()) {
            redis.opsForHash().putAll(Constants.REDIS_SYS_CONFIG, map);
            redis.expire(Constants.REDIS_SYS_CONFIG, 30, TimeUnit.DAYS);
        }
    }

    /** 获取配置（优先读缓存，回源DB） */
    public String get(String key) {
        Object v = redis.opsForHash().get(Constants.REDIS_SYS_CONFIG, key);
        if (v != null) {
            return v.toString();
        }
        // 回源
        SysConfig c = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", key));
        if (c != null) {
            redis.opsForHash().put(Constants.REDIS_SYS_CONFIG, key, c.getConfigValue());
            return c.getConfigValue();
        }
        return null;
    }

    /** 获取全部配置 */
    public Map<Object, Object> getAll() {
        Map<Object, Object> entries = redis.opsForHash().entries(Constants.REDIS_SYS_CONFIG);
        if (entries.isEmpty()) {
            loadCache();
            entries = redis.opsForHash().entries(Constants.REDIS_SYS_CONFIG);
        }
        return entries;
    }

    /** 更新配置（DB + 缓存） */
    public void update(String key, String value) {
        SysConfig c = sysConfigMapper.selectOne(new QueryWrapper<SysConfig>().eq("config_key", key));
        if (c != null) {
            c.setConfigValue(value);
            sysConfigMapper.updateById(c);
        } else {
            c = new SysConfig();
            c.setConfigKey(key);
            c.setConfigValue(value);
            sysConfigMapper.insert(c);
        }
        redis.opsForHash().put(Constants.REDIS_SYS_CONFIG, key, value);
    }
}
