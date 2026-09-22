package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import java.util.List;
import java.util.Set;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationAttendanceSwitch;

/**
 * @author syf
 * @date 2025/7/16
 **/
@Mapper
public interface CollaborationAttendanceSwitchMapper extends BaseMapper<CollaborationAttendanceSwitch> {

    /**
     * 根据用户id查询协同岗开关状态
     * 
     * @param personId 人员id
     * @return 剩余人数
     */
    CollaborationAttendanceSwitch getByPersonId(@Param("personId") Long personId);

    List<CollaborationAttendanceSwitch> getByPersonIds(@Param("personIds") List<Long> personIds);

    /**
     * 根据用户id列表获取在岗人员数量
     * 
     * @param personIds 人员id
     * @return 剩余人数
     */
    int countOnDutyByPersonIds(@Param("personIds") Set<Long> personIds);

    /**
     * 根据用户id列表筛选在岗人员
     * 
     * @param personIds 人员id
     * @return 在岗人员
     */
    List<Long> listOnDutyPeople(@Param("personIds") Set<Long> personIds);

    void insertBatch(@Param("list") List<CollaborationAttendanceSwitch> list);
}
