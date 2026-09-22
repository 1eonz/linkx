package com.tdtech.cloudcmd.auth.task;

import cloudcmd.dto.SystemConfigDto;
import cloudcmd.service.rpc.SystemConfigRpcService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.ImUserDto;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 历史数据加密任务
 * 将tb_im_user表的历史数据的敏感字段加密
 * 
 * 仅在项目启动后执行一次，后续通过接口触发
 */
@Slf4j
@Component
@Order(2)
public class DataEncryptionTask {

    /**
     * 分页处理数据大小
     */
    private static final int BATCH_SIZE = 200;

    /**
     * 历史数据是否处理完成
     */
    public static final String ENCRYPTION_COMPLETED_KEY = "DB_IM_USER_ENCRYPT_COMPLETED";

    /**
     * 已经处理的数据的最大时间
     */
    private static final String ENCRYPTION_LAST_TIME = "DB_IM_USER_ENCRYPT_TIME";

    /**
     * 已经处理的数据的最大ID
     */
    private static final String ENCRYPTION_LAST_ID = "DB_IM_USER_ENCRYPT_ID";

    @Resource
    private ImUserMapper imUserMapper;
    
    @Resource
    private EncryptionService encryptionService;

    @DubboReference
    private SystemConfigRpcService configRpcService;

    /**
     * 项目启动后执行一次
     */
//    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动完成，开始执行历史数据加密任务");
        encryptHistoricalData();
    }

    /**
     * 加密历史数据
     */
    public void encryptHistoricalData() {
        log.info("开始执行历史数据加密任务");

        // id不具备时序性，接口可以指定，但是同步的时间范围，已经限定死了。因此不会有问题
        try {
            // 检查是否开启了加密
            if (!encryptionService.encryptEnabled()) {
                log.info("加密开关关闭，跳过历史数据加密任务的执行");
                return;
            }

            // 检查是否已完成加密
            if (isEncryptionCompleted()) {
                log.info("历史数据加密已完成，跳过任务执行");
                return;
            }
            
            // 仅处理此日期之前的数据
            Date maxDate = getMaxDate();


            // 获取上次处理的lastId（用于断点续传）
            Long lastId = getLastId();
            log.info("从lastId={}开始处理", lastId);
            
            // 查询需要加密的数据总数
            Long totalCount = countUnencryptedData(maxDate, lastId);
            if (totalCount == null || totalCount == 0L) {
                log.info("没有需要加密的历史数据，标记为已完成");
                markEncryptionCompleted();
                return;
            }
            
            log.info("发现{}条需要加密的历史数据", totalCount);
            // 分批处理数据
            encryptData(maxDate, lastId, totalCount);

        } catch (Exception e) {
            log.error("历史数据加密任务执行失败", e);
        }
    }

    private void encryptData(Date maxDate, Long lastId, Long totalCount) {
        // 分页处理
        int processedCount = 0;
        int successCount = 0;
        int failCount = 0;

        while (true) {
            // 分页查询未加密的数据（按主键排序）
            List<ImUserDO> userList = queryUnencryptedData(maxDate, lastId, BATCH_SIZE);
            if (CollectionUtils.isEmpty(userList)) {
                break;
            }

            // 批量加密并更新数据
            try {
                // 批量加密并更新
                lastId = userList.get(userList.size() - 1).getId();
                saveLastId(lastId);
                batchEncryptAndUpdate(userList);
                successCount += userList.size();
                processedCount += userList.size();
                log.info("批次处理成功，已处理{}/{}条数据，lastId更新为: {}", processedCount, totalCount, lastId);
            } catch (Exception e) {
                log.error("批量加密用户数据失败，batch size: {}", userList.size(), e);
                failCount += userList.size();
                processedCount += userList.size();
                // 单批次失败，停止处理，下次从lastId继续
                log.warn("批次处理失败，停止任务，下次从lastId={}继续", lastId);
                break;
            }
        }

        // 如果全部成功，标记为已完成
        if (failCount == 0 && processedCount == totalCount.intValue()) {
            markEncryptionCompleted();
            log.info("历史数据加密任务完成，全部成功，共处理{}条数据，已标记为已完成", processedCount);
        } else {
            log.warn("历史数据加密任务完成，共处理{}条数据，成功{}条，失败{}条，下次从lastId={}继续",
                processedCount, successCount, failCount, lastId);
        }
    }

    /**
     * 获取需要处理的数据的最大的时间，只处理对应时间之前的数据
     */
    private Date getMaxDate() {
        String maxDate = configRpcService.getValueByKey(ENCRYPTION_LAST_TIME);
        if (StringUtils.isBlank(maxDate)) {
            Date date = new Date();
            configRpcService.saveOrUpdate(new SystemConfigDto(ENCRYPTION_LAST_TIME,
                    DateFormatUtil.format(date, DateFormatUtil.YYYYMMDDHHMMSSSSS)));
            return date;
        }
        return DateFormatUtil.parseDate(maxDate, DateFormatUtil.YYYYMMDDHHMMSSSSS);
    }
    
    /**
     * 检查是否已完成加密
     */
    private boolean isEncryptionCompleted() {
        String config = configRpcService.getValueByKey(ENCRYPTION_COMPLETED_KEY);
        return StringUtils.isNotBlank(config) && Boolean.parseBoolean(config);
    }
    
    /**
     * 标记加密已完成
     */
    private void markEncryptionCompleted() {
        configRpcService.saveOrUpdate(new SystemConfigDto(ENCRYPTION_COMPLETED_KEY, Boolean.TRUE.toString()));
    }
    
    /**
     * 获取上次处理的lastId
     */
    private Long getLastId() {
        String lastId = configRpcService.getValueByKey(ENCRYPTION_LAST_ID);
        return StringUtils.isBlank(lastId) ? null : Long.parseLong(lastId);
    }
    
    /**
     * 保存lastId
     */
    private void saveLastId(Long lastId) {
        configRpcService.saveOrUpdate(new SystemConfigDto(ENCRYPTION_LAST_ID, lastId.toString()));
    }
    
    /**
     * 统计需要加密的数据总数
     */
    private Long countUnencryptedData(Date beforeTime, Long lastId) {
        LambdaQueryWrapper<ImUserDO> wrapper = Wrappers.lambdaQuery(ImUserDO.class)
            .gt(lastId != null, ImUserDO::getId, lastId)
            .lt(ImUserDO::getGmtCreated, beforeTime);
        return imUserMapper.selectCount(wrapper);
    }
    
    /**
     * 分页查询未加密的数据
     */
    private List<ImUserDO> queryUnencryptedData(Date beforeTime, Long lastId, int pageSize) {
        // 不能直接使用mybatis-plus方式查询数据，拦截器会解密
        List<ImUserDto> imUserDtos = imUserMapper.getList(lastId, beforeTime, pageSize);
        return BeanCopyUtils.copyList(imUserDtos, ImUserDO::new);
    }
    
    /**
     * 批量加密并更新数据
     */
    private void batchEncryptAndUpdate(List<ImUserDO> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return;
        }

        // 批量更新数据库, 拦截器字段加密
        imUserMapper.updateById(userList);
        log.debug("批量加密用户数据成功，count: {}", userList.size());
    }
}
