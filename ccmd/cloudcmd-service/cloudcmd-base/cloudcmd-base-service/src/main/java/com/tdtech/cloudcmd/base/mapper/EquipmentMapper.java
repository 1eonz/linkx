package com.tdtech.cloudcmd.base.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.base.entity.Equipments;

/**
 * <p>
 * 装备资源信息表（包括设备和坐席） Mapper 接口
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Mapper
public interface EquipmentMapper extends BaseMapper<Equipments> {

}
