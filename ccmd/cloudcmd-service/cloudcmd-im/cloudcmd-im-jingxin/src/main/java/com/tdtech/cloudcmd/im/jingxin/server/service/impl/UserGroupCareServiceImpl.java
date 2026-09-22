package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserGroupCare;
import com.tdtech.cloudcmd.im.jingxin.server.service.UserGroupCareService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.UserGroupCareMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Service
public class UserGroupCareServiceImpl extends ServiceImpl<UserGroupCareMapper, UserGroupCare> implements UserGroupCareService {

    private static final String GROUP_CARE = "GROUP_CARE";

    private static final String MSG_TOPIC = "cloudcmd-cagent";

    @Resource
    private UserGroupCareMapper userGroupCareMapper;

    @Resource
    private StreamBridge streamBridge;


    @Override
    public boolean careGroup(Long userId, Long groupId) throws Exception {
        // 1. 验证参数
        validateUserIdAndGroupId(userId, groupId);

        // 2. 检查是否已关注
        int count = userGroupCareMapper.countByUserIdAndGroupId(userId, groupId);
        if (count > 0) {
            throw new RuntimeException("Already following this group");
        }

        // 3. 新增关注记录
        UserGroupCare userGroupCare = new UserGroupCare();
        userGroupCare.setUserId(userId);
        userGroupCare.setGroupId(groupId);
        userGroupCare.setGmtCreated(new Date());
        return save(userGroupCare);
    }

    @Override
    public boolean uncareGroup(Long userId, Long groupId) throws Exception {
        // 1. 验证参数
        validateUserIdAndGroupId(userId, groupId);

        // 2. 检查是否已关注
        int count = userGroupCareMapper.countByUserIdAndGroupId(userId, groupId);
        if (count == 0) {
            throw new RuntimeException("Not following this group");
        }

        // 3. 删除关注记录
        boolean result = lambdaUpdate()
                .eq(UserGroupCare::getUserId, userId)
                .eq(UserGroupCare::getGroupId, groupId)
                .remove();

        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_CARE).broadcast()
                .body(GROUP_CARE, GROUP_CARE, groupId).build();
        streamBridge.send(MSG_TOPIC, cagentMqFrame);

        return result;
    }

    @Override
    public boolean batchUncareGroups(Long userId, List<Long> groupIds) throws Exception {
        // 1. 验证参数
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Invalid user ID");
        }
        if (groupIds == null || groupIds.isEmpty()) {
            throw new RuntimeException("Group ID list cannot be empty");
        }

        // 2. 批量删除
        int affectedRows = userGroupCareMapper.deleteByUserIdAndGroupIds(userId, groupIds);
        return affectedRows > 0;
    }

    @Override
    public List<Long> getCaredGroupIds(Long userId) throws Exception {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Invalid user ID");
        }
        return userGroupCareMapper.selectGroupIdsByUserId(userId);
    }

    @Override
    public IPage<Long> getCaredGroupsPage(Long userId, Integer pageNum, Integer pageSize) throws Exception {
        // 1. 验证参数
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Invalid user ID");
        }
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }

        // 2. 分页查询群组ID（如需群组详情需关联群组表）
        Page<Long> page = new Page<>(pageNum, pageSize);
        // 注：实际使用时需关联群组表查询完整信息，此处仅返回ID作为示例
        List<Long> groupIds = userGroupCareMapper.selectGroupIdsByUserId(userId);
        page.setRecords(groupIds);
        page.setTotal(groupIds.size());

        return page;
    }

    @Override
    public boolean checkIsCared(Long userId, Long groupId) throws Exception {
        validateUserIdAndGroupId(userId, groupId);
        int count = userGroupCareMapper.countByUserIdAndGroupId(userId, groupId);
        return count > 0;
    }

    /**
     * 验证用户ID和群组ID有效性
     */
    private void validateUserIdAndGroupId(Long userId, Long groupId) throws Exception {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("Invalid user ID");
        }
        if (groupId == null || groupId <= 0) {
            throw new RuntimeException("Invalid group ID");
        }
    }
}

