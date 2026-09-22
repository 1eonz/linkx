package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TasksConfigVO;

import java.util.List;

/**
 * 任务标准件配置 Service（PC 端展示）
 */
public interface TasksConfigService {

    /**
     * 查询 PC 展示的任务标准件模块列表
     */
    List<TasksConfigVO> listPcModules();

    /**
     * 通过模块名称分页查询任务列表
     *
     * @param module   模块名称（必填）
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param name     任务名称关键字
     * @param content  任务内容关键字
     * @param scope    范围：1全部(默认)、2创建人、3执行人、4创建人+执行人
     * @param idCard   身份证号；scope != 1 时用于过滤，为空由 Controller 层补当前登录人
     * @return 任务分页
     */
    Page<TasksVO> pageTasksByModule(String module, int pageNum, int pageSize,
                                    String name, String content,
                                    Integer scope, String idCard);
}