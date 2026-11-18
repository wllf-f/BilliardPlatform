package com.billiard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.billiard.entity.Comment;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

public interface CommentMapper extends BaseMapper<Comment> {
    /** 评论列表（关联用户昵称头像） */
    List<Map<String, Object>> selectByPostId(@Param("postId") Long postId);
}
