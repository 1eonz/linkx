package com.tdtech.cloudcmd.base.service.rpc;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.tdtech.cloudcmd.base.api.service.EquipmentRpcService;
import com.tdtech.cloudcmd.base.service.IEquipmentService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lsm254891
 * @date 2021/6/03 14:35
 */
@DubboService
@Slf4j
public class EquipmentRpcServiceImpl implements EquipmentRpcService {

    @Autowired
    private IEquipmentService equipmentService;

    @Override
    public Integer countEquipments() {
        return equipmentService.countEquipments();
    }

}
