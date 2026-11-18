package com.billiard.controller;

import com.billiard.common.Result;
import com.billiard.entity.CustomOrder;
import com.billiard.entity.Orders;
import com.billiard.service.OrderService;
import com.billiard.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户端：仁杆定制
 */
@RestController
@RequestMapping("/api/custom")
public class CustomController {

    @Autowired
    private OrderService orderService;

    /** 提交仁杆定制（含完整表单 + addressId） */
    @PostMapping("/create")
    public Result<Orders> create(@RequestBody Map<String, Object> body) {
        Object addressIdVal = body.get("addressId");
        if (addressIdVal == null) {
            return Result.error("请选择收货地址");
        }
        Long addressId;
        try {
            addressId = Long.valueOf(addressIdVal.toString().trim());
        } catch (NumberFormatException e) {
            return Result.error("收货地址参数错误");
        }
        CustomOrder form = new CustomOrder();
        form.setPlayType((String) body.get("playType"));
        form.setPowerStyle((String) body.get("powerStyle"));
        form.setTechPref((String) body.get("techPref"));
        form.setFeelChoice((String) body.get("feelChoice"));
        form.setFeelDesc((String) body.get("feelDesc"));
        form.setOldCue((String) body.get("oldCue"));
        form.setCueStructure((String) body.get("cueStructure"));
        form.setTotalLength((String) body.get("totalLength"));
        form.setTotalWeight((String) body.get("totalWeight"));
        form.setBalancePoint((String) body.get("balancePoint"));
        form.setFrontJointModel((String) body.get("frontJointModel"));
        form.setTipDiameter((String) body.get("tipDiameter"));
        form.setGripDiameter((String) body.get("gripDiameter"));
        form.setTaperReq((String) body.get("taperReq"));
        form.setTipMaterial((String) body.get("tipMaterial"));
        form.setTipSpec((String) body.get("tipSpec"));
        form.setBaseWood((String) body.get("baseWood"));
        form.setGripType((String) body.get("gripType"));
        form.setInsertPlan((String) body.get("insertPlan"));
        form.setInlayConfig((String) body.get("inlayConfig"));
        form.setLacquerReq((String) body.get("lacquerReq"));
        form.setDecorRemark((String) body.get("decorRemark"));
        form.setGrainClause((String) body.get("grainClause"));
        form.setPhotoReq((String) body.get("photoReq"));
        form.setWoodShip((String) body.get("woodShip"));
        form.setNoReturn((String) body.get("noReturn"));
        form.setParamAdjust((String) body.get("paramAdjust"));
        form.setOtherRemark((String) body.get("otherRemark"));
        return Result.success(orderService.createCustomOrder(UserContext.getCurrentUser(), form, addressId));
    }
}
