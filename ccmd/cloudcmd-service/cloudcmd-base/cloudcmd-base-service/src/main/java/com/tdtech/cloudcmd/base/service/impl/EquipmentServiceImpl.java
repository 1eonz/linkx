package com.tdtech.cloudcmd.base.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.base.entity.Equipments;
import com.tdtech.cloudcmd.base.mapper.EquipmentMapper;
import com.tdtech.cloudcmd.base.service.IEquipmentService;

/**
 * <p>
 * 装备资源信息表（包括设备和坐席）
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Service
public class EquipmentServiceImpl extends ServiceImpl<EquipmentMapper, Equipments> implements IEquipmentService {

    @Autowired
    private EquipmentMapper equipmentMapper;

    @Override
    public Integer countEquipments() {
        return equipmentMapper.selectCount(null).intValue();
    }

}
