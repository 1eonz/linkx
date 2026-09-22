//package com.tdtech.cloudcmd.im.jingxin.server.controller;
//
//import com.alibaba.excel.EasyExcel;
//import com.alibaba.excel.ExcelWriter;
//import com.alibaba.excel.write.metadata.WriteSheet;
//import com.tdtech.cloudcmd.im.jingxin.server.entity.ImportDutyScheduleResult;
//import com.tdtech.cloudcmd.im.jingxin.server.excel.DutyScheduleExcel;
//import com.tdtech.cloudcmd.im.jingxin.server.service.IDutyScheduleService;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.mock.web.MockMultipartFile;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
///**
// * DutyScheduleController 导入功能测试类
// */
//@ExtendWith(MockitoExtension.class)
//class DutyScheduleControllerTest {
//
//    private static final String TEST_RESOURCES_PATH = "src/test/resources/";
//    private static Path testResourcesDir;
//
//    @Mock
//    private IDutyScheduleService dutyScheduleService;
//
//    @InjectMocks
//    private DutyScheduleController dutyScheduleController;
//
//    private MockMvc mockMvc;
//
//    @BeforeAll
//    static void setupAll() throws IOException {
//        testResourcesDir = Paths.get(TEST_RESOURCES_PATH);
//        if (!Files.exists(testResourcesDir)) {
//            Files.createDirectories(testResourcesDir);
//        }
//        // 生成测试Excel文件
//        generateTestExcelFiles();
//    }
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.standaloneSetup(dutyScheduleController).build();
//    }
//
//    @Test
//    @DisplayName("导入排班信息 - 成功")
//    void importExcel_success() throws Exception {
//        // 准备mock结果
//        ImportDutyScheduleResult result = new ImportDutyScheduleResult();
//        result.setSuccessList(createSuccessList());
//        result.setErrorMap(new HashMap<>());
//        when(dutyScheduleService.importExcel(any())).thenReturn(result);
//
//        // 读取测试Excel文件
//        MockMultipartFile file = loadTestExcelFile("duty_schedule_success.xlsx");
//
//        // 执行测试 - code=0 表示成功
//        mockMvc.perform(multipart("/collaboration/duty/schedule/import").file(file))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(0));
//    }
//
//    @Test
//    @DisplayName("导入排班信息 - 跨天值班成功")
//    void importExcel_crossDay_success() throws Exception {
//        // 准备mock结果
//        ImportDutyScheduleResult result = new ImportDutyScheduleResult();
//        result.setSuccessList(createCrossDaySuccessList());
//        result.setErrorMap(new HashMap<>());
//        when(dutyScheduleService.importExcel(any())).thenReturn(result);
//
//        // 读取跨天值班测试文件
//        MockMultipartFile file = loadTestExcelFile("duty_schedule_cross_day.xlsx");
//
//        // 执行测试 - code=0 表示成功
//        mockMvc.perform(multipart("/collaboration/duty/schedule/import").file(file))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(0));
//    }
//
//    @Test
//    @DisplayName("导入排班信息 - 同一天重叠")
//    void importExcel_sameDay_overlap() throws Exception {
//        // 准备mock结果 - 包含重叠错误
//        ImportDutyScheduleResult result = new ImportDutyScheduleResult();
//        result.setSuccessList(new ArrayList<>());
//        Map<Integer, List<String>> errorMap = new HashMap<>();
//        errorMap.put(4, List.of("该用户同一类型存在时间重叠的排班计划"));
//        result.setErrorMap(errorMap);
//        when(dutyScheduleService.importExcel(any())).thenReturn(result);
//
//        // 读取重叠测试文件
//        MockMultipartFile file = loadTestExcelFile("duty_schedule_overlap_same_day.xlsx");
//
//        // 执行测试 - code=0 表示成功（接口调用成功，但数据有错误）
//        mockMvc.perform(multipart("/collaboration/duty/schedule/import").file(file))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(0));
//    }
//
//    @Test
//    @DisplayName("导入排班信息 - 跨天值班重叠")
//    void importExcel_crossDay_overlap() throws Exception {
//        // 准备mock结果 - 包含跨天重叠错误
//        ImportDutyScheduleResult result = new ImportDutyScheduleResult();
//        result.setSuccessList(new ArrayList<>());
//        Map<Integer, List<String>> errorMap = new HashMap<>();
//        errorMap.put(4, List.of("张三的排班时间2026-06-01 20:00至2026-06-02 20:00与现有排班2026-06-01 08:00至2026-06-02 08:00存在冲突"));
//        result.setErrorMap(errorMap);
//        when(dutyScheduleService.importExcel(any())).thenReturn(result);
//
//        // 读取跨天重叠测试文件
//        MockMultipartFile file = loadTestExcelFile("duty_schedule_overlap_cross_day.xlsx");
//
//        // 执行测试 - code=0 表示成功（接口调用成功，但数据有错误）
//        mockMvc.perform(multipart("/collaboration/duty/schedule/import").file(file))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.code").value(0));
//    }
//
//    /**
//     * 加载测试Excel文件
//     */
//    private MockMultipartFile loadTestExcelFile(String fileName) throws IOException {
//        Path filePath = testResourcesDir.resolve(fileName);
//        File file = filePath.toFile();
//        if (!file.exists()) {
//            throw new RuntimeException("测试文件不存在: " + filePath);
//        }
//        try (FileInputStream fis = new FileInputStream(file)) {
//            byte[] content = fis.readAllBytes();
//            return new MockMultipartFile(
//                    "file",
//                    fileName,
//                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
//                    content
//            );
//        }
//    }
//
//    /**
//     * 生成测试Excel文件
//     */
//    private static void generateTestExcelFiles() throws IOException {
//        // 1. 生成成功导入的测试文件
//        generateSuccessExcel();
//
//        // 2. 生成跨天值班测试文件
//        generateCrossDayExcel();
//
//        // 3. 生成同一天重叠测试文件
//        generateOverlapSameDayExcel();
//
//        // 4. 生成跨天重叠测试文件
//        generateOverlapCrossDayExcel();
//    }
//
//    /**
//     * 生成成功导入的Excel文件
//     */
//    private static void generateSuccessExcel() throws IOException {
//        List<DutyScheduleExcel> dataList = new ArrayList<>();
//
//        // 正常排班数据（同一天）
//        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "08:00", "2026-06-01", "12:00", "白班", ""));
//        dataList.add(createDutyScheduleExcel("1002", "李四", "2026-06-01", "14:00", "2026-06-01", "18:00", "白班", ""));
//        dataList.add(createDutyScheduleExcel("1003", "王五", "2026-06-02", "08:00", "2026-06-02", "18:00", "全天班", ""));
//
//        writeExcel(dataList, "duty_schedule_success.xlsx");
//    }
//
//    /**
//     * 生成跨天值班Excel文件
//     */
//    private static void generateCrossDayExcel() throws IOException {
//        List<DutyScheduleExcel> dataList = new ArrayList<>();
//
//        // 跨天值班数据
//        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "20:00", "2026-06-02", "08:00", "夜班", ""));
//        dataList.add(createDutyScheduleExcel("1002", "李四", "2026-06-02", "20:00", "2026-06-03", "08:00", "夜班", ""));
//        // 跨两天值班
//        dataList.add(createDutyScheduleExcel("1003", "王五", "2026-06-03", "08:00", "2026-06-05", "08:00", "长班", ""));
//
//        writeExcel(dataList, "duty_schedule_cross_day.xlsx");
//    }
//
//    /**
//     * 生成同一天重叠Excel文件
//     */
//    private static void generateOverlapSameDayExcel() throws IOException {
//        List<DutyScheduleExcel> dataList = new ArrayList<>();
//
//        // 同一天同一类型时间重叠
//        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "08:00", "2026-06-01", "14:00", "白班", ""));
//        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "12:00", "2026-06-01", "18:00", "白班", "")); // 与上一条重叠
//
//        writeExcel(dataList, "duty_schedule_overlap_same_day.xlsx");
//    }
//
//    /**
//     * 生成跨天重叠Excel文件
//     */
//    private static void generateOverlapCrossDayExcel() throws IOException {
//        List<DutyScheduleExcel> dataList = new ArrayList<>();
//
//        // 跨天值班重叠
//        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "08:00", "2026-06-02", "08:00", "夜班", ""));
//        dataList.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "20:00", "2026-06-02", "20:00", "夜班", "")); // 与上一条重叠
//
//        writeExcel(dataList, "duty_schedule_overlap_cross_day.xlsx");
//    }
//
//    /**
//     * 创建DutyScheduleExcel对象
//     */
//    private static DutyScheduleExcel createDutyScheduleExcel(
//            String userId, String userName,
//            String dutyStartDate, String dutyStartTime,
//            String dutyEndDate, String dutyEndTime,
//            String dutyType, String dutyContent) {
//        DutyScheduleExcel excel = new DutyScheduleExcel();
//        excel.setUserId(userId);
//        excel.setUserName(userName);
//        excel.setDutyStartDate(dutyStartDate);
//        excel.setDutyStartTime(dutyStartTime);
//        excel.setDutyEndDate(dutyEndDate);
//        excel.setDutyEndTime(dutyEndTime);
//        excel.setDutyType(dutyType);
//        excel.setDutyContent(dutyContent);
//        return excel;
//    }
//
//    /**
//     * 写入Excel文件
//     */
//    private static void writeExcel(List<DutyScheduleExcel> dataList, String fileName) throws IOException {
//        String filePath = testResourcesDir.resolve(fileName).toString();
//        try (ExcelWriter excelWriter = EasyExcel.write(filePath).head(DutyScheduleExcel.class).build()) {
//            WriteSheet writeSheet = EasyExcel.writerSheet("值班信息").build();
//            excelWriter.write(dataList, writeSheet);
//        }
//    }
//
//    /**
//     * 创建成功导入的数据列表
//     */
//    private List<DutyScheduleExcel> createSuccessList() {
//        List<DutyScheduleExcel> list = new ArrayList<>();
//        list.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "08:00", "2026-06-01", "12:00", "1", ""));
//        list.add(createDutyScheduleExcel("1002", "李四", "2026-06-01", "14:00", "2026-06-01", "18:00", "1", ""));
//        list.add(createDutyScheduleExcel("1003", "王五", "2026-06-02", "08:00", "2026-06-02", "18:00", "2", ""));
//        return list;
//    }
//
//    /**
//     * 创建跨天值班成功导入的数据列表
//     */
//    private List<DutyScheduleExcel> createCrossDaySuccessList() {
//        List<DutyScheduleExcel> list = new ArrayList<>();
//        list.add(createDutyScheduleExcel("1001", "张三", "2026-06-01", "20:00", "2026-06-02", "08:00", "1", ""));
//        list.add(createDutyScheduleExcel("1002", "李四", "2026-06-02", "20:00", "2026-06-03", "08:00", "1", ""));
//        list.add(createDutyScheduleExcel("1003", "王五", "2026-06-03", "08:00", "2026-06-05", "08:00", "2", ""));
//        return list;
//    }
//}
