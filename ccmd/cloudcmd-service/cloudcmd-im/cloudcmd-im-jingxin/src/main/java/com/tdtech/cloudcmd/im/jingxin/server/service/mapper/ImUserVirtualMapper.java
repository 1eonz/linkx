package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tdtech.cloudcmd.im.jingxin.server.aiclient.entity.AiVirtualUser;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.GroupAiBindEntity;
import com.tdtech.cloudcmd.im.jingxin.server.entity.dto.ImUserVirtual;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

@Mapper
public interface ImUserVirtualMapper extends BaseMapper<ImUserVirtual> {
    /**
     * update imUserVirtual
     *
     * @param imUserVirtual the updated record
     * @return update count
     */
    int updateByPrimaryKey(ImUserVirtual imUserVirtual);

    int batchInsert(@Param("list") List<ImUserVirtual> list);

    int deleteByPrimaryKeyIn(List<Long> list);

    AiVirtualUser selectByClientId(@Param("appId") Serializable appId);

    /**
     * 按 appId 查询虚拟用户（包含未绑定智能体的）。
     * 与 selectByClientId 的区别：不附带 ta.agent_id is not null 过滤，
     * 未绑定智能体的虚拟用户（如三方平台双向虚拟用户）也能查到，用于发送消息场景。
     */
    AiVirtualUser selectAiVirtualUserIncludeUnboundByAppId(@Param("appId") Serializable appId);

    List<AiVirtualUser> selectAll();

    AiVirtualUser selectDefaultUser();

    /**
     * 查询所有默认虚拟用户（已绑定智能体），用于多默认用户场景的负载均衡
     */
    List<AiVirtualUser> selectDefaultUsers();

    GroupAiBindEntity selectGroupAiBind(@Param("appId") String appId);

    List<ImUserVirtual> selectAppIdByIds(@Param("ids") List<Long> ids);

    ImUserVirtual selectVirtualUser(@Param("user") ImUserVirtual imUserVirtual);

    ImUserVirtual selectVirtualUserByUserName(@Size(max = 100, message = "虚拟用户名称（同警信后台设置的名称）最大长度要小于 100") @NotBlank(message = "虚拟用户名称（同警信后台设置的名称）不能为空") @Param("userName") String userName);

    ImUserVirtual selectVirtualUserByAppId(@Size(max = 64, message = "应用ID（警信后台开户获取）最大长度要小于 64") @NotBlank(message = "应用ID（警信后台开户获取）不能为空") @Param("appId") String appId);

    void deleteUserById(@NotNull(message = "主键ID不能为null") @Param("id") Long id);
}