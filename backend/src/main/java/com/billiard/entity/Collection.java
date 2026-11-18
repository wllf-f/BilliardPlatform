package com.billiard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collection")
@Alias("ProductCollection")
public class Collection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private LocalDateTime createTime;
}
