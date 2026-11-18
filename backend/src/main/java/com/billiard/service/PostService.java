package com.billiard.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.billiard.common.Constants;
import com.billiard.entity.Comment;
import com.billiard.entity.Post;
import com.billiard.entity.PostLike;
import com.billiard.exception.BusinessException;
import com.billiard.mapper.CommentMapper;
import com.billiard.mapper.PostLikeMapper;
import com.billiard.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 帖子服务：发帖(含敏感词过滤+审核模式)、列表、详情、浏览量Redis计数
 */
@Service
public class PostService {

    @Autowired
    private PostMapper postMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private PostLikeMapper postLikeMapper;
    @Autowired
    private SensitiveWordService sensitiveWordService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private StringRedisTemplate redis;

    /** 发帖：自动审核模式判断 */
    public Post create(Long userId, String title, String content, String images) {
        String bad = sensitiveWordService.check(title);
        if (bad != null) {
            throw new BusinessException("标题含违规词：" + bad);
        }
        bad = sensitiveWordService.check(content);
        if (bad != null) {
            throw new BusinessException("正文含违规词：" + bad);
        }
        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setImages(images);
        // 审核模式：auto 自动通过，manual 待审核
        String mode = configService.get(Constants.CFG_AUDIT_MODE);
        if ("auto".equalsIgnoreCase(mode)) {
            post.setStatus(Constants.POST_PUBLISH);
        } else {
            post.setStatus(Constants.POST_AUDIT);
        }
        postMapper.insert(post);
        return post;
    }

    /** 已发布帖子列表（用户端），如果有当前用户ID，会包含该用户的隐藏帖子 */
    public IPage<Map<String, Object>> publishedPage(int page, int size, String keyword, Long currentUserId) {
        Page<Map<String, Object>> p = new Page<>(page, size);
        // 如果有当前用户，使用自定义查询逻辑
        return postMapper.selectPostPageWithCurrentUser(p, Constants.POST_PUBLISH, null, keyword, currentUserId);
    }

    /** 我的帖子（含所有状态） */
    public IPage<Map<String, Object>> myPosts(int page, int size, Long userId) {
        Page<Map<String, Object>> p = new Page<>(page, size);
        return postMapper.selectPostPage(p, null, userId, null);
    }

    /** 管理后台帖子列表（按状态筛选） */
    public IPage<Map<String, Object>> adminPage(int page, int size, Integer status) {
        Page<Map<String, Object>> p = new Page<>(page, size);
        return postMapper.selectPostPage(p, status, null, null);
    }

    public Post getById(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        return post;
    }

    /** 帖子详情（管理端用，无可见性限制） */
    public Map<String, Object> getDetailById(Long id) {
        Map<String, Object> post = postMapper.selectDetailMap(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        return post;
    }

    /**
     * 帖子详情（用户端用，加可见性校验）
     * - 已删除(4)：一律不可见 → 帖子不存在
     * - 已发布(1)：所有人可见
     * - 隐藏(3)/待审核(0)/驳回(2)：仅作者本人可见，其他用户返回「帖子不存在或已隐藏」
     */
    public Map<String, Object> getDetailById(Long id, Long currentUserId) {
        Map<String, Object> post = postMapper.selectDetailMap(id);
        if (post == null) {
            throw new BusinessException("帖子不存在");
        }
        Integer status = post.get("status") != null ? ((Number) post.get("status")).intValue() : null;
        Object userIdObj = post.get("userId");
        Long authorId = userIdObj != null ? Long.valueOf(userIdObj.toString()) : null;
        boolean isAuthor = currentUserId != null && authorId != null && currentUserId.equals(authorId);
        if (status != null) {
            if (status == Constants.POST_DELETED) {
                throw new BusinessException("帖子不存在");
            }
            if (status == Constants.POST_HIDDEN
                    || status == Constants.POST_AUDIT
                    || status == Constants.POST_REJECT) {
                if (!isAuthor) {
                    throw new BusinessException("帖子不存在或已隐藏");
                }
            }
        }
        return post;
    }

    /** 浏览量+1（Redis Hash计数 + 同步更新DB） */
    public long incrView(Long postId) {
        long v = redis.opsForHash().increment(Constants.REDIS_POST_VIEWS, postId.toString(), 1);
        // 同步到数据库，保证列表页也显示真实浏览量
        postMapper.updateViewCount(postId, (int) v);
        return v;
    }

    public long getView(Long postId) {
        Object v = redis.opsForHash().get(Constants.REDIS_POST_VIEWS, postId.toString());
        return v != null ? Long.parseLong(v.toString()) : 0L;
    }

    /** 审核：通过/驳回 */
    public void audit(Long postId, Integer status, String reason) {
        Post post = getById(postId);
        post.setStatus(status);
        if (reason != null) {
            post.setRejectReason(reason);
        }
        postMapper.updateById(post);
    }

    /** 后台删除帖子（软删除：置状态为已删除，保留数据避免评论/点赞变孤儿） */
    public void delete(Long postId) {
        Post post = getById(postId);
        post.setStatus(Constants.POST_DELETED);
        postMapper.updateById(post);
    }

    /** 用户删除自己的帖子（软删除：置状态为已删除，保留数据避免评论/点赞变孤儿） */
    public void deleteByUser(Long postId, Long userId) {
        Post post = getById(postId);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException("只能删除自己发布的帖子");
        }
        post.setStatus(Constants.POST_DELETED);
        postMapper.updateById(post);
    }

    /** 后台硬删除帖子（物理删除，同时清理该帖子的所有评论与点赞记录，并清除Redis浏览量缓存） */
    public void hardDelete(Long postId) {
        getById(postId); // 校验帖子存在
        commentMapper.delete(new QueryWrapper<Comment>().eq("post_id", postId));
        postLikeMapper.delete(new QueryWrapper<PostLike>().eq("post_id", postId));
        postMapper.deleteById(postId);
        redis.opsForHash().delete(Constants.REDIS_POST_VIEWS, postId.toString());
    }

    /** 一键清除所有已删除(软删除)的帖子，返回清除条数 */
    public int clearDeleted() {
        List<Post> deleted = postMapper.selectList(new QueryWrapper<Post>().eq("status", Constants.POST_DELETED));
        for (Post p : deleted) {
            hardDelete(p.getId());
        }
        return deleted.size();
    }

    /** 用户隐藏/取消隐藏自己的帖子 */
    public void toggleHidden(Long postId, Long userId) {
        Post post = getById(postId);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException("只能操作自己发布的帖子");
        }
        if (post.getStatus() == Constants.POST_PUBLISH) {
            post.setStatus(Constants.POST_HIDDEN);
        } else if (post.getStatus() == Constants.POST_HIDDEN) {
            post.setStatus(Constants.POST_PUBLISH);
        }
        postMapper.updateById(post);
    }
}
