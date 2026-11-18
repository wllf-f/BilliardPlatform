package com.billiard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long userId;
    private Integer orderType;
    private BigDecimal totalAmount;
    private Integer status;
    private String consignee;
    private String phone;
    private String address;
    private String remark;
    private String expressCompany;
    private String expressNo;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime shipTime;
    private LocalDateTime finishTime;
    private LocalDateTime updateTime;
}
