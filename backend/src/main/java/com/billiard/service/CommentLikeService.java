package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.billiard.entity.Comment;
import com.billiard.entity.PostLike;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.CommentMapper;
import com.billiard.mapper.PostLikeMapper;
import com.billiard.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论与点赞服务
 */
@Service
public class CommentLikeService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private SensitiveWordService sensitiveWordService;

    public List<Map<String, Object>> listByPost(Long postId) {
        return commentMapper.selectByPostId(postId);
    }

    public Comment create(Long userId, Long postId, String content) {
        String bad = sensitiveWordService.check(content);
        if (bad != null) {
            throw new BusinessException("评论含违规词：" + bad);
        }
        Comment c = new Comment();
        c.setUserId(userId);
        c.setPostId(postId);
        c.setContent(content);
        commentMapper.insert(c);
        // 同步帖子评论数
        postMapper.incrCommentCount(postId);
        return c;
    }

    public void delete(Long id) {
        // 先查出 postId
        Comment c = commentMapper.selectById(id);
        if (c != null) {
            commentMapper.deleteById(id);
            postMapper.decrCommentCount(c.getPostId());
        }
    }

    /** 点赞/取消点赞（toggle）——用原子 ±1 替代全表 COUNT 回写，提升性能 */
    public boolean toggleLike(Long userId, Long postId) {
        PostLike exist = postLikeMapper.selectOne(new QueryWrapper<PostLike>()
                .eq("user_id", userId).eq("post_id", postId));
        if (exist != null) {
            postLikeMapper.deleteById(exist.getId());
            postMapper.decrLikeCount(postId);
            return false;
        }
        PostLike like = new PostLike();
        like.setUserId(userId);
        like.setPostId(postId);
        postLikeMapper.insert(like);
        postMapper.incrLikeCount(postId);
        return true;
    }

    public long likeCount(Long postId) {
        return postLikeMapper.selectCount(new QueryWrapper<PostLike>().eq("post_id", postId));
    }

    public boolean isLiked(Long userId, Long postId) {
        return postLikeMapper.selectCount(new QueryWrapper<PostLike>()
                .eq("user_id", userId).eq("post_id", postId)) > 0;
    }

    /** 批量查询当前用户对多个帖子的点赞状态 */
    public Map<Long, Boolean> batchIsLiked(Long userId, List<Long> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<PostLike> likes = postLikeMapper.selectList(new QueryWrapper<PostLike>()
                .eq("user_id", userId).in("post_id", postIds));
        Set<Long> likedIds = likes.stream().map(PostLike::getPostId).collect(Collectors.toSet());
        Map<Long, Boolean> result = new HashMap<>();
        for (Long id : postIds) {
            result.put(id, likedIds.contains(id));
        }
        return result;
    }
}
