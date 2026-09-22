package com.tdtech.cloudcmd.linkx.dashboard.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.linkx.dashboard.entity.StatisticLogin;
import com.tdtech.cloudcmd.linkx.dashboard.enums.StatisticLoginClientTypeEnum;
import com.tdtech.cloudcmd.linkx.dashboard.enums.StatisticLoginResultEnum;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.StatisticLoginMapper;
import com.tdtech.cloudcmd.linkx.dashboard.service.IStatisticLoginService;
import com.tdtech.cloudcmd.linkx.dashboard.vo.StatisticLoginVO;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import dto.StatisticLoginDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LINKX增加日活数据统计
 */
@Slf4j
@Service
public class StatisticLoginServiceImpl implements IStatisticLoginService {
    // 单页数据量（可根据服务器内存调整，建议1000-5000，避免OOM）
    private static final int PAGE_SIZE = 5000;

    // 模板文件路径（resources/templates/ 下）
    private static final String TEMPLATE_PATH = "templates/statistic_login_export_template.xlsx";

    @Resource
    private StatisticLoginMapper statisticLoginMapper;

    @Override
    public void statisticLogin(StatisticLoginDTO... statisticLogins) {
        try {
            if (ArrayUtils.isEmpty(statisticLogins)) {
                log.info("Statistic logins is empty.");
                return;
            }

            List<StatisticLogin> dataList = BeanCopyUtils.copyList(Arrays.asList(statisticLogins),
                    StatisticLogin::new);
            statisticLoginMapper.insert(dataList);
            log.info("insert statisticLogins: {}", dataList);
        } catch (Exception e) {
            log.error("Statistic login error.", e);
        }
    }

    private Pair<Date, Date> getStartDateAndEndDatePair(String startTime, String endTime) {
        Date startDate;
        Date endDate;
        if (StringUtils.isBlank(startTime)) {
            endDate = StringUtils.isBlank(endTime) ? new Date()
                    : DateFormatUtil.parseDate(endTime, DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
            startDate = DateUtils.addYears(endDate, -1);
        } else if (StringUtils.isBlank(endTime)) {
            startDate = DateFormatUtil.parseDate(startTime, DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
            endDate = new Date();
        } else {
            startDate = DateFormatUtil.parseDate(startTime, DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
            endDate = DateFormatUtil.parseDate(endTime, DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
        }

        return MutablePair.of(startDate, endDate);
    }

    @Override
    public void export(String startTime, String endTime, HttpServletResponse response) throws IOException {
        Pair<Date, Date> startDateAndEndDatePair = getStartDateAndEndDatePair(startTime, endTime);

        // 1. 先查询总条数，计算总页数
        long totalCount = statisticLoginMapper.selectCount(Wrappers.lambdaQuery(StatisticLogin.class)
                .between(StatisticLogin::getLoginTime, startDateAndEndDatePair.getLeft(),
                        startDateAndEndDatePair.getRight()));

        // 2. 设置响应头，解决中文文件名乱码、前端下载识别
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("日活数据统计表", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-disposition",
                "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 3. 加载Excel模板
        ClassPathResource templateResource = new ClassPathResource(TEMPLATE_PATH);
        try (InputStream templateInputStream = templateResource.getInputStream(); BufferedOutputStream bos =
                new BufferedOutputStream(response.getOutputStream()); ExcelWriter excelWriter =
                     EasyExcel.write(response.getOutputStream())
                             .withTemplate(templateInputStream)
                             .autoCloseStream(false)
                             .build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet().build();
            if (totalCount <= 0) {
                excelWriter.fill(Collections.emptyList(), writeSheet);
                log.info("日活导出完成，无数据导出。");
                return;
            }

            // ========== 4. 游标分页循环写入 ==========
            long lastId = 0L;
            int currentPage = 0;
            long startTimeMs = System.currentTimeMillis();
            long totalPage = (totalCount + PAGE_SIZE - 1) / PAGE_SIZE;

            while (true) {
                // 分批查询
                currentPage++;
                long perStartTimeMs = System.currentTimeMillis();
                List<StatisticLogin> batchList = statisticLoginMapper.selectByLoginTimeAndCursor(
                        startDateAndEndDatePair.getLeft(),
                        startDateAndEndDatePair.getRight(), lastId, PAGE_SIZE);

                if (CollectionUtils.isEmpty(batchList)) {
                    break;
                }

                if (batchList.size() < PAGE_SIZE) {
                    writeExcel(excelWriter, batchList, writeSheet, currentPage, totalPage, perStartTimeMs);
                    break;
                }

                lastId = writeExcel(excelWriter, batchList, writeSheet, currentPage, totalPage, perStartTimeMs);
            }

            bos.flush();
            log.info("导出完成，共{}条，总耗时{}ms", totalCount, System.currentTimeMillis() - startTimeMs);
        }
    }

    private long writeExcel(ExcelWriter excelWriter, List<StatisticLogin> batchList, WriteSheet writeSheet,
                            int currentPage, long totalPage, long startTimeMs) {
        // 转换为Excel导出实体（和模板占位符对应）
        List<StatisticLoginVO> excelVOList = batchList.stream()
                .map(this::entityConvertToVO)
                .collect(Collectors.toList());

        // 写入当前页数据到模板，自动追加行，不覆盖已有内容
        excelWriter.fill(excelVOList, writeSheet);

        // 更新游标
        long lastId = batchList.get(batchList.size() - 1).getId();

        // 清空集合，释放内存
        batchList.clear();
        excelVOList.clear();

        log.info("导出进度：{}/{}，本批次耗时 {}ms", currentPage, totalPage, System.currentTimeMillis() - startTimeMs);
        return lastId;
    }

    private StatisticLoginVO entityConvertToVO(StatisticLogin entity) {
        StatisticLoginVO vo = new StatisticLoginVO();
        vo.setUserId(entity.getUserId());
        vo.setAppId(entity.getAppId());
        vo.setClientType(StatisticLoginClientTypeEnum.CODE_TO_CLIENT_TYPE
                .getOrDefault(entity.getClientType(), StringUtils.EMPTY));
        vo.setOs(entity.getOs());
        vo.setBrowser(entity.getBrowser());
        vo.setScreen(entity.getScreen());
        String loginTime = DateFormatUtil.format(entity.getLoginTime(), DateFormatUtil.YYYY_MM_DD_HH_MM_SS);
        vo.setLoginTime(StringUtils.isBlank(loginTime) ? StringUtils.EMPTY : loginTime);
        vo.setLoginResult(StatisticLoginResultEnum.CODE_TO_LOGIN_RESULT
                .getOrDefault(entity.getLoginResult(), StringUtils.EMPTY));
        return vo;
    }
}
