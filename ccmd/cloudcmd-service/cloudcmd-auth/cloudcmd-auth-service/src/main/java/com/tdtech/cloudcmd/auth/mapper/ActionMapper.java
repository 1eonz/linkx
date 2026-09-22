package com.tdtech.cloudcmd.auth.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.Action;

/**
 * <p>
 * 操作权限信息表 Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Mapper
public interface ActionMapper extends BaseMapper<Action> {

}
