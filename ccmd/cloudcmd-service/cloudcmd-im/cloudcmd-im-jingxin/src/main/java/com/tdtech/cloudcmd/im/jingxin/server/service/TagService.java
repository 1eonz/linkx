package com.tdtech.cloudcmd.im.jingxin.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupTagVO;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Label;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Tag;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;

import java.util.List;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
public interface TagService extends IService<Tag> {

    /**
     * Get tag list by page
     * @param pageNum page number
     * @param pageSize page size
     * @return paginated tag list
     */
    IPage<Tag> getPageList(Integer pageNum, Integer pageSize, String name);

    /**
     * Save or update tag
     * @param tag tag entity
     * @return operation result
     * @throws Exception if validation fails
     */
    boolean saveTag(Tag tag) throws Exception;

    /**
     * Delete tag by ID
     *
     * @param id tag ID
     * @return operation result
     * @throws Exception if tag is in use or ID is invalid
     */
    int deleteTag(Long id) throws Exception;

    int deleteTag(Tag tag) throws Exception;

    /**
     * Get tag by ID
     * @param id tag ID
     * @return tag entity
     * @throws Exception if tag does not exist
     */
    Tag getTagById(Long id) throws Exception;

    List<Label> getAllList();

    R<Boolean> deleteBatchTag(List<Long> ids);

    R<Boolean> deleteTagList(@LogReportParam List<Tag> tags);

    List<Tag> selectBatchByIds(List<Long> ids);

    List<GroupTagVO> listTagsByGroups(List<Long> ids);
}