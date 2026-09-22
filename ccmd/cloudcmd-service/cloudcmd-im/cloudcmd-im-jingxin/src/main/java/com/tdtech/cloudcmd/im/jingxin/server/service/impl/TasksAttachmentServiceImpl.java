package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksAttachment;
import com.tdtech.cloudcmd.im.jingxin.server.enums.Constant;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksActionEnum;
import com.tdtech.cloudcmd.im.jingxin.server.enums.TasksStatusEnum;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksAttachmentService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksProcessesService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ITasksService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TasksAttachmentMapper;
import com.tdtech.cloudcmd.im.jingxin.server.util.AttachmentUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 三方任务附件 Service 实现类
 */
@Service
@Slf4j
public class TasksAttachmentServiceImpl extends ServiceImpl<TasksAttachmentMapper, TasksAttachment> implements ITasksAttachmentService {

    @Resource
    private IdWorker idWorker;

    @Resource
    private ITasksProcessesService tasksProcessesService;

    @Resource
    private ITasksService tasksService;
    /**
     * 附件存储根路径
     */
    private static final String ATTACHMENT_DIR = "/data/linkx/data/archive/task/";
    private static final String ATTACHMENT_URL_PREFIX = "/collaboration/static/archive";

    // /data/linkx/data/archive/task/xxx
    // /collaboration/static/archive/data/linkx/data/archive/task/xxxx
    @Override
    public TasksAttachmentVO uploadAttachment(MultipartFile file) {
        AttachmentUtil.UploadResult upload = AttachmentUtil.upload(file, ATTACHMENT_DIR);
        TasksAttachmentVO attachmentVO = new TasksAttachmentVO();
        attachmentVO.setFileName(upload.getOriginalFilename());
        attachmentVO.setFilePath(upload.getFilePath());
        attachmentVO.setFileType(upload.getContentType());
        attachmentVO.setFileSize((int) upload.getFileSize());
        attachmentVO.setFileUrl(ATTACHMENT_URL_PREFIX + upload.getFilePath());
        return attachmentVO;
    }

    private void checkFilePath(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            throw new BusinessException("附件路径不能为空");
        }
        if (!filePath.startsWith(ATTACHMENT_DIR)) {
            throw new BusinessException("附件路径不正确");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(String taskNumber, TasksAttachmentCreateReq createReq) {
        // 校验参数
        List<TasksAttachmentVO> attachments = createReq.getAttachments();
        checkAttachmentParam(attachments);
        // 处理任务状态
        TasksProcessesCreateReq tasksProcessesCreateReq = new TasksProcessesCreateReq();
        tasksProcessesCreateReq.setTaskNumber(taskNumber);
        tasksProcessesCreateReq.setStatus(createReq.getStatus());
        List<TasksExecutors> nextExecutors = createReq.getNextExecutors();
        tasksProcessesCreateReq.setNextExecutors(nextExecutors);
        UserInfo user = getUser();
        String forwardingRemark = "";
        int action = TasksActionEnum.DISPOSITION.getCode();
        // 转换状态
        if (CollectionUtils.isNotEmpty(nextExecutors)) {
            action = TasksActionEnum.FORWARDING.getCode();
            forwardingRemark = user.getUserName() + "转派给" + nextExecutors.stream().map(TasksExecutors::getName).collect(Collectors.joining("、"));
        }
//        else if (Objects.equals(createReq.getStatus(), TasksStatusEnum.TO_BE_PROCESSED.getMsg())) {
//            // 待处理->回退
//            action = TasksActionEnum.BACK_OFF.getCode();
//        } else if (Objects.equals(createReq.getStatus(), TasksStatusEnum.IN_PROGRESS.getMsg())) {
//            // 处理中->认领
//            action = TasksActionEnum.CLAIM.getCode();
//        }
        else if (Objects.equals(createReq.getStatus(), TasksStatusEnum.COMPLETED.getMsg())) {
            // 已完成->完成
            action = TasksActionEnum.COMPLETED.getCode();
        }
        tasksProcessesCreateReq.setAction(action);
        tasksProcessesCreateReq.setOperatorId(user.getUserId());
        tasksProcessesCreateReq.setOperatorName(user.getUserName());
        String remark = createReq.getRemark();
        if (StringUtils.isNotBlank(forwardingRemark)) {
            remark = StringUtils.isBlank(remark) ? forwardingRemark : remark + ":" + forwardingRemark;
        }
        tasksProcessesCreateReq.setRemark(remark);
        tasksProcessesService.save(taskNumber, tasksProcessesCreateReq);
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        // 保存附件记录到数据库
        List<TasksAttachment> tasksAttachments = BeanCopyUtils.copyList(attachments, TasksAttachment::new);
        for (TasksAttachment attachment : tasksAttachments) {
            attachment.setId(idWorker.nextId());
            attachment.setTaskNumber(taskNumber);
            attachment.setGmtCreated(new Date());
            attachment.setUserId(user.getUserId());
            attachment.setDeleted(Constant.VALID);
        }
        saveBatch(tasksAttachments);
    }

    private void checkAttachmentParam(List<TasksAttachmentVO> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return;
        }
        if (attachments.stream().anyMatch(it -> StringUtils.isBlank(it.getFilePath()))) {
            throw new BusinessException("附件路径不能为空");
        }
        if (attachments.stream().anyMatch(it -> StringUtils.isBlank(it.getFileName()))) {
            throw new BusinessException("附件名称不能为空");
        }
        if (attachments.stream().anyMatch(it -> StringUtils.isBlank(it.getFileUrl()))) {
            throw new BusinessException("附件URL不能为空");
        }
    }


    /**
     * 获取当前登录用户
     * @return  当前登录用户
     */
    private UserInfo getUser() {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new BusinessException("未获取到当前登录用户信息");
        }
        return user;
    }

    @Override
    public List<TasksAttachmentVO> listByTaskNumber(String taskNumber) {
        LambdaQueryWrapper<TasksAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TasksAttachment::getTaskNumber, taskNumber)
                .orderByDesc(TasksAttachment::getGmtCreated);
        List<TasksAttachment> attachments = list(wrapper);
        return BeanCopyUtils.copyList(attachments, TasksAttachmentVO::new);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFile(TasksAttachmentDelReq tasksAttachmentDelReq) {
        if (tasksAttachmentDelReq.getId() != null) {
            return deleteById(tasksAttachmentDelReq.getId());
        }
        checkFilePath(tasksAttachmentDelReq.getFilePath());
        return AttachmentUtil.delete(tasksAttachmentDelReq.getFilePath());
    }

    @Override
    public boolean deleteById(Long id) {
        // 1. 查询附件信息
        TasksAttachment attachment = getById(id);
        if (attachment == null) {
            return false;
        }
        checkFilePath(attachment.getFilePath());
        // 删除服务器数据
        AttachmentUtil.delete(attachment.getFilePath());
        // 逻辑删除数据库记录
        return removeById(id);
    }

    @Override
    public boolean deleteByTaskNumber(String taskNumber) {
        // 逻辑删除数据库记录
        LambdaUpdateWrapper<TasksAttachment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(TasksAttachment::getTaskNumber, taskNumber);
        // 删除服务器数据
        List<TasksAttachment> tasksAttachments = list(Wrappers.lambdaQuery(TasksAttachment.class)
                .eq(TasksAttachment::getTaskNumber, taskNumber));
        for (TasksAttachment attachment : tasksAttachments) {
            checkFilePath(attachment.getFilePath());
            AttachmentUtil.delete(attachment.getFilePath());
        }
        return remove(updateWrapper);
    }

    @Override
    public List<TasksAttachmentVO> listByTaskNumbers(List<String> taskNumbers) {
        if (CollectionUtils.isEmpty(taskNumbers)) {
            return List.of();
        }
        LambdaQueryWrapper<TasksAttachment> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(TasksAttachment::getTaskNumber, taskNumbers)
                .orderByDesc(TasksAttachment::getTaskNumber)
                .orderByDesc(TasksAttachment::getGmtCreated);
        List<TasksAttachment> tasksAttachments = list(wrapper);
        return BeanCopyUtils.copyList(tasksAttachments, TasksAttachmentVO::new);
    }

    @Override
    public boolean deleteByTaskNumbers(List<String> taskNumbers) {
        if (CollectionUtils.isEmpty(taskNumbers)) {
            return true;
        }
        LambdaUpdateWrapper<TasksAttachment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(TasksAttachment::getTaskNumber, taskNumbers);
        // 删除服务器数据
        List<TasksAttachment> tasksAttachments = list(Wrappers.lambdaQuery(TasksAttachment.class)
                .in(TasksAttachment::getTaskNumber, taskNumbers));
        for (TasksAttachment attachment : tasksAttachments) {
            checkFilePath(attachment.getFilePath());
            AttachmentUtil.delete(attachment.getFilePath());
        }
        return remove(updateWrapper);
    }

    @Override
    public boolean batchSave(List<TasksAttachment> attachments) {
        if (CollectionUtils.isEmpty(attachments)) {
            return true;
        }
        return saveBatch(attachments);
    }

}
