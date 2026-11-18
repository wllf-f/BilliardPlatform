package com.billiard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.billiard.entity.Post;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

public interface PostMapper extends BaseMapper<Post> {
    /** 帖子列表（关联用户昵称头像） */
    IPage<Map<String, Object>> selectPostPage(IPage<Map<String, Object>> page,
                                              @Param("status") Integer status,
                                              @Param("userId") Long userId,
                                              @Param("keyword") String keyword);
    /** 帖子列表（支持包含当前用户的隐藏帖子） */
    IPage<Map<String, Object>> selectPostPageWithCurrentUser(IPage<Map<String, Object>> page,
                                                              @Param("status") Integer status,
                                                              @Param("userId") Long userId,
                                                              @Param("keyword") String keyword,
                                                              @Param("currentUserId") Long currentUserId);
    /** 更新浏览量 */
    int updateViewCount(@Param("id") Long id, @Param("count") int count);
    /** 评论数+1 */
    int incrCommentCount(@Param("id") Long id);
    /** 评论数-1 */
    int decrCommentCount(@Param("id") Long id);
    /** 更新点赞数（设置为指定值，保留兼容） */
    int updateLikeCount(@Param("id") Long id, @Param("count") int count);
    /** 点赞数+1（原子操作，替代全表count回写） */
    int incrLikeCount(@Param("id") Long id);
    /** 点赞数-1（不低于0，原子操作） */
    int decrLikeCount(@Param("id") Long id);
    /** 帖子详情（关联用户昵称头像） */
    Map<String, Object> selectDetailMap(@Param("id") Long id);
}
