package com.tdtech.cloudcmd.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.auth.entity.TrUserRole;

/**
 * <p>
 * 用户-角色信息表（无勤务时，后台设定；有勤务时，根据排班设定）--角色与执行者关联还是与用户关联后续再看，现在先跟用户关联。 服务类
 * </p>
 *
 * @author mWX556161
 * @since 2020-11-24
 */
public interface ITrUserRoleService extends IService<TrUserRole> {

}
