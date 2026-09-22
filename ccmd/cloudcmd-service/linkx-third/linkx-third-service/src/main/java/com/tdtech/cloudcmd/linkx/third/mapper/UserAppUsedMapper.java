package com.tdtech.cloudcmd.linkx.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.third.entity.UserAppUsed;
import org.apache.ibatis.annotations.Mapper;

/**
 * 北向应用使用记录表 Mapper 接口
 *
 * @author wb
 * @since 2026-05-09
 */
@Mapper
public interface UserAppUsedMapper extends BaseMapper<UserAppUsed> {

}
