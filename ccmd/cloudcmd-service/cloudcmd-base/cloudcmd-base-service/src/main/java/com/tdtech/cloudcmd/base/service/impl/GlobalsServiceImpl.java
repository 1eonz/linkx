package com.tdtech.cloudcmd.base.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.base.entity.Globals;
import com.tdtech.cloudcmd.base.mapper.GlobalsMapper;
import com.tdtech.cloudcmd.base.mapper.TenantGlobalsMapper;
import com.tdtech.cloudcmd.base.service.IGlobalsService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 全局变量信息表 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Service
@Slf4j
public class GlobalsServiceImpl extends ServiceImpl<GlobalsMapper, Globals> implements IGlobalsService {

    @Autowired
    private GlobalsMapper globalsMapper;

    @Resource
    private TenantGlobalsMapper tenantGlobalsMapper;

    @Override
    public String getValueByName(String name) {
        QueryWrapper<Globals> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Globals.NAME, name);
        String value = null;
        try {
            Globals globals = globalsMapper.selectOne(queryWrapper);
            if (globals != null) {
                value = globals.getValue();
            }
        } catch (Exception e) {
            log.error("get globals name error", e);
        }
        return value;
    }

    @Override
    public List<Globals> getGlobalsList() {
        QueryWrapper<Globals> wrapper = new QueryWrapper<>();
        wrapper.eq(Globals.STATUS, 0);
        return globalsMapper.selectList(wrapper);
    }

    @Override
    public List<Globals> getTenantGlobalsList(UserInfo user) {
        Long organizationId = user.getOrganizationId();
        List<Globals> globalsList = tenantGlobalsMapper.selectGlobalsByOrganizationId(organizationId);
        if (globalsList != null && globalsList.size() > 0) {
            for (Globals globals : globalsList) {
                globals.setValue(I18nUtil.get(globals.getValue()));
            }
        }
        return globalsList;
    }
}
