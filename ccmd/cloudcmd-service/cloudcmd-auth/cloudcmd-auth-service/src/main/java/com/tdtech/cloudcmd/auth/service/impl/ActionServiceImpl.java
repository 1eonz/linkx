package com.tdtech.cloudcmd.auth.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.entity.Action;
import com.tdtech.cloudcmd.auth.mapper.ActionMapper;
import com.tdtech.cloudcmd.auth.service.IActionService;

/**
 * <p>
 * 操作权限信息表 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Service
public class ActionServiceImpl extends ServiceImpl<ActionMapper, Action> implements IActionService {

}
