package com.tdtech.cloudcmd.im.jingxin.server.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationClient;
import com.tdtech.cloudcmd.im.jingxin.server.entity.CollaborationClientVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author lsc
 * @date 2025/8/12
 **/
@Mapper
public interface CollaborationClientMapper extends BaseMapper<CollaborationClient> {

    Page<CollaborationClient> selectClientPage(Page<CollaborationClient> page,
                                               @Param("client") CollaborationClientVO collaborationClientVO);

    CollaborationClient selectClientByNameAndSecret(@Param("clientId") String clientId,
                                                    @Param("clientSecret") String clientSecret);
}
