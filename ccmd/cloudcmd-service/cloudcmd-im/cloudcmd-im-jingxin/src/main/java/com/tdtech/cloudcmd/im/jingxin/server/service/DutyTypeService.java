package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyType;

import java.util.List;
import java.util.Map;

public interface DutyTypeService extends IService<DutyType> {

    IPage<DutyType> getPageList(Integer pageNum, Integer pageSize, String name);

    DutyType getDutyTypeByType(Long type);

    boolean saveDutyType(DutyType dutyType);

    boolean deleteDutyType(Long type);

    List<DutyType> getAllList();

    Map<Long, DutyType> getTypeMap();
}
