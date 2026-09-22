package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationTaskStatusHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 设施资源信息表（包括摄像头、治安岗亭、消防栓等） Mapper 接口
 * </p>
 *
 * @author mWX556161
 * @since 2020-06-11
 */
@Mapper
public interface CollaborationTaskStatusHistoryMapper extends BaseMapper<CollaborationTaskStatusHistory> {

    CollaborationTaskStatusHistory getCTStatusHistory(@Param("taskId") Long taskId, @Param("userId") Long userId);

}
