package com.tdtech.cloudcmd.auth.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.auth.entity.Menu;
import com.tdtech.cloudcmd.auth.mapper.MenuMapper;
import com.tdtech.cloudcmd.auth.service.IMenuService;

/**
 * <p>
 * 权限菜单信息表 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-01
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements IMenuService {

}
