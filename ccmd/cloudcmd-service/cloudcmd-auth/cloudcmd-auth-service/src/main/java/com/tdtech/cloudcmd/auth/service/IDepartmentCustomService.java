package com.tdtech.cloudcmd.auth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomCO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomDutyScheduleVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomUserVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentCustomVO;
import com.tdtech.cloudcmd.auth.dto.DepartmentNodeCustomCO;
import com.tdtech.cloudcmd.auth.dto.DepartmentNodeCustomVO;
import com.tdtech.cloudcmd.auth.dto.ImUserQO;
import com.tdtech.cloudcmd.auth.entity.DepartmentCustom;
import com.tdtech.cloudcmd.auth.entity.DepartmentNodeCustom;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;

import java.util.List;

public interface IDepartmentCustomService extends IService<DepartmentCustom> {

    List<DepartmentCustomVO> listTrees(Long imUserId);

    DepartmentCustom detailTree(Long id, Long imUserId);

    DepartmentCustom createTree(DepartmentCustomCO co);

    DepartmentCustom updateTree(Long id, DepartmentCustomCO co);

    void deleteTree(Long id);

    List<DepartmentNodeCustomVO> nodeTree(Long departmentCustomId, Long imUserId);

    List<DepartmentNodeCustomVO> children(Long departmentCustomId, Long parentId, String keyword, Long imUserId);

    List<DepartmentNodeCustomVO> listNodes(Long departmentCustomId, Long parentId, String keyword, Long imUserId);

    DepartmentNodeCustom detailNode(Long id, Long imUserId);

    DepartmentNodeCustom createNode(DepartmentNodeCustomCO co);

    DepartmentNodeCustom updateNode(Long id, DepartmentNodeCustomCO co);

    void deleteNode(Long id);

    List<DepartmentCustomUserVO> listUsers(Long nodeId, Long imUserId);

    IPage<DepartmentCustomUserVO> pageUsers(Long nodeId, Long pageNum, Long pageSize, String keyword, Long imUserId);

    List<DepartmentCustomDutyScheduleVO> listDutyScheduleUsers(Long nodeId, String dutyStartDate,
                                                               String dutyEndDate, Long dutyType, Long imUserId);

    void addUsers(Long nodeId, List<ImUserDO> userIds);

    void deleteUsers(Long nodeId, List<Long> userIds);

    IPage<ImUserDO> pageAvailableUsers(Long pageNum, Long pageSize, ImUserQO qo);
}
