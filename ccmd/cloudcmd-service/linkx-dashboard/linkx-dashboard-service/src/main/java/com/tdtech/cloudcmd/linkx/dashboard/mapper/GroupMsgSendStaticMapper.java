package com.tdtech.cloudcmd.linkx.dashboard.mapper;

import com.tdtech.cloudcmd.linkx.dashboard.vo.GroupMsgSendStaticVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.linkx.dashboard.entity.GroupMsgSendStatic;

import java.util.Date;
import java.util.List;

/**
 * 群组消息统计 Mapper 接口
 *
 * @author: S063874
 * @date: 2026-03-10
 */
@Mapper
public interface GroupMsgSendStaticMapper extends BaseMapper<GroupMsgSendStatic> {

    /**
     * 批量插入群组消息统计
     *
     * @param list 群组消息统计列表
     * @return 插入数量
     */
    int insertBatch(@Param("list") List<GroupMsgSendStatic> list);

    /**
     * 查询已存在的消息ID列表
     *
     * @param msgIds 消息ID列表
     * @return 已存在的消息ID列表
     */
    List<Long> selectExistingMsgIds(@Param("msgIds") List<Long> msgIds);

    /**
     * 根据用户ID查询最新的消息统计（msgTime最晚的一条）
     *
     * @param userId 用户ID
     * @return 群组消息统计
     */
    GroupMsgSendStatic selectLatestByUserId(@Param("userId") Long userId);


    List<GroupMsgSendStaticVO> countTopMessageByUserId(@Param("departmentIds") List<Long> departmentIds,
                                                       @Param("startTime") Date startTime,
                                                       @Param("endTime") Date endTime,
                                                       @Param("topn") Integer topn);

}