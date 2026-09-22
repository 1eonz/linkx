package com.tdtech.cloudcmd.im.openapi.repo.notification;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@DS("linkx_open")
public interface NotificationTargetMapper extends BaseMapper<NotificationTarget> {
}
