package com.tdtech.cloudcmd.auth.service.rpc;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.ImUserES;
import com.tdtech.cloudcmd.auth.entity.OrganizationUser;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.mapper.OrganizationUserMapper;
import com.tdtech.cloudcmd.auth.service.IIMUserRPCService;
import com.tdtech.cloudcmd.auth.service.ImUserEsService;
import com.tdtech.cloudcmd.auth.service.impl.OAuthLoginServiceImpl;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.PBKDF2Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mWX556161
 * @date 2020/9/27 14:12
 */
@DubboService
@Slf4j
public class IMUserRpcServiceImpl implements IIMUserRPCService {

    @Resource
    private ImUserMapper imUserMapper;

    @Resource
    private OrganizationUserMapper organizationUserMapper;

    @Autowired
    private ImUserEsService imUserEsService;

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public List<ImUserDto> findIdCardList(List<Long> deptIdList) {
        if (deptIdList == null || deptIdList.isEmpty()) {
            return new LinkedList<>();
        }
        LambdaQueryWrapper<ImUserDO> queryWrapper =
            new LambdaQueryWrapper<ImUserDO>().in(CollectionUtils.isNotEmpty(deptIdList), ImUserDO::getDepartmentId,
                deptIdList);
        var imUserDOS = imUserMapper.selectList(queryWrapper);
        return BeanCopyUtils.copyList(imUserDOS, ImUserDto::new);
    }

    @Override
    public List<ImUserDto> listByIdcards(List<String> idcards) {
        if (idcards == null || idcards.isEmpty()) {
            return new LinkedList<>();
        }
        
        // 判断是否使用ES查询
        if (false) {
            // 使用ES查询（idCard是明文）
            return listByIdcardsFromES(idcards);
        } else {
            // 使用MySQL查询
            return listByIdcardsFromMySQL(idcards);
        }
    }
    
    /**
     * 从ES查询（idCard IN查询）
     */
    private List<ImUserDto> listByIdcardsFromES(List<String> idcards) {
        List<ImUserES> esResults = new ArrayList<>();
        for (String idCard : idcards) {
//            List<ImUserES> results = esSearchService.searchUserByIdCard(idCard);
            List<ImUserES> results = Collections.emptyList();
            esResults.addAll(results);
        }
        // 去重
        Map<Long, ImUserES> userMap = new HashMap<>();
        for (ImUserES user : esResults) {
            userMap.put(user.getId(), user);
        }
        return userMap.values().stream()
            .map(user -> BeanCopyUtils.copyBean(user, ImUserDto::new))
            .collect(Collectors.toList());
    }
    
    /**
     * 从MySQL查询（idCard IN查询）
     * 注意：由于idCard已加密，这个方法可能无法正常工作
     * 建议使用ES查询
     */
    private List<ImUserDto> listByIdcardsFromMySQL(List<String> idcards) {
        LambdaQueryWrapper<ImUserDO> queryWrapper =
            new LambdaQueryWrapper<ImUserDO>().in(ImUserDO::getIdCard, idcards);
        var imUserDOS = imUserMapper.selectList(queryWrapper);
        return BeanCopyUtils.copyList(imUserDOS, ImUserDto::new);
    }

    @Override
    public List<Long> findOrgByUserId(Long userId) {
        List<OrganizationUser> organizationUsers = organizationUserMapper.selectByUserId(userId);
        if(CollectionUtils.isNotEmpty(organizationUsers)){
            return organizationUsers.stream().map(OrganizationUser::getImOrgId).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    @Override
    public List<ImUserDto> listByDeptCodes(List<String> deptCodes) {
        if (deptCodes == null || deptCodes.isEmpty()){
            return new LinkedList<>();
        }

        List<ImUserDO> imUserDOS = imUserMapper.selectList(new LambdaQueryWrapper<ImUserDO>().in(ImUserDO::getDepartmentCode, deptCodes));
        return BeanCopyUtils.copyList(imUserDOS, ImUserDto::new);
    }

    @Override
    public ImUserDto getByIdCard(String idCard) {
        return imUserEsService.getByIdCard(idCard);
    }

    @Override
    public ImUserDto getById(Long userId) {
        if (userId == null) {
            return null;
        }
        ImUserDO imUserDO = imUserMapper.selectById(userId);
        if (imUserDO == null) {
            return null;
        }
        return BeanCopyUtils.copyBean(imUserDO, ImUserDto::new);
    }

    @Override
    public List<ImUserDto> listByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<ImUserDO> imUserDOS = imUserMapper.selectBatchIds(userIds);
        return BeanCopyUtils.copyList(imUserDOS, ImUserDto::new);
    }

    @Override
    public PageResult<ImUserDto> pageByDepartmentIds(List<Long> deptIdList, String keywords, Integer pageNum,
        Integer pageSize) {
        if (deptIdList == null || deptIdList.isEmpty()) {
            return new PageResult<>(0L, Long.valueOf(pageNum), 0L, Long.valueOf(pageSize), new LinkedList<>());
        }
        
        boolean hasKeywords = keywords != null && !keywords.isBlank();
        
        // 判断是否需要使用ES查询
        boolean useES = encryptionService.encryptEnabled() && hasKeywords;

        if (useES) {
            // 使用ES查询
            return imUserEsService.pageByDepartmentIds(deptIdList, keywords, pageNum, pageSize);
        } else {
            // 使用MySQL查询
            return pageByDepartmentIdsFromMySQL(deptIdList, keywords, pageNum, pageSize);
        }
    }

    /**
     * 从MySQL分页查询
     */
    private PageResult<ImUserDto> pageByDepartmentIdsFromMySQL(List<Long> deptIdList, String keywords, Integer pageNum,
        Integer pageSize) {
        boolean hasKeywords = keywords != null && !keywords.isBlank();
        LambdaQueryWrapper<ImUserDO> queryWrapper =
            new LambdaQueryWrapper<ImUserDO>().in(ImUserDO::getDepartmentId, deptIdList)
                .and(hasKeywords, wrapper -> wrapper.like(ImUserDO::getName, keywords).or()
                    .like(ImUserDO::getMobile, keywords).or().like(ImUserDO::getIdCard, keywords).or()
                    .like(ImUserDO::getCode, keywords))
                .orderByDesc(ImUserDO::getGmtUpdated)
                    .select(
                            ImUserDO::getId, ImUserDO::getCode, ImUserDO::getName,
                            ImUserDO::getAvatar, ImUserDO::getGender, ImUserDO::getStatus
                    );
        Page<ImUserDO> page = imUserMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.fromIPage(page, BeanCopyUtils.copyList(page.getRecords(), ImUserDto::new));
    }

    @Override
    public int upsertImUserBatch(List<ImUserDto> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return 0;
        }
        // upsertBatch 为自定义 XML 方法，不触发 EncryptionInnerInterceptor，敏感字段需手动加密
        List<ImUserDO> doList = userList.stream().map(dto -> {
            ImUserDO target = BeanCopyUtils.copyBean(dto, ImUserDO::new);
            target.setName(encryptionService.encrypt(dto.getName()));
            target.setMobile(encryptionService.encrypt(dto.getMobile()));
            target.setIdCard(encryptionService.encrypt(dto.getIdCard()));
            // 新建用户写入默认密码（PBKDF2 哈希）；已存在用户 upsert 不覆盖 password/pwd_time
            target.setPassword(buildDefaultPassword());
            target.setPwdTime(new Date());
            return target;
        }).collect(Collectors.toList());
        int affected = 0;
        int batchSize = 200;
        for (List<ImUserDO> batch : Lists.partition(doList, batchSize)) {
            affected += imUserMapper.upsertBatch(batch);
        }
        return affected;
    }

    private String buildDefaultPassword() {
        try {
            return PBKDF2Util.PBKDF2ForPassStandard(OAuthLoginServiceImpl.DEFAULT_PASSWORD, PBKDF2Util.generateSalt());
        } catch (Exception e) {
            log.error("生成默认密码哈希失败, error={}", e.getMessage(), e);
            throw new RuntimeException("生成默认密码哈希失败", e);
        }
    }
}