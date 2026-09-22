package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelDO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelMemberDO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import com.tdtech.cloudcmd.im.jingxin.server.service.CoopLevelMemberService;
import com.tdtech.cloudcmd.im.jingxin.server.service.ImCommonService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CoopLevelMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CoopLevelMemberMapper;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CoopLevelMemberServiceImpl extends ServiceImpl<CoopLevelMemberMapper, CoopLevelMemberDO> implements CoopLevelMemberService {

    @Resource
    CoopLevelMemberMapper coopLevelMemberMapper;

    @Resource
    CollaborationPostMapper collaborationPostMapper;

    @Resource
    CoopLevelMapper coopLevelMapper;

    @Resource
    ImCommonService imCommonService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Resource
    ReportUtil reportUtil;

    @Override
    public Page<CoopUser> getMembers(String levelId, long pageNum, long pageSize, Long orgId) {
        log.info("获取协同岗层级成员开始: levelId=***, pageNum={}, pageSize={}, orgId=***", pageNum, pageSize);
        
        if ("0".equals(levelId)) {
            log.info("根节点，返回空结果");
            return new Page<>();
        }

        CoopLevelDO coopLevelDO = coopLevelMapper.selectInfoById(levelId);
        if (coopLevelDO == null) {
            log.error("协同岗层级不存在: levelId={}", levelId);
            throw new BusinessException("协同岗层级不存在");
        }

        // 数据权限
        // Set<String> dept = imCommonService.getUserDepartments();

        List<String> orgIds =
                organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                        .map(String::valueOf).collect(Collectors.toList());
        log.info("查询到{}个组织ID用于筛选", orgIds.size());

//        if (CollectionUtils.isEmpty(orgIds)) {
//            return new Page<>();
//        }

        Page<CoopUser> result = coopLevelMemberMapper.getMembers(new Page<>(pageNum, pageSize), levelId, orgIds);
        log.info("获取协同岗层级成员结束: 总记录数={}", result.getTotal());
        return result;
    }

    @Override
    public Page<CoopUser> searchMembers(String name, String levelId, Long orgId, long pageNum, long pageSize, String startTime, String endTime) {
        log.info("搜索协同岗层级成员开始: name=***, levelId=***, orgId=***, pageNum={}, pageSize={}, startTime=***, endTime=***",
                pageNum, pageSize);

        // 数据权限
        // Set<String> dept = imCommonService.getUserDepartments();

        List<String> orgIds =
                organizationDiversionService.queryDepartmentForListById(orgId).stream().map(ImDepartment::getId)
                        .map(String::valueOf).collect(Collectors.toList());
        log.info("查询到{}个组织ID用于筛选", orgIds.size());

//        if (CollectionUtils.isEmpty(orgIds)) {
//            log.info("组织ID列表为空，返回空结果");
//            return new Page<>();
//        }

        Page<CoopUser> result = coopLevelMemberMapper.searchMembers(new Page<>(pageNum, pageSize), name, levelId, orgIds, startTime, endTime);
        log.info("搜索协同岗层级成员结束: 总记录数={}", result.getTotal());
        return result;
    }

    @Override
    public boolean putMembers(String levelId, List<String> coopUserIds) {
        log.info("绑定协同岗成员开始: levelId=***, coopUserIds数量={}", coopUserIds != null ? coopUserIds.size() : 0);

        if ("0".equals(levelId)) {
            log.warn("尝试绑定根节点");
            throw new BusinessException("根节点不能绑定协同岗");
        }

        if (coopUserIds == null || coopUserIds.isEmpty()) {
            log.warn("协同岗列表为空");
            throw new BusinessException("协同岗不能为空");
        }

        CoopLevelDO coopLevelDO = coopLevelMapper.selectInfoById(levelId);
        if (coopLevelDO == null) {
            log.error("协同岗层级不存在: levelId={}", levelId);
            throw new BusinessException("协同岗层级不存在");
        }

        List<CoopUser> members = coopLevelMemberMapper.getMemberList(levelId);

        if (members != null && !members.isEmpty()) {
            log.info("检查已绑定成员，当前已绑定{}个成员", members.size());
            members.forEach(member -> {
                if (coopUserIds.contains(String.valueOf(member.getId()))) {
                    log.warn("协同岗已绑定到该节点下: memberId={}", member.getId());
                    throw new BusinessException("部分协同岗已绑定到该节点下");
                }
            });
        }

        int memberCount = coopLevelMemberMapper.getMemberCountByLevelId(Long.parseLong(levelId));
        log.info("当前层级已绑定成员数: {}, 新增成员数: {}", memberCount, coopUserIds.size());

        if (memberCount + coopUserIds.size() > 50) {
            log.warn("绑定成员数超过限制: 当前{} + 新增{} > 50", memberCount, coopUserIds.size());
            throw new BusinessException("协同岗最多绑定50个");
        }

        List<String> bindPosts = coopLevelMemberMapper.getBindMember(coopUserIds);
        log.info("检查已绑定的协同岗数量: {}", bindPosts != null ? bindPosts.size() : 0);

        if (CollectionUtils.isNotEmpty(bindPosts)) {
            String errorMsg = String.format("协同岗 %s 已被绑定", String.join(",", bindPosts));
            log.warn("协同岗已被绑定: {}", errorMsg);
            throw new BusinessException(errorMsg);
        }

        // 数据权限
//        Set<String> dept = imCommonService.getUserDepartments();
//
//        if (dept == null || CollectionUtils.isEmpty(dept)) {
//            throw new BusinessException("用户无权限");
//        }

        log.info("查询协同岗列表");
        List<CollaborationPost> postList = collaborationPostMapper.findPostListByIds(coopUserIds, null);
        List<String> postNames = postList.stream().map(CollaborationPost::getPostName).collect(Collectors.toList());
        Set<String> postIdList = postList.stream().map(p -> String.valueOf(p.getId())).collect(Collectors.toSet());
        boolean allExist = postIdList.containsAll(coopUserIds);
        if (!allExist) {
            log.error("部分协同岗不存在");
            throw new BusinessException("部分协同岗不存在");
        }

        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            log.error("用户未认证");
            throw new SecurityUtils.UnAuthException("access token invalid");
        }

        List<CoopLevelMemberDO> coopLevelMembers = new ArrayList<>();
        postList.forEach(post -> {
            CoopLevelMemberDO coopLevelMemberDO = new CoopLevelMemberDO();
            coopLevelMemberDO.setCoopUserId(post.getId());
            coopLevelMemberDO.setCoopLevelId(Long.parseLong(levelId));
            coopLevelMemberDO.setCreateBy(user.getUserId());
            coopLevelMembers.add(coopLevelMemberDO);
        });

        log.info("插入{}个协同岗成员记录", coopLevelMembers.size());
        coopLevelMemberMapper.insertBatch(coopLevelMembers);
        log.info("绑定协同岗成员结束");

        OperationLog operationLog = new OperationLog(OperationTypeEnum.COOP_LEVEL_MEMBER_UPDATE);
        operationLog.setOperation(String.format(operationLog.getOperation(), postNames,  coopLevelDO.getName()));
        operationLog.setOperator(user.getUserName());
        reportUtil.saveOperationLog(operationLog);
        return true;
    }

    @Override
    public boolean deleteMembers(List<String> ids) {
        log.info("删除协同岗成员开始: ids数量={}", ids != null ? ids.size() : 0);

        if (ids == null || ids.isEmpty()) {
            log.info("ID列表为空，无需删除");
            return true;
        }

        // 数据权限
//        Set<String> dept = imCommonService.getUserDepartments();
//        if (dept == null || CollectionUtils.isEmpty(dept)) {
//            throw new BusinessException("用户无权限");
//        }
//        List<CoopUser> members = coopLevelMemberMapper.getMemberWithoutPermission(ids, dept);
//        if (CollectionUtils.isNotEmpty(members)) {
//            throw new BusinessException("部分协同岗无权限删除");
//        }

        // 先查询关联数据用于记录日志
        List<CoopLevelMemberDO> memberList = coopLevelMemberMapper.selectList(new LambdaQueryWrapper<CoopLevelMemberDO>()
                .in(CoopLevelMemberDO::getId, ids));
        List<String> levelNames = CollectionUtils.isEmpty(memberList) ? new ArrayList<>() :
                coopLevelMapper.selectByIds(memberList.stream().map(m -> String.valueOf(m.getCoopLevelId())).collect(Collectors.toList()))
                        .stream().map(CoopLevelDO::getName).collect(Collectors.toList());

        List<String> coopUserIds = memberList.stream().map(m -> String.valueOf(m.getCoopUserId())).collect(Collectors.toList());

        List<String> postNames = collaborationPostMapper.findPostListByIds(coopUserIds, null)
                .stream().map(CollaborationPost::getPostName).collect(Collectors.toList());

        coopLevelMemberMapper.deleteByCoopUserIds(ids);

        // 记录操作日志
        OperationLog operationLog = new OperationLog(OperationTypeEnum.COOP_LEVEL_MEMBER_DELETE);
        operationLog.setOperation(String.format(operationLog.getOperation(), levelNames, postNames));
        UserInfo user = SecurityUtils.getUser();
        operationLog.setOperator(user.getUserName());
        reportUtil.saveOperationLog(operationLog);

        log.info("删除协同岗成员结束");
        return true;
    }
}
