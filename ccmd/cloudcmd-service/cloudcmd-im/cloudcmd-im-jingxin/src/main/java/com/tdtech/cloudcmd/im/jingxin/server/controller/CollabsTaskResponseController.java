package com.tdtech.cloudcmd.im.jingxin.server.controller;

import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.im.jingxin.server.entity.RelyAllTaskResponseCO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponseCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponseDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollabsTaskResponseListReqCO;
import com.tdtech.cloudcmd.im.jingxin.server.service.ICollaborationTaskResponseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestMapping("/collaboration/v1/tasks/response")
@RestController
@RequiredArgsConstructor
@Tag(name = "协作任务响应管理", description = "处理协作任务的响应信息，包括保存、批量答复和查询功能")
public class CollabsTaskResponseController {

    @Resource
    private ICollaborationTaskResponseService collaborationTaskResponseService;

    @PostMapping("/save")
    @Operation(summary = "保存任务响应", description = "保存单个协作任务的响应信息")
    public R<Void> addCollabsTaskResponse(@Validated @RequestBody CollaborationTaskResponseCO collaborationTaskResponseCO) {
        collaborationTaskResponseService.save(collaborationTaskResponseCO);
        return R.success();
    }

    /**
     * 批量答复消息
     *
     */
    @PostMapping("/responses")
    @Operation(summary = "批量答复任务", description = "批量处理多个协作任务的响应信息")
    public R createTaskResponse(@Validated  @RequestBody List<CollaborationTaskResponseCO> imTaskResponses) {
        if(imTaskResponses==null||imTaskResponses.isEmpty()){
            log.warn("empty task response");
            return R.success();
        }
        log.info("imTaskResponses:{}",imTaskResponses);
        collaborationTaskResponseService.createTaskResponse(imTaskResponses);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    @PostMapping("/reply/all")
    @Operation(summary = "回复协同岗所有任务", description = "任务回复开关打开时，一次回复协同岗所有任务")
    public R replyAll(@Validated  @RequestBody RelyAllTaskResponseCO relyAllTaskResponseCO) {
        if(Objects.isNull(relyAllTaskResponseCO)){
            log.warn("empty task response");
            return R.success();
        }
        log.info("relyAllTaskResponseCO: {}", relyAllTaskResponseCO);
        collaborationTaskResponseService.replyAll(relyAllTaskResponseCO);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    @GetMapping("")
    @Operation(summary = "获取任务响应列表", description = "分页获取指定用户的所有协作任务响应列表")
    public R<PageResult<CollaborationTaskResponseDTO>> getCollabsTaskResponseList(@RequestParam("pageSize") Integer pageSize, @RequestParam("page") Integer page, @RequestParam("userId") Long userId) {
        PageResult<CollaborationTaskResponseDTO> data = collaborationTaskResponseService.listTaskResponse(new CollabsTaskResponseListReqCO( page, pageSize,userId));
        return R.success(data);
    }
}
