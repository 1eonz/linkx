package com.tdtech.cloudcmd.linkx.dashboard.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.linkx.dashboard.vo.GroupMsgSendStaticVO;
import com.tdtech.cloudcmd.util.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.GroupMsgSendStatic;
import com.tdtech.cloudcmd.linkx.dashboard.mapper.GroupMsgSendStaticMapper;
import com.tdtech.cloudcmd.linkx.dashboard.service.IGroupMsgSendStaticService;
import com.tdtech.cloudcmd.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 群组消息统计 Service 实现类
 *
 * @author: S063874
 * @date: 2026-03-10
 */
@Slf4j
@Service
public class GroupMsgSendStaticServiceImpl implements IGroupMsgSendStaticService {

    @Resource
    private GroupMsgSendStaticMapper groupMsgSendStaticMapper;

    @DubboReference
    private DepartmentRpcApi departmentRpcApi;


    @Override
    public Long createGroupMsgSendStatic(GroupMsgSendStatic groupMsgSendStatic) {
        groupMsgSendStatic.setGmtCreated(new Date());
        groupMsgSendStaticMapper.insert(groupMsgSendStatic);
        return groupMsgSendStatic.getMsgId();
    }

    @Override
    public int batchCreateGroupMsgSendStatic(List<GroupMsgSendStatic> groupMsgSendStaticList) {
        if (CollectionUtils.isEmpty(groupMsgSendStaticList)) {
            return 0;
        }
        // 设置创建时间
        Date now = new Date();
        groupMsgSendStaticList.forEach(item -> item.setGmtCreated(now));
        // 批量插入
        return groupMsgSendStaticMapper.insertBatch(groupMsgSendStaticList);
    }

    @Override
    public int batchCreateGroupMsgSendStaticWithFilter(List<GroupMsgSendStatic> groupMsgSendStaticList) {
        if (CollectionUtils.isEmpty(groupMsgSendStaticList)) {
            log.warn("批量新增群组消息统计：传入的数据列表为空");
            return 0;
        }

        // 提取所有消息ID
        List<Long> msgIds = groupMsgSendStaticList.stream()
                .map(GroupMsgSendStatic::getMsgId)
                .collect(Collectors.toList());

        // 查询已存在的消息ID
        List<Long> existingMsgIds = groupMsgSendStaticMapper.selectExistingMsgIds(msgIds);

        // 过滤掉已存在的数据
        List<GroupMsgSendStatic> newDataList = groupMsgSendStaticList.stream()
                .filter(item -> !existingMsgIds.contains(item.getMsgId()))
                .collect(Collectors.toList());

        // 如果过滤后没有数据，打印日志提示
        if (CollectionUtils.isEmpty(newDataList)) {
            log.warn("批量新增群组消息统计：所有数据都已存在，过滤后的数据为空。原始数据数量：{}，已存在数量：{}", 
                    groupMsgSendStaticList.size(), existingMsgIds.size());
            return 0;
        }

        // 打印过滤信息
        if (!CollectionUtils.isEmpty(existingMsgIds)) {
            log.info("批量新增群组消息统计：过滤掉已存在的消息ID，原始数据数量：{}，已存在数量：{}，待插入数量：{}", 
                    groupMsgSendStaticList.size(), existingMsgIds.size(), newDataList.size());
        }

        // 设置创建时间并批量插入
        Date now = new Date();
        newDataList.forEach(item -> item.setGmtCreated(now));
        return groupMsgSendStaticMapper.insertBatch(newDataList);
    }

    @Override
    public void updateGroupMsgSendStatic(GroupMsgSendStatic groupMsgSendStatic) {
        // 校验存在
        validateGroupMsgSendStaticExists(groupMsgSendStatic.getMsgId());
        // 更新
        groupMsgSendStaticMapper.updateById(groupMsgSendStatic);
    }

    @Override
    public void deleteGroupMsgSendStatic(Long msgId) {
        // 校验存在
        validateGroupMsgSendStaticExists(msgId);
        // 删除
        groupMsgSendStaticMapper.deleteById(msgId);
    }

    @Override
    public void deleteGroupMsgSendStaticByIds(List<Long> msgIds) {
        // 校验存在
        validateGroupMsgSendStaticExists(msgIds);
        // 删除
        groupMsgSendStaticMapper.deleteBatchIds(msgIds);
    }

    @Override
    public GroupMsgSendStatic getGroupMsgSendStaticById(Long msgId) {
        return groupMsgSendStaticMapper.selectById(msgId);
    }

    @Override
    public List<GroupMsgSendStatic> listGroupMsgSendStatic() {
        LambdaQueryWrapper<GroupMsgSendStatic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(GroupMsgSendStatic::getMsgTime);
        return groupMsgSendStaticMapper.selectList(queryWrapper);
    }

    @Override
    public List<GroupMsgSendStatic> listBySessionId(Long sessionId) {
        LambdaQueryWrapper<GroupMsgSendStatic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupMsgSendStatic::getSessionId, sessionId)
                .orderByDesc(GroupMsgSendStatic::getMsgTime);
        return groupMsgSendStaticMapper.selectList(queryWrapper);
    }

    @Override
    public List<GroupMsgSendStatic> listByUserId(Long userId) {
        LambdaQueryWrapper<GroupMsgSendStatic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupMsgSendStatic::getUserId, userId)
                .orderByDesc(GroupMsgSendStatic::getMsgTime);
        return groupMsgSendStaticMapper.selectList(queryWrapper);
    }

    @Override
    public List<GroupMsgSendStatic> listByDepartmentId(Long departmentId) {
        LambdaQueryWrapper<GroupMsgSendStatic> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GroupMsgSendStatic::getDepartmentId, departmentId)
                .orderByDesc(GroupMsgSendStatic::getMsgTime);
        return groupMsgSendStaticMapper.selectList(queryWrapper);
    }

    @Override
    public GroupMsgSendStatic getLatestByUserId(Long userId) {
        return groupMsgSendStaticMapper.selectLatestByUserId(userId);
    }

    @Override
    public List<GroupMsgSendStaticVO> countTopMessageByUserId(String departmentCode, String startTime, String endTime, Integer topn) {
        // 将 startTime 转为 YYYY-MM-DD 00:00:00
        Date startDateTime = parseStartTime(startTime);
        // 将 endTime 转为 YYYY-MM-DD 23:59:59
        Date endDateTime = parseEndTime(endTime);
        // 获取departmentCode的所有子部门
        List<OrganizationVO> organizationVOs = departmentRpcApi.queryDepartmentForList(departmentCode);
        List<Long> departmentIds = organizationVOs.stream().map(OrganizationVO::getId).collect(Collectors.toList());
        return groupMsgSendStaticMapper.countTopMessageByUserId(departmentIds, startDateTime, endDateTime, topn);
    }

    /**
     * 将开始时间字符串转为 Date 类型，格式为 YYYY-MM-DD 00:00:00
     *
     * @param startTime 开始时间字符串（格式：YYYY-MM-DD）
     * @return Date 类型的时间
     */
    private Date parseStartTime(String startTime) {
        if (StringUtils.isBlank(startTime)) {
            return null;
        }
        try {
            // 使用 Java 8 的 LocalDateTime
            LocalDate localDate = LocalDate.parse(startTime);
            LocalDateTime localDateTime = localDate.atTime(0, 0, 0);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error("开始时间格式转换失败: {}", startTime, e);
            return null;
        }
    }

    /**
     * 将结束时间字符串转为 Date 类型，格式为 YYYY-MM-DD 23:59:59
     *
     * @param endTime 结束时间字符串（格式：YYYY-MM-DD）
     * @return Date 类型的时间
     */
    private Date parseEndTime(String endTime) {
        if (StringUtils.isBlank(endTime)) {
            return null;
        }
        try {
            // 使用 Java 8 的 LocalDateTime
            LocalDate localDate = LocalDate.parse(endTime);
            LocalDateTime localDateTime = localDate.atTime(23, 59, 59);
            return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error("结束时间格式转换失败: {}", endTime, e);
            return null;
        }
    }

    /**
     * 校验群组消息统计是否存在
     *
     * @param msgId 消息ID
     */
    private void validateGroupMsgSendStaticExists(Long msgId) {
        if (groupMsgSendStaticMapper.selectById(msgId) == null) {
            throw new RuntimeException("群组消息统计不存在");
        }
    }

    /**
     * 校验群组消息统计是否存在
     *
     * @param msgIds 消息ID列表
     */
    private void validateGroupMsgSendStaticExists(List<Long> msgIds) {
        List<GroupMsgSendStatic> list = groupMsgSendStaticMapper.selectBatchIds(msgIds);
        if (CollectionUtils.isEmpty(list) || list.size() != msgIds.size()) {
            throw new RuntimeException("群组消息统计不存在");
        }
    }
}