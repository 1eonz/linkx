package com.tdtech.cloudcmd.icp.proxy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.icp.proxy.entity.IsdnTypeDefine;
import com.tdtech.cloudcmd.icp.proxy.repo.IsdnTypeDefineMapper;
import com.tdtech.cloudcmd.icp.proxy.repo.IsdnTypeMapper;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ISDN类型管理Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IsdnTypeService {

    private static final String REDIS_LOCK_KEY_ISDN_TYPE_SYNC = "cloudcmd:icp:isdntype:sync-lock";
    private static final long REDIS_LOCK_WAIT_SECONDS = 10L;

    private final IsdnTypeMapper isdnTypeMapper;
    private final IsdnTypeDefineMapper isdnTypeDefineMapper;
    private final IdWorker idWorker;
    private final RedisLockFactory redisLockFactory;

    /**
     * 分页查询ISDN类型
     *
     * @param pageParam    分页参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    public CcmdPage<IsdnType> selectPage(CcmdPageParam pageParam, LambdaQueryWrapper<IsdnType> queryWrapper) {
        return isdnTypeMapper.selectPageX(pageParam, queryWrapper);
    }

    /**
     * 查询所有ISDN类型
     *
     * @param queryWrapper 查询条件
     * @return 列表结果
     */
    public List<IsdnType> selectAll(LambdaQueryWrapper<IsdnType> queryWrapper) {
        return isdnTypeMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID查询ISDN类型
     *
     * @param id ID
     * @return ISDN类型
     */
    public IsdnType selectById(Long id) {
        return isdnTypeMapper.selectById(id);
    }

    /**
     * 新增ISDN类型
     *
     * @param isdnType ISDN类型信息
     */
    public void insert(IsdnType isdnType) {
        isdnType.setId(idWorker.nextId());
        Date now = new Date();
        isdnType.setGmtCreated(now);
        isdnType.setGmtLastModified(now);
        isdnTypeMapper.insert(isdnType);
    }

    /**
     * 更新ISDN类型
     *
     * @param isdnType ISDN类型信息
     */
    public void update(IsdnType isdnType) {
        isdnType.setGmtLastModified(new Date());
        isdnTypeMapper.updateById(isdnType);
    }

    public void updateIsShow(Long id, Integer isShow) {
        LambdaUpdateWrapper<IsdnType> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(IsdnType::getId, id)
                .set(IsdnType::getIsShow, isShow);
        isdnTypeMapper.update(null, updateWrapper);
    }

    /**
     * 根据ID删除ISDN类型
     *
     * @param id ID
     */
    public void deleteById(Long id) {
        isdnTypeMapper.deleteById(id);
    }


    public String buildIsdnTypeKey(String category, String subusercategory, String apptype) {
        return String.join("-", toStr(category), toStr(subusercategory), toStr(apptype));
    }

    public String toStr(String str) {
        return str == null ? "" : str;
    }

    public Map<String, IsdnType> getIsdnTypeMap() {
        List<IsdnType> isdnTypes = isdnTypeMapper.selectList(Wrappers.lambdaQuery(IsdnType.class));
        return isdnTypes.stream().collect(Collectors.toMap(
                isdnType -> buildIsdnTypeKey(isdnType.getCategory(), isdnType.getSubusercategory(), isdnType.getApptype()),
                Function.identity(),
                (oldValue, newValue) -> oldValue));
    }

    public Map<String, IsdnTypeDefine> getIsdnTypeDefineMap() {
        List<IsdnTypeDefine> isdnTypeDefines = isdnTypeDefineMapper.selectList(Wrappers.lambdaQuery(IsdnTypeDefine.class).eq(IsdnTypeDefine::getInvolve, 1));
        return isdnTypeDefines.stream().collect(Collectors.toMap(
                isdnType -> buildIsdnTypeKey(isdnType.getCategory(), isdnType.getSubusercategory(), isdnType.getApptype()),
                Function.identity(),
                (oldValue, newValue) -> oldValue));
    }

    /**
     * 按 (category, subusercategory, apptype) 合并多来源的 IsdnType 列表，相同 key 保留先出现者。
     * 用于跨表（tb_isdn / tb_camera）合并去重。
     * @param sources 多个来源的 IsdnType 列表，null 视为空
     * @return 合并去重后的列表，保持插入顺序
     */
    @SafeVarargs
    public final List<IsdnType> mergeIsdnTypeByKey(List<IsdnType>... sources) {
        Map<String, IsdnType> mergedMap = new LinkedHashMap<>();
        if (sources != null) {
            for (List<IsdnType> source : sources) {
                if (source == null || source.isEmpty()) {
                    continue;
                }
                for (IsdnType t : source) {
                    String key = buildIsdnTypeKey(t.getCategory(), t.getSubusercategory(), t.getApptype());
                    mergedMap.putIfAbsent(key, t);
                }
            }
        }
        return new ArrayList<>(mergedMap.values());
    }

    public void syncIsdnType(List<IsdnType> isdnTypes) {
        if (isdnTypes == null || isdnTypes.isEmpty()) {
            return;
        }
        var redisLock = redisLockFactory.newRedisLock(REDIS_LOCK_KEY_ISDN_TYPE_SYNC, Duration.ofMinutes(3L));
        boolean locked = false;
        try {
            locked = redisLock.tryLock(REDIS_LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!locked) {
                log.warn("syncIsdnType lock busy, skip this sync: count={}", isdnTypes.size());
                return;
            }
            doSyncIsdnType(isdnTypes);
        } finally {
            if (locked) {
                redisLock.unlock();
            }
        }
    }

    private void doSyncIsdnType(List<IsdnType> isdnTypes) {
        log.info("开始同步ISDN类型数据: count={}, isdnTypes={}", isdnTypes.size(), isdnTypes);
        // 查询tb_isdn_type_define表
        Map<String, IsdnType> isdnTypeMap = getIsdnTypeMap();
        Map<String, IsdnTypeDefine> isdnTypeDefineMap = getIsdnTypeDefineMap();
        List<IsdnType> insertList = new ArrayList<>();
        for (IsdnType isdnType : isdnTypes) {
            String key = buildIsdnTypeKey(isdnType.getCategory(), isdnType.getSubusercategory(), isdnType.getApptype());
            IsdnTypeDefine isdnTypeDefine = isdnTypeDefineMap.get(key);
            IsdnType type = isdnTypeMap.get(key);
            if (isdnTypeDefine != null) {
                if (type == null) {
                    isdnType.setId(idWorker.nextId());
                } else {
                    isdnType.setId(type.getId());
                    isdnType.setIcon(type.getIcon());
                    isdnType.setIconUri(type.getIconUri());
                    isdnType.setIsShow(type.getIsShow());
                }
                isdnType.setName(StringUtils.isNullBlank(isdnTypeDefine.getSubTypeName()) ? isdnTypeDefine.getTypeName() : isdnTypeDefine.getSubTypeName());
                insertList.add(isdnType);
            }
        }
        if (!insertList.isEmpty()) {
            // 批量插入或更新
            isdnTypeMapper.insertOrUpdateBatch(insertList);
        }
        log.info("同步ISDN类型数据完成: count={}, insertList={}", insertList.size(), insertList);
    }
}