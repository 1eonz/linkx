package com.tdtech.cloudcmd.auth.controller;

import cloudcmd.dto.SystemConfigDto;
import cloudcmd.service.rpc.SystemConfigRpcService;
import com.tdtech.cloudcmd.auth.entity.ImUserDO;
import com.tdtech.cloudcmd.auth.entity.ImUserDto;
import com.tdtech.cloudcmd.auth.mapper.ImUserMapper;
import com.tdtech.cloudcmd.auth.task.DataEncryptionTask;
import com.tdtech.cloudcmd.auth.task.DataSyncToESTask;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.encryptor.constants.Constants;
import com.tdtech.cloudcmd.encryptor.service.CachedConfig;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 数据加解密管理接口
 * 用于手动触发历史数据加密、解密操作和ES同步任务
 */
@Slf4j
@RestController
@RequestMapping("/auth/v1/encryption")
public class EncryptionController {
    @Resource
    private DataEncryptionTask dataEncryptionTask;

    @Resource
    private DataSyncToESTask dataSyncToESTask;

    @Autowired
    private SystemConfigRpcService configRpcService;

    @Autowired
    private ImUserMapper imUserMapper;

    @Autowired
    private CachedConfig cachedConfig;
    /**
     * 触发历史数据加密任务
     */
    @PostMapping("/historical/execute")
    public R<String> executeEncryption() {
        log.info("手动触发历史数据加密任务");
        dataEncryptionTask.encryptHistoricalData();
        return R.success("历史数据加密任务执行完成");
    }

    /**
     * 触发数据同步到ES任务
     */
    @PostMapping("/es-sync/execute")
    public R<String> executeEsSync() {
        log.info("手动触发数据同步到ES任务");
        configRpcService.saveOrUpdate(new SystemConfigDto(DataSyncToESTask.SYNC_COMPLETED_KEY,
                Boolean.FALSE.toString()));
        dataSyncToESTask.syncDataToES();
        return R.success("数据同步到ES任务执行完成");
    }

    /**
     * 启用数据加密
     */
    @PostMapping("/enable")
    public R<String> encryptEnable() {
        log.info("手动触发,启用数据加密");
        cachedConfig.clearSystemConfig();
        configRpcService.saveOrUpdate(new SystemConfigDto(Constants.ENABLE_ENCRYPT, Boolean.TRUE.toString()));
        return R.success("启用数据加密成功");
    }

    /**
     * 启用数据加密并处理历史数据
     */
    @PostMapping("/enableAndExecute")
    public R<String> enableAndExecute() {
        log.info("手动触发,启用数据加密");
        cachedConfig.clearSystemConfig();
        configRpcService.saveOrUpdate(new SystemConfigDto(Constants.ENABLE_ENCRYPT, Boolean.TRUE.toString()));
        try {
            TimeUnit.MILLISECONDS.sleep(500);
            dataEncryptionTask.encryptHistoricalData();
            TimeUnit.MILLISECONDS.sleep(500);
            dataSyncToESTask.syncDataToES();
        } catch (InterruptedException e) {
            log.error("数据加密失败处理失败", e);
        }
        return R.success("启用数据加密成功");
    }

    /**
     * 禁用数据加密
     */
    @PostMapping("/disable")
    public R<String> encryptDisable() {
        log.info("手动触发,禁用数据加密");
        cachedConfig.clearSystemConfig();
        configRpcService.saveOrUpdate(new SystemConfigDto(Constants.ENABLE_ENCRYPT, Boolean.FALSE.toString()));
        return R.success("禁用数据加密成功");
    }

    /**
     * 根据用户ID集合加密tb_im_user表的敏感字段
     * 加密字段：name（姓名）、mobile（手机号）、idCard（身份证号）
     * <p>
     * 实现逻辑：
     * 1. 使用自定义查询 selectByIds 获取原始数据（不会自动解密）
     * 2. 转换为 DO 对象
     * 3. 使用 MyBatis-Plus 的 updateById 更新，拦截器会自动加密
     *
     * @param userIds 用户ID集合
     * @return 操作结果
     */
    @PostMapping("/users/encrypt")
    public R<String> encryptUsers(@RequestBody List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return R.failure("用户ID集合不能为空");
        }

        log.info("开始加密用户数据，用户数量: {}", userIds.size());
        // 1. 查询原始数据（不会被拦截器自动解密）
        List<ImUserDto> userDtos = imUserMapper.listByIds(userIds);
        if (CollectionUtils.isEmpty(userDtos)) {
            return R.success("未找到符合条件的用户数据, 跳过执行");
        }

        // 2. 转换为 DO 并更新（拦截器会自动加密）
        List<ImUserDO> imUserDOS = BeanCopyUtils.copyList(userDtos, ImUserDO::new);
        imUserMapper.updateById(imUserDOS);
        return R.success("加密用户数据完成");
    }

    /**
     * 根据用户ID集合解密tb_im_user表的敏感字段
     * 解密字段：name（姓名）、mobile（手机号）、idCard（身份证号）
     * <p>
     * 实现逻辑：
     * 1. 使用自定义查询 selectByIds 获取原始加密数据（不会自动解密）
     * 2. 手动解密敏感字段
     * 3. 使用自定义更新 batchUpdateDecryptedData 批量写入（不会自动加密）
     *
     * @param userIds 用户ID集合
     * @return 操作结果
     */
    @PostMapping("/users/decrypt")
    public R<String> decryptUsers(@RequestBody List<Long> userIds) {
        if (CollectionUtils.isEmpty(userIds)) {
            return R.failure("用户ID集合不能为空");
        }

        log.info("开始解密用户数据，用户数量: {}", userIds.size());
        // 1. 查询原始数据，自动解密
        List<ImUserDO> userDoS = imUserMapper.selectByIds(userIds);
        if (CollectionUtils.isEmpty(userDoS)) {
            return R.success("未找到符合条件的用户数据");
        }

        // 2. 批量更新解密后的数据（不会被拦截器自动加密）
        int updated = imUserMapper.batchUpdateDecryptedData(userDoS);
        log.info("批量更新解密数据成功，更新数量: {}", updated);
        return R.success("批量更新解密数据成功");
    }

    /**
     * 查询加密开关状态
     */
    @GetMapping("/status")
    public R<Boolean> getEncryptStatus() {
        String config = configRpcService.getValueByKey(Constants.ENABLE_ENCRYPT);
        boolean enabled = StringUtils.isNotBlank(config) && Boolean.parseBoolean(config);
        return R.success(enabled);
    }
}
