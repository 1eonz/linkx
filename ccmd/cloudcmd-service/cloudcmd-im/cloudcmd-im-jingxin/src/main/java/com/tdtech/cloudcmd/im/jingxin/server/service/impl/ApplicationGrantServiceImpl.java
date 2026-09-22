package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ApplicationGrant;
import com.tdtech.cloudcmd.im.jingxin.server.entity.ApplicationGrantVO;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.service.IIMUserRPCService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ApplicationGrantService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.ApplicationGrantMapper;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 系统应用授权信息服务实现（操作新表 linkx_open.tb_application_grant）
 */
@Slf4j
@Service
public class ApplicationGrantServiceImpl implements ApplicationGrantService {

    @Resource
    private ApplicationGrantMapper applicationGrantMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private IIMUserRPCService imUserRPCService;

    @Override
    public Long createApplicationGrant(ApplicationGrantVO vo) {
        // system_code 未传则后台生成（时间戳+随机数），已传则做唯一性校验
        if (StringUtils.isBlank(vo.getSystemCode())) {
            vo.setSystemCode(generateSystemCode());
        } else {
            ApplicationGrant exist = applicationGrantMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationGrant>()
                            .eq(ApplicationGrant::getSystemCode, vo.getSystemCode()));
            if (Objects.nonNull(exist)) {
                throw new BusinessException("系统编码已存在: " + vo.getSystemCode());
            }
        }
        // client_id 唯一性校验
        if (StringUtils.isNotBlank(vo.getClientId())) {
            ApplicationGrant existClient = applicationGrantMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationGrant>()
                            .eq(ApplicationGrant::getClientId, vo.getClientId()));
            if (Objects.nonNull(existClient)) {
                throw new BusinessException("应用ID已存在: " + vo.getClientId());
            }
        }
        // 应用名称不可重复
        if (StringUtils.isNotBlank(vo.getSystemName())) {
            ApplicationGrant existName = applicationGrantMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationGrant>()
                            .eq(ApplicationGrant::getSystemName, vo.getSystemName()));
            if (Objects.nonNull(existName)) {
                throw new BusinessException("应用名称已存在: " + vo.getSystemName());
            }
        }
        // application_id 未设值时置为 3
        if (vo.getApplicationId() == null) {
            vo.setApplicationId(3L);
        }

        ApplicationGrant grant = new ApplicationGrant();
        BeanUtils.copyProperties(vo, grant);
        grant.setId(idWorker.nextId());
        // 授权时间默认为当前时间
        if (grant.getGrantTime() == null) {
            grant.setGrantTime(new Date());
        }
        // 默认状态为启用(1)
        if (grant.getStatus() == null) {
            grant.setStatus(1);
        }
        // client_type 默认 "0"
        if (StringUtils.isBlank(grant.getClientType())) {
            grant.setClientType("0");
        }
        // 默认授权人为当前登录用户
        if (grant.getGrantUserId() == null) {
            grant.setGrantUserId(currentUserId());
        }
        applicationGrantMapper.insert(grant);
        return grant.getId();
    }

    @Override
    public Page<ApplicationGrant> pageApplicationGrant(ApplicationGrantVO vo) {
        Page<ApplicationGrant> page = new Page<>(vo.getPageNum(), vo.getPageSize());
        LambdaQueryWrapper<ApplicationGrant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(vo.getSystemName())) {
            wrapper.like(ApplicationGrant::getSystemName, vo.getSystemName());
        }
        if (StringUtils.isNotBlank(vo.getSystemCode())) {
            wrapper.eq(ApplicationGrant::getSystemCode, vo.getSystemCode());
        }
        if (StringUtils.isNotBlank(vo.getClientId())) {
            wrapper.eq(ApplicationGrant::getClientId, vo.getClientId());
        }
        if (StringUtils.isNotBlank(vo.getClientType())) {
            wrapper.eq(ApplicationGrant::getClientType, vo.getClientType());
        }
        if (vo.getStatus() != null) {
            wrapper.eq(ApplicationGrant::getStatus, vo.getStatus());
        }
        wrapper.orderByDesc(ApplicationGrant::getGmtCreated);
        Page<ApplicationGrant> result = applicationGrantMapper.selectPage(page, wrapper);
        // 批量填充授权人姓名
        fillGrantUserName(result.getRecords());
        return result;
    }

    @Override
    public ApplicationGrant getApplicationGrant(Long id) {
        ApplicationGrant grant = applicationGrantMapper.selectById(id);
        if (Objects.isNull(grant)) {
            throw new BusinessException("应用授权不存在: " + id);
        }
        // 查询授权人姓名
        if (grant.getGrantUserId() != null) {
            try {
                List<ImUserDto> users = imUserRPCService.listByIds(Collections.singletonList(grant.getGrantUserId()));
                if (!users.isEmpty() && StringUtils.isNotBlank(users.get(0).getName())) {
                    grant.setGrantUserName(users.get(0).getName());
                }
            } catch (Exception e) {
                log.warn("查询授权人姓名失败, grantUserId={}", grant.getGrantUserId(), e);
            }
        }
        return grant;
    }

    @Override
    public void updateApplicationGrant(ApplicationGrantVO vo) {
        ApplicationGrant existing = applicationGrantMapper.selectById(vo.getId());
        if (Objects.isNull(existing)) {
            throw new BusinessException("应用授权不存在: " + vo.getId());
        }
        // system_code 变更时校验唯一
        if (StringUtils.isNotBlank(vo.getSystemCode())
                && !vo.getSystemCode().equals(existing.getSystemCode())) {
            ApplicationGrant exist = applicationGrantMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationGrant>()
                            .eq(ApplicationGrant::getSystemCode, vo.getSystemCode())
                            .ne(ApplicationGrant::getId, vo.getId()));
            if (Objects.nonNull(exist)) {
                throw new BusinessException("系统编码已存在: " + vo.getSystemCode());
            }
        }
        // client_id 变更时校验唯一
        if (StringUtils.isNotBlank(vo.getClientId())
                && !vo.getClientId().equals(existing.getClientId())) {
            ApplicationGrant exist = applicationGrantMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationGrant>()
                            .eq(ApplicationGrant::getClientId, vo.getClientId())
                            .ne(ApplicationGrant::getId, vo.getId()));
            if (Objects.nonNull(exist)) {
                throw new BusinessException("应用ID已存在: " + vo.getClientId());
            }
        }
        // 应用名称变更时校验唯一
        if (StringUtils.isNotBlank(vo.getSystemName())
                && !vo.getSystemName().equals(existing.getSystemName())) {
            ApplicationGrant exist = applicationGrantMapper.selectOne(
                    new LambdaQueryWrapper<ApplicationGrant>()
                            .eq(ApplicationGrant::getSystemName, vo.getSystemName())
                            .ne(ApplicationGrant::getId, vo.getId()));
            if (Objects.nonNull(exist)) {
                throw new BusinessException("应用名称已存在: " + vo.getSystemName());
            }
        }
        // VO → entity：MyBatis-Plus 默认 NOT_NULL 策略自动跳过 null 字段，保留原值；
        // expired 字段 @TableField(updateStrategy = FieldStrategy.ALWAYS)，传 null 也会写入（设为永久）
        // gmt_modified 由数据库 ON UPDATE CURRENT_TIMESTAMP 自动维护，无需代码 set
        ApplicationGrant update = BeanCopyUtils.copyBean(vo, ApplicationGrant::new);
        applicationGrantMapper.updateById(update);
    }

    @Override
    public void deleteApplicationGrant(Long id) {
        ApplicationGrant existing = applicationGrantMapper.selectById(id);
        if (Objects.isNull(existing)) {
            throw new BusinessException("应用授权不存在: " + id);
        }
        applicationGrantMapper.deleteById(id);
    }

    /**
     * 批量填充授权人姓名
     */
    private void fillGrantUserName(List<ApplicationGrant> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> userIds = records.stream()
                .map(ApplicationGrant::getGrantUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (userIds.isEmpty()) {
            return;
        }
        try {
            List<ImUserDto> users = imUserRPCService.listByIds(userIds);
            Map<Long, String> userNameMap = users.stream()
                    .filter(u -> u.getId() != null && StringUtils.isNotBlank(u.getName()))
                    .collect(Collectors.toMap(ImUserDto::getId, ImUserDto::getName, (a, b) -> a));
            records.forEach(r -> {
                if (r.getGrantUserId() != null) {
                    r.setGrantUserName(userNameMap.get(r.getGrantUserId()));
                }
            });
        } catch (Exception e) {
            log.warn("批量查询授权人姓名失败, userIds={}", userIds, e);
        }
    }

    /**
     * 生成 system_code：时间戳(毫秒) + 4位随机数，共17位
     */
    private String generateSystemCode() {
        return System.currentTimeMillis() + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * 获取当前登录用户ID
     */
    private Long currentUserId() {
        UserInfo user = SecurityUtils.getUser();
        if (user == null) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        return user.getUserId();
    }

}