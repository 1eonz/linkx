package com.tdtech.cloudcmd.auth.task;

import cloudcmd.dto.SystemConfigDto;
import cloudcmd.service.rpc.SystemConfigRpcService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.ImUserES;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.service.EsSyncService;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.es.EsIndexInitUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据同步到ES任务
 * 将tb_im_user表的数据同步到ES
 * <p>
 * 仅在项目启动后执行一次，后续通过接口触发
 */
@Slf4j
@Component
@Order(4)
public class DataSyncToESTask {

    /**
     * 查询数据库的批次大小
     */
    private static final int PAGE_SIZE = 200;

    /**
     * es批量操作的批次大小
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 历史数据是否处理完成
     */
    public static final String SYNC_COMPLETED_KEY = "ES_IM_USER_SYNC_COMPLETED";

    @Autowired
    private ImUserMapper imUserMapper;

    @Autowired
    private EsSyncService esSyncService;

    @Autowired
    private EncryptionService encryptService;

    @Autowired
    private SystemConfigRpcService configRpcService;

    @Autowired
    private EsIndexInitUtil indexInitUtil;


    /**
     * 项目启动后执行一次
     */
//    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，开始执行数据同步到ES任务");
        syncDataToES();
    }

    /**
     * 同步数据到ES
     */
    public void syncDataToES() {
        // 是否启用加密，启用才执行
        if (!encryptService.encryptEnabled()) {
            log.info("加密未启用, 跳过ES同步任务执行");
            return;
        }

        // 必须处理完数据库的加密，才能执行同步到es
        if (!isDbEncryptSuccess()) {
            log.info("db数据未处理完成, 跳过ES同步任务执行");
            return;
        }

        // 检查是否已完成加密
        if (isSyncCompleted()) {
            log.info("历史数据同步至es已完成，跳过任务执行");
            return;
        }

        log.info("开始执行数据同步到ES任务");
        // 同步到es中，只需将数据库的数据全部往es里丢，存在则更新，不存在则写入，不处理应该在es删除的数据(接口触发自动同步)
        // 创建索引
        indexInitUtil.createIndexIfNotExists(ImUserES.class);
        try {
            // 查询总数，全量同步
            Long totalCount = countDataToSync();
            if (totalCount == null || totalCount == 0L) {
                log.info("没有需要同步的数据，标记为已完成");
                markEncryptionCompleted();
                return;
            }

            log.info("发现{}条需要同步的数据", totalCount);

            // 分页处理
            int processedCount = 0;
            int successCount = 0;
            int failCount = 0;
            Long lastId = null;

            while (true) {
                // 分页查询数据（按主键排序）
                List<ImUserDO> userList = queryDataToSync(lastId, PAGE_SIZE);

                if (CollectionUtils.isEmpty(userList)) {
                    log.info("DataSyncToESTask 查询到的数据为空，终止数据同步!");
                    break;
                }

                // 批量同步到ES（每100条一批）
                List<List<ImUserDO>> partitionList = Lists.partition(userList, BATCH_SIZE);
                for (List<ImUserDO> batch : partitionList) {
                    try {
                        // 批量同步（幂等性）, 存在则更新不存在则新增
                        esSyncService.batchSyncUsersToEs(batch);
                        processedCount += batch.size();
                        successCount += batch.size();
                    } catch (Exception e) {
                        log.error("批量同步用户数据到ES失败，batch size: {}", batch.size(), e);
                        failCount += batch.size();
                        processedCount += batch.size();
                        break;
                    }
                }
                // 更新lastId（用于下次分页）
                lastId = userList.get(userList.size() - 1).getId();

                log.info("已处理{}/{}条数据，成功{}条，失败{}条",
                        processedCount, totalCount, successCount, failCount);

                // 休息一下，避免对ES造成压力
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            // 记录日志
            log.warn("数据同步到ES任务完成，共处理{}条数据，成功{}条，失败{}条",
                    processedCount, successCount, failCount);
            if (processedCount == successCount) {
                log.info("同步数据到es成功，标记为已完成");
                markEncryptionCompleted();
            }
        } catch (Exception e) {
            log.error("数据同步到ES任务执行失败", e);
        }
    }

    private boolean isDbEncryptSuccess() {
        String config = configRpcService.getValueByKey(DataEncryptionTask.ENCRYPTION_COMPLETED_KEY);
        return StringUtils.isNotBlank(config) && Boolean.parseBoolean(config);
    }

    /**
     * 统计需要同步的数据总数
     */
    private Long countDataToSync() {
        return imUserMapper.selectCount(new QueryWrapper<>());
    }

    /**
     * 分页查询需要同步的数据
     */
    private List<ImUserDO> queryDataToSync(Long lastId, int pageSize) {
        // 按主键ID排序，从lastId开始查询
        LambdaQueryWrapper<ImUserDO> wrapper = Wrappers.lambdaQuery(ImUserDO.class)
                .gt(lastId != null, ImUserDO::getId, lastId)
                .orderByAsc(ImUserDO::getId)
                .last("LIMIT " + pageSize);

        return imUserMapper.selectList(wrapper);
    }
    /**
     * 标记同步已完成
     */
    private void markEncryptionCompleted() {
        configRpcService.saveOrUpdate(new SystemConfigDto(SYNC_COMPLETED_KEY, Boolean.TRUE.toString()));
    }

    /**
     * 检查是否已完成同步
     */
    private boolean isSyncCompleted() {
        String config = configRpcService.getValueByKey(SYNC_COMPLETED_KEY);
        return StringUtils.isNotBlank(config) && Boolean.parseBoolean(config);
    }
}
