package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.api.entity.collaborations.GroupCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CreateGroup;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CreateGroupCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.StaticCreateGroupAggVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.UserCreationCountVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupWithArchiveStatusDTO;
import com.tdtech.cloudcmd.mysql.enhance.ExBaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Set;

/**
 * @author lsc
 * @date 2025/7/18
 **/
@Mapper
public interface CreateGroupMapper extends ExBaseMapper<CreateGroup> {
    Logger log = LoggerFactory.getLogger(CreateGroupMapper.class);

    List<GroupCreationCountVO> groupCreateCountByCode(@Param("departmentCode") List<String> departmentCode,
        @Param("startTime") String startTime, @Param("endTime") String endTime, @Param("source") Integer source);

    List<UserCreationCountVO> groupCreateCountByUser(@Param("departmentCode") List<String> departmentCode,
        @Param("startTime") String startTime, @Param("endTime") String endTime);

    int groupCreateAllCount(@Param("departmentCode") List<String> departmentCode, @Param("startTime") String startTime,
        @Param("endTime") String endTime, @Param("source") Integer source);

    default void upsert(CreateGroup createGroup, LambdaUpdateWrapper<CreateGroup> update) {
        try {
            this.insert(createGroup);
        } catch (DuplicateKeyException e) {
            log.warn("on duplicated insert:{}", createGroup);
            if (update != null) {
                this.update(update);
            }
        }
    }

    int deleteByGroupId(@Param("groupId") Long groupId);

    CreateGroup selectByGroupId(@Param("groupId") Long groupId);

    List<CreateGroupCountVO> findListByOrgCodeList(@Param("orgCodeList") List<String> orgCodeList);

    Set<Long> selectByGroupIds(@Param("groupIds") Set<Long> groupIds);

    Page<CreateGroup> selectUserGroupsByKeywords(Page<CreateGroup> page, @Param("userId") Long userId,
        @Param("keywords") String keywords);

    /**
     * 通过群组ID联表查询群组信息和归档状态
     *
     * @param groupId 群组ID
     * @return 群组与归档状态DTO
     */
    GroupWithArchiveStatusDTO selectGroupWithArchiveStatus(@Param("groupId") Long groupId);

    /**
     * 游标分页拉取建群记录统计明细（一条 SQL 关联出可联表的聚合字段）。
     *
     * <p>关联：
     * <ul>
     *   <li>tb_group_extends（group_type，LEFT JOIN by group_id）</li>
     * </ul>
     *
     * <p>注：department_id 字段实为 IM 部门 code，code→id 转换需查警信 IM 服务
     * （跨服务 RPC，无法 SQL 联表），由应用层按 department_id 批量查 imService 补充。
     *
     * <p>双游标：{@code (update_time, group_id) > (timeAfter, idAfter)}，即
     * {@code update_time > timeAfter OR (update_time = timeAfter AND group_id > idAfter)}。
     * 首次拉取 timeAfter/idAfter 传 null。多查 1 条（pageSize+1）用于判断 hasMore。
     * 注意：游标用 group_id（业务唯一键），不是主键 id。
     *
     * <p>SQL 实现见 mapper/CreateGroupMapper.xml#selectStaticCreateGroupAggByCursor。
     *
     * @param timeAfter 游标时间（上次最后一条的 update_time），首次传 null
     * @param idAfter   游标ID（上次最后一条的 group_id），首次传 null
     * @param pageSize  每页大小（实际查 pageSize+1 条）
     * @return 聚合 VO 列表
     */
    List<StaticCreateGroupAggVO> selectStaticCreateGroupAggByCursor(@Param("timeAfter") java.util.Date timeAfter,
                                                                    @Param("idAfter") Long idAfter,
                                                                    @Param("pageSize") int pageSize);
}