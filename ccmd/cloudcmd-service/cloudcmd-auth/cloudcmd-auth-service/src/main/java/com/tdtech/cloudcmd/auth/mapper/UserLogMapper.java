package com.tdtech.cloudcmd.auth.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.UserLog;

/**
 * <p>
 * 用户操作日志记录表（目前只记录登陆日志。安全审计使用） Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Mapper
public interface UserLogMapper extends BaseMapper<UserLog> {

}
