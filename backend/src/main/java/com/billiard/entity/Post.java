package com.billiard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("post")
public class Post {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String images;
    private Integer status;
    private String rejectReason;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    // 关联查询字段（非数据库字段）
    @TableField(exist = false)
    private String nickname;
    @TableField(exist = false)
    private String avatar;
}
