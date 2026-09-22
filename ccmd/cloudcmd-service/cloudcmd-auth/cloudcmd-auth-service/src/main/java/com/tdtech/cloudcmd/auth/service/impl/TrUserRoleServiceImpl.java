package com.tdtech.cloudcmd.auth.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.entity.TrUserRole;
import com.tdtech.cloudcmd.auth.mapper.TrUserRoleMapper;
import com.tdtech.cloudcmd.auth.service.ITrUserRoleService;

/**
 * <p>
 * 用户-角色信息表（无勤务时，后台设定；有勤务时，根据排班设定）--角色与执行者关联还是与用户关联后续再看，现在先跟用户关联。 服务实现类
 * </p>
 *
 * @author mWX556161
 * @since 2020-11-24
 */
@Service
public class TrUserRoleServiceImpl extends ServiceImpl<TrUserRoleMapper, TrUserRole> implements ITrUserRoleService {

}
