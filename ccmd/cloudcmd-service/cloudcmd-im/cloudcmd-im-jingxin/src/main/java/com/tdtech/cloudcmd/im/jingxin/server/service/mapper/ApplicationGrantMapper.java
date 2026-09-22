package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ApplicationGrant;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统应用授权信息 Mapper（跨库访问 linkx_open.tb_application_grant）
 */
@Mapper
public interface ApplicationGrantMapper extends BaseMapper<ApplicationGrant> {
}