package com.billiard.controller;

import com.billiard.common.Constants;
import com.billiard.common.Result;
import com.billiard.entity.Post;
import com.billiard.service.CommentLikeService;
import com.billiard.service.PostService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端：社区帖子
 */
@RestController
@RequestMapping("/api/post")
public class PostController {

    @Autowired
    private PostService postService;
    @Autowired
    private CommentLikeService commentLikeService;
    @Autowired
    private StringRedisTemplate redis;

    /** 已发布帖子列表 */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String keyword,
                          HttpServletRequest request) {
        Long currentUserId = getUserIdFromToken(request);
        return Result.success(postService.publishedPage(page, size, keyword, currentUserId));
    }

    /** 发帖 */
    @PostMapping("/create")
    public Result<Post> create(@RequestBody Map<String, String> body) {
        return Result.success(postService.create(UserContext.getCurrentUser(),
                body.get("title"), body.get("content"), body.get("images")));
    }

    /** 我的帖子 */
    @GetMapping("/mine")
    public Result<?> mine(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size) {
        return Result.success(postService.myPosts(page, size, UserContext.getCurrentUser()));
    }

    /** 帖子详情（浏览量+1，可见性校验） */
    @GetMapping("/detail/{id}")
    public Result<?> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserIdFromToken(request);
        Map<String, Object> post = postService.getDetailById(id, userId);
        postService.incrView(id);
        Map<String, Object> m = new HashMap<>();
        m.put("post", post);
        m.put("views", postService.getView(id));
        m.put("likeCount", commentLikeService.likeCount(id));
        m.put("comments", commentLikeService.listByPost(id));
        if (userId != null) {
            m.put("liked", commentLikeService.isLiked(userId, id));
        } else {
            m.put("liked", false);
        }
        return Result.success(m);
    }

    /** 从请求头中解析用户ID（无token时返回null，不抛异常） */
    private Long getUserIdFromToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) return null;
        String userId = redis.opsForValue().get(Constants.REDIS_USER_TOKEN + token);
        if (userId == null) return null;
        return Long.valueOf(userId);
    }

    /** 点赞/取消点赞 */
    @PostMapping("/like/{id}")
    public Result<?> like(@PathVariable Long id) {
        boolean liked = commentLikeService.toggleLike(UserContext.getCurrentUser(), id);
        Map<String, Object> m = new HashMap<>();
        m.put("liked", liked);
        m.put("likeCount", commentLikeService.likeCount(id));
        return Result.success(m);
    }

    /** 批量查询当前用户点赞状态 */
    @PostMapping("/liked/batch")
    public Result<?> batchLiked(@RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return Result.success(new HashMap<>());
        }
        Long userId = getUserIdFromToken(request);
        if (userId == null) {
            return Result.success(new HashMap<>());
        }
        return Result.success(commentLikeService.batchIsLiked(userId, ids));
    }

    /** 删除自己的帖子 */
    @PostMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        postService.deleteByUser(id, UserContext.getCurrentUser());
        return Result.success("删除成功");
    }

    /** 隐藏/取消隐藏自己的帖子 */
    @PostMapping("/toggle-hidden/{id}")
    public Result<?> toggleHidden(@PathVariable Long id) {
        postService.toggleHidden(id, UserContext.getCurrentUser());
        return Result.success("操作成功");
    }
}
