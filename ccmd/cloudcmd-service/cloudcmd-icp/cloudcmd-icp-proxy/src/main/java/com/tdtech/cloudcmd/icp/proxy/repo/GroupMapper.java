package com.tdtech.cloudcmd.icp.proxy.repo;

import com.tdtech.cloudcmd.icp.proxy.entity.Group;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper extends ExBaseMapper<Group> {
}
