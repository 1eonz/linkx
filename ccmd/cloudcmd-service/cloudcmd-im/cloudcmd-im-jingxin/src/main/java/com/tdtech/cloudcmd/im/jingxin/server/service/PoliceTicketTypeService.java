package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketType;
import com.tdtech.cloudcmd.im.jingxin.server.entity.PoliceTicketTypeQO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.TrPoliceTicketTypePost;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface PoliceTicketTypeService {
    int create(PoliceTicketType policeTicketType);

    PoliceTicketType findById(Long id);

    List<PoliceTicketType> findByIdList(List<Long> idList);

    int update(PoliceTicketType policeTicketType);

    int deleteById(Long id);

    List<PoliceTicketType> findAll(PoliceTicketTypeQO query);

    Page<PoliceTicketType> findPage(Page<PoliceTicketType> page, PoliceTicketType policeTicketType);

    void bindPost(@NotNull Long postId, @NotNull List<Long> typeIds);

    void deleteBinding(Long postId);

    List<Long> getPosts(String tag);

    List<String> getTag(Long postId);

    List<Long> getTypeIds(Long postId);
}
