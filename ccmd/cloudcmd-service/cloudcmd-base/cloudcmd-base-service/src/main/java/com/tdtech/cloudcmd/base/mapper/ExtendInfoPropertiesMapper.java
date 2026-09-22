package com.tdtech.cloudcmd.base.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.base.entity.ExtendInfoProperties;

/**
 * <p>
 * 扩展信息属性定义，通过反射实现扩展类 Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Mapper
public interface ExtendInfoPropertiesMapper extends BaseMapper<ExtendInfoProperties> {

}
