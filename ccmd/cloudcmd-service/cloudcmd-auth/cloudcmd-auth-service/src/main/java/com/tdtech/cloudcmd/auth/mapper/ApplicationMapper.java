package com.tdtech.cloudcmd.auth.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.auth.entity.Application;

/**
 * <p>
 * 应用信息表(1,指挥调度应用(CDC) 2-指挥处警应用(CAPP) 3-数据管理 4,勤务管理应用) Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Mapper
public interface ApplicationMapper extends BaseMapper<Application> {

}
