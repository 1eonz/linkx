package com.tdtech.cloudcmd.linkx.third.service;

import com.tdtech.cloudcmd.linkx.third.api.dto.AppDataToDbDto;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * <p>
 * 南向应用数据同步 服务类
 * </p>
 *
 * @author author
 * @since 2026-04-16
 */
public interface AppCallableDataSyncService {

    /**
     * 同步数据（无时间范围，兼容原有逻辑）
     *
     * @param appCallable 南向应用配置
     */
    void doSync(AppCallableDetailVo appCallable);

    /**
     * 带时间范围的同步数据，供 forward/backward 执行器调用
     * <p>
     * 包含完整的分页拉取、建表、插入逻辑，返回 AppDataToDbDto 以便调用方获取 minDateTimeSign/maxDateTimeSign。
     * DB 类型实现可忽略时间范围参数，API 类型实现会使用时间范围替换请求参数中的时间表达式。
     * </p>
     *
     * @param appCallable    南向应用配置
     * @param timeRangeStart 时间范围起始（null 表示不限制）
     * @param timeRangeEnd   时间范围结束（null 表示不限制）
     * @return 同步结果（包含 minDateTimeSign/maxDateTimeSign）
     */
    AppDataToDbDto doSync(AppCallableDetailVo appCallable, LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd);

    /**
     * 拉取南向应用的事务数据
     *
     * @param detailVo 南向应用详情
     * @param current  分页查询需要指定当前页
     * @param pageSize 分页查询需要指定页面大小
     * @return 数据
     */
    AppDataToDbDto pullData(AppCallableDetailVo detailVo, Long current, Long pageSize) throws SQLException;

    /**
     * 带时间范围的拉取南向应用事务数据
     * <p>
     * 默认委托给3参数版本（忽略时间范围），API 类型实现覆写此方法以支持时间表达式替换。
     * </p>
     *
     * @param detailVo       南向应用详情
     * @param current        分页查询需要指定当前页
     * @param pageSize       分页查询需要指定页面大小
     * @param timeRangeStart 时间范围起始（null 表示不替换时间表达式）
     * @param timeRangeEnd   时间范围结束（null 表示不替换时间表达式）
     * @return 数据
     */
    default AppDataToDbDto pullData(AppCallableDetailVo detailVo, Long current, Long pageSize,
                                    LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) throws SQLException {
        return pullData(detailVo, current, pageSize);
    }
}