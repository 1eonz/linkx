package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.CreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.LabelVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserIDNameInfo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author lsc
 * @date 2025/7/15
 **/
public interface LabelService extends IService<Label> {
    void upsertBindingsBatch(Label label, List<LabelBinding> bindings);

    void delete(Long id);

    void delete(Label label);

    Label getById(Long id);

    List<LabelBinding> bindingsByLabel(Long id);

    List<LabelBindingUser> bindingsUserByLabel(Long id);

    void removePostIdFromBindings(Long postId);

    List<LabelBinding> findBindingByPostId(Long postId, Integer type, Long departmentId);

    List<Label> getLabelsByLevel(Integer level); // 根据层级获取标签

    List<Label> getChildrenLabels(Long parentId, String name ); // 获取子标签

    List<LabelVO> getLabelVOs(String name, Integer scope, int level);

    Long createGroup(CreateGroupCO createGroupVO);

    Long executeCreatGroup(CreateGroupCO createGroupVO, List<CollaborationPost> posts, LinkedList<Long> collaborationPostIds, LinkedList<String> collaborationPostNames, AtomicBoolean oneO1p4BFlag, int source);

    List<Label> getLabelsByCollaborationPostId(Long collaborationPostId);

    void updateGroupLocation(GroupLocationVO groupLocationVO);

    void saveLabel(LabelCO label);

    void updateLabel(LabelCO label);

    R<Boolean> deleteBatch(List<Long> ids);

    R<Boolean> deleteBatchLabels(@LogReportParam List<Label> labels);

    List<Label> selectAllByIds(List<Long> ids);

    void bindingUser(LabelCO label);

    List<UserIDNameInfo> getBindingUser(Long id,Long departmentId);

    IPage<Label> getLabelVOs(Integer pageNum, Integer pageSize, String name);

    List<Label> getLableByName(String name);

    List<Label> getAllLabel(Integer scope);

    List<UserIDNameInfo> queryUserDetailsByUserIds(List<Long> allUserIds);
}
