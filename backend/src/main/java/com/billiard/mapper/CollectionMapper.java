package com.billiard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.billiard.entity.Collection;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

public interface CollectionMapper extends BaseMapper<Collection> {
    /** 我的收藏列表（关联商品与比价） */
    IPage<Map<String, Object>> selectCollectionPage(IPage<Map<String, Object>> page,
                                                     @Param("userId") Long userId,
                                                     @Param("baseUrl") String baseUrl);
}
