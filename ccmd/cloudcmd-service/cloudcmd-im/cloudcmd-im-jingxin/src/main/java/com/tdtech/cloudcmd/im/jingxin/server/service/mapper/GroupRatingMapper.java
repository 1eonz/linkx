package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupRatingStatQueryQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.GroupRating;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupRatingWithGroupInfoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author 群组归档评价
 * @date 2025/06/05
 */
@Mapper
public interface GroupRatingMapper extends BaseMapper<GroupRating> {

    /**
     * 查询群组评价及关联群组信息
     *
     * @param query 查询参数
     * @return 评价列表
     */
    List<GroupRatingWithGroupInfoDTO> selectGroupRatingWithGroupInfo(@Param("query") GroupRatingStatQueryQO query);

    /**
     * 分批查询群组评价及关联群组信息
     *
     * @param query 查询参数
     * @param offset 偏移量
     * @param limit 数量
     * @return 评价列表
     */
    List<GroupRatingWithGroupInfoDTO> selectGroupRatingWithGroupInfoPage(
            @Param("query") GroupRatingStatQueryQO query,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 查询评价总数
     *
     * @param query 查询参数
     * @return 总数
     */
    int selectGroupRatingCount(@Param("query") GroupRatingStatQueryQO query);

    /**
     * 查询群组评价及关联群组信息（TopN协同岗位）
     *
     * @param query 查询参数
     * @return 评价列表
     */
    List<GroupRatingWithGroupInfoDTO> selectTopNCoop(@Param("query") GroupRatingStatQueryQO query, @Param("topN") Integer topN);

    /**
     * 查询所有评价数据（用于统计TopN协同岗）
     *
     * @param query 查询参数
     * @param offset 偏移量
     * @param limit 数量
     * @return 评价列表
     */
    List<GroupRatingWithGroupInfoDTO> selectAllCoopRatingsPage(
            @Param("query") GroupRatingStatQueryQO query,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 查询协同岗评价总数
     *
     * @param query 查询参数
     * @return 总数
     */
    int selectAllCoopRatingsCount(@Param("query") GroupRatingStatQueryQO query);

    /**
     * 查询指定协同岗的评价数据
     *
     * @param query 查询参数
     * @param coopUserIds 协同岗ID列表
     * @param offset 偏移量
     * @param limit 数量
     * @return 评价列表
     */
    List<GroupRatingWithGroupInfoDTO> selectCoopRatingsByUserIdsPage(
            @Param("query") GroupRatingStatQueryQO query,
            @Param("coopUserIds") List<Long> coopUserIds,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 查询指定协同岗的评价总数
     *
     * @param query 查询参数
     * @param coopUserIds 协同岗ID列表
     * @return 总数
     */
    int selectCoopRatingsByUserIdsCount(
            @Param("query") GroupRatingStatQueryQO query,
            @Param("coopUserIds") List<Long> coopUserIds);
}
