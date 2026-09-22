package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyScheduleVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 跨天值班日历展开测试类
 * <p>
 * 测试跨天值班在日历视图中的展开逻辑：
 * - 同一天值班：只在当天显示，使用原始时间
 * - 跨天值班：在每一天都显示，时间拆分为多段
 *   - 第一天：开始时间 -> 23:59:59
 *   - 中间天：00:00:00 -> 23:59:59
 *   - 最后一天：00:00:00 -> 结束时间
 */
class DutyScheduleCalendarExpandTest {

//    // 一天的结束时间：23:59:59
//    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);
//    // 一天的开始时间：00:00:00
//    private static final LocalTime START_OF_DAY = LocalTime.of(0, 0, 0);
//
//    @Test
//    @DisplayName("同一天值班 - 使用原始时间")
//    void expand_sameDay_useOriginalTime() {
//        // 准备数据：同一天值班
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(8, 0),
//            LocalDate.of(2026, 6, 1), LocalTime.of(18, 0)));
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：只在6月1日显示
//        assertEquals(1, result.size());
//        assertTrue(result.containsKey("2026-06-01"));
//        assertEquals(1, result.get("2026-06-01").size());
//
//        // 验证时间：使用原始时间
//        DutyScheduleVO vo = result.get("2026-06-01").get(0);
//        assertEquals(LocalDate.of(2026, 6, 1), vo.getDisplayDate());
//        assertEquals(LocalTime.of(8, 0), vo.getDisplayStartTime());   // 08:00
//        assertEquals(LocalTime.of(18, 0), vo.getDisplayEndTime());    // 18:00
//    }
//
//    @Test
//    @DisplayName("跨两天值班 - 时间拆分为两段")
//    void expand_crossTwoDays_splitTime() {
//        // 准备数据：夜班跨两天 20:00 -> 次日 08:00
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 2), LocalTime.of(8, 0)));
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：在6月1日和6月2日都显示
//        assertEquals(2, result.size());
//
//        // 验证6月1日：20:00 -> 23:59:59
//        DutyScheduleVO vo1 = result.get("2026-06-01").get(0);
//        assertEquals(LocalDate.of(2026, 6, 1), vo1.getDisplayDate());
//        assertEquals(LocalTime.of(20, 0), vo1.getDisplayStartTime());      // 20:00
//        assertEquals(END_OF_DAY, vo1.getDisplayEndTime());                // 23:59:59
//
//        // 验证6月2日：00:00:00 -> 08:00
//        DutyScheduleVO vo2 = result.get("2026-06-02").get(0);
//        assertEquals(LocalDate.of(2026, 6, 2), vo2.getDisplayDate());
//        assertEquals(START_OF_DAY, vo2.getDisplayStartTime());            // 00:00:00
//        assertEquals(LocalTime.of(8, 0), vo2.getDisplayEndTime());         // 08:00
//    }
//
//    @Test
//    @DisplayName("跨三天值班 - 时间拆分为三段")
//    void expand_crossThreeDays_splitTime() {
//        // 准备数据：长班跨三天 6月1日 20:00 -> 6月3日 08:00
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 3), LocalTime.of(8, 0)));
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：在3天都显示
//        assertEquals(3, result.size());
//
//        // 验证6月1日（第一天）：20:00 -> 23:59:59
//        DutyScheduleVO vo1 = result.get("2026-06-01").get(0);
//        assertEquals(LocalTime.of(20, 0), vo1.getDisplayStartTime());
//        assertEquals(END_OF_DAY, vo1.getDisplayEndTime());
//
//        // 验证6月2日（中间天）：00:00:00 -> 23:59:59
//        DutyScheduleVO vo2 = result.get("2026-06-02").get(0);
//        assertEquals(START_OF_DAY, vo2.getDisplayStartTime());
//        assertEquals(END_OF_DAY, vo2.getDisplayEndTime());
//
//        // 验证6月3日（最后一天）：00:00:00 -> 08:00
//        DutyScheduleVO vo3 = result.get("2026-06-03").get(0);
//        assertEquals(START_OF_DAY, vo3.getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), vo3.getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("跨五天值班 - 时间拆分为五段")
//    void expand_crossFiveDays_splitTime() {
//        // 准备数据：长班跨五天
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(8, 0),
//            LocalDate.of(2026, 6, 5), LocalTime.of(18, 0)));
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：在5天都显示
//        assertEquals(5, result.size());
//
//        // 验证6月1日（第一天）：08:00 -> 23:59:59
//        DutyScheduleVO vo1 = result.get("2026-06-01").get(0);
//        assertEquals(LocalTime.of(8, 0), vo1.getDisplayStartTime());
//        assertEquals(END_OF_DAY, vo1.getDisplayEndTime());
//
//        // 验证6月2日、3日、4日（中间天）：00:00:00 -> 23:59:59
//        for (int i = 2; i <= 4; i++) {
//            String dateKey = String.format("2026-06-%02d", i);
//            DutyScheduleVO vo = result.get(dateKey).get(0);
//            assertEquals(START_OF_DAY, vo.getDisplayStartTime());
//            assertEquals(END_OF_DAY, vo.getDisplayEndTime());
//        }
//
//        // 验证6月5日（最后一天）：00:00:00 -> 18:00
//        DutyScheduleVO vo5 = result.get("2026-06-05").get(0);
//        assertEquals(START_OF_DAY, vo5.getDisplayStartTime());
//        assertEquals(LocalTime.of(18, 0), vo5.getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("多条跨天值班混合场景")
//    void expand_multipleCrossDayDuties_splitCorrectly() {
//        // 准备数据：
//        // 1. 6月1日白班（同一天）08:00-18:00
//        // 2. 6月1日夜班跨6月2日 20:00-08:00
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(8, 0),
//            LocalDate.of(2026, 6, 1), LocalTime.of(18, 0)));  // 白班
//        list.add(createDutyScheduleVO(2L, "李四",
//            LocalDate.of(2026, 6, 1), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 2), LocalTime.of(8, 0)));  // 夜班
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：2天都有数据
//        assertEquals(2, result.size());
//
//        // 验证6月1日：白班 + 夜班第一段
//        List<DutyScheduleVO> day1List = result.get("2026-06-01");
//        assertEquals(2, day1List.size());
//
//        // 白班：08:00-18:00
//        DutyScheduleVO dayShift = day1List.stream()
//            .filter(v -> v.getUserId().equals(1L))
//            .findFirst().orElseThrow();
//        assertEquals(LocalTime.of(8, 0), dayShift.getDisplayStartTime());
//        assertEquals(LocalTime.of(18, 0), dayShift.getDisplayEndTime());
//
//        // 夜班第一段：20:00-23:59:59
//        DutyScheduleVO nightShift1 = day1List.stream()
//            .filter(v -> v.getUserId().equals(2L))
//            .findFirst().orElseThrow();
//        assertEquals(LocalTime.of(20, 0), nightShift1.getDisplayStartTime());
//        assertEquals(END_OF_DAY, nightShift1.getDisplayEndTime());
//
//        // 验证6月2日：夜班第二段 00:00:00-08:00
//        List<DutyScheduleVO> day2List = result.get("2026-06-02");
//        assertEquals(1, day2List.size());
//        assertEquals(START_OF_DAY, day2List.get(0).getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), day2List.get(0).getDisplayEndTime());
//    }
//
//    @Test
//    @DisplayName("原始时间保持不变")
//    void expand_originalTimeUnchanged() {
//        // 准备数据：跨两天值班
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(20, 0),
//            LocalDate.of(2026, 6, 2), LocalTime.of(8, 0)));
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：原始时间不变
//        result.forEach((date, voList) -> {
//            voList.forEach(vo -> {
//                assertEquals(LocalDate.of(2026, 6, 1), vo.getDutyStartDate());
//                assertEquals(LocalDate.of(2026, 6, 2), vo.getDutyEndDate());
//                assertEquals(LocalTime.of(20, 0), vo.getDutyStartTime());
//                assertEquals(LocalTime.of(8, 0), vo.getDutyEndTime());
//            });
//        });
//    }
//
//    @Test
//    @DisplayName("按显示开始时间排序")
//    void expand_sortedByDisplayStartTime() {
//        // 准备数据：同一天多条排班，顺序打乱
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 1), LocalTime.of(14, 0),
//            LocalDate.of(2026, 6, 1), LocalTime.of(18, 0)));
//        list.add(createDutyScheduleVO(2L, "李四",
//            LocalDate.of(2026, 6, 1), LocalTime.of(8, 0),
//            LocalDate.of(2026, 6, 1), LocalTime.of(12, 0)));
//        list.add(createDutyScheduleVO(3L, "王五",
//            LocalDate.of(2026, 6, 1), LocalTime.of(10, 0),
//            LocalDate.of(2026, 6, 1), LocalTime.of(14, 0)));
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：按显示开始时间排序
//        List<DutyScheduleVO> dayList = result.get("2026-06-01");
//        assertEquals(3, dayList.size());
//        assertEquals(LocalTime.of(8, 0), dayList.get(0).getDisplayStartTime());
//        assertEquals(LocalTime.of(10, 0), dayList.get(1).getDisplayStartTime());
//        assertEquals(LocalTime.of(14, 0), dayList.get(2).getDisplayStartTime());
//    }
//
//    @Test
//    @DisplayName("跨月值班正确拆分")
//    void expand_crossMonth_splitCorrectly() {
//        // 准备数据：跨月值班（6月30日 20:00 到 7月2日 08:00）
//        List<DutyScheduleVO> list = new ArrayList<>();
//        list.add(createDutyScheduleVO(1L, "张三",
//            LocalDate.of(2026, 6, 30), LocalTime.of(20, 0),
//            LocalDate.of(2026, 7, 2), LocalTime.of(8, 0)));
//
//        // 执行展开
//        Map<String, List<DutyScheduleVO>> result = expandCrossDayDutySchedules(list);
//
//        // 验证：跨3天
//        assertEquals(3, result.size());
//
//        // 验证6月30日（第一天）：20:00 -> 23:59:59
//        assertEquals(LocalTime.of(20, 0), result.get("2026-06-30").get(0).getDisplayStartTime());
//        assertEquals(END_OF_DAY, result.get("2026-06-30").get(0).getDisplayEndTime());
//
//        // 验证7月1日（中间天）：00:00:00 -> 23:59:59
//        assertEquals(START_OF_DAY, result.get("2026-07-01").get(0).getDisplayStartTime());
//        assertEquals(END_OF_DAY, result.get("2026-07-01").get(0).getDisplayEndTime());
//
//        // 验证7月2日（最后一天）：00:00:00 -> 08:00
//        assertEquals(START_OF_DAY, result.get("2026-07-02").get(0).getDisplayStartTime());
//        assertEquals(LocalTime.of(8, 0), result.get("2026-07-02").get(0).getDisplayEndTime());
//    }
//
//    /**
//     * 创建测试用的DutyScheduleVO对象
//     */
//    private DutyScheduleVO createDutyScheduleVO(Long userId, String userName,
//                                                 LocalDate dutyStartDate, LocalTime dutyStartTime,
//                                                 LocalDate dutyEndDate, LocalTime dutyEndTime) {
//        DutyScheduleVO vo = new DutyScheduleVO();
//        vo.setId(System.currentTimeMillis());
//        vo.setUserId(userId);
//        vo.setUserName(userName);
//        vo.setDutyStartDate(dutyStartDate);
//        vo.setDutyStartTime(dutyStartTime);
//        vo.setDutyEndDate(dutyEndDate);
//        vo.setDutyEndTime(dutyEndTime);
//        return vo;
//    }
//
//    /**
//     * 展开跨天值班到每一天的日历中（与DutyScheduleServiceImpl中的实现一致）
//     */
//    private Map<String, List<DutyScheduleVO>> expandCrossDayDutySchedules(List<DutyScheduleVO> dutyScheduleVoList) {
//        Map<String, List<DutyScheduleVO>> resultMap = new LinkedHashMap<>();
//
//        for (DutyScheduleVO vo : dutyScheduleVoList) {
//            LocalDate startDate = vo.getDutyStartDate();
//            LocalDate endDate = vo.getDutyEndDate();
//            LocalTime startTime = vo.getDutyStartTime();
//            LocalTime endTime = vo.getDutyEndTime();
//
//            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
//                String dateKey = date.toString();
//
//                DutyScheduleVO dateVo = new DutyScheduleVO();
//                dateVo.setId(vo.getId());
//                dateVo.setUserId(vo.getUserId());
//                dateVo.setUserName(vo.getUserName());
//                dateVo.setDutyStartDate(vo.getDutyStartDate());
//                dateVo.setDutyStartTime(vo.getDutyStartTime());
//                dateVo.setDutyEndDate(vo.getDutyEndDate());
//                dateVo.setDutyEndTime(vo.getDutyEndTime());
//                dateVo.setDisplayDate(date);
//
//                // 计算该段的开始时间和结束时间
//                boolean isFirstDay = date.equals(startDate);
//                boolean isLastDay = date.equals(endDate);
//
//                if (isFirstDay && isLastDay) {
//                    // 同一天：使用原始时间
//                    dateVo.setDisplayStartTime(startTime);
//                    dateVo.setDisplayEndTime(endTime);
//                } else if (isFirstDay) {
//                    // 第一天：开始时间 -> 23:59:59
//                    dateVo.setDisplayStartTime(startTime);
//                    dateVo.setDisplayEndTime(END_OF_DAY);
//                } else if (isLastDay) {
//                    // 最后一天：00:00:00 -> 结束时间
//                    dateVo.setDisplayStartTime(START_OF_DAY);
//                    dateVo.setDisplayEndTime(endTime);
//                } else {
//                    // 中间天：00:00:00 -> 23:59:59
//                    dateVo.setDisplayStartTime(START_OF_DAY);
//                    dateVo.setDisplayEndTime(END_OF_DAY);
//                }
//
//                resultMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(dateVo);
//            }
//        }
//
//        resultMap.forEach((date, list) -> {
//            list.sort(java.util.Comparator.comparing(DutyScheduleVO::getDisplayStartTime));
//        });
//
//        return resultMap;
//    }
}
