package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;

import java.util.List;

public interface CoopLevelMemberService {
    Page<CoopUser> getMembers(String levelId, long pageNum, long pageSize, Long orgId);

    boolean putMembers(String levelId, List<String> coopUserIds);

    boolean deleteMembers(List<String> ids);

    Page<CoopUser> searchMembers(String name, String levelId, Long orgId, long pageNum, long pageSize, String startTime, String endTime);
}
