package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskResponse;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollabsTaskResponseListReqCO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.StaticTaskResponseAggVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 设施资源信息表（包括摄像头、治安岗亭、消防栓等） Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-11
 */
@Mapper
public interface CollaborationTaskResponseMapper extends BaseMapper<CollaborationTaskResponse> {

    Page<CollaborationTaskResponse> listPage(Page<CollaborationTaskResponse> page,@Param("qo")  CollabsTaskResponseListReqCO collabsTaskResponseListReqCO);

    List<CollaborationTaskResponse> getCTaskResps(@Param("taskId") Long taskId, @Param("userId") Long userId,@Param("postId")Long postId);

    Long queryNumById(@Param("userId")Long userId,@Param("postIdList")List<Long> postIdList,@Param("userIdList")List<Long> userIdList,@Param("startTime")String startTime,@Param("endTime")String endTime);

    /**
     * 游标分页拉取任务回复统计明细（一条 SQL 关联出全部聚合字段）。
     *
     * <p>关联：tb_collaboration_post（post_name，LEFT JOIN by post_id）。
     *
     * <p>双游标：{@code (gmt_created, id) > (timeAfter, idAfter)}，即
     * {@code gmt_created > timeAfter OR (gmt_created = timeAfter AND id > idAfter)}。
     * 首次拉取 timeAfter/idAfter 传 null。多查 1 条（pageSize+1）用于判断 hasMore。
     *
     * <p>SQL 实现见 mapper/CollaborationTaskResponseMapper.xml#selectStaticTaskResponseAggByCursor。
     *
     * @param timeAfter 游标时间（上次最后一条的 gmt_created），首次传 null
     * @param idAfter   游标ID（上次最后一条的 id），首次传 null
     * @param pageSize  每页大小（实际查 pageSize+1 条）
     * @return 聚合 VO 列表
     */
    List<StaticTaskResponseAggVO> selectStaticTaskResponseAggByCursor(@Param("timeAfter") Date timeAfter,
                                                                     @Param("idAfter") Long idAfter,
                                                                     @Param("pageSize") int pageSize);
}