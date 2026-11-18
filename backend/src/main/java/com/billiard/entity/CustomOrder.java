package com.billiard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 定制订单拓展表（仁杆定制完整表单字段）
 */
@Data
@TableName("custom_order")
public class CustomOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String playType;
    private String powerStyle;
    private String techPref;
    private String feelChoice;
    private String feelDesc;
    private String oldCue;
    private String cueStructure;
    private String totalLength;
    private String totalWeight;
    private String balancePoint;
    private String frontJointModel;
    private String tipDiameter;
    private String gripDiameter;
    private String taperReq;
    private String tipMaterial;
    private String tipSpec;
    private String baseWood;
    private String gripType;
    private String insertPlan;
    private String inlayConfig;
    private String lacquerReq;
    private String decorRemark;
    private String grainClause;
    private String photoReq;
    private String woodShip;
    private String noReturn;
    private String paramAdjust;
    private String otherRemark;
}
