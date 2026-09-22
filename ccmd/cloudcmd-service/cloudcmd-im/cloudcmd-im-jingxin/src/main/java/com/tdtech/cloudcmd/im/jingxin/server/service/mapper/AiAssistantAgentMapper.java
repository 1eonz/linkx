package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUserVirtualRespVO;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.AiAssistantAgent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;

@Mapper
public interface AiAssistantAgentMapper extends BaseMapper<AiAssistantAgent> {

    int updateByPrimaryKey(AiAssistantAgent record);

    int batchInsert(@Param("list") List<AiAssistantAgent> list);

    int deleteByPrimaryKeyIn(@Param("list") List<Long> list);

    /**
     * 查询已绑定智能体的虚拟用户连接信息。
     */
    List<AiVirtualUser> selectAiVirtualUserByVirtualUserIds(@Param("ids") List<Long> ids);

    List<ImUserVirtualRespVO> selectBinsUser(@Param("ids") List<Long> ids);

    void deleteByVirtualUserId(@NotNull(message = "主键ID不能为null") @Param("id") Long id);

    void deleteByVirtualUserIds(@Param("ids") Collection<Long> ids);
}
