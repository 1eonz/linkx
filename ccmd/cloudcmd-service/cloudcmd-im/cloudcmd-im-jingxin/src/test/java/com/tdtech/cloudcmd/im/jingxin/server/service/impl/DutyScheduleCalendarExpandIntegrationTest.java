package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutySchedule;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyScheduleVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.IDutyScheduleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 跨天值班日历展开集成测试类
 * <p>
 * 通过调用实际的 IDutyScheduleService.calendar() 接口进行测试，
 * 确保跨天值班在日历视图中正确展开。
 * <p>
 * 测试场景：
 * - 同一天值班：只在当天显示
 * - 跨天值班：在每一天都显示
 * - 多条跨天值班：正确展开到各天
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
class DutyScheduleCalendarExpandIntegrationTest {

//    @Resource
//    private IDutyScheduleService dutyScheduleService;
//
//    /**
//     * 测试数据ID列表，用于测试后清理
//     */
//    private List<Long> testDataIds = new ArrayList<>();
//
//    @AfterEach
//    void cleanup() {
//        // 清理测试数据
//        if (!testDataIds.isEmpty()) {
//            dutyScheduleService.removeByIds(testDataIds);
//            testDataIds.clear();
//        }
//    }

//    @Test
//    @DisplayName("同一天值班 - 只返回1条数据并验证时间")
//    void calendar_sameDay_returnOneRecord() {
//        // 准备数据：同一天值班
//        Long userId = 1001L;
//        LocalDate dutyDate = LocalDate.of(2026, 6, 1);
//        createDutySchedule(userId, "张三", dutyDate, LocalTime.of(8, 0), dutyDate, LocalTime.of(18, 0));
//
//        // 调用实际接口
//        Map<String, List<DutyScheduleVO>> result = dutyScheduleService.calendar(
//            String.valueOf(userId), null, null, null, "2026-06", null, null);
//
//        // 验证：只在6月1日显示
//        assertNotNull(result);
//        assertTrue(result.containsKey("2026-06-01"));
//        assertEquals(1, result.get("2026-06-01").size());
//
//        // 验证displayDate
//        DutyScheduleVO vo = result.get("2026-06-01").get(0);
//        assertEquals(LocalDate.of(2026, 6, 1), vo.getDisplayDate());
//
//        // 验证时间：同一天使用原始时间
//        assertEquals(LocalTime.of(8, 0), vo.getDisplayStartTime());
//        assertEquals(LocalTime.of(18, 0), vo.getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("跨两天值班 - 返回2条数据并验证时间拆分")
//    void calendar_crossTwoDays_returnTwoRecords() {
//        // 准备数据：夜班跨两天
//        Long userId = 1002L;
//        createDutySchedule(userId, "李四",
//            LocalDate.of(2026, 6, 1), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 2), LocalTime.of(8, 0));
//
//        // 调用实际接口
//        Map<String, List<DutyScheduleVO>> result = dutyScheduleService.calendar(
//            String.valueOf(userId), null, null, null, "2026-06", null, null);
//
//        // 验证：在6月1日和6月2日都显示
//        assertNotNull(result);
//        assertEquals(2, result.size());
//        assertTrue(result.containsKey("2026-06-01"));
//        assertTrue(result.containsKey("2026-06-02"));
//
//        // 验证每天的记录数
//        assertEquals(1, result.get("2026-06-01").size());
//        assertEquals(1, result.get("2026-06-02").size());
//
//        // 验证displayDate
//        assertEquals(LocalDate.of(2026, 6, 1), result.get("2026-06-01").get(0).getDisplayDate());
//        assertEquals(LocalDate.of(2026, 6, 2), result.get("2026-06-02").get(0).getDisplayDate());
//
//        // 验证时间拆分：6月1日 20:00 -> 23:59:59
//        DutyScheduleVO vo1 = result.get("2026-06-01").get(0);
//        assertEquals(LocalTime.of(20, 0), vo1.getDisplayStartTime());
//        assertEquals(LocalTime.of(23, 59, 59), vo1.getDisplayEndTime());
//
//        // 验证时间拆分：6月2日 00:00:00 -> 08:00
//        DutyScheduleVO vo2 = result.get("2026-06-02").get(0);
//        assertEquals(LocalTime.of(0, 0), vo2.getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), vo2.getDisplayEndTime());
//
//        // 验证原始时间不变
//        assertEquals(LocalDate.of(2026, 6, 1), vo1.getDutyStartDate());
//        assertEquals(LocalDate.of(2026, 6, 2), vo1.getDutyEndDate());
//        assertEquals(LocalTime.of(20, 0), vo1.getDutyStartTime());
//        assertEquals(LocalTime.of(8, 0), vo1.getDutyEndTime());
//    }
//
//    @Test
//    @DisplayName("跨五天值班 - 返回5条数据并验证时间拆分")
//    void calendar_crossFiveDays_returnFiveRecords() {
//        // 准备数据：长班跨五天
//        Long userId = 1003L;
//        createDutySchedule(userId, "王五",
//            LocalDate.of(2026, 6, 1), LocalTime.of(8, 0),
//            LocalDate.of(2026, 6, 5), LocalTime.of(18, 0));
//
//        // 调用实际接口
//        Map<String, List<DutyScheduleVO>> result = dutyScheduleService.calendar(
//            String.valueOf(userId), null, null, null, "2026-06", null, null);
//
//        // 验证：在5天都显示
//        assertNotNull(result);
//        assertEquals(5, result.size());
//        assertTrue(result.containsKey("2026-06-01"));
//        assertTrue(result.containsKey("2026-06-02"));
//        assertTrue(result.containsKey("2026-06-03"));
//        assertTrue(result.containsKey("2026-06-04"));
//        assertTrue(result.containsKey("2026-06-05"));
//
//        // 验证每天的记录数和displayDate
//        for (int i = 1; i <= 5; i++) {
//            String dateKey = String.format("2026-06-%02d", i);
//            assertEquals(1, result.get(dateKey).size());
//            assertEquals(LocalDate.of(2026, 6, i), result.get(dateKey).get(0).getDisplayDate());
//        }
//
//        // 验证时间拆分：6月1日（第一天）08:00 -> 23:59:59
//        DutyScheduleVO vo1 = result.get("2026-06-01").get(0);
//        assertEquals(LocalTime.of(8, 0), vo1.getDisplayStartTime());
//        assertEquals(LocalTime.of(23, 59, 59), vo1.getDisplayEndTime());
//
//        // 验证时间拆分：6月2日、3日、4日（中间天）00:00:00 -> 23:59:59
//        for (int i = 2; i <= 4; i++) {
//            String dateKey = String.format("2026-06-%02d", i);
//            DutyScheduleVO vo = result.get(dateKey).get(0);
//            assertEquals(LocalTime.of(0, 0), vo.getDisplayStartTime());
//            assertEquals(LocalTime.of(23, 59, 59), vo.getDisplayEndTime());
//        }
//
//        // 验证时间拆分：6月5日（最后一天）00:00:00 -> 18:00
//        DutyScheduleVO vo5 = result.get("2026-06-05").get(0);
//        assertEquals(LocalTime.of(0, 0), vo5.getDisplayStartTime());
//        assertEquals(LocalTime.of(18, 0), vo5.getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("多条跨天值班混合场景并验证时间拆分")
//    void calendar_multipleCrossDayDuties_returnCorrectRecords() {
//        // 准备数据：
//        // 1. 6月1日白班（同一天）
//        // 2. 6月1日夜班跨6月2日
//        // 3. 6月2日夜班跨6月3日
//        Long userId1 = 1011L;
//        Long userId2 = 1012L;
//        Long userId3 = 1013L;
//
//        createDutySchedule(userId1, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(8, 0),
//            LocalDate.of(2026, 6, 1), LocalTime.of(18, 0));  // 白班
//
//        createDutySchedule(userId2, "李四",
//            LocalDate.of(2026, 6, 1), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 2), LocalTime.of(8, 0));  // 夜班1
//
//        createDutySchedule(userId3, "王五",
//            LocalDate.of(2026, 6, 2), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 3), LocalTime.of(8, 0));  // 夜班2
//
//        // 调用实际接口（查询所有用户）
//        Map<String, List<DutyScheduleVO>> result = dutyScheduleService.calendar(
//            null, null, null, null, "2026-06", null, null);
//
//        // 验证：3天都有数据
//        assertNotNull(result);
//        assertTrue(result.size() >= 3);
//
//        // 验证6月1日：白班 + 夜班1 = 2条
//        assertTrue(result.containsKey("2026-06-01"));
//        long count1 = result.get("2026-06-01").stream()
//            .filter(v -> v.getUserId().equals(userId1) || v.getUserId().equals(userId2))
//            .count();
//        assertEquals(2, count1);
//
//        // 验证白班时间（同一天）：08:00 -> 18:00
//        DutyScheduleVO dayShift = result.get("2026-06-01").stream()
//            .filter(v -> v.getUserId().equals(userId1))
//            .findFirst().orElseThrow();
//        assertEquals(LocalTime.of(8, 0), dayShift.getDisplayStartTime());
//        assertEquals(LocalTime.of(18, 0), dayShift.getDisplayEndTime());
//
//        // 验证夜班1在6月1日时间拆分：20:00 -> 23:59:59
//        DutyScheduleVO nightShift1Day1 = result.get("2026-06-01").stream()
//            .filter(v -> v.getUserId().equals(userId2))
//            .findFirst().orElseThrow();
//        assertEquals(LocalTime.of(20, 0), nightShift1Day1.getDisplayStartTime());
//        assertEquals(LocalTime.of(23, 59, 59), nightShift1Day1.getDisplayEndTime());
//
//        // 验证6月2日：夜班1 + 夜班2 = 2条
//        assertTrue(result.containsKey("2026-06-02"));
//        long count2 = result.get("2026-06-02").stream()
//            .filter(v -> v.getUserId().equals(userId2) || v.getUserId().equals(userId3))
//            .count();
//        assertEquals(2, count2);
//
//        // 验证夜班1在6月2日时间拆分：00:00:00 -> 08:00
//        DutyScheduleVO nightShift1Day2 = result.get("2026-06-02").stream()
//            .filter(v -> v.getUserId().equals(userId2))
//            .findFirst().orElseThrow();
//        assertEquals(LocalTime.of(0, 0), nightShift1Day2.getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), nightShift1Day2.getDisplayEndTime());
//
//        // 验证夜班2在6月2日时间拆分：20:00 -> 23:59:59
//        DutyScheduleVO nightShift2Day2 = result.get("2026-06-02").stream()
//            .filter(v -> v.getUserId().equals(userId3))
//            .findFirst().orElseThrow();
//        assertEquals(LocalTime.of(20, 0), nightShift2Day2.getDisplayStartTime());
//        assertEquals(LocalTime.of(23, 59, 59), nightShift2Day2.getDisplayEndTime());
//
//        // 验证6月3日：夜班2 = 1条
//        assertTrue(result.containsKey("2026-06-03"));
//        long count3 = result.get("2026-06-03").stream()
//            .filter(v -> v.getUserId().equals(userId3))
//            .count();
//        assertEquals(1, count3);
//
//        // 验证夜班2在6月3日时间拆分：00:00:00 -> 08:00
//        DutyScheduleVO nightShift2Day3 = result.get("2026-06-03").stream()
//            .filter(v -> v.getUserId().equals(userId3))
//            .findFirst().orElseThrow();
//        assertEquals(LocalTime.of(0, 0), nightShift2Day3.getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), nightShift2Day3.getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("同一用户多条跨天值班并验证时间拆分")
//    void calendar_sameUserMultipleCrossDayDuties_returnCorrectRecords() {
//        // 准备数据：同一用户连续三天夜班
//        Long userId = 1021L;
//        createDutySchedule(userId, "赵六",
//            LocalDate.of(2026, 6, 1), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 2), LocalTime.of(8, 0));
//        createDutySchedule(userId, "赵六",
//            LocalDate.of(2026, 6, 2), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 3), LocalTime.of(8, 0));
//        createDutySchedule(userId, "赵六",
//            LocalDate.of(2026, 6, 3), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 4), LocalTime.of(8, 0));
//
//        // 调用实际接口
//        Map<String, List<DutyScheduleVO>> result = dutyScheduleService.calendar(
//            String.valueOf(userId), null, null, null, "2026-06", null, null);
//
//        // 验证：4天都有数据
//        assertNotNull(result);
//        assertEquals(4, result.size());
//
//        // 验证每天的记录数
//        assertEquals(1, result.get("2026-06-01").size());  // 夜班1
//        assertEquals(2, result.get("2026-06-02").size());  // 夜班1 + 夜班2
//        assertEquals(2, result.get("2026-06-03").size());  // 夜班2 + 夜班3
//        assertEquals(1, result.get("2026-06-04").size());  // 夜班3
//
//        // 验证所有记录都是同一用户
//        result.forEach((date, voList) -> {
//            voList.forEach(vo -> {
//                assertEquals(userId, vo.getUserId());
//                assertEquals("赵六", vo.getUserName());
//            });
//        });
//
//        // 验证6月1日夜班1时间拆分：20:00 -> 23:59:59
//        DutyScheduleVO vo1 = result.get("2026-06-01").get(0);
//        assertEquals(LocalTime.of(20, 0), vo1.getDisplayStartTime());
//        assertEquals(LocalTime.of(23, 59, 59), vo1.getDisplayEndTime());
//
//        // 验证6月4日夜班3时间拆分：00:00:00 -> 08:00
//        DutyScheduleVO vo4 = result.get("2026-06-04").get(0);
//        assertEquals(LocalTime.of(0, 0), vo4.getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), vo4.getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("跨月值班正确展开并验证时间拆分")
//    void calendar_crossMonth_returnCorrectRecords() {
//        // 准备数据：跨月值班（6月30日到7月2日）
//        Long userId = 1031L;
//        createDutySchedule(userId, "钱七",
//            LocalDate.of(2026, 6, 30), LocalTime.of(20, 0),
//            LocalDate.of(2026, 7, 2), LocalTime.of(8, 0));
//
//        // 调用实际接口（查询6月）
//        Map<String, List<DutyScheduleVO>> resultJune = dutyScheduleService.calendar(
//            String.valueOf(userId), null, null, null, "2026-06", null, null);
//
//        // 验证6月：只有6月30日
//        assertNotNull(resultJune);
//        assertTrue(resultJune.containsKey("2026-06-30"));
//        assertEquals(1, resultJune.get("2026-06-30").size());
//
//        // 验证6月30日时间拆分：20:00 -> 23:59:59
//        DutyScheduleVO voJune = resultJune.get("2026-06-30").get(0);
//        assertEquals(LocalTime.of(20, 0), voJune.getDisplayStartTime());
//        assertEquals(LocalTime.of(23, 59, 59), voJune.getDisplayEndTime());
//
//        // 调用实际接口（查询7月）
//        Map<String, List<DutyScheduleVO>> resultJuly = dutyScheduleService.calendar(
//            String.valueOf(userId), null, null, null, "2026-07", null, null);
//
//        // 验证7月：7月1日和7月2日
//        assertNotNull(resultJuly);
//        assertEquals(2, resultJuly.size());
//        assertTrue(resultJuly.containsKey("2026-07-01"));
//        assertTrue(resultJuly.containsKey("2026-07-02"));
//
//        // 验证7月1日时间拆分（中间天）：00:00:00 -> 23:59:59
//        DutyScheduleVO voJuly1 = resultJuly.get("2026-07-01").get(0);
//        assertEquals(LocalTime.of(0, 0), voJuly1.getDisplayStartTime());
//        assertEquals(LocalTime.of(23, 59, 59), voJuly1.getDisplayEndTime());
//
//        // 验证7月2日时间拆分（最后一天）：00:00:00 -> 08:00
//        DutyScheduleVO voJuly2 = resultJuly.get("2026-07-02").get(0);
//        assertEquals(LocalTime.of(0, 0), voJuly2.getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), voJuly2.getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("按日期范围查询跨天值班")
//    void calendar_dateRangeQuery_returnCorrectRecords() {
//        // 准备数据：跨五天值班
//        Long userId = 1041L;
//        createDutySchedule(userId, "孙八",
//            LocalDate.of(2026, 6, 1), LocalTime.of(8, 0),
//            LocalDate.of(2026, 6, 5), LocalTime.of(18, 0));
//
//        // 调用实际接口（查询6月2日到6月4日）
//        Map<String, List<DutyScheduleVO>> result = dutyScheduleService.calendar(
//            String.valueOf(userId), null, "2026-06-02", "2026-06-04", null, null, null);
//
//        // 验证：返回3天
//        assertNotNull(result);
//        assertEquals(3, result.size());
//        assertTrue(result.containsKey("2026-06-02"));
//        assertTrue(result.containsKey("2026-06-03"));
//        assertTrue(result.containsKey("2026-06-04"));
//    }
//
//    /**
//     * 创建测试用的排班记录
//     */
//    private void createDutySchedule(Long userId, String userName,
//                                    LocalDate dutyStartDate, LocalTime dutyStartTime,
//                                    LocalDate dutyEndDate, LocalTime dutyEndTime) {
//        DutySchedule dutySchedule = new DutySchedule();
//        dutySchedule.setUserId(userId);
//        dutySchedule.setUserName(userName);
//        dutySchedule.setDutyStartDate(dutyStartDate);
//        dutySchedule.setDutyStartTime(dutyStartTime);
//        dutySchedule.setDutyEndDate(dutyEndDate);
//        dutySchedule.setDutyEndTime(dutyEndTime);
//        dutySchedule.setDutyType(1L);  // 默认排班类型
//        dutySchedule.setGmtCreated(new Date());
//        dutySchedule.setGmtModified(new Date());
//
//        // 保存到数据库
//        dutyScheduleService.save(dutySchedule);
//        testDataIds.add(dutySchedule.getId());
//    }
}
