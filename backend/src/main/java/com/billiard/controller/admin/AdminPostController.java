package com.billiard.controller.admin;

import com.billiard.common.Result;
import com.billiard.service.CommentLikeService;
import com.billiard.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理后台：内容管理（社区审核）
 */
@RestController
@RequestMapping("/admin/post")
public class AdminPostController {

    @Autowired
    private PostService postService;
    @Autowired
    private CommentLikeService commentLikeService;

    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String status) {
        // 用 String 接收，手动转 Integer，避免前端传空字符串导致 NumberFormatException
        Integer statusInt = null;
        if (status != null && !status.trim().isEmpty()) {
            try { statusInt = Integer.valueOf(status.trim()); } catch (NumberFormatException e) { statusInt = null; }
        }
        return Result.success(postService.adminPage(page, size, statusInt));
    }

    @PostMapping("/audit")
    public Result<?> audit(@RequestBody Map<String, Object> body) {
        Long id = toLongOrNull(body.get("id"));
        Integer status = toIntOrNull(body.get("status") == null ? null : body.get("status").toString());
        if (id == null || status == null) {
            return Result.error("参数错误");
        }
        String reason = body.get("reason") != null ? body.get("reason").toString().trim() : null;
        postService.audit(id, status, reason);
        return Result.success();
    }

    private Long toLongOrNull(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        if (s.isEmpty()) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }

    private Integer toIntOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Integer.valueOf(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    @PostMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.success();
    }

    @PostMapping("/hard-delete/{id}")
    public Result<?> hardDelete(@PathVariable Long id) {
        postService.hardDelete(id);
        return Result.success();
    }

    @PostMapping("/clear-deleted")
    public Result<?> clearDeleted() {
        return Result.success(postService.clearDeleted());
    }

    @GetMapping("/comment/{postId}")
    public Result<?> comments(@PathVariable Long postId) {
        return Result.success(commentLikeService.listByPost(postId));
    }

    @PostMapping("/comment/delete/{id}")
    public Result<?> deleteComment(@PathVariable Long id) {
        commentLikeService.delete(id);
        return Result.success();
    }
}
