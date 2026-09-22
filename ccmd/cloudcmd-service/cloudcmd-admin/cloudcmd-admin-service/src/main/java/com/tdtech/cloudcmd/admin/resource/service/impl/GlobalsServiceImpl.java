package com.tdtech.cloudcmd.admin.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.message.MqPublisher;
import com.tdtech.cloudcmd.admin.resource.entity.Globals;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AiDeployConfigVO;
import com.tdtech.cloudcmd.admin.resource.mapper.GlobalsMapper;
import com.tdtech.cloudcmd.admin.resource.service.IGlobalsService;
import com.tdtech.cloudcmd.base.api.service.CacheRpcService;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.net.URI;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tdtech.cloudcmd.admin.exception.AdminErrorEnum.COMMON_ERROR_511;
import static com.tdtech.cloudcmd.admin.message.NotifyType.GLOBAL_UPDATE;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.AI_SEPARATED_DEPLOY;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.GROUP_AI_FRONTEND_HOST;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.GROUP_AI_HOST;

/**
 * <p>
 * 全局变量信息表 服务实现类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Service
@Slf4j
public class GlobalsServiceImpl extends ServiceImpl<GlobalsMapper, Globals> implements IGlobalsService {

    @Autowired
    private GlobalsMapper globalsMapper;
    @Autowired
    private IdWorker idWorker;
    @DubboReference
    private CacheRpcService cacheRpcService;
    @Autowired
    private MqPublisher mqPublisher;

    @Resource
    private LicenseUtil licenseUtil;

    @Resource
    private ReportUtil reportUtil;

    @Override
    public List<Globals> getGlobalsList(String keyword) {
        QueryWrapper<Globals> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Globals.NAME, keyword)
                    .or().like(Globals.VALUE, keyword)
                    .or().like(Globals.REMARK, keyword)
                    .or().like(Globals.REMARK_EN, keyword));
        }
        wrapper.orderByDesc(Globals.CLASSIFY).orderByAsc(Globals.NAME);
        return globalsMapper.selectList(wrapper);
    }

    @Override
    public void createGlobals(Globals globals) {
        var count = globalsMapper.selectCount(new QueryWrapper<Globals>().eq(Globals.NAME, globals.getName()));
        if (count > 0) {
            throw new AdminException(COMMON_ERROR_511.getCode(), I18nUtil.get(COMMON_ERROR_511.getMsg()));
        }
        globals.setId(idWorker.nextId());
        globalsMapper.insert(globals);
        cacheRpcService.initGlobals();
    }

    @Override
    public void updateGlobals(Globals globals) {
        Globals dbGlobal = globalsMapper.selectById(globals.getId());
        if (!dbGlobal.getName().equals(globals.getName())) {
            var count = globalsMapper.selectCount(new QueryWrapper<Globals>().eq(Globals.NAME, globals.getName()));
            if (count > 0) {
                throw new AdminException(COMMON_ERROR_511.getCode(), I18nUtil.get(COMMON_ERROR_511.getMsg()));
            }
        }

        List<String> changes = buildChangeLog(dbGlobal, globals);

        globals.setRemark(dbGlobal.getRemark());
        globalsMapper.updateById(globals);
        cacheRpcService.initGlobals();
        mqPublisher.sendGlobalMessage(GLOBAL_UPDATE, globals);

        saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_UPDATE, dbGlobal.getRemarkEn(), changes);
    }

    @Override
    public void enableGlobals(Globals globals) {
        Globals dbGlobal = globalsMapper.selectById(globals.getId());
        List<String> changes = buildChangeLog(dbGlobal, globals);

        updateGlobals(globals);
        saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_ENABLE, dbGlobal.getRemark(), changes);
    }

    @Override
    public String deleteGlobalsById(Long id) {
        globalsMapper.batchLogicDeleteGlobalsById(id);
        cacheRpcService.initGlobals();
        return "success";
    }

    @Override
    @LogReport(type = OperationTypeEnum.GLOBAL_CONFIG_DELETE)
    public String deleteGlobals(@LogReportParam(field = "remark", international = true) Globals globals) {
        deleteGlobalsById(globals.getId());
        return "success";
    }

    @Override
    public void batchDeleteGlobalsByIds(List<Long> globalsIds) {
        globalsMapper.deleteBatchIds(globalsIds);
    }

    @Override
    public void batchInsertGlobals(List<Globals> globalsList) {
        globalsMapper.batchInsertGlobals(globalsList);
    }

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

    private List<String> buildChangeLog(Globals oldGlobals, Globals newGlobals) {
        List<String> changes = new ArrayList<>();
        changes.add(formatChangeLog(oldGlobals.getValue(), newGlobals.getValue()));

        if (!Objects.equals(oldGlobals.getStatus(), newGlobals.getStatus())) {
            String oldStatus = oldGlobals.getStatus() == 0 ? I18nUtil.get("GLOBAL_STATUS_ENABLED") : I18nUtil.get("GLOBAL_STATUS_DISABLED");
            String newStatus = newGlobals.getStatus() == 0 ? I18nUtil.get("GLOBAL_STATUS_ENABLED") : I18nUtil.get("GLOBAL_STATUS_DISABLED");
            changes.add(formatChangeLog(oldStatus, newStatus));
        }

        return changes;
    }

    private String formatChangeLog(Object oldValue, Object newValue) {
        return oldValue + "->" + newValue;
    }

    private void saveOperationLog(OperationTypeEnum operationType, String configName, List<String> changes) {
        try {
            OperationLog operationLog = new OperationLog(operationType);
            operationLog.setOperation(String.format(operationLog.getOperation(), configName, String.join("，", changes)));

            UserInfo user = SecurityUtils.getUser();
            if (Objects.nonNull(user)) {
                operationLog.setOperator(user.getUserName());
            }
            reportUtil.saveOperationLog(operationLog);
        } catch (Exception e) {
            log.error("saveOperationLog error: {}", e.getMessage());
        }
    }

    @Override
    public AiDeployConfigVO getAiDeployConfig() {
        AiDeployConfigVO result = new AiDeployConfigVO();
        result.setSeparatedDeploy(parseSeparatedDeploy(getValueByName(AI_SEPARATED_DEPLOY)));
        result.setGroupAiHost(getValueByName(GROUP_AI_HOST));
        result.setGroupAiFrontendHost(getValueByName(GROUP_AI_FRONTEND_HOST));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAiDeployConfig(AiDeployConfigVO aiDeployConfigVO) {
        String groupAiHost = normalizeUrl(aiDeployConfigVO.getGroupAiHost(), "群AI助手对接地址");
        String groupAiFrontendHost = normalizeUrl(aiDeployConfigVO.getGroupAiFrontendHost(), "AI分离部署前端页面地址");
        upsertGlobal(AI_SEPARATED_DEPLOY,
            String.valueOf(parseSeparatedDeploy(String.valueOf(aiDeployConfigVO.getSeparatedDeploy()))), "AI是否分离部署 0否1是","AI部署方式");
        upsertGlobal(GROUP_AI_HOST, groupAiHost, "群AI助手对接地址", "群AI助手对接地址");
        Globals notifyGlobal = upsertGlobal(GROUP_AI_FRONTEND_HOST, groupAiFrontendHost, "AI分离部署前端页面地址", "AI分离部署前端页面地址");
        cacheRpcService.initGlobals();
        mqPublisher.sendGlobalMessage(GLOBAL_UPDATE, notifyGlobal);
    }

    private Globals getGlobalByName(String name) {
        List<Globals> globals = globalsMapper.selectList(new QueryWrapper<Globals>().eq(Globals.NAME, name).last("limit 1"));
        return globals.isEmpty() ? null : globals.get(0);
    }

    private Globals upsertGlobal(String name, String value, String remark, String remarkEn) {
        Globals globals = getGlobalByName(name);
        Date now = new Date();
        if (globals == null) {
            globals = new Globals();
            globals.setId(idWorker.nextId());
            globals.setName(name);
            globals.setValue(value);
            globals.setRemark(remark);
            globals.setRemarkEn(remarkEn);
            globals.setStatus(0);
            globals.setGmtCreated(now);
            globals.setGmtModified(now);
            globalsMapper.insert(globals);
        } else {
            globals.setValue(value);
            globals.setGmtModified(now);
            globals.setRemark(StringUtils.isEmpty(remark) ? globals.getRemark() : remark);
            globals.setRemarkEn(StringUtils.isEmpty(remarkEn) ? globals.getRemarkEn() : remarkEn);
            globalsMapper.updateById(globals);
        }
        return globals;
    }

    private int parseSeparatedDeploy(String value) {
        return "1".equals(value) ? 1 : 0;
    }

    private String normalizeUrl(String value, String fieldName) {
        String url = StringUtils.trimToEmpty(value);
        if (StringUtils.isBlank(url)) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            if (StringUtils.isBlank(uri.getScheme()) || StringUtils.isBlank(uri.getHost())) {
                throw new IllegalArgumentException();
            }
            return url;
        } catch (Exception ex) {
            throw new AdminException(COMMON_ERROR_511.getCode(), fieldName + "URL格式错误");
        }
    }
}