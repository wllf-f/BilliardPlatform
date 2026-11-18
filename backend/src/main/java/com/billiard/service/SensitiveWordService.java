package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.entity.SensitiveWord;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.SensitiveWordMapper;
import com.billiard.util.SensitiveWordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 敏感词服务：启动时将词库载入DFA前缀树；增删后实时刷新
 */
@Service
public class SensitiveWordService {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    /** 启动时初始化DFA敏感词树 */
    @PostConstruct
    public void init() {
        reload();
    }

    /** 重新加载词库到DFA树 */
    public void reload() {
        List<SensitiveWord> list = sensitiveWordMapper.selectList(null);
        Set<String> set = new HashSet<>();
        for (SensitiveWord w : list) {
            if (w.getWord() != null && !w.getWord().trim().isEmpty()) {
                set.add(w.getWord().trim());
            }
        }
        SensitiveWordUtil.initKeyWord(set);
    }

    public List<SensitiveWord> list(String category) {
        QueryWrapper<SensitiveWord> qw = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            qw.eq("category", category);
        }
        qw.orderByDesc("id");
        return sensitiveWordMapper.selectList(qw);
    }

    public void add(String word, String category) {
        if (word == null || word.trim().isEmpty()) {
            throw new BusinessException("敏感词不能为空");
        }
        String trimmed = word.trim();
        if (trimmed.length() > 50) {
            throw new BusinessException("敏感词长度不能超过50个字符");
        }
        SensitiveWord w = new SensitiveWord();
        w.setWord(trimmed);
        w.setCategory(category);
        sensitiveWordMapper.insert(w);
        reload();
    }

    public void delete(Long id) {
        sensitiveWordMapper.deleteById(id);
        reload();
    }

    /** 校验文本是否含敏感词，命中返回敏感词，否则null */
    public String check(String text) {
        return SensitiveWordUtil.containsSensitiveWord(text);
    }
}
