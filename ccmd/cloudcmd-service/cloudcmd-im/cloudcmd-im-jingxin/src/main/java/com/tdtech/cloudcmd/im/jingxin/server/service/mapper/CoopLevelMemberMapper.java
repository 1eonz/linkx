package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopLevelMemberDO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CoopUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface CoopLevelMemberMapper extends BaseMapper<CoopLevelMemberDO> {
    int getMemberCountByLevelId(@Param("levelId") Long levelId);

    Page<CoopUser> getMembers(Page<?> page, @Param("levelId") String levelId, @Param("departments") Collection<String> dept);

    List<CoopUser> getMemberList(@Param("levelId") String levelId);

    void insertBatch(@Param("coopLevelMembers") List<CoopLevelMemberDO> coopLevelMembers);

    void deleteByCoopUserIds(@Param("ids") List<String> ids);

    List<CoopLevelMemberDO> getMemberByLevelId(@Param("levelId") Long levelIdNum);

    List<String> getBindMember(@Param("ids") List<String> coopUserIds);

    Page<CoopUser> searchMembers(Page<?> page, @Param("name") String name, @Param("levelId") String levelId,
                                 @Param("departments") Collection<String> dept, @Param("startTime") String startTime,
                                 @Param("endTime") String endTime);

    List<CoopUser> getMemberWithoutPermission(@Param("ids") List<String> ids, @Param("departments") Collection<String> dept);
}
