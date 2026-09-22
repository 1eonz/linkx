
package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatQueryQO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatRespVO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupTagVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationPost;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Label;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.RatingDimensionItemVO;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupRatingStatService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.*;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 群组评分统计服务实现类
 * 提供标签评分统计和协同岗评分统计功能
 */
@Slf4j
@Service
public class GroupRatingStatServiceImpl implements GroupRatingStatService {
    /**
     * 统计结果中展示的TopN数量
     */
    private static final int TOP_N = 10;

    /**
     * 每批查询的数据量（避免内存溢出）
     */
    private static final int BATCH_SIZE = 1000;

    private static final List<RatingDimensionItemDTO> DEFAULT_DIMENSION_LIST = Arrays.asList(
            new RatingDimensionItemDTO(1L, "supportResponse", "支撑响应速度"),
            new RatingDimensionItemDTO(2L, "approvalResponse", "审批响应速度"),
            new RatingDimensionItemDTO(3L, "dataValidity", "数据内容有效性"),
            new RatingDimensionItemDTO(4L, "overall", "协同岗综合评分")
    );

    @Autowired
    private GroupRatingMapper groupRatingMapper;

    @Autowired
    private CollaborationPostMapper collaborationPostMapper;

    @Autowired
    private GroupTagMapper groupTagMapper;

    @Autowired
    private OrganizationDiversionService orgService;

    @Autowired
    private LabelMapper labelMapper;

    /**
     * 获取群组标签评分统计
     * 按时间范围查询标签的平均评分TopN，以及各维度评分
     *
     * @param queryQO 查询条件
     * @return 评分统计响应
     */
    @Override
    public GroupRatingStatRespVO getGroupTagRating(GroupRatingStatQueryQO queryQO) {
        log.info("getGroupTagRating开始执行, queryQO: {}", queryQO);
        queryQO.convertTimeStrings();
        // 综合评价应该包含群主评价
        queryQO.setType(queryQO.getType() == 1 ? 1 : null);
        // 获取部门及其子部门编码, 无部门代表筛选全部
        List<ImDepartment> imDepartments = orgService.queryDepartmentForList(queryQO.getDepartmentCode());
        List<Long> deptIds = new ArrayList<>();
        List<String> deptCodes = new ArrayList<>();
        for (ImDepartment imDepartment : imDepartments) {
            deptIds.add(imDepartment.getId());
            deptCodes.add(imDepartment.getCode());
        }
        queryQO.setDepartmentIds(deptIds);
        queryQO.setDepartmentCodes(deptCodes);

        // 查询对应时间范围内，群组关联标签
        List<GroupTagVO> groupTagVOS = groupTagMapper.listTagsByTimeRange(queryQO.getEndTimeDateTime(), queryQO.getDepartmentCodes());
        log.info("查询到时间范围内的群组标签数量: {}", groupTagVOS.size());

        // 如果无任何标签，则直接返回空结果
        if (CollectionUtils.isEmpty(groupTagVOS)) {
            return buildEmptyCoopResponse();
        }

        // 构建标签到群组的映射，以及群组到标签的映射
        Map<Long, Set<Long>> tagToGroupIdsMap = new HashMap<>();
        Map<Long, Set<Long>> groupToTagsMap = new HashMap<>();
        Map<Long, GroupTagVO> groupTagMap = new HashMap<>();
        Set<Long> allGroupIds = new HashSet<>();
        List<Label> allTags = new ArrayList<>();
        buildTagGroupMaps(groupTagVOS, tagToGroupIdsMap, groupToTagsMap, groupTagMap, allGroupIds, allTags);
        log.info("构建标签-群组映射完成, 标签数量: {}, 群组数量: {}", tagToGroupIdsMap.size(), allGroupIds.size());

        // 如果没有群组数据，返回空结果
        if (allGroupIds.isEmpty()) {
            return buildEmptyCoopResponse();
        }
        queryQO.setGroupIds(new ArrayList<>(allGroupIds));

        // 查询总数据量
        int totalCount = groupRatingMapper.selectGroupRatingCount(queryQO);
        log.info("查询到群组评分总数据量: {}", totalCount);

        // 如果无任何评分数据，标签全补0
        if (totalCount == 0) {
            return buildEmptyCoopResponse();
        }

        // 步骤1：分批查询并处理评分数据，先统计每个群组的各维度统计信息（只计算总和和数量）
        Map<Long, GroupRatingStaDTO> groupRatingStaMap = new HashMap<>();
        processGroupRatingsBatch(queryQO, totalCount, groupRatingStaMap);
        log.info("分批处理群组评分完成, 群组统计数量: {}", groupRatingStaMap.size());

        // 步骤2：遍历完所有数据后，计算每个群组每个维度的平均分
        calculateGroupAvgRatings(groupRatingStaMap);

        // 步骤3：构建标签的统计信息
        Map<Long, GroupRatingTagStaDTO> tagStaMap = buildTagRatingSta(groupTagVOS, tagToGroupIdsMap, groupRatingStaMap);
        log.info("构建标签统计信息完成, 有评分数据的标签数量: {}", tagStaMap.size());

        // 步骤4：排序并获取TopN标签
        List<GroupRatingTagStaDTO> sortedTags = sortAndGetTopNTags(tagStaMap);
        log.info("排序并获取TopN标签完成, TopN标签数量: {}", sortedTags.size());
        if (sortedTags.isEmpty()) {
            return buildEmptyCoopResponse();
        }

        // 特殊处理，如果排序后的数据不足TOP_N个，且标签的数量超过这里的TOP_N的标签统计数据，剩下的标签需要补上0的评分数据，最多补到TOP_N个
        // 与SE测试对齐，从评价数据构建标签，没有补零的场景
//        fillZeroRatingTags(sortedTags, allTags, queryQO.getType());
        // 步骤5：构建响应
        return buildTagResponse(sortedTags);
    }

    /**
     * 获取协同岗评分统计
     * 按时间范围查询协同岗的平均评分TopN，以及各维度评分
     *
     * @param queryQO 查询条件
     * @return 评分统计响应
     */
    @Override
    public GroupRatingStatRespVO getCoopUserRating(GroupRatingStatQueryQO queryQO) {
        log.info("getCoopUserRating开始执行, queryQO: {}", queryQO);
        queryQO.convertTimeStrings();
        // 综合评价应该包含群主评价
        queryQO.setType(queryQO.getType() == 1 ? 1 : null);
        // 获取部门及其子部门编码
        List<ImDepartment> imDepartments = orgService.queryDepartmentForList(queryQO.getDepartmentCode());
        List<String> deptCodes = new ArrayList<>();
        for (ImDepartment imDepartment : imDepartments) {
            deptCodes.add(imDepartment.getCode());
        }
        queryQO.setDepartmentCodes(deptCodes);

        // 查询总数据量
        int totalCount = groupRatingMapper.selectAllCoopRatingsCount(queryQO);
        log.info("查询到协同岗评分总数据量: {}", totalCount);
        if (totalCount == 0) {
            // 无协同岗评分数据，返回空响应
            return buildEmptyCoopResponse();
        }

        // 分批查询并处理协同岗评分数据
        Map<Long, GroupRatingCoopStaDTO> coopStatsMap = new HashMap<>();
        processCoopRatingsBatch(queryQO, totalCount, coopStatsMap);
        log.info("分批处理协同岗评分完成, 协同岗统计数量: {}", coopStatsMap.size());

        // 计算各维度平均分和综合平均分
        calculateCoopAvgRatings(coopStatsMap);

        // 排序并获取TopN协同岗
        List<GroupRatingCoopStaDTO> sortedCoopUsers = sortAndGetTopNCoopUsers(coopStatsMap);
        log.info("排序并获取TopN协同岗完成, TopN协同岗数量: {}", sortedCoopUsers.size());
        if (sortedCoopUsers.isEmpty()) {
            return buildEmptyCoopResponse();
        }

        // 特殊处理，如果协同岗的数量超过这里的TOP_N的协同岗统计数据，剩下的协同岗需要补上0的评分数据，最多补到TOP_N个
        // 从评价数据构建协同岗，没有补零的场景
//        fillZeroRatingCoopUsers(sortedCoopUsers, posts, queryQO.getType());
        // 构建响应
        return buildCoopResponse(sortedCoopUsers);
    }

    /**
     * 构建标签-群组映射关系
     *
     * @param groupTagVOS      群组标签数据
     * @param tagToGroupIdsMap 标签到群组ID的映射（输出）
     * @param groupToTagsMap   群组ID到标签的映射（输出）
     * @param groupTagMap      群组ID到标签VO的映射（输出）
     * @param allGroupIds      所有群组ID集合（输出）
     */
    private void buildTagGroupMaps(List<GroupTagVO> groupTagVOS, Map<Long, Set<Long>> tagToGroupIdsMap,
                                   Map<Long, Set<Long>> groupToTagsMap, Map<Long, GroupTagVO> groupTagMap,
                                   Set<Long> allGroupIds, List<Label> allTags) {
        Map<Long, Label> tagMap = new HashMap<>();
        for (GroupTagVO groupTag : groupTagVOS) {
            if (groupTag.getId() != null && groupTag.getGroupId() != null) {
                tagToGroupIdsMap.computeIfAbsent(groupTag.getId(), k -> new HashSet<>()).add(groupTag.getGroupId());
                groupToTagsMap.computeIfAbsent(groupTag.getGroupId(), k -> new HashSet<>()).add(groupTag.getId());
                groupTagMap.put(groupTag.getGroupId(), groupTag);
                allGroupIds.add(groupTag.getGroupId());
                Label tag = tagMap.get(groupTag.getId());
                if (tag == null) {
                    tag = new Label();
                    tag.setId(groupTag.getId());
                    tag.setName(groupTag.getName());
                }
                tagMap.putIfAbsent(groupTag.getId(), tag);
            }
        }
        allTags.addAll(tagMap.values().stream().sorted(Comparator.comparing(Label::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList()));
    }

    /**
     * 分批处理群组评分数据
     *
     * @param queryQO           查询条件
     * @param totalCount        总数据量
     * @param groupRatingStaMap 群组统计信息（输出）
     */
    private void processGroupRatingsBatch(GroupRatingStatQueryQO queryQO, int totalCount,
                                          Map<Long, GroupRatingStaDTO> groupRatingStaMap) {
        int offset = 0;
        int batchCount = 0;
        while (offset < totalCount) {
            // 分批查询数据
            List<GroupRatingWithGroupInfoDTO> batch = groupRatingMapper.selectGroupRatingWithGroupInfoPage(queryQO, offset, BATCH_SIZE);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            batchCount++;
            log.info("处理群组评分批次 {}, 偏移量: {}, 批次数据量: {}", batchCount, offset, batch.size());

            // 处理当前批次数据
            processSingleGroupBatch(batch, groupRatingStaMap);

            // 清空批次数据，帮助GC回收内存
            batch.clear();
            offset += BATCH_SIZE;
        }
        log.info("群组评分批量处理完成, 总批次: {}", batchCount);
    }

    /**
     * 处理单个批次的群组评分数据
     *
     * @param batch             批次数据
     * @param groupRatingStaMap 群组统计信息（输出）
     */
    private void processSingleGroupBatch(List<GroupRatingWithGroupInfoDTO> batch,
                                         Map<Long, GroupRatingStaDTO> groupRatingStaMap) {
        for (GroupRatingWithGroupInfoDTO rating : batch) {
            try {
                // 解析JSON格式的评分详情
                List<RatingDimensionItemVO> details = JSON.parseArray(rating.getRatingDetails(), RatingDimensionItemVO.class);
                if (details == null || details.isEmpty()) {
                    continue;
                }

                // 获取或创建群组统计信息
                Long groupId = rating.getGroupId();
                GroupRatingStaDTO groupSta = getOrCreateGroupSta(rating, groupId, groupRatingStaMap);

                // 更新各维度统计信息
                updateGroupDimensionStats(details, groupSta);
            } catch (Exception e) {
                log.warn("解析评价详情失败，ratingId: {}", rating.getId(), e);
                throw new RuntimeException("解析评价详情失败，ratingId: " + rating.getId(), e);
            }
        }
    }


    /**
     * 获取或创建群组统计信息
     *
     * @param rating            评分数据
     * @param groupId           群组ID
     * @param groupRatingStaMap 群组统计信息映射
     * @return 群组统计信息
     */
    private GroupRatingStaDTO getOrCreateGroupSta(GroupRatingWithGroupInfoDTO rating, Long groupId,
                                                  Map<Long, GroupRatingStaDTO> groupRatingStaMap) {
        return groupRatingStaMap.computeIfAbsent(groupId, k -> {
            GroupRatingStaDTO s = GroupRatingStaDTO.builder()
                    .groupId(groupId)
                    .raterType(rating.getRaterType())
                    .dimensionMap(new LinkedHashMap<>())
                    .build();
            return s;
        });
    }

    /**
     * 更新群组维度统计信息
     *
     * @param details  评分详情
     * @param groupSta 群组统计信息（输出）
     */
    private void updateGroupDimensionStats(List<RatingDimensionItemVO> details, GroupRatingStaDTO groupSta) {
        for (RatingDimensionItemVO dimension : details) {
            Long dimensionId = dimension.getId();
            GroupRatingDimensionDTO dimStat = groupSta.getDimensionMap().computeIfAbsent(dimensionId, k ->
                    GroupRatingDimensionDTO.builder()
                            .id(dimensionId)
                            .code(dimension.getCode())
                            .name(dimension.getName())
                            .ratingSum(BigDecimal.ZERO)
                            .ratingCount(0)
                            .build());

            // 累加评分和数量
            dimStat.setRatingSum(dimStat.getRatingSum().add(BigDecimal.valueOf(dimension.getScore())));
            dimStat.setRatingCount(dimStat.getRatingCount() + 1);
        }
    }

    /**
     * 计算群组各维度平均分和综合平均分
     *
     * @param groupRatingStaMap 群组统计信息（输入输出）
     */
    private void calculateGroupAvgRatings(Map<Long, GroupRatingStaDTO> groupRatingStaMap) {
        for (GroupRatingStaDTO groupSta : groupRatingStaMap.values()) {
            BigDecimal totalOverallSum = BigDecimal.ZERO;
            int totalOverallCount = 0;

            // 计算每个维度的平均分
            for (GroupRatingDimensionDTO dimStat : groupSta.getDimensionMap().values()) {
                if (dimStat.getRatingCount() > 0) {
                    dimStat.setAvgScore(dimStat.getRatingSum().divide(BigDecimal.valueOf(dimStat.getRatingCount()), 2, RoundingMode.HALF_DOWN));
                }
                // 累加用于计算综合平均分
                totalOverallSum = totalOverallSum.add(dimStat.getRatingSum());
                totalOverallCount += dimStat.getRatingCount();
            }

            // 计算综合平均分
            if (totalOverallCount > 0 && !groupSta.getDimensionMap().isEmpty()) {
                BigDecimal totalAvgScore = totalOverallSum.divide(BigDecimal.valueOf(totalOverallCount), 2, RoundingMode.HALF_DOWN);
                groupSta.setAvgRating(totalAvgScore);
            }
        }
    }

    /**
     * 构建标签评分统计信息
     *
     * @param groupTagVOS       群组标签数据
     * @param tagToGroupIdsMap  标签到群组ID的映射
     * @param groupRatingStaMap 群组统计信息
     * @return 标签统计信息映射
     */
    private Map<Long, GroupRatingTagStaDTO> buildTagRatingSta(List<GroupTagVO> groupTagVOS,
                                                              Map<Long, Set<Long>> tagToGroupIdsMap,
                                                              Map<Long, GroupRatingStaDTO> groupRatingStaMap) {
        Map<Long, GroupRatingTagStaDTO> tagStaMap = new HashMap<>();

        // 构建标签到标签VO的映射（方便获取标签名称和ID）
        Map<Long, GroupTagVO> tagIdToTagMap = new HashMap<>();
        for (GroupTagVO groupTag : groupTagVOS) {
            if (groupTag.getId() != null) {
                tagIdToTagMap.put(groupTag.getId(), groupTag);
            }
        }

        // 遍历每个标签，构建标签统计信息
        for (Map.Entry<Long, Set<Long>> entry : tagToGroupIdsMap.entrySet()) {
            Long tagId = entry.getKey();
            Set<Long> groupIds = entry.getValue();
            GroupTagVO tagVO = tagIdToTagMap.get(tagId);

            if (tagVO == null) {
                continue;
            }

            // 收集该标签对应的群组统计信息
            List<GroupRatingStaDTO> groupStaList = new ArrayList<>();
            for (Long groupId : groupIds) {
                GroupRatingStaDTO groupSta = groupRatingStaMap.get(groupId);
                if (groupSta != null && groupSta.getAvgRating() != null) {
                    groupStaList.add(groupSta);
                }
            }

            if (groupStaList.isEmpty()) {
                continue;
            }

            // 构建标签统计信息
            GroupRatingTagStaDTO tagSta = GroupRatingTagStaDTO.builder()
                    .tagName(tagVO.getName())
                    .tagId(tagVO.getId())
                    .raterType(groupStaList.get(0).getRaterType())
                    .groupRatingStaList(groupStaList)
                    .dimensionMap(new LinkedHashMap<>())
                    .build();

            // 计算标签各维度平均分：各群组平均分的总和除以群组数量
            calculateTagAvgRatings(tagSta);

            tagStaMap.put(tagId, tagSta);
        }

        return tagStaMap;
    }

    /**
     * 计算标签各维度平均分和综合平均分
     *
     * @param tagSta 标签统计信息（输入输出）
     */
    private void calculateTagAvgRatings(GroupRatingTagStaDTO tagSta) {
        List<GroupRatingStaDTO> groupStaList = tagSta.getGroupRatingStaList();
        if (CollectionUtils.isEmpty(groupStaList)) {
            return;
        }

        // 使用第一个群组的维度信息来初始化标签的维度映射
        GroupRatingStaDTO firstGroupSta = groupStaList.get(0);
        for (GroupRatingDimensionDTO groupDimStat : firstGroupSta.getDimensionMap().values()) {
            GroupRatingDimensionDTO tagDimStat = GroupRatingDimensionDTO.builder()
                    .id(groupDimStat.getId())
                    .code(groupDimStat.getCode())
                    .name(groupDimStat.getName())
                    .ratingSum(BigDecimal.ZERO)
                    .ratingCount(0)
                    .build();
            tagSta.getDimensionMap().put(groupDimStat.getId(), tagDimStat);
        }

        // 累加各群组的平均分
        BigDecimal totalOverallSum = BigDecimal.ZERO;
        for (GroupRatingStaDTO groupSta : groupStaList) {
            if (groupSta.getAvgRating() != null) {
                totalOverallSum = totalOverallSum.add(groupSta.getAvgRating());
            }

            for (Map.Entry<Long, GroupRatingDimensionDTO> entry : groupSta.getDimensionMap().entrySet()) {
                Long dimensionId = entry.getKey();
                GroupRatingDimensionDTO groupDimStat = entry.getValue();
                GroupRatingDimensionDTO tagDimStat = tagSta.getDimensionMap().get(dimensionId);

                if (tagDimStat != null && groupDimStat.getAvgScore() != null) {
                    tagDimStat.setRatingSum(tagDimStat.getRatingSum().add(groupDimStat.getAvgScore()));
                    tagDimStat.setRatingCount(tagDimStat.getRatingCount() + 1);
                }
            }
        }

        // 计算标签各维度平均分
        for (GroupRatingDimensionDTO tagDimStat : tagSta.getDimensionMap().values()) {
            if (tagDimStat.getRatingCount() > 0) {
                tagDimStat.setAvgScore(tagDimStat.getRatingSum().divide(BigDecimal.valueOf(tagDimStat.getRatingCount()), 2, RoundingMode.HALF_DOWN));
            }
        }

        // 计算标签综合平均分
        if (!groupStaList.isEmpty()) {
            BigDecimal tagAvgRating = totalOverallSum.divide(BigDecimal.valueOf(groupStaList.size()), 2, RoundingMode.HALF_DOWN);
            tagSta.setAvgRating(tagAvgRating);
        }
    }

    /**
     * 排序并获取TopN标签
     *
     * @param tagStaMap 标签统计信息
     * @return 排序后的TopN标签
     */
    private List<GroupRatingTagStaDTO> sortAndGetTopNTags(Map<Long, GroupRatingTagStaDTO> tagStaMap) {
        return tagStaMap.values().stream()
                .filter(e -> e.getAvgRating() != null)
                .sorted((e1, e2) -> e2.getAvgRating().compareTo(e1.getAvgRating()))
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    /**
     * 构建标签评分响应
     *
     * @param sortedTags 排序后的TopN标签
     * @return 评分统计响应
     */
    private GroupRatingStatRespVO buildTagResponse(List<GroupRatingTagStaDTO> sortedTags) {
        List<String> xAxis = sortedTags.stream().map(GroupRatingTagStaDTO::getTagName).collect(Collectors.toList());
        List<String> legend = DEFAULT_DIMENSION_LIST.stream().map(RatingDimensionItemDTO::getName).collect(Collectors.toList());
        List<GroupRatingStatRespVO.SeriesItemVO> series = new ArrayList<>();

        // 为每个维度构建数据序列
        for (RatingDimensionItemDTO dimension : DEFAULT_DIMENSION_LIST) {
            String name = dimension.getName();
            Long dimensionId = dimension.getId();
            List<BigDecimal> data = new ArrayList<>();

            // 为每个标签获取该维度的平均分
            for (GroupRatingTagStaDTO tagSta : sortedTags) {
                GroupRatingDimensionDTO dimStat = tagSta.getDimensionMap().get(dimensionId);
                if (dimStat == null || dimStat.getAvgScore() == null) {
                    data.add(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN));
                } else {
                    data.add(dimStat.getAvgScore());
                }
            }

            series.add(GroupRatingStatRespVO.SeriesItemVO.builder()
                    .name(name)
                    .data(data)
                    .build());
        }

        return GroupRatingStatRespVO.builder()
                .xAxis(xAxis)
                .legend(legend)
                .series(series)
                .build();
    }

    /**
     * 分批处理协同岗评分数据
     *
     * @param queryQO      查询条件
     * @param totalCount   总数据量
     * @param coopStatsMap 协同岗统计信息（输出）
     */
    private void processCoopRatingsBatch(GroupRatingStatQueryQO queryQO, int totalCount,
                                         Map<Long, GroupRatingCoopStaDTO> coopStatsMap) {
        int offset = 0;
        int batchCount = 0;
        while (offset < totalCount) {
            // 分批查询数据
            List<GroupRatingWithGroupInfoDTO> batch = groupRatingMapper.selectAllCoopRatingsPage(queryQO, offset, BATCH_SIZE);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            batchCount++;
            log.info("处理协同岗评分批次 {}, 偏移量: {}, 批次数据量: {}", batchCount, offset, batch.size());

            // 处理当前批次数据
            processSingleCoopBatch(batch, coopStatsMap);

            // 清空批次数据，帮助GC回收内存
            batch.clear();
            offset += BATCH_SIZE;
        }
        log.info("协同岗评分批量处理完成, 总批次: {}", batchCount);
    }

    /**
     * 处理单个批次的协同岗评分数据
     *
     * @param batch        批次数据
     * @param coopStatsMap 协同岗统计信息（输出）
     */
    private void processSingleCoopBatch(List<GroupRatingWithGroupInfoDTO> batch, Map<Long, GroupRatingCoopStaDTO> coopStatsMap) {
        for (GroupRatingWithGroupInfoDTO rating : batch) {
            try {
                // 解析JSON格式的评分详情
                List<RatingDimensionItemVO> details = JSON.parseArray(rating.getRatingDetails(), RatingDimensionItemVO.class);
                if (CollectionUtils.isEmpty(details)) {
                    continue;
                }

                // 获取或创建协同岗统计信息
                Long coopUserId = rating.getCoopUserId();
                GroupRatingCoopStaDTO coopStat = getOrCreateCoopStat(rating, coopUserId, coopStatsMap);

                // 更新各维度统计信息
                updateCoopDimensionStats(details, coopStat);
            } catch (Exception e) {
                log.warn("解析评价详情失败，ratingId: {}", rating.getId(), e);
                throw new BusinessException("解析评价详情失败，ratingId: " + rating.getId(), e);
            }
        }
    }

    /**
     * 获取或创建协同岗统计信息
     *
     * @param rating       评分数据
     * @param coopUserId   协同岗ID
     * @param coopStatsMap 协同岗统计信息映射
     * @return 协同岗统计信息
     */
    private GroupRatingCoopStaDTO getOrCreateCoopStat(GroupRatingWithGroupInfoDTO rating, Long coopUserId,
                                                      Map<Long, GroupRatingCoopStaDTO> coopStatsMap) {
        return coopStatsMap.computeIfAbsent(coopUserId, k -> {
            GroupRatingCoopStaDTO s = GroupRatingCoopStaDTO.builder()
                    .coopUserId(coopUserId)
                    .coopUserName(rating.getCoopUserName())
                    .raterType(rating.getRaterType())
                    .dimensionMap(new LinkedHashMap<>())
                    .build();
            return s;
        });
    }

    /**
     * 更新协同岗维度统计信息
     *
     * @param details  评分详情
     * @param coopStat 协同岗统计信息（输出）
     */
    private void updateCoopDimensionStats(List<RatingDimensionItemVO> details, GroupRatingCoopStaDTO coopStat) {
        for (RatingDimensionItemVO dimension : details) {
            Long dimensionId = dimension.getId();
            GroupRatingDimensionDTO dimStat = coopStat.getDimensionMap().computeIfAbsent(dimensionId, k ->
                    GroupRatingDimensionDTO.builder()
                            .id(dimensionId)
                            .code(dimension.getCode())
                            .name(dimension.getName())
                            .ratingSum(BigDecimal.ZERO)
                            .ratingCount(0)
                            .build());

            // 累加评分和数量
            dimStat.setRatingSum(dimStat.getRatingSum().add(BigDecimal.valueOf(dimension.getScore())));
            dimStat.setRatingCount(dimStat.getRatingCount() + 1);
        }
    }

    /**
     * 计算协同岗各维度平均分和综合平均分
     *
     * @param coopStatsMap 协同岗统计信息（输入输出）
     */
    private void calculateCoopAvgRatings(Map<Long, GroupRatingCoopStaDTO> coopStatsMap) {
        for (GroupRatingCoopStaDTO coopStat : coopStatsMap.values()) {
            BigDecimal totalOverallSum = BigDecimal.ZERO;
            int totalOverallCount = 0;

            // 计算每个维度的平均分
            for (GroupRatingDimensionDTO dimStat : coopStat.getDimensionMap().values()) {
                if (dimStat.getRatingCount() > 0) {
                    dimStat.setAvgScore(dimStat.getRatingSum().divide(BigDecimal.valueOf(dimStat.getRatingCount()), 2, RoundingMode.HALF_DOWN));
                }
                // 累加用于计算综合平均分
                totalOverallSum = totalOverallSum.add(dimStat.getRatingSum());
                totalOverallCount += dimStat.getRatingCount();
            }

            // 计算综合平均分
            if (totalOverallCount > 0 && !coopStat.getDimensionMap().isEmpty()) {
                BigDecimal totalAvgScore = totalOverallSum.divide(BigDecimal.valueOf(totalOverallCount), 2, RoundingMode.HALF_DOWN);
                coopStat.setAvgRating(totalAvgScore);
            }
        }
    }

    /**
     * 排序并获取TopN协同岗
     *
     * @param coopStatsMap 协同岗统计信息
     * @return 排序后的TopN协同岗
     */
    private List<GroupRatingCoopStaDTO> sortAndGetTopNCoopUsers(Map<Long, GroupRatingCoopStaDTO> coopStatsMap) {
        return coopStatsMap.values().stream()
                .filter(e -> e.getAvgRating() != null)
                .sorted((e1, e2) -> e2.getAvgRating().compareTo(e1.getAvgRating()))
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    /**
     * 构建协同岗评分响应
     *
     * @param sortedCoopUsers 排序后的TopN协同岗
     * @return 评分统计响应
     */
    private GroupRatingStatRespVO buildCoopResponse(List<GroupRatingCoopStaDTO> sortedCoopUsers) {
        List<String> xAxis = new ArrayList<>();
        List<String> legend = new ArrayList<>();
        Map<String, GroupRatingStatRespVO.SeriesItemVO> seriesMap = new LinkedHashMap<>();
        boolean first = true;

        for (GroupRatingCoopStaDTO coopStat : sortedCoopUsers) {
            xAxis.add(coopStat.getCoopUserName());

            // 收集legend（只在第一次收集）
            if (first && !coopStat.getDimensionMap().isEmpty()) {
                legend = coopStat.getDimensionMap().values().stream()
                        .map(GroupRatingDimensionDTO::getName)
                        .collect(Collectors.toList());
                first = false;
            }

            // 构建每个维度的数据序列
            for (GroupRatingDimensionDTO dim : coopStat.getDimensionMap().values()) {
                GroupRatingStatRespVO.SeriesItemVO seriesItem = seriesMap.get(dim.getName());
                if (seriesItem == null) {
                    seriesItem = new GroupRatingStatRespVO.SeriesItemVO();
                    seriesItem.setName(dim.getName());
                    seriesItem.setData(new ArrayList<>());
                    seriesMap.put(dim.getName(), seriesItem);
                }
                seriesItem.getData().add(dim.getAvgScore());
            }
        }

        return GroupRatingStatRespVO.builder()
                .xAxis(xAxis)
                .legend(legend)
                .series(new ArrayList<>(seriesMap.values()))
                .build();
    }

    /**
     * 补充0分的协同岗数据
     *
     * @param sortedCoopUsers 已排序的协同岗列表（输入输出）
     * @param posts           所有协同岗列表
     * @param raterType       评价类型
     */
    private void fillZeroRatingCoopUsers(List<GroupRatingCoopStaDTO> sortedCoopUsers,
                                         List<CollaborationPost> posts,
                                         Integer raterType) {
        if (sortedCoopUsers.size() >= TOP_N || posts.size() <= sortedCoopUsers.size()) {
            return;
        }

        // 获取已有评分的协同岗ID集合
        Set<Long> existingCoopUserIds = sortedCoopUsers.stream()
                .map(GroupRatingCoopStaDTO::getCoopUserId)
                .collect(Collectors.toSet());

        // 找出还没有评分数据的协同岗
        List<CollaborationPost> remainingPosts = posts.stream()
                .filter(post -> !existingCoopUserIds.contains(post.getId()))
                .sorted(Comparator.comparing(CollaborationPost::getDeleted)
                        .thenComparing(CollaborationPost::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // 补上0的评分数据
        int beforeSize = sortedCoopUsers.size();
        for (CollaborationPost post : remainingPosts) {
            if (sortedCoopUsers.size() >= TOP_N) {
                break;
            }
            // 创建0分的协同岗统计数据
            GroupRatingCoopStaDTO zeroCoopStat = createZeroCoopStat(post, raterType);
            sortedCoopUsers.add(zeroCoopStat);
        }
        log.info("补充0分协同岗数据完成, 补充前数量: {}, 补充后数量: {}, 补充了: {}个",
                beforeSize, sortedCoopUsers.size(), sortedCoopUsers.size() - beforeSize);
    }

    /**
     * 创建0分的协同岗统计数据
     *
     * @param post 协同岗数据
     * @return 0分的协同岗统计数据
     */
    private GroupRatingCoopStaDTO createZeroCoopStat(CollaborationPost post, Integer raterType) {
        Map<Long, GroupRatingDimensionDTO> dimensionMap = new LinkedHashMap<>();
        for (RatingDimensionItemDTO dimensionItemDTO : DEFAULT_DIMENSION_LIST) {
            GroupRatingDimensionDTO dimensionDTO = GroupRatingDimensionDTO.builder()
                    .id(dimensionItemDTO.getId())
                    .code(dimensionItemDTO.getCode())
                    .name(dimensionItemDTO.getName())
                    .ratingSum(BigDecimal.ZERO)
                    .ratingCount(0)
                    .avgScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN))
                    .build();
            dimensionMap.put(dimensionItemDTO.getId(), dimensionDTO);
        }
        return GroupRatingCoopStaDTO.builder()
                .coopUserId(post.getId())
                .coopUserName(post.getPostName())
                .raterType(raterType)
                .dimensionMap(dimensionMap)
                .avgRating(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN))
                .build();
    }

    /**
     * 构建空的协同岗评分响应
     *
     * @return 空的评分统计响应
     */
    private GroupRatingStatRespVO buildEmptyCoopResponse() {
        return GroupRatingStatRespVO.builder()
                .xAxis(Collections.emptyList())
                .legend(Collections.emptyList())
                .series(Collections.emptyList())
                .build();
    }

    /**
     * 补充0分的标签数据
     *
     * @param sortedTags 已排序的标签列表（输入输出）
     * @param allTags    所有标签列表
     * @param raterType  评价类型
     */
    private void fillZeroRatingTags(List<GroupRatingTagStaDTO> sortedTags,
                                    List<Label> allTags,
                                    Integer raterType) {
        if (sortedTags.size() >= TOP_N || allTags.size() <= sortedTags.size()) {
            return;
        }

        // 获取已有评分的标签名称集合
        Set<Long> existingTagIds = sortedTags.stream()
                .map(GroupRatingTagStaDTO::getTagId)
                .collect(Collectors.toSet());

        // 找出还没有评分数据的标签
        List<Label> remainingTags = allTags.stream()
                .filter(tag -> !existingTagIds.contains(tag.getId()))
                .sorted(Comparator.comparing(Label::getIsDeleted)
                        .thenComparing(Label::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        // 补上0的评分数据
        int beforeSize = sortedTags.size();
        for (Label tag : remainingTags) {
            if (sortedTags.size() >= TOP_N) {
                break;
            }
            // 创建0分的标签统计数据
            GroupRatingTagStaDTO zeroTagStat = createZeroTagStat(tag, raterType);
            sortedTags.add(zeroTagStat);
        }
        log.info("补充0分标签数据完成, 补充前数量: {}, 补充后数量: {}, 补充了: {}个",
                beforeSize, sortedTags.size(), sortedTags.size() - beforeSize);
    }

    /**
     * 创建0分的标签统计数据
     *
     * @param label     标签数据
     * @param raterType 评价类型
     * @return 0分的标签统计数据
     */
    private GroupRatingTagStaDTO createZeroTagStat(Label label, Integer raterType) {
        Map<Long, GroupRatingDimensionDTO> dimensionMap = new LinkedHashMap<>();
        for (RatingDimensionItemDTO dimensionItemDTO : DEFAULT_DIMENSION_LIST) {
            GroupRatingDimensionDTO dimensionDTO = GroupRatingDimensionDTO.builder()
                    .id(dimensionItemDTO.getId())
                    .code(dimensionItemDTO.getCode())
                    .name(dimensionItemDTO.getName())
                    .ratingSum(BigDecimal.ZERO)
                    .ratingCount(0)
                    .avgScore(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN))
                    .build();
            dimensionMap.put(dimensionItemDTO.getId(), dimensionDTO);
        }
        return GroupRatingTagStaDTO.builder()
                .tagId(label.getId())
                .tagName(label.getName())
                .raterType(raterType)
                .dimensionMap(dimensionMap)
                .avgRating(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_DOWN))
                .build();
    }

    /**
     * 构建空的协同岗评分响应
     *
     * @return 空的评分统计响应
     */
    private GroupRatingStatRespVO buildZeroCoopResponse(List<String> xAxis) {
        List<GroupRatingStatRespVO.SeriesItemVO> series = new ArrayList<>();
        for (RatingDimensionItemDTO dimensionItemDTO : DEFAULT_DIMENSION_LIST) {
            GroupRatingStatRespVO.SeriesItemVO seriesItemVO = GroupRatingStatRespVO.SeriesItemVO.builder()
                    .name(dimensionItemDTO.getName())
                    .data(Collections.nCopies(xAxis.size(), new BigDecimal("0.00")
                            .setScale(2, RoundingMode.HALF_DOWN)))
                    .build();
            series.add(seriesItemVO);
        }

        return GroupRatingStatRespVO.builder()
                .xAxis(xAxis)
                .legend(DEFAULT_DIMENSION_LIST.stream().map(RatingDimensionItemDTO::getName).collect(Collectors.toList()))
                .series(series)
                .build();
    }
}

