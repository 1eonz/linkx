package com.tdtech.cloudcmd.linkx.dashboard.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.converters.longconverter.LongStringConverter;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteTableHolder;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticCoopDutySwitch;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticCreateGroup;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticPhotoCheck;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticTask;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StaticTaskResponse;
import com.tdtech.cloudcmd.linkx.dashboard.enums.CheckResultEnum;
import com.tdtech.cloudcmd.linkx.dashboard.enums.GroupCreateTypeEnum;
import com.tdtech.cloudcmd.linkx.dashboard.enums.GroupSubTypeEnum;
import com.tdtech.cloudcmd.linkx.dashboard.enums.GroupTypeEnum;
import com.tdtech.cloudcmd.linkx.dashboard.enums.StaticTaskStatusEnum;
import com.tdtech.cloudcmd.linkx.dashboard.enums.SwitchEnum;
import com.tdtech.cloudcmd.linkx.dashboard.enums.SwitchTypeEnum;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticCoopDutySwitchMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticCreateGroupMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticPhotoCheckMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticTaskMapper;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StaticTaskResponseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.service.IStaticExportService;
import com.tdtech.cloudcmd.linkx.dashboard.vo.StaticCoopDutySwitchExportVO;
import com.tdtech.cloudcmd.linkx.dashboard.vo.StaticCreateGroupExportVO;
import com.tdtech.cloudcmd.linkx.dashboard.vo.StaticPhotoCheckExportVO;
import com.tdtech.cloudcmd.linkx.dashboard.vo.StaticTaskExportVO;
import com.tdtech.cloudcmd.linkx.dashboard.vo.StaticTaskResponseExportVO;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 协同监测统计明细导出服务实现。
 *
 * <p>按部门编码与时间范围筛选，从 5 张统计明细表（协同任务、建群记录、任务回复、
 * 人员核查、协同岗上下岗）游标分页读取数据，写入同一个 Excel 文件的 5 个 sheet。
 *
 * <p>关键设计：
 * <ul>
 *   <li>游标分页（{@link #PAGE_SIZE}）：避免一次性加载大数据量导致 OOM</li>
 *   <li>双游标推进（时间 DESC + 主键 DESC）：防止同秒同分页边界记录漏读</li>
 *   <li>{@code Long} → {@code String} 转换：防止 19 位雪花 ID 在 Excel 中精度丢失</li>
 *   <li>枚举码 → 中文名转换：导出列直接呈现可读文本</li>
 *   <li>5 张表并行查询 + 串行写入：{@link ExcelWriter} 非线程安全，写操作用
 *       {@code synchronized(writer)} 互斥；查询与转换并行，缩短总耗时</li>
 *   <li>半开区间时间筛选：{@code >= startTime AND < endTime}，避免 {@code BETWEEN}
 *       在不同 DATETIME 精度下的边界二义性</li>
 * </ul>
 */
@Slf4j
@Service
public class StaticExportServiceImpl implements IStaticExportService {

    private static final int PAGE_SIZE = 5000;

    private static final String SHEET_TASK = "协同任务统计明细";
    private static final String SHEET_CREATE_GROUP = "建群记录统计明细";
    private static final String SHEET_TASK_RESPONSE = "群组消息回复记录明细";
    private static final String SHEET_PHOTO_CHECK = "人员核查信息统计明细";
    private static final String SHEET_COOP_DUTY_SWITCH = "协同岗上下岗统计明细";

    /**
     * 导出专用线程池：固定 5 线程对应 5 张表并行查询，daemon 线程不阻止 JVM 退出。
     */
    private static final AtomicInteger THREAD_SEQ = new AtomicInteger(0);
    private static final ExecutorService EXPORT_EXECUTOR = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r, "static-export-" + THREAD_SEQ.incrementAndGet());
        t.setDaemon(true);
        t.setUncaughtExceptionHandler((thread, e) ->
                log.error("static-export线程发生未捕获异常: thread={}", thread.getName(), e));
        return t;
    });

    @Resource
    private StaticTaskMapper staticTaskMapper;

    @Resource
    private StaticCreateGroupMapper staticCreateGroupMapper;

    @Resource
    private StaticTaskResponseMapper staticTaskResponseMapper;

    @Resource
    private StaticPhotoCheckMapper staticPhotoCheckMapper;

    @Resource
    private StaticCoopDutySwitchMapper staticCoopDutySwitchMapper;

    @DubboReference
    private DepartmentRpcApi departmentRpcApi;

    /**
     * 导出协同监测明细 Excel。
     *
     * <p>流程：参数校验 → 时间范围半年校验 → 解析部门 ID → 设置响应头 →
     * 构建 ExcelWriter → 并行写入 5 个 sheet → 异常时关闭 writer。
     *
     * @param departmentCode  部门编码，为空表示不按部门筛选（全量导出）
     * @param startTime       开始时间（yyyy-MM-dd，必填），转为本日 00:00:00（闭区间下界）
     * @param endTime         结束时间（yyyy-MM-dd，必填），转为次日 00:00:00（排他上界）
     * @param includeChildren 是否包含子部门（null 或 1=包含；0=仅本级）
     * @param response        HTTP 响应，Excel 直接写入响应输出流
     * @throws IOException 写入响应流失败时抛出
     */
    @Override
    public void export(String departmentCode, String startTime, String endTime,
                       Integer includeChildren, HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
            throw new BusinessException("开始时间和结束时间不能为空");
        }

        LocalDate startLocalDate = DateFormatUtil.parseLocalDate(startTime, DateFormatUtil.YYYY_MM_DD);
        LocalDate endLocalDate = DateFormatUtil.parseLocalDate(endTime, DateFormatUtil.YYYY_MM_DD);

        Date startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        // endTime 取次日 00:00:00 作为排他上界，配合 SQL 的 `< #{endTime}` 半开区间，
        // 避免 DATETIME 小数秒四舍五入导致次日 0 点记录被误纳入
        Date endExclusive = Date.from(endLocalDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        validateHalfYearLimit(startLocalDate, endLocalDate);

        List<Long> deptIds = resolveDepartmentIds(departmentCode, includeChildren);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("协同监测明细", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-disposition",
                "attachment;filename*=utf-8''" + fileName + ".xlsx");

        OutputStream outputStream = null;
        ExcelWriter excelWriter = null;
        try {
            outputStream = response.getOutputStream();
            excelWriter = EasyExcel.write(outputStream)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .registerWriteHandler(new HeadCommentWriteHandler(SHEET_COOP_DUTY_SWITCH,
                            new HashSet<>(Arrays.asList(1, 2, 3, 4)), "手工切换有值"))
                    .registerConverter(new LongStringConverter())
                    .autoCloseStream(false)
                    .build();

            // 串行预创建 5 个 sheet 并写入表头（空数据触发 POI createSheet + head 写入），
            // 锁定 sheet 在 workbook 中的物理顺序为 0→4；若放在并行阶段，各线程争锁顺序不定，
            // POI createSheet(name) 追加到末尾会导致 sheet 顺序失序。
            WriteSheet sheetTask = EasyExcel.writerSheet(0, SHEET_TASK).head(StaticTaskExportVO.class).build();
            WriteSheet sheetCreateGroup = EasyExcel.writerSheet(1, SHEET_CREATE_GROUP).head(StaticCreateGroupExportVO.class).build();
            WriteSheet sheetTaskResponse = EasyExcel.writerSheet(2, SHEET_TASK_RESPONSE).head(StaticTaskResponseExportVO.class).build();
            WriteSheet sheetPhotoCheck = EasyExcel.writerSheet(3, SHEET_PHOTO_CHECK).head(StaticPhotoCheckExportVO.class).build();
            WriteSheet sheetCoopDutySwitch = EasyExcel.writerSheet(4, SHEET_COOP_DUTY_SWITCH).head(StaticCoopDutySwitchExportVO.class).build();
            excelWriter.write(java.util.Collections.emptyList(), sheetTask);
            excelWriter.write(java.util.Collections.emptyList(), sheetCreateGroup);
            excelWriter.write(java.util.Collections.emptyList(), sheetTaskResponse);
            excelWriter.write(java.util.Collections.emptyList(), sheetPhotoCheck);
            excelWriter.write(java.util.Collections.emptyList(), sheetCoopDutySwitch);

            List<CompletableFuture<Void>> futures = Arrays.asList(
                    writeSheetAsync(excelWriter, sheetTask, SHEET_TASK,
                            staticTaskMapper::selectByCursor, StaticTask::getCreateTime, StaticTask::getId,
                            this::convertTask, StaticTaskExportVO::new, deptIds, startDate, endExclusive),
                    writeSheetAsync(excelWriter, sheetCreateGroup, SHEET_CREATE_GROUP,
                            staticCreateGroupMapper::selectByCursor, StaticCreateGroup::getGmtCreateTime, StaticCreateGroup::getId,
                            this::convertCreateGroup, StaticCreateGroupExportVO::new, deptIds, startDate, endExclusive),
                    writeSheetAsync(excelWriter, sheetTaskResponse, SHEET_TASK_RESPONSE,
                            staticTaskResponseMapper::selectByCursor, StaticTaskResponse::getResponseTime, StaticTaskResponse::getId,
                            this::convertTaskResponse, StaticTaskResponseExportVO::new, deptIds, startDate, endExclusive),
                    writeSheetAsync(excelWriter, sheetPhotoCheck, SHEET_PHOTO_CHECK,
                            staticPhotoCheckMapper::selectByCursor, StaticPhotoCheck::getGmtCreateTime, StaticPhotoCheck::getId,
                            this::convertPhotoCheck, StaticPhotoCheckExportVO::new, deptIds, startDate, endExclusive),
                    writeSheetAsync(excelWriter, sheetCoopDutySwitch, SHEET_COOP_DUTY_SWITCH,
                            staticCoopDutySwitchMapper::selectByCursor, StaticCoopDutySwitch::getGmtCreateTime, StaticCoopDutySwitch::getId,
                            this::convertCoopDutySwitch, StaticCoopDutySwitchExportVO::new, deptIds, startDate, endExclusive)
            );
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException ce) {
                // unwrap CompletableFuture 包装异常，还原原始业务/系统异常
                throw unwrap(ce);
            }
        } catch (Exception e) {
            log.error("导出协同监测明细异常: departmentCode={}, startTime={}, endTime={}, includeChildren={}",
                    departmentCode, startTime, endTime, includeChildren, e);
            // response 已 committed（EasyExcel 已写入 header/部分字节）时不能再 throw e，
            // 否则全局 ExceptionAdvice 会向 xlsx 二进制流追加 JSON，导致下载文件损坏、Excel 打不开。
            if (!response.isCommitted()) {
                throw e;
            }
        } finally {
            // 先 finish ExcelWriter（flush 缓冲到 outputStream），再关闭底层流。
            // 即使 build() 抛异常导致 excelWriter==null，outputStream 也由 finally 兜底关闭。
            // finish() 单独 try-catch：避免 finish 失败导致 outputStream 未关闭、缓冲区数据丢失。
            if (excelWriter != null) {
                try {
                    excelWriter.finish();
                } catch (Exception finishEx) {
                    log.error("ExcelWriter finish 失败", finishEx);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ioe) {
                    log.warn("关闭导出响应流失败", ioe);
                }
            }
        }
    }

    /**
     * 解包 {@link CompletionException}，返回原始 cause。
     * 若 cause 是 {@link RuntimeException} 直接返回；否则包装为 {@link BusinessException}。
     */
    private RuntimeException unwrap(CompletionException ce) {
        Throwable cause = ce.getCause() != null ? ce.getCause() : ce;
        if (cause instanceof RuntimeException) {
            return (RuntimeException) cause;
        }
        return new BusinessException("导出失败: " + cause.getMessage(), cause);
    }

    /**
     * 校验时间范围不超过 6 个自然月。
     *
     * <p>用 {@link YearMonth} 计算自然月差值，防止用户导出过大时间范围导致数据量过大、响应超时。
     *
     * @throws BusinessException 时间跨度 > 6 自然月时抛出
     */
    private void validateHalfYearLimit(LocalDate startDate, LocalDate endDate) {
        YearMonth startYm = YearMonth.from(startDate);
        YearMonth endYm = YearMonth.from(endDate);
        long months = startYm.until(endYm, ChronoUnit.MONTHS);
        if (months > 6) {
            throw new BusinessException("导出时间范围不能超过半年");
        }
    }

    /**
     * 解析部门 ID 列表。
     *
     * <p>departmentCode 为空返回 {@code null}，表示不按部门筛选（Mapper 据此去掉部门 IN 条件，走 idx_time 全量索引）。
     * 非空时调用 {@link DepartmentRpcApi#queryDepartmentForList} 拿本级+子部门列表，
     * 再按 includeChildren 决定取全部（含子部门）或仅本级。
     *
     * @return 部门 ID 列表；{@code null} 表示不按部门筛
     * @throws BusinessException 部门不存在或本级匹配不到时抛出
     */
    private List<Long> resolveDepartmentIds(String departmentCode, Integer includeChildren) {
        if (StringUtils.isBlank(departmentCode)) {
            return null;
        }

        List<OrganizationVO> orgList = departmentRpcApi.queryDepartmentForList(departmentCode);
        if (orgList == null || orgList.isEmpty()) {
            throw new BusinessException("未找到部门信息：" + departmentCode);
        }

        if (includeChildren == null || includeChildren == 1) {
            return orgList.stream().map(OrganizationVO::getId).collect(Collectors.toList());
        }

        List<Long> filtered = orgList.stream()
                .filter(o -> departmentCode.equals(o.getCode()))
                .map(OrganizationVO::getId)
                .collect(Collectors.toList());
        if (filtered.isEmpty()) {
            throw new BusinessException("未找到匹配的本级部门：" + departmentCode);
        }
        return filtered;
    }

    /**
     * 单 sheet 异步写入：游标分页查 + 转 VO + 写 Excel。
     *
     * <p>查询与转换在 {@link #EXPORT_EXECUTOR} 线程并行执行；写 Excel 因 {@link ExcelWriter}
     * 非线程安全，用 {@code synchronized(writer)} 互斥（临界区仅为单批写入，极短）。
     *
     * <p>双游标推进（时间 DESC + 主键 DESC）防止同秒同分页边界记录漏读。
     *
     * <p>注意：传入的 {@code sheet} 必须由调用方在并行开始前串行预创建（write 空列表触发 POI
     * createSheet），否则并行场景下各线程争锁顺序不定，POI {@code createSheet(name)} 追加到末尾
     * 会导致 sheet 物理顺序失序。预创建后复用同一 WriteSheet 对象，EasyExcel 不会重复写表头。
     *
     * @param sheet       预创建好的 WriteSheet（含 sheetNo/sheetName/head）
     * @param sheetName   sheet 名称（仅日志用）
     * @param fetcher     游标查询方法引用（5 个 Mapper 的 selectByCursor 签名一致）
     * @param timeGetter  从 entity 取游标时间字段（各表字段名不同：create_time / gmt_create_time / response_time）
     * @param idGetter    从 entity 取主键（均为 getId）
     * @param converter   entity → VO 转换方法引用
     * @param emptyVoSupplier 空数据占位 VO 工厂（无参构造引用），用于空数据场景兜底
     * @param deptIds     部门 ID 列表，null 表示不按部门筛
     * @param startTime   闭区间下界
     * @param endTime     排他上界
     */
    private <E, V> CompletableFuture<Void> writeSheetAsync(ExcelWriter excelWriter,
                                                           WriteSheet sheet, String sheetName,
                                                           CursorFetcher<E> fetcher,
                                                           Function<E, Date> timeGetter,
                                                           Function<E, Long> idGetter,
                                                           Function<E, V> converter,
                                                           Supplier<V> emptyVoSupplier,
                                                           List<Long> deptIds, Date startTime, Date endTime) {
        return CompletableFuture.runAsync(() -> {
            long startMs = System.currentTimeMillis();
            int rowCount = 0;
            log.info("导出sheet[{}]开始: sheetNo={}, deptIdsSize={}, startTime={}, endTime={}",
                    sheetName, sheet.getSheetNo(), deptIds == null ? 0 : deptIds.size(), startTime, endTime);
            Date lastTime = null;
            Long lastId = null;
            try {
                while (true) {
                    List<E> batch = fetcher.fetch(deptIds, startTime, endTime, lastTime, lastId, PAGE_SIZE);
                    if (batch == null || batch.isEmpty()) {
                        break;
                    }
                    int batchSize = batch.size();
                    List<V> voList = batch.stream().map(converter).collect(Collectors.toList());
                    synchronized (excelWriter) {
                        excelWriter.write(voList, sheet);
                    }
                    rowCount += batchSize;
                    E last = batch.get(batch.size() - 1);
                    lastTime = timeGetter.apply(last);
                    lastId = idGetter.apply(last);
                    batch.clear();
                    voList.clear();
                    if (batchSize < PAGE_SIZE) {
                        break;
                    }
                }
                // 空数据兜底：EasyExcel 默认使用 SXSSFWorkbook（流式写），sheet 没有任何数据行时
                // finish() 生成的 xlsx 文件不完整/损坏（Excel 打不开）。写入一行空数据占位，
                // 触发 SXSSFWorkbook 正确序列化 sheet，确保导出文件始终可被 Excel 打开。
                if (rowCount == 0) {
                    V emptyVo = emptyVoSupplier.get();
                    synchronized (excelWriter) {
                        excelWriter.write(java.util.Collections.singletonList(emptyVo), sheet);
                    }
                    rowCount++;
                }
            } finally {
                log.info("导出sheet[{}]结束: rowCount={}, 耗时={}ms",
                        sheetName, rowCount, System.currentTimeMillis() - startMs);
            }
        }, EXPORT_EXECUTOR);
    }

    /**
     * 游标查询方法引用类型：5 个 Mapper 的 selectByCursor 签名一致，统一抽象。
     */
    @FunctionalInterface
    private interface CursorFetcher<E> {
        List<E> fetch(List<Long> deptIds, Date startTime, Date endTime,
                      Date lastTime, Long lastId, Integer pageSize);
    }

    /** 协同任务 entity → 导出 VO：状态枚举转中文、日期格式化、Long 转 String 防精度丢失。 */
    private StaticTaskExportVO convertTask(StaticTask e) {
        StaticTaskExportVO vo = new StaticTaskExportVO();
        vo.setId(longToStr(e.getId()));
        vo.setTaskId(longToStr(e.getTaskId()));
        vo.setFromUserId(longToStr(e.getFromUserId()));
        vo.setFromUserName(e.getFromUserName());
        vo.setFromUserDepartmentId(longToStr(e.getFromUserDepartmentId()));
        vo.setFromUserDepartmentName(e.getFromUserDepartmentName());
        vo.setStatus(StaticTaskStatusEnum.CODE_TO_NAME.getOrDefault(e.getStatus(), StringUtils.EMPTY));
        vo.setMsgSentTime(formatDate(e.getMsgSentTime()));
        vo.setMsgSeqid(longToStr(e.getMsgSeqid()));
        vo.setPostId(longToStr(e.getPostId()));
        vo.setPostName(e.getPostName());
        vo.setCreateTime(formatDate(e.getCreateTime()));
        vo.setExpiredTime(formatDate(e.getExpiredTime()));
        vo.setResponseTime(formatDate(e.getResponseTime()));
        vo.setResponseUserId(longToStr(e.getResponseUserId()));
        vo.setResponseUserName(e.getResponseUserName());
        vo.setIgnoreTime(formatDate(e.getIgnoreTime()));
        vo.setIgnoreUserId(longToStr(e.getIgnoreUserId()));
        vo.setIgnoreUserName(e.getIgnoreUserName());
        vo.setTrackTime(formatDate(e.getTrackTime()));
        vo.setTrackUserId(longToStr(e.getTrackUserId()));
        vo.setTrackUserName(e.getTrackUserName());
        vo.setFinishTime(formatDate(e.getFinishTime()));
        vo.setFinishUserId(longToStr(e.getFinishUserId()));
        vo.setFinishUserName(e.getFinishUserName());
        return vo;
    }

    /** 建群记录 entity → 导出 VO：群组类型/建群方式/建群入口枚举转中文、日期格式化、Long 转 String。 */
    private StaticCreateGroupExportVO convertCreateGroup(StaticCreateGroup e) {
        StaticCreateGroupExportVO vo = new StaticCreateGroupExportVO();
        vo.setId(longToStr(e.getId()));
        vo.setUserId(longToStr(e.getUserId()));
        vo.setUserName(e.getUserName());
        vo.setUserDepartmentId(longToStr(e.getUserDepartmentId()));
        vo.setUserDepartmentName(e.getUserDepartmentName());
        vo.setGroupId(longToStr(e.getGroupId()));
        vo.setGroupName(e.getGroupName());
        vo.setGroupType(GroupTypeEnum.CODE_TO_NAME.getOrDefault(e.getGroupType(), StringUtils.EMPTY));
        vo.setGroupSubType(GroupSubTypeEnum.CODE_TO_NAME.getOrDefault(e.getGroupSubType(), StringUtils.EMPTY));
        vo.setGmtCreateTime(formatDate(e.getGmtCreateTime()));
        return vo;
    }

    /** 任务回复 entity → 导出 VO：日期格式化、Long 转 String 防精度丢失。 */
    private StaticTaskResponseExportVO convertTaskResponse(StaticTaskResponse e) {
        StaticTaskResponseExportVO vo = new StaticTaskResponseExportVO();
        vo.setId(longToStr(e.getId()));
        vo.setTaskId(longToStr(e.getTaskId()));
        vo.setResponseUserId(longToStr(e.getResponseUserId()));
        vo.setResponseUserName(e.getResponseUserName());
        vo.setResponseUserDepartmentId(longToStr(e.getResponseUserDepartmentId()));
        vo.setResponseUserDepartmentName(e.getResponseUserDepartmentName());
        vo.setResponseCoopUserId(longToStr(e.getResponseCoopUserId()));
        vo.setResponseCoopUserName(e.getResponseCoopUserName());
        vo.setResponseTime(formatDate(e.getResponseTime()));
        vo.setResponseMsgSeqid(longToStr(e.getResponseMsgSeqid()));
        return vo;
    }

    /** 人员核查 entity → 导出 VO：核查结果枚举转中文、日期格式化、Long 转 String。 */
    private StaticPhotoCheckExportVO convertPhotoCheck(StaticPhotoCheck e) {
        StaticPhotoCheckExportVO vo = new StaticPhotoCheckExportVO();
        vo.setId(longToStr(e.getId()));
        vo.setCheckUserId(longToStr(e.getCheckUserId()));
        vo.setCheckUserName(e.getCheckUserName());
        vo.setCheckUserDepartmentId(longToStr(e.getCheckUserDepartmentId()));
        vo.setCheckUserDepartmentName(e.getCheckUserDepartmentName());
        vo.setCoopUserId(longToStr(e.getCoopUserId()));
        vo.setCoopUserName(e.getCoopUserName());
        vo.setCheckDataId(longToStr(e.getCheckDataId()));
        vo.setGmtCreateTime(formatDate(e.getGmtCreateTime()));
        return vo;
    }

    /** 协同岗上下岗 entity → 导出 VO：上下岗类型/上下岗标识枚举转中文、日期格式化、Long 转 String。 */
    private StaticCoopDutySwitchExportVO convertCoopDutySwitch(StaticCoopDutySwitch e) {
        StaticCoopDutySwitchExportVO vo = new StaticCoopDutySwitchExportVO();
        vo.setId(longToStr(e.getId()));
        vo.setUserId(longToStr(e.getUserId()));
        vo.setUserName(e.getUserName());
        vo.setUserDepartmentId(longToStr(e.getUserDepartmentId()));
        vo.setUserDepartmentName(e.getUserDepartmentName());
        vo.setCoopUserId(longToStr(e.getCoopUserId()));
        vo.setCoopUserName(e.getCoopUserName());
        vo.setSwithType(SwitchTypeEnum.CODE_TO_NAME.getOrDefault(e.getSwithType(), StringUtils.EMPTY));
        vo.setSwitchFlag(SwitchEnum.CODE_TO_NAME.getOrDefault(e.getSwitchFlag(), StringUtils.EMPTY));
        vo.setGmtCreateTime(formatDate(e.getGmtCreateTime()));
        return vo;
    }

    /**
     * Long 安全转字符串，{@code null} 返回空串（避免 Excel 单元格显示 "null"）。
     */
    private String longToStr(Long val) {
        return val == null ? StringUtils.EMPTY : String.valueOf(val);
    }

    /**
     * 日期格式化为 yyyy-MM-dd HH:mm:ss，{@code null} 返回空串。
     */
    private String formatDate(Date date) {
        if (date == null) {
            return StringUtils.EMPTY;
        }
        String formatted = DateFormatUtil.format(date, DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
        return StringUtils.isBlank(formatted) ? StringUtils.EMPTY : formatted;
    }

    /**
     * 给指定 sheet 的指定表头列添加 Excel 批注（Cell Comment）。
     *
     * <p>仅在表头写入时（{@code isHead=true}）触发，对数据行无影响。批注在 Excel 中鼠标悬停单元格
     * 时显示，不污染表头文字内容，适合补充"该列仅在某种条件下有值"这类业务说明。
     *
     * <p>本类为不可变对象（targetSheetName/targetColumnIndexes/comment 均 final），且 POI 批注操作
     * 仅在表头写入时触发（串行预创建阶段），无需考虑线程安全。
     */
    private static class HeadCommentWriteHandler implements CellWriteHandler {

        private final String targetSheetName;
        private final Set<Integer> targetColumnIndexes;
        private final String comment;

        HeadCommentWriteHandler(String targetSheetName, Set<Integer> targetColumnIndexes, String comment) {
            this.targetSheetName = targetSheetName;
            this.targetColumnIndexes = targetColumnIndexes;
            this.comment = comment;
        }

        @Override
        public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
                                     List<WriteCellData<?>> cellDataList, Cell cell, Head head,
                                     Integer relativeRowIndex, Boolean isHead) {
            if (!Boolean.TRUE.equals(isHead) || cell == null) {
                return;
            }
            Sheet sheet = cell.getSheet();
            if (sheet == null || !targetSheetName.equals(sheet.getSheetName())) {
                return;
            }
            if (!targetColumnIndexes.contains(cell.getColumnIndex())) {
                return;
            }
            Drawing<?> drawing = sheet.getDrawingPatriarch();
            if (drawing == null) {
                drawing = sheet.createDrawingPatriarch();
            }
            CreationHelper factory = sheet.getWorkbook().getCreationHelper();
            ClientAnchor anchor = factory.createClientAnchor();
            anchor.setCol1(cell.getColumnIndex());
            anchor.setRow1(cell.getRowIndex());
            anchor.setCol2(cell.getColumnIndex() + 2);
            anchor.setRow2(cell.getRowIndex() + 3);
            Comment commentObj = drawing.createCellComment(anchor);
            commentObj.setString(factory.createRichTextString(comment));
            cell.setCellComment(commentObj);
        }
    }
}