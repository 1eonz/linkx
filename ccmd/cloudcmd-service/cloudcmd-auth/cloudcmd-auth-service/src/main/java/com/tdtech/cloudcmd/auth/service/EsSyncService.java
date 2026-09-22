package com.tdtech.cloudcmd.auth.service;

import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ES数据同步服务
 * 负责将MySQL数据同步到ES
 */
@Slf4j
@Service
public class EsSyncService {

    @Autowired
    private ImUserEsService imUserEsService;


    /**
     * 批量检查ES中是否存在这些用户
     *
     * @param userIds 用户ID列表
     * @return 存在的用户ID列表
     */
    public List<Long> batchExistsInEs(List<Long> userIds) {
        return imUserEsService.existIds(userIds);
    }

    /**
     * 批量同步用户数据到ES（幂等性）
     * 先检查哪些用户已存在，只同步不存在的用户
     *
     * @param userDOs MySQL用户数据列表
     * @return 同步成功的数量
     */
    public void batchSyncUsersToEs(List<ImUserDO> userDOs) {
        if (CollectionUtils.isEmpty(userDOs)) {
            return;
        }

        try {
            // 提取所有用户ID
            List<Long> userIds = userDOs.stream()
                    .map(ImUserDO::getId)
                    .collect(Collectors.toList());

            // 批量检查哪些用户已存在
            List<Long> existingIds = batchExistsInEs(userIds);

            // 过滤出不存在的用户
            List<ImUserDO> newUsers = userDOs.stream()
                    .filter(user -> !existingIds.contains(user.getId()))
                    .collect(Collectors.toList());

            List<ImUserDO> updateUsers = userDOs.stream()
                    .filter(user -> existingIds.contains(user.getId()))
                    .collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(newUsers)) {
                // 同步至es
                imUserEsService.insert(newUsers);
            }
            if (CollectionUtils.isNotEmpty(updateUsers)) {
                imUserEsService.updateByIds(updateUsers);
            }

            log.debug("批量同步用户到ES成功: 新增{}条，更新{}条", newUsers.size(), existingIds.size());
        } catch (Exception e) {
            log.error("批量同步用户到ES失败", e);
        }
    }
}
