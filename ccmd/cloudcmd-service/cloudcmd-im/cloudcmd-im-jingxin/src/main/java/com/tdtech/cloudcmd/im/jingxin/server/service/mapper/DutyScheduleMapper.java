package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutySchedule;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyScheduleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface DutyScheduleMapper extends BaseMapper<DutySchedule> {

    /**
     * 查询当前正在值班的人员列表
     *
     * @param dutyDate 值班日期
     * @param currentTime 当前时间
     * @param orgIds 部门ID（可选）
     * @return 正在值班的人员列表
     */
    List<DutyScheduleVO> selectOnDutyPersonnel(@Param("dutyDate") LocalDate dutyDate,
                                               @Param("currentTime") LocalTime currentTime,
                                               @Param("orgIds") List<Long> orgIds);

    /**
     * 批量插入值班安排
     *
     * @param list 值班安排列表
     * @return 插入记录数
     */
    int insertBatch(@Param("list") List<DutySchedule> list);

}
