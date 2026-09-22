package com.tdtech.cloudcmd.im.jingxin.server.rpc;

import com.tdtech.cloudcmd.im.jingxin.api.ImUserRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.ImUserPageResult;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.ImUserVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.virtualUser.UserDepartmentVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.UserFailVo;
import com.tdtech.cloudcmd.im.jingxin.api.entity.im.UserGetResultVo;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImPage;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserGetVo;
import com.tdtech.cloudcmd.im.jingxin.server.service.UserProfileService;
import org.apache.commons.lang.StringUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@DubboService
public class ImUserRpc implements ImUserRpcApi {

    @Resource
    private ImHttpClient imHttpClient;

    @Resource
    private UserProfileService userProfileService;

    @Override
    public UserGetResultVo getUsersInfo(String userIds, String idCards, String xUserId) {
        boolean hasUserIds = StringUtils.isNotBlank(userIds);
        boolean hasIdCards = StringUtils.isNotBlank(idCards);

        // 只传一种参数，直接查
        if (hasUserIds && !hasIdCards) {
            return queryAndConvert(userIds, null);
        }
        if (hasIdCards && !hasUserIds) {
            return queryAndConvert(null, idCards);
        }

        // 同时传入 userIds 和 idCards，分两次查询并合并去重
        UserGetResultVo byUserIds = queryAndConvert(userIds, null);
        UserGetResultVo byIdCards = queryAndConvert(null, idCards);

        List<ImUserVo> merged = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        for (ImUserVo user : byUserIds.getResults()) {
            if (seenIds.add(user.getId())) {
                merged.add(user);
            }
        }
        for (ImUserVo user : byIdCards.getResults()) {
            if (seenIds.add(user.getId())) {
                merged.add(user);
            }
        }

        List<UserFailVo> mergedFailures = new ArrayList<>(byUserIds.getFailures());
        Set<String> seenFailIdCards = byUserIds.getFailures().stream()
                .map(UserFailVo::getIdCard)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        for (UserFailVo fail : byIdCards.getFailures()) {
            if (fail.getIdCard() == null || seenFailIdCards.add(fail.getIdCard())) {
                mergedFailures.add(fail);
            }
        }

        UserGetResultVo result = new UserGetResultVo();
        result.setResults(merged);
        result.setFailures(mergedFailures);
        return result;
    }

    @Override
    public Integer getMySortedType(Long userId) {
        return userProfileService.getSortedType(userId);
    }

    @Override
    public ImUserPageResult pageByDepartment(Integer pageNum, Integer pageSize, String deptId, Integer includeChildren, String name) {
        ImPage<ImUser> imPage = imHttpClient.userPageByDepartment(
                pageNum, pageSize, null, includeChildren, null, deptId, name, null);
        ImUserPageResult result = new ImUserPageResult();
        if (imPage == null) {
            result.setPageNo(pageNum);
            result.setPageSize(pageSize);
            result.setTotal(0);
            result.setRecords(Collections.emptyList());
            return result;
        }
        result.setPageNo(imPage.getPageNo() != null ? imPage.getPageNo() : pageNum);
        result.setPageSize(imPage.getPageSize() != null ? imPage.getPageSize() : pageSize);
        result.setTotal(imPage.getTotal() != null ? imPage.getTotal() : 0);
        if (imPage.getRecords() != null) {
            result.setRecords(imPage.getRecords().stream()
                    .map(this::convertToImUserVo)
                    .collect(Collectors.toList()));
        } else {
            result.setRecords(Collections.emptyList());
        }
        return result;
    }

    private UserGetResultVo queryAndConvert(String userIds, String idCards) {
        UserGetVo userGetVo = imHttpClient.userPageByIdOrIdCard(userIds, idCards);
        UserGetResultVo result = new UserGetResultVo();

        if (userGetVo == null) {
            result.setResults(Collections.emptyList());
            result.setFailures(Collections.emptyList());
            return result;
        }

        // 转换成功结果
        if (userGetVo.getResults() != null) {
            result.setResults(userGetVo.getResults().stream()
                    .map(this::convertToImUserVo)
                    .collect(Collectors.toList()));
        } else {
            result.setResults(Collections.emptyList());
        }

        // 转换失败结果
        if (userGetVo.getFailures() != null) {
            result.setFailures(userGetVo.getFailures().stream()
                    .map(this::convertToUserFailVo)
                    .collect(Collectors.toList()));
        } else {
            result.setFailures(Collections.emptyList());
        }

        return result;
    }

    private ImUserVo convertToImUserVo(ImUser imUser) {
        ImUserVo vo = new ImUserVo();
        BeanUtils.copyProperties(imUser, vo);
        vo.setDirectLeaderId(imUser.getDirectLeaderId() != null ? String.valueOf(imUser.getDirectLeaderId()) : null);
        if (imUser.getUserDepartments() != null) {
            vo.setUserDepartments(imUser.getUserDepartments().stream()
                    .map(this::convertToUserDepartmentVo)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private UserDepartmentVo convertToUserDepartmentVo(ImUser.UserDepartment department) {
        UserDepartmentVo vo = new UserDepartmentVo();
        vo.setId(department.getId());
        vo.setCode(department.getDepartmentCode());
        vo.setName(department.getDepartmentName());
        vo.setSort(department.getSort());
        vo.setIsPrimary(department.getIsPrimary());
        return vo;
    }

    private UserFailVo convertToUserFailVo(com.tdtech.cloudcmd.im.jingxin.client.entity.UserFailVo failVo) {
        UserFailVo vo = new UserFailVo();
        BeanUtils.copyProperties(failVo, vo);
        return vo;
    }
}