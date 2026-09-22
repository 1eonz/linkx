package com.tdtech.cloudcmd.linkx.third.service;

import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.linkx.third.api.dto.AppDataToDbDto;
import com.tdtech.cloudcmd.linkx.third.api.dto.ColumnInfo;
import com.tdtech.cloudcmd.linkx.third.enums.AppTypeEnum;
import com.tdtech.cloudcmd.linkx.third.enums.PageModeEnum;
import com.tdtech.cloudcmd.linkx.third.mapper.AppCallableSyncMapper;
import com.tdtech.cloudcmd.linkx.third.vo.AppCallableDetailVo;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.JDBCType;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public abstract class AbstractAppCallableDataSyncServiceImpl implements AppCallableDataSyncService {
    /**
     * 创建时间字段
     */
    protected static final String CREATED_TIME_COLUMN = "gmt_created";

    /**
     * 更新时间字段
     */
    protected static final String UPDATE_TIME_COLUMN = "gmt_updated";

    /**
     * 事务数据表主键字段
     */
    public static final String LINKX_ID_COLUMN = "linkx_id";

    /**
     * 分批次拉取数据最大循环间隔
     */
    protected static final String PAGE_MAX_LOOPS = "THIRD_APP_DATA_SYNC_PAGE_MAX_LOOPS";

    @Autowired
    private IAppCallableService appService;

    @Autowired
    private AppCallableSyncMapper syncMapper;

    @Autowired
    private GlobalsRpcService globalsRpcService;

    @Autowired
    private AppCallableCreateTableService tableService;

    /**
     * 无时间范围的 doSync，委托给5参数版本
     */
    @Override
    public void doSync(AppCallableDetailVo appCallable) {
        doSync(appCallable, null, null);
    }

    /**
     * 带时间范围的 doSync，包含完整的分页拉取、建表、插入逻辑
     * <p>
     * 这是所有同步流程的统一入口：
     * <ul>
     *     <li>DB 类型：timeRangeStart/timeRangeEnd 为 null，走原有逻辑</li>
     *     <li>API 类型：forward/backward 执行器传入时间范围，pullData 会使用时间表达式替换</li>
     * </ul>
     * </p>
     *
     * @param appCallable    南向应用配置
     * @param timeRangeStart 时间范围起始（null 表示不限制）
     * @param timeRangeEnd   时间范围结束（null 表示不限制）
     * @return 同步结果（包含 minDateTimeSign/maxDateTimeSign）
     */
    @Override
    public AppDataToDbDto doSync(AppCallableDetailVo appCallable, LocalDateTime timeRangeStart, LocalDateTime timeRangeEnd) {
        try {
            // 配置获取，分页大小，最大页限制，每页执行时间间隔
            String pageSizeColumn = AppTypeEnum.API.getCode().equals(appCallable.getType()) ?
                    AppTypeEnum.API.getPageSizeConfigColumn() : AppTypeEnum.DB.getPageSizeConfigColumn();
            String pageTimeIntervalColumn = AppTypeEnum.API.getCode().equals(appCallable.getType()) ?
                    AppTypeEnum.API.getPageTimeIntervalColumn() : AppTypeEnum.DB.getPageTimeIntervalColumn();
            String pageSizeStr = globalsRpcService.getGlobalsValueByName(pageSizeColumn);
            String pageTimeIntervalStr = globalsRpcService.getGlobalsValueByName(pageTimeIntervalColumn);
            String maxPageStr = globalsRpcService.getGlobalsValueByName(PAGE_MAX_LOOPS);
            long pageSize = StringUtils.isBlank(pageSizeStr) ? 200 : Long.parseLong(pageSizeStr);
            int pageTimeInterval = StringUtils.isBlank(pageTimeIntervalStr) ? 10 : Integer.parseInt(pageTimeIntervalStr);
            long maxPage = StringUtils.isBlank(maxPageStr) ? 1000 : Long.parseLong(maxPageStr);

            // 拉取数据, 先拉取第一页的, 若是偏移量分页，则取偏移量0
            boolean offsetPage = Objects.equals(appCallable.getPagenationType(), PageModeEnum.OFFSET.getCode());
            AppDataToDbDto appDataToDbDto = pullData(appCallable, offsetPage ? 0L : 1L, pageSize, timeRangeStart, timeRangeEnd);
            // 无字段信息或者无数据-跳过执行
            if (CollectionUtils.isEmpty(appDataToDbDto.getColumnInfoList())
                    || CollectionUtils.isEmpty(appDataToDbDto.getResultDataList())) {
                log.warn("无字段或者数据，跳过数据同步");
                return appDataToDbDto;
            }

            // 创建表，如果表不存在
            tableService.createTableAndInsertData(appCallable, appDataToDbDto);

            // 跨分页追踪 dateTimeSign 的最早和最晚值
            String globalMin = appDataToDbDto.getMinDateTimeSign();
            String globalMax = appDataToDbDto.getMaxDateTimeSign();

            // 处理分批次拉取数据的逻辑, 可分页-执行到拉取数据量小于分页数量则停止
            boolean pageable = AppTypeEnum.DB.getCode().equals(appCallable.getType()) || (appCallable.getPagenation() == 1);
            int loopTimes = 1;
            if (pageable) {
                // 如果第一次拉取数据，就少于页面大小，不再进行第二次拉取
                if (CollectionUtils.isEmpty(appDataToDbDto.getResultDataList()) || appDataToDbDto.getResultDataList().size() < pageSize) {
                    return appDataToDbDto;
                }
                // 若是偏移量，第二批次则是pageSize
                long page = offsetPage ? pageSize : 2L;
                while (true) {
                    try {
                        AppDataToDbDto appData = pullData(appCallable, page, pageSize, timeRangeStart, timeRangeEnd);
                        List<JSONObject> dataList = appData.getResultDataList();
                        // 跨分页合并 min/max
                        if (appData.getMinDateTimeSign() != null
                                && (globalMin == null || appData.getMinDateTimeSign().compareTo(globalMin) < 0)) {
                            globalMin = appData.getMinDateTimeSign();
                        }
                        if (appData.getMaxDateTimeSign() != null
                                && (globalMax == null || appData.getMaxDateTimeSign().compareTo(globalMax) > 0)) {
                            globalMax = appData.getMaxDateTimeSign();
                        }
                        // 无数据则跳出
                        if (CollectionUtils.isEmpty(dataList)) {
                            break;
                        }
                        tableService.doInsertData(appCallable, appData);
                        // 小于分页大小则停止，避免多一次的数据获取
                        if (dataList.size() < pageSize) {
                            break;
                        }
                        // 分批拉取应该有时间间隔
                        TimeUnit.MILLISECONDS.sleep(pageTimeInterval);
                        page = offsetPage ? page + pageSize : page + 1;
                        loopTimes++;
                        // 查询批次超过1000次，停止，避免一直拉取
                        if (loopTimes > maxPage) {
                            log.warn("停止拉取数据，原因是超过最大拉取次数{}", maxPage);
                            break;
                        }
                    } catch (InterruptedException e) {
                        // 正确处理中断
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }

            // 设置跨分页合并后的 min/max
            appDataToDbDto.setMinDateTimeSign(globalMin);
            appDataToDbDto.setMaxDateTimeSign(globalMax);
            return appDataToDbDto;
        } catch (Exception e) {
            log.error("同步南向应用事务数据失败:", e);
            throw new BusinessException(e);
        }
    }

    protected void addExtraColumn(List<ColumnInfo> columnInfoList) {
        // 添加必要列
        columnInfoList.add(ColumnInfo.builder()
                .columnName(CREATED_TIME_COLUMN)
                .columnType("DATETIME")
                .jdbcType(JDBCType.TIMESTAMP.getVendorTypeNumber())
                .comment("创建时间")
                .build());
        columnInfoList.add(ColumnInfo.builder()
                .columnName(UPDATE_TIME_COLUMN)
                .columnType("DATETIME")
                .jdbcType(JDBCType.TIMESTAMP.getVendorTypeNumber())
                .comment("更新时间")
                .build());
        columnInfoList.add(ColumnInfo.builder()
                .columnName(LINKX_ID_COLUMN)
                .columnType("BIGINT")
                .primaryKey(true)
                .jdbcType(JDBCType.BIGINT.getVendorTypeNumber())
                .comment("主键")
                .build());
    }

    @Override
    public abstract AppDataToDbDto pullData(AppCallableDetailVo detailVo, Long page, Long pageSize) throws SQLException;
}