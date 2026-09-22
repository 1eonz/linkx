package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutySchedule;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyScheduleVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ImportDutyScheduleResult;
import com.tdtech.cloudcmd.im.jingxin.server.service.IDutyScheduleService;
import com.tdtech.cloudcmd.util.CollectionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 值班信息管理控制器
 * <p>
 * 提供值班排班的各项功能，包括：
 * - 模板下载：下载排班信息导入模板
 * - 数据导入：通过Excel批量导入排班数据
 * - 分页查询：按条件分页查询排班信息
 * - 日历查询：按日期/月份查询排班日历数据
 * - 批量删除：批量删除排班记录
 * - 在岗查询：获取当前正在值班的人员列表
 * <p>
 * 支持跨天值班场景，排班数据包含：值班开始日期+时间、值班结束日期+时间
 *
 * @author ly
 * @since 1.0.0
 */
@Tag(name = "值班信息")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/duty/schedule")
@RequiredArgsConstructor
@Validated
public class DutyScheduleController {

    @Resource
    private IDutyScheduleService dutyScheduleService;

    /**
     * 下载排班信息导入模板
     * <p>
     * 生成Excel模板文件，包含以下内容：
     * - 主Sheet：排班信息录入区域，包含人员ID、姓名、值班开始日期、值班开始时间、
     *   值班结束日期、值班结束时间、排班类型、值班内容等列
     * - 用户Sheet：可选的用户列表，方便填写人员信息
     * - 排班类型下拉框：在主Sheet中提供排班类型的下拉选择
     * <p>
     * 模板格式说明：
     * - 第一行：标题行
     * - 第二行：表头行
     * - 第三行起：数据行
     *
     * @param response HTTP响应对象，用于输出Excel文件流
     * @throws IOException 文件读写异常
     */
    @Operation(summary = "排班信息模板下载", description = "下载排班信息Excel导入模板，包含人员信息和排班类型下拉选项")
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        dutyScheduleService.template(response);
    }

    /**
     * 导入排班信息
     * <p>
     * 通过Excel文件批量导入排班数据，执行以下处理：
     * 1. 解析Excel文件内容
     * 2. 数据校验：必填项、格式、业务规则
     * 3. 重叠检测：检查同一用户同一类型是否存在时间重叠
     * 4. 数据入库：保存有效的排班记录
     * <p>
     * 导入规则：
     * - 完全相同的记录（用户、时间、类型）会被替换
     * - 时间重叠的记录会被标记为无效，不导入
     * - 支持跨天值班：值班开始和结束可以是不同日期
     * <p>
     * 返回结果包含：
     * - successList：成功导入的记录列表
     * - errorMap：导入失败的记录及错误信息（key为行号，value为错误信息列表）
     *
     * @param file 上传的Excel文件
     * @return 导入结果，包含成功列表和错误信息
     * @throws IOException 文件读写异常
     */
    @Operation(summary = "排班信息导入", description = "通过Excel批量导入排班数据，支持数据校验和重叠检测")
    @PostMapping("/import")
    public R<ImportDutyScheduleResult> importExcel(MultipartFile file) throws IOException {
        return R.success(dutyScheduleService.importExcel(file));
    }

    /**
     * 分页查询排班信息
     * <p>
     * 按条件分页查询排班记录，支持以下查询条件：
     * - 支撑人员ID：精确匹配
     * - 支撑人员姓名：模糊匹配
     * - 值班日期范围：查询与指定日期范围有交集的排班
     * - 部门ID：按部门筛选
     * - 排班类型：按类型筛选
     * <p>
     * 日期范围查询说明（支持跨天值班）：
     * - 查询条件：值班开始日期 <= endDate AND 值班结束日期 >= startDate
     * - 例如：查询6月1日的排班，会查出6月1日当天开始的、以及跨天覆盖6月1日的排班
     * <p>
     * 返回结果按值班开始日期、开始时间、用户ID排序。
     *
     * @param pageNum      当前页码，默认1
     * @param pageSize     每页大小，默认10
     * @param userId       支撑人员ID（可选）
     * @param userName     支撑人员姓名（可选，模糊匹配）
     * @param startDate    值班开始日期，格式yyyy-MM-dd（可选）
     * @param endDate      值班结束日期，格式yyyy-MM-dd（可选）
     * @param departmentId 部门ID（可选）
     * @param dutyType     排班类型（可选）
     * @return 分页结果，包含排班记录列表和分页信息
     */
    @Operation(summary = "分页查询排班信息", description = "按条件分页查询排班记录，支持日期范围、人员、部门、类型等筛选条件")
    @GetMapping("/page")
    public Page<DutyScheduleVO> getPage(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页大小", example = "10") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "支撑人员ID") @RequestParam(required = false) String userId,
            @Parameter(description = "支撑人员姓名（支持模糊查询）") @RequestParam(required = false) String userName,
            @Parameter(description = "值班开始日期，格式yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "值班结束日期，格式yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "排班类型") @RequestParam(required = false) Long dutyType) {
        return dutyScheduleService.findPage(pageNum, pageSize, userId, userName, startDate, endDate, departmentId,
            dutyType);
    }

    /**
     * 查询排班日历信息
     * <p>
     * 按条件查询排班数据，并按日期分组返回，适用于日历视图展示。
     * 支持以下查询条件：
     * - 支撑人员ID：精确匹配
     * - 支撑人员姓名：模糊匹配，支持按部门名称搜索
     * - 日期范围：查询与指定范围有交集的排班
     * - 月份：查询指定月份的所有排班，格式yyyy-MM
     * - 部门ID：按部门筛选
     * - 排班类型：按类型筛选
     * <p>
     * 返回结果说明：
     * - Map的key为日期字符串（yyyy-MM-dd格式）
     * - Map的value为该日期的排班记录列表，按开始时间排序
     * - 使用LinkedHashMap保证日期顺序
     * <p>
     * 跨天值班处理：
     * - 跨天值班会出现在开始日期和结束日期两天的日历中
     * - 例如：6月1日20:00至6月2日08:00的夜班，会出现在6月1日和6月2日的日历中
     *
     * @param userId       支撑人员ID（可选）
     * @param userName     支撑人员姓名或部门名称（可选，模糊查询）
     * @param startDate    值班开始日期，格式yyyy-MM-dd（可选）
     * @param endDate      值班结束日期，格式yyyy-MM-dd（可选）
     * @param month        月份，格式yyyy-MM（可选，如2026-06）
     * @param departmentId 部门ID（可选）
     * @param dutyType     排班类型（可选）
     * @return 按日期分组的排班记录Map
     */
    @Operation(summary = "查询排班日历信息", description = "按日期分组查询排班数据，适用于日历视图展示，支持跨天值班")
    @GetMapping("/calendar")
    public R<Map<String, List<DutyScheduleVO>>> calendar(
            @Parameter(description = "支撑人员ID") @RequestParam(required = false) String userId,
            @Parameter(description = "支撑人员姓名或部门名称（支持模糊查询）") @RequestParam(required = false) String userName,
            @Parameter(description = "值班开始日期，格式yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "值班结束日期，格式yyyy-MM-dd") @RequestParam(required = false) String endDate,
            @Parameter(description = "月份，格式yyyy-MM，如2026-06") @RequestParam(required = false) String month,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "排班类型") @RequestParam(required = false) Long dutyType) {
        return R.success(dutyScheduleService.calendar(userId, userName, startDate, endDate, month, departmentId,
            dutyType));
    }

    /**
     * 批量删除值班信息
     * <p>
     * 根据ID列表批量删除排班记录，执行以下校验：
     * 1. 检查ID列表是否为空
     * 2. 检查数据是否存在
     * 3. 执行批量删除
     * <p>
     * 删除说明：
     * - 物理删除，不可恢复
     * - 不检查是否为历史排班，允许删除任意记录
     *
     * @param ids 值班信息ID列表
     * @return 删除结果，成功返回true，失败返回错误信息
     */
    @Operation(summary = "批量删除值班信息", description = "根据ID列表批量删除排班记录，物理删除不可恢复")
    @DeleteMapping("/deleteBatch")
    public R<Boolean> deleteBatch(
            @Parameter(description = "值班信息ID列表", required = true) @RequestBody List<Long> ids) {
        List<DutySchedule> dataList = dutyScheduleService.listByIds(ids);
        if (CollectionUtils.isEmpty(dataList)) {
            return R.failure("数据不存在");
        }
        dutyScheduleService.removeByIds(ids);
        return R.success(true);
    }

    /**
     * 获取当前值班人员信息
     * <p>
     * 查询指定部门当前正在值班的人员列表，用于展示在岗人员信息。
     * <p>
     * 查询条件：
     * - 当前日期：值班开始日期 <= 今天 <= 值班结束日期
     * - 当前时间：值班开始时间 <= 当前时间 <= 值班结束时间
     * - 部门ID：按部门筛选
     * <p>
     * 跨天值班处理：
     * - 支持查询跨天值班的人员
     * - 例如：夜班人员从昨天20:00值班到今天08:00，会被正确查询出来
     * <p>
     * 权限控制：
     * - 需要检查用户是否有查看该部门数据的权限
     * - 过滤掉当前登录用户自己（不返回自己）
     *
     * @param departmentId 部门ID（可选，不传则查询所有可见部门）
     * @return 当前值班人员列表，包含人员基本信息和排班信息
     */
    @Operation(summary = "获取当前值班人员信息", description = "查询指定部门当前正在值班的人员列表，支持跨天值班场景")
    @GetMapping("/onDutyPersonal")
    public R<List<DutyScheduleVO>> getOnDutyPersonalList(
            @Parameter(description = "部门ID") @RequestParam(value = "departmentId", required = false) Long departmentId) {
        return R.success(dutyScheduleService.getOnDutyPersonalList(departmentId));
    }
}
