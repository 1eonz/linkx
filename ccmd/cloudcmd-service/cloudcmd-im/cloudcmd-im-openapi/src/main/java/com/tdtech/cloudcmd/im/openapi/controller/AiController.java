package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.AiRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.AiRecordQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.Category;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.ai.RecordCountResp;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.im.openapi.controller.entity.AiRecordCountByCategory;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.keyvalue.AbstractKeyValue;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "ai调用记录", description = "ai调用记录")
@RestController
@RequestMapping("/openapi/v1/ai")
@RequiredArgsConstructor
@Validated
@OpenApiOauth(BusinessScopeEnum.COLLABORATIVE_STATISTICS)
@RequestLimit(business = "ai调用记录")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
public class AiController {
    @DubboReference
    private AiRpcApi aiRpcApi;

    /**
     * 人员核查统计数量
     */
    @Operation(summary = "调用统计", description = "调用统计")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/record/count")
    public R<List<RecordCountResp>> recordCount(@Validated AiRecordQO aiRecordQO,
        @Parameter(name = "countStrategy", description = "统计维度，0：人员 1：智能体 2：人员+智能体",
            required = true) @RequestParam("countStrategy") Integer countStrategy) {
        //狗屎DUBBO序列化
        LinkedList<RecordCountReq.GroupEnum> group = new LinkedList<>();
        switch (countStrategy) {
            case 0: {// 人员维度
                group.add(RecordCountReq.GroupEnum.person);
                break;
            }
            case 1: {// agent维度
                group.add(RecordCountReq.GroupEnum.agent);
                break;
            }
            case 2: {// 人员和Agent维度
                group.add(RecordCountReq.GroupEnum.person);
                group.add(RecordCountReq.GroupEnum.agent);
                break;
            }
            default: {
                throw new BusinessException("countStrategy: " + countStrategy + " 参数错误");
            }
        }

        var resp = aiRpcApi.agentAskCount(aiRecordQO, group);
        switch (countStrategy) {
            case 0:
            case 1:
            case 2: {
                return R.success(resp);
            }
            default: {
                throw new BusinessException("countStrategy: " + countStrategy + " 参数错误");
            }
        }
    }

    /**
     * 人员核查统计数量
     */
    @Operation(summary = "调用统计", description = "统计维度是AGENT标签")
    @Parameter(name = "Authorization", description = "认证令牌", required = true, in = ParameterIn.HEADER)
    @GetMapping("/record/count/category")
    public R<List<AiRecordCountByCategory>> recordCountBycategory(@Validated AiRecordQO aiRecordQO) {
        LinkedList<RecordCountReq.GroupEnum> group = new LinkedList<>();
        group.add(RecordCountReq.GroupEnum.agent);
        var resp = aiRpcApi.agentAskCount(aiRecordQO, group);
        if (resp == null || resp.isEmpty()) {
            return R.success(Collections.emptyList());
        }
        var list = resp.stream()//
            .filter(a -> a.getCategorys() != null && !a.getCategorys().isEmpty())//
            .flatMap(a -> a.getCategorys().stream().map(c -> new DefaultKeyValue<>(c.getId(), new Object[] {c, a})))//
            .collect(Collectors.groupingBy(AbstractKeyValue::getKey, Collectors.toList()))//
            .values()//
            .stream()//
            .map(values -> {
                var acc = new AiRecordCountByCategory();
                acc.setCount(0L);
                for (var value : values) {
                    var ca = (Category)value.getValue()[0];
                    var r = (RecordCountResp)value.getValue()[1];
                    acc.setCount(acc.getCount() + r.getCount());
                    acc.setId(ca.getId());
                    acc.setName(ca.getName());
                }
                return acc;
            }).collect(Collectors.toList());
        return R.success(list);
    }
}
