package com.tdtech.cloudcmd.auth.mapper;

import com.tdtech.cloudcmd.auth.entity.Organization;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollabsOrganizationMapper {
    List<Organization> selectByIds(@Param("orgIds") List<Long> orgIds);

    List<Organization> selectDetailByIds(@Param("orgIds") List<Long> orgIds);
}