package com.tdtech.cloudcmd.im.jingxin.server.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.tdtech.cloudcmd.im.jingxin.server.excel.DutyScheduleExcel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试Excel文件生成工具
 * 运行此类生成测试用的Excel文件
 */
public class TestExcelGenerator {

    private static final String TEST_RESOURCES_PATH = "src/test/resources/";

    public static void main(String[] args) throws IOException {
        Path testResourcesDir = Paths.get(TEST_RESOURCES_PATH);
        if (!Files.exists(testResourcesDir)) {
            Files.createDirectories(testResourcesDir);
        }

        // 生成所有测试Excel文件
        generateSuccessExcel(testResourcesDir);
        generateCrossDayExcel(testResourcesDir);
        generateOverlapSameDayExcel(testResourcesDir);
        generateOverlapCrossDayExcel(testResourcesDir);

        System.out.println("测试Excel文件生成完成！路径: " + testResourcesDir.toAbsolutePath());
    }

    /**
     * 生成成功导入的Excel文件
     */
    private static void generateSuccessExcel(Path testResourcesDir) throws IOException {
        List<DutyScheduleExcel> dataList = new ArrayList<>();

        // 正常排班数据（同一天）
        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "08:00", "2026-06-01", "12:00", "白班", ""));
        dataList.add(createDutyScheduleExcel("1002", "李四", "2026-06-01", "14:00", "2026-06-01", "18:00", "白班", ""));
        dataList.add(createDutyScheduleExcel("1003", "王五", "2026-06-02", "08:00", "2026-06-02", "18:00", "全天班", ""));

        writeExcel(dataList, testResourcesDir.resolve("duty_schedule_success.xlsx").toString());
        System.out.println("生成: duty_schedule_success.xlsx");
    }

    /**
     * 生成跨天值班Excel文件
     */
    private static void generateCrossDayExcel(Path testResourcesDir) throws IOException {
        List<DutyScheduleExcel> dataList = new ArrayList<>();

        // 跨天值班数据
        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "20:00", "2026-06-02", "08:00", "夜班", ""));
        dataList.add(createDutyScheduleExcel("1002", "李四", "2026-06-02", "20:00", "2026-06-03", "08:00", "夜班", ""));
        // 跨两天值班
        dataList.add(createDutyScheduleExcel("1003", "王五", "2026-06-03", "08:00", "2026-06-05", "08:00", "长班", ""));

        writeExcel(dataList, testResourcesDir.resolve("duty_schedule_cross_day.xlsx").toString());
        System.out.println("生成: duty_schedule_cross_day.xlsx");
    }

    /**
     * 生成同一天重叠Excel文件
     */
    private static void generateOverlapSameDayExcel(Path testResourcesDir) throws IOException {
        List<DutyScheduleExcel> dataList = new ArrayList<>();

        // 同一天同一类型时间重叠
        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "08:00", "2026-06-01", "14:00", "白班", ""));
        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "12:00", "2026-06-01", "18:00", "白班", "")); // 与上一条重叠

        writeExcel(dataList, testResourcesDir.resolve("duty_schedule_overlap_same_day.xlsx").toString());
        System.out.println("生成: duty_schedule_overlap_same_day.xlsx");
    }

    /**
     * 生成跨天重叠Excel文件
     */
    private static void generateOverlapCrossDayExcel(Path testResourcesDir) throws IOException {
        List<DutyScheduleExcel> dataList = new ArrayList<>();

        // 跨天值班重叠
        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "08:00", "2026-06-02", "08:00", "夜班", ""));
        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "20:00", "2026-06-02", "20:00", "夜班", "")); // 与上一条重叠

        writeExcel(dataList, testResourcesDir.resolve("duty_schedule_overlap_cross_day.xlsx").toString());
        System.out.println("生成: duty_schedule_overlap_cross_day.xlsx");
    }

    /**
     * 创建DutyScheduleExcel对象
     */
    private static DutyScheduleExcel createDutyScheduleExcel(
            String userId, String userName,
            String dutyStartDate, String dutyStartTime,
            String dutyEndDate, String dutyEndTime,
            String dutyType, String dutyContent) {
        DutyScheduleExcel excel = new DutyScheduleExcel();
        excel.setUserId(userId);
        excel.setUserName(userName);
        excel.setDutyStartDate(dutyStartDate);
        excel.setDutyStartTime(dutyStartTime);
        excel.setDutyEndDate(dutyEndDate);
        excel.setDutyEndTime(dutyEndTime);
        excel.setDutyType(dutyType);
        excel.setDutyContent(dutyContent);
        return excel;
    }

    /**
     * 写入Excel文件
     */
    private static void writeExcel(List<DutyScheduleExcel> dataList, String filePath) throws IOException {
        try (ExcelWriter excelWriter = EasyExcel.write(filePath).head(DutyScheduleExcel.class).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("值班信息").build();
            excelWriter.write(dataList, writeSheet);
        }
    }
}
