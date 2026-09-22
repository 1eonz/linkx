package com.tdtech.cloudcmd.linkx.third.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableCreateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableUpdateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.SearchPair;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.linkx.third.enums.AppTypeEnum;
import com.tdtech.cloudcmd.linkx.third.enums.Constants;
import com.tdtech.cloudcmd.linkx.third.vo.*;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableTransactionTaskVo;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableVo;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 南向应用信息表 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-15
 */
public interface IAppCallableService extends IService<AppCallable> {

    /**
     * 查询南向应用列表
     *
     * @param name       名称
     * @param page       页码
     * @param pageSize   页面大小
     * @return 南向应用列表
     */
    Page<AppCallableVo> pageAppCallable(String name, long page, long pageSize);

    /**
     * 创建南向应用
     *
     * @param dto 入参
     */
    void createAppCallable(@Valid AppCallableCreateDto dto);

    /**
     * 更新南向应用
     *
     * @param dto 入参
     */
    void updateAppCallable(@Valid AppCallableUpdateDto dto);

    /**
     * 删除南向应用
     *
     * @param callableId 南向应用id
     */
    void deleteAppCallable(Long callableId);

    /**
     * 分页查询可调用南向应用的数据
     *
     * @param callableId 应用id
     * @param table      表名
     * @param current    当前页码
     * @param size       每页记录数
     * @param searchPair 搜索键值对, 仅支持模糊查询
     * @return 分页结果
     */
    Page<JSONObject> pageAppData(Long callableId, String table, Long current, Long size, SearchPair<String, String> searchPair);

    /**
     * 南向应用事务数据转任务标准件
     *
     * @param callableId  应用id
     * @param toTaskVo   转成任务所需信息
     */
    String appDataToTask(Long callableId, AppCallableToTaskVo toTaskVo);

    /**
     * 更新南向应用mapper
     * @param callableId 南向应用id
     * @param mapperVo 更新的南向应用mapper
     */
    void updateAppCallableMapper(Long callableId, AppCallableMapperVo mapperVo);

    /**
     * 更新南向应用任务标准件派发配置
     *
     * @param callableId 南向应用id
     * @param configVo 任务标准件派发配置
     */
    void updateTaskConfig(Long callableId, AppCallableTaskConfigVo configVo);

    /**
     * 获取南向应用任务标准件派发配置
     *
     * @param callableId 南向应用id
     * @return 任务标准件派发配置
     */
    AppCallableTaskConfigVo getTaskConfig(Long callableId);

    /**
     * 查询南向应用详情
     *
     * @param callableId 南向应用id
     */
    AppCallableDetailVo getAppCallable(Long callableId);

    /**
     * 获取南向应用对应数据表列表
     *
     * @param callableId 南向应用id
     */
    List<String> listAppCallableTables(Long callableId);

    /**
     * 获取事务转成的任务状态
     *
     * @param callableId 主键id
     * @param tableDataId 数据表ID，对应的是业务数据的linkx_id
     * @param tableName 表名
     */
    TasksVO getTaskStatus(Long callableId, Long tableDataId, String tableName);

    /**
     * 获取拼接后的表名
     *
     * @param app 应用信息
     * @return 表名
     */
    String getTableName(AppCallableDetailVo app);

    /**
     * 执行南向应用事务数据的同步逻辑
     *
     * @param callableId 南向应用id
     */
    void runSync(Long callableId);

    /**
     * 获取事务数据的字段信息
     *
     * @param callableId 南向应用id
     * @param tableName  表名
     * @return 南向应用字段信息
     */
    List<AppCallableDataTableColumnInfoVo> getAppDataColumnInfo(Long callableId, String tableName);

    /**
     * 任务标准件获取事务数据的字段信息
     * @param taskNo 任务编号
     * @param taskId 任务ID
     * @return 南向应用字段信息
     */
    List<AppCallableDataTableColumnInfoVo> getAppDataColumnInfo4App(Long taskId, String taskNo);
}