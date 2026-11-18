package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.entity.Comment;
import com.billiard.service.CommentLikeService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端：评论
 */
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    @Autowired
    private CommentLikeService commentLikeService;

    @GetMapping("/list/{postId}")
    public Result<?> list(@PathVariable Long postId) {
        return Result.success(commentLikeService.listByPost(postId));
    }

    @PostMapping("/create")
    public Result<?> create(@RequestBody Map<String, String> body) {
        String postIdStr = body.get("postId");
        String content = body.get("content");
        if (postIdStr == null || postIdStr.isEmpty()) {
            return Result.error("帖子ID不能为空");
        }
        if (content == null || content.trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }
        Long postId;
        try {
            postId = Long.valueOf(postIdStr);
        } catch (NumberFormatException e) {
            return Result.error("帖子ID格式错误");
        }
        return Result.success(commentLikeService.create(UserContext.getCurrentUser(), postId, content.trim()));
    }
}
