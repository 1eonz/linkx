package com.tdtech.cloudcmd.linkx.third.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.tasks.TasksVO;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableCreateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppCallableUpdateDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppDataToDbDto;
import com.tdtech.cloudcmd.linkx.third.entity.AppCallable;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableMapperVo;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableToTaskVo;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.util.List;

/**
 * <p>
 * 南向应用创建表 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-18
 */
public interface AppCallableCreateTableService {

    /**
     * 创建表和索引
     *
     * @param appCallable    南向应用信息
     * @param appDataToDbDto 建表相关信息
     */
    void createTable(AppCallableDetailVo appCallable, AppDataToDbDto appDataToDbDto);


    /**
     * 创建表和索引, 并写入数据
     *
     * @param appCallable    南向应用信息
     * @param appDataToDbDto 建表相关信息
     */
    void createTableAndInsertData(AppCallableDetailVo appCallable, AppDataToDbDto appDataToDbDto);

    /**
     * 写入南向应用数据
     *
     * @param appCallable 南向应用信息
     * @param appDataToDbDto 数据等相关信息
     */
    void doInsertData(AppCallableDetailVo appCallable, AppDataToDbDto appDataToDbDto);
}
