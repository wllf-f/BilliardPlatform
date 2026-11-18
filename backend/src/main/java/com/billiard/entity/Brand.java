package com.billiard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("brand")
public class Brand {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String name;
    private Integer sort;
}
