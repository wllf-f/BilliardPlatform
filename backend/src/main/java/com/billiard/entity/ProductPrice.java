package com.billiard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("product_price")
public class ProductPrice {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private BigDecimal officialPrice;
    private BigDecimal pddPrice;
    private BigDecimal platformPrice;
}
