package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.CoopUserRatingItemDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupRatingReqDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupWithArchiveStatusDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.RatingDetailsDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.RatingDimensionItemDTO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.vo.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.GroupRatingService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CollaborationPostMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.CreateGroupMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.GroupRatingMapper;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GroupRatingServiceImpl extends ServiceImpl<GroupRatingMapper, GroupRating> implements GroupRatingService {
    @Resource
    private CreateGroupMapper createGroupMapper;

    @Resource
    private CollaborationPostMapper collaborationPostMapper;

    @Autowired
    private IdWorker idWorker;
    /**
     * 获取当前用户对指定群组的已评价列表
     *
     * @param groupId 群组ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param currentUserId 当前用户ID
     * @return 评价列表响应对象
     */
    @Override
    public GroupRatingListRespVO getRatedList(Long groupId, Integer pageNum, Integer pageSize, Long currentUserId) {
        // 创建分页对象
        Page<GroupRating> page = new Page<>(pageNum, pageSize);
        
        // 使用 MyBatis-Plus 分页查询当前用户在指定群组的评价记录，按评价时间倒序排列
        IPage<GroupRating> ratingPage = this.page(page,
            new LambdaQueryWrapper<GroupRating>().eq(GroupRating::getGroupId, groupId)
                    .eq(GroupRating::getRaterId, currentUserId)
                    .orderByDesc(GroupRating::getGmtCreated));

        // 将实体对象转换为响应对象，并将JSON格式的评价详情转换为对象
        List<GroupRatingListItemVO> itemVOs = ratingPage.getRecords().stream().map(rating -> {
            GroupRatingListItemVO itemVO = new GroupRatingListItemVO();
            BeanUtils.copyProperties(rating, itemVO);
            RatingDetailsVO ratingDetailsVO = new RatingDetailsVO();
            ratingDetailsVO.setDimensions(JSON.parseArray(rating.getRatingDetails(), RatingDimensionItemVO.class));
            ratingDetailsVO.setComment(rating.getComment());
            itemVO.setRatingDetails(ratingDetailsVO);
            return itemVO;
        }).collect(Collectors.toList());

        // 组装分页响应对象
        GroupRatingListRespVO respVO = new GroupRatingListRespVO();
        respVO.setRecords(itemVOs);
        respVO.setTotal(ratingPage.getTotal());
        respVO.setPageNum(ratingPage.getCurrent());
        respVO.setPageSize(ratingPage.getSize());

        return respVO;
    }

    /**
     * 对群组协同岗发起评分
     *
     * @param groupId 群组ID
     * @param reqDTO 评价请求对象
     * @param currentUserId 当前用户ID
     * @return 评价响应对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupRatingRespVO createRating(Long groupId, GroupRatingReqDTO reqDTO, Long currentUserId) {
        // 联表查询群组信息和归档状态，只需一次数据库查询
        GroupWithArchiveStatusDTO groupStatus = createGroupMapper.selectGroupWithArchiveStatus(groupId);

        // 校验群组是否存在
        if (groupStatus == null) {
            throw new BusinessException("群组不存在");
        }

        // 校验群组是否已归档
        if (groupStatus.getArchived() == null || !groupStatus.getArchived().equals(2)) {
            throw new BusinessException("只能对已归档的群组进行评价");
        }

        // 校验群组类型是否为协同群组
        if (groupStatus.getGroupType() == null || groupStatus.getGroupType() != 2) {
            throw new BusinessException("只能对协同群组进行评价");
        }
        // 校验评价者是否为群组成员
        List<String> memberIdList = Arrays.asList(groupStatus.getMemberIds().split(","));
        if (!memberIdList.contains(String.valueOf(currentUserId))) {
            throw new BusinessException("只有群组成员才能对协同群组进行评价");
        }

        // 判断评价者类型：如果当前用户ID等于群主ID，则为群主评价，否则为成员评价
        Integer raterType = currentUserId.toString().equals(groupStatus.getOwnerId()) 
            ? RaterTypeEnum.OWNER.getCode() 
            : RaterTypeEnum.MEMBER.getCode();

        // 获取当前用户在该群组已评价的协同岗ID列表
        List<Long> existingCoopUserIds = this.list(
            new LambdaQueryWrapper<GroupRating>()
                .eq(GroupRating::getGroupId, groupId)
                .eq(GroupRating::getRaterId, currentUserId)
        ).stream().map(GroupRating::getCoopUserId).collect(Collectors.toList());

        List<CoopUserRatingRespItemVO> respItems = new ArrayList<>();
        // 遍历待评价的协同岗列表
        for (CoopUserRatingItemDTO ratingItem : reqDTO.getRatings()) {
            // 校验是否重复评价
            if (existingCoopUserIds.contains(ratingItem.getCoopUserId())) {
                throw new BusinessException("已对协同岗 " + ratingItem.getCoopUserName() + " 进行过评分，不可重复评价");
            }

            RatingDetailsDTO details = ratingItem.getRatingDetails();
            // 校验每个维度的评分范围是否在1-5之间
            for (RatingDimensionItemDTO dimension : details.getDimensions()) {
                if (dimension.getScore() < 1 || dimension.getScore() > 5) {
                    throw new BusinessException("评分范围必须在1-5之间");
                }
            }

            // 校验评论内容长度是否超过500字符
            if (details.getComment() != null && details.getComment().length() > 500) {
                throw new BusinessException("评价内容不能超过500字符");
            }

            // 计算平均评分
            BigDecimal avgRating = calculateAvgRating(details.getDimensions());

            // 创建评价记录
            GroupRating rating = new GroupRating();
            rating.setId(idWorker.nextId());
            rating.setGroupId(groupId);
            rating.setRaterId(currentUserId);
            rating.setRaterType(raterType);
            rating.setCoopUserId(ratingItem.getCoopUserId());
            rating.setCoopUserName(ratingItem.getCoopUserName());
            rating.setRatingDetails(JSON.toJSONString(details.getDimensions()));
            rating.setAvgRating(avgRating);
            rating.setComment(details.getComment());
            rating.setGmtCreated(LocalDateTime.now());

            // 保存评价记录
            this.save(rating);

            // 组装响应对象
            CoopUserRatingRespItemVO respItem = new CoopUserRatingRespItemVO();
            respItem.setId(rating.getId());
            respItem.setCoopUserId(rating.getCoopUserId());
            respItem.setCoopUserName(rating.getCoopUserName());
            RatingDetailsVO detailsVO = new RatingDetailsVO();
            detailsVO.setComment(rating.getComment());
            detailsVO.setDimensions(JSON.parseArray(rating.getRatingDetails(), RatingDimensionItemVO.class));
            respItem.setRatingDetails(detailsVO);
            respItem.setAvgRating(rating.getAvgRating());
            respItem.setGmtCreated(rating.getGmtCreated());

            respItems.add(respItem);
        }

        // 组装最终响应对象
        GroupRatingRespVO respVO = new GroupRatingRespVO();
        respVO.setGroupId(groupId);
        respVO.setRatings(respItems);

        return respVO;
    }

    /**
     * 获取指定群组的协同岗列表
     *
     * @param groupId 群组ID
     * @return 协同岗列表
     */
    @Override
    public List<CoopUserVO> getCoopUserList(Long groupId) {
        // 查询群组信息
        CreateGroup createGroup = createGroupMapper.selectByGroupId(groupId);

        if (createGroup == null || createGroup.getUserIds() == null) {
            return new ArrayList<>();
        }

        // 解析群组中的协同岗ID列表（逗号分隔）
        List<Long> userIds = Arrays.stream(createGroup.getUserIds().split(","))
            .filter(id -> !id.isEmpty())
            .map(Long::parseLong)
            .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询协同岗信息,包含已删除的
        List<CollaborationPost> posts = collaborationPostMapper.selectList(
            new LambdaQueryWrapper<CollaborationPost>()
                .in(CollaborationPost::getId, userIds)
                .in(CollaborationPost::getDeleted, Arrays.asList(0, 1))
                    .orderByAsc(CollaborationPost::getId)
        );

        // 转换为响应对象
        return posts.stream().map(post -> {
            CoopUserVO vo = new CoopUserVO();
            vo.setCoopUserId(post.getId());
            vo.setCoopUserName(post.getPostName());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 查询当前用户对指定群组的评价状态
     *
     * @param groupId 群组ID
     * @param currentUserId 当前用户ID
     * @return 评价状态响应对象
     */
    @Override
    public GroupRatingStatusRespVO getRatingStatus(Long groupId, Long currentUserId) {
        // 获取该群组的协同岗总数
        List<CoopUserVO> coopUsers = getCoopUserList(groupId);
        long totalCoopUsers = coopUsers.size();

        // 统计当前用户已评价的协同岗数量
        long ratedCoopUsers = this.count(new LambdaQueryWrapper<GroupRating>()
                .eq(GroupRating::getGroupId, groupId)
                .eq(GroupRating::getRaterId, currentUserId));

        GroupRatingStatusRespVO respVO = new GroupRatingStatusRespVO();
        // 判断是否完成所有评价（当协同岗数量>0且已评价数量等于总数时为完成）
        respVO.setCompleted(totalCoopUsers > 0 && ratedCoopUsers == totalCoopUsers);
        respVO.setTotalCoopUsers(totalCoopUsers);
        respVO.setRatedCoopUsers(ratedCoopUsers);

        // 如果完成所有评价，获取最新的评价时间作为完成时间
        if (totalCoopUsers > 0 && ratedCoopUsers == totalCoopUsers) {
            GroupRating latestRating = this.getOne(
                new LambdaQueryWrapper<GroupRating>()
                    .eq(GroupRating::getGroupId, groupId)
                    .eq(GroupRating::getRaterId, currentUserId)
                    .orderByDesc(GroupRating::getGmtCreated)
                    .last("LIMIT 1")
            );
            if (latestRating != null) {
                respVO.setGmtCompleted(latestRating.getGmtCreated());
            }
        }

        return respVO;
    }

    /**
     * 计算各维度评分的平均值
     *
     * @param dimensions 维度评分列表
     * @return 平均评分（保留两位小数，四舍五入）
     */
    private BigDecimal calculateAvgRating(List<RatingDimensionItemDTO> dimensions) {
        if (CollectionUtils.isEmpty(dimensions)) {
            return BigDecimal.ZERO;
        }

        // 计算所有维度评分的总和
        int sum = dimensions.stream().mapToInt(RatingDimensionItemDTO::getScore).sum();
        // 计算平均值：总和 / 维度数量，保留两位小数
        return BigDecimal.valueOf(sum)
            .divide(BigDecimal.valueOf(dimensions.size()), 2, RoundingMode.HALF_DOWN);
    }
}
