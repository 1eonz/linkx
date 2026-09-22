
package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutySchedule;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyScheduleVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ImportDutyScheduleResult;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IDutyScheduleService extends IService<DutySchedule> {

    void template(HttpServletResponse response) throws IOException;

    ImportDutyScheduleResult importExcel(MultipartFile file) throws IOException;

    List<DutySchedule> findList(List<Long> userIdList);

    List<DutyScheduleVO> listDutyScheduleUsers(List<Long> userIdList, LocalDateTime dutyStartDate, LocalDateTime dutyEndDate,
                                               Long dutyType);

    Page<DutyScheduleVO> findPage(int pageNum, int pageSize, String userId, String userName, String startDate,
                                  String endDate, Long departmentId, Long dutyType);

    Map<String, List<DutyScheduleVO>> calendar(String userId, String userName, String startDate, String endDate,
                                               String month, Long departmentId, Long dutyType);

    List<DutySchedule> findList(LocalDate dutyStartDate);

    /**
     * 根据日期范围查询排班列表（支持跨天值班）
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班列表
     */
    List<DutySchedule> findListByDateRange(LocalDate startDate, LocalDate endDate);

    boolean resetUserCache();

    List<DutyScheduleVO> getOnDutyPersonalList(Long departmentId);
}
