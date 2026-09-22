package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksAttachmentCreateReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksAttachmentDelReq;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksAttachmentVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 三方任务附件 Service 接口
 */
public interface ITasksAttachmentService extends IService<TasksAttachment> {

    /**
     * 根据任务编号查询附件列表
     *
     * @param taskNumber 任务编号
     * @return 附件列表
     */
    List<TasksAttachmentVO> listByTaskNumber(String taskNumber);

    /**
     * 根据附件ID删除附件
     *
     * @param id 附件ID
     * @return 是否删除成功
     */
    boolean deleteById(Long id);

    /**
     * 根据任务编号删除所有附件
     *
     * @param taskNumber 任务编号
     * @return 是否删除成功
     */
    boolean deleteByTaskNumber(String taskNumber);

    /**
     * 根据任务编号列表批量查询附件
     *
     * @param taskNumbers 任务编号列表
     * @return 附件列表
     */
    List<TasksAttachmentVO> listByTaskNumbers(List<String> taskNumbers);

    /**
     * 根据任务编号列表批量删除附件
     *
     * @param taskNumbers 任务编号列表
     * @return 是否删除成功
     */
    boolean deleteByTaskNumbers(List<String> taskNumbers);

    /**
     * 批量保存附件
     *
     * @param attachments 附件列表
     * @return 是否保存成功
     */
    boolean batchSave(List<TasksAttachment> attachments);

    TasksAttachmentVO uploadAttachment(MultipartFile file);

    void save(String taskNumber, TasksAttachmentCreateReq createReq);

    Boolean deleteFile(TasksAttachmentDelReq tasksAttachmentDelReq);
}
