package com.tdtech.cloudcmd.base.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.base.entity.Globals;

/**
 * <p>
 * 全局变量信息表 Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Mapper
public interface GlobalsMapper extends BaseMapper<Globals> {

}
