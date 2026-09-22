package com.tdtech.cloudcmd.im.openapi.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.openapi.aop.OAuthContext;
import com.tdtech.cloudcmd.im.openapi.controller.entity.Token;
import com.tdtech.cloudcmd.im.openapi.controller.entity.OpenTasksConfigVO;
import com.tdtech.cloudcmd.im.openapi.repo.OpenApplicationGrant;
import com.tdtech.cloudcmd.im.openapi.repo.OpenApplicationGrantMapper;
import com.tdtech.cloudcmd.im.openapi.repo.OpenTasksConfig;
import com.tdtech.cloudcmd.im.openapi.repo.OpenTasksConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 任务标准件配置实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenTasksConfigServiceImpl implements OpenTasksConfigService {

    private final OpenTasksConfigMapper OpenTasksConfigMapper;
    private final OpenApplicationGrantMapper OpenApplicationGrantMapper;

    @Override
    public boolean setConfig(OpenTasksConfigVO vo) {
        if (StringUtils.isBlank(vo.getModule())) {
            throw new BusinessException("module不能为空");
        }
        if (vo.getShowInPC() == null) {
            throw new BusinessException("showInPC不能为空");
        }
        if (vo.getShowInPC() != 0 && vo.getShowInPC() != 1) {
            throw new BusinessException("showInPC必须为0或1");
        }
        String systemCode = currentSystemCode();

        // module 全局唯一校验：已存在但归属其他系统则拒绝
        OpenTasksConfig existing = OpenTasksConfigMapper.selectOne(
                Wrappers.lambdaQuery(OpenTasksConfig.class)
                        .eq(OpenTasksConfig::getModule, vo.getModule()));
        if (existing != null) {
            if (!systemCode.equals(existing.getSystemCode())) {
                throw new BusinessException("module已被其他系统占用: " + vo.getModule());
            }
            // 同 system_code 同 module：更新 show_in_pc
            existing.setShowInPc(vo.getShowInPC() == null ? 0 : vo.getShowInPC());
            OpenTasksConfigMapper.updateById(existing);
            return true;
        }

        // 不存在：新增
        OpenTasksConfig config = new OpenTasksConfig()
                .setSystemCode(systemCode)
                .setModule(vo.getModule())
                .setShowInPc(vo.getShowInPC() == null ? 0 : vo.getShowInPC());
        OpenTasksConfigMapper.insert(config);
        return true;
    }

    @Override
    public void ensureModule(String module) {
        if (StringUtils.isBlank(module)) {
            return;
        }
        String systemCode = currentSystemCode();
        OpenTasksConfig existing = OpenTasksConfigMapper.selectOne(
                Wrappers.lambdaQuery(OpenTasksConfig.class)
                        .eq(OpenTasksConfig::getModule, module));
        if (existing == null) {
            // 当前 systemCode 下未登记：自动插入一条 show_in_pc=0 的记录
            OpenTasksConfig config = new OpenTasksConfig()
                    .setSystemCode(systemCode)
                    .setModule(module)
                    .setShowInPc(0);
            OpenTasksConfigMapper.insert(config);
            return;
        }
        if (!systemCode.equals(existing.getSystemCode())) {
            throw new BusinessException("module已被其他系统占用: " + module);
        }
        // 已登记且属于当前 systemCode：放行
    }

    /**
     * 获取当前调用方的 systemCode（基于 token -> clientId -> OpenApplicationGrant.systemCode）
     */
    private String currentSystemCode() {
        Token token = OAuthContext.getToken();
        if (token == null || StringUtils.isBlank(token.getClientId())) {
            throw new BusinessException("token无效或缺失clientId");
        }
        OpenApplicationGrant grant = OpenApplicationGrantMapper.selectOne(
                Wrappers.lambdaQuery(OpenApplicationGrant.class)
                        .eq(OpenApplicationGrant::getClientId, token.getClientId()));
        if (grant == null || StringUtils.isBlank(grant.getSystemCode())) {
            throw new BusinessException("当前token对应的systemCode不存在: clientId=" + token.getClientId());
        }
        return grant.getSystemCode();
    }
}