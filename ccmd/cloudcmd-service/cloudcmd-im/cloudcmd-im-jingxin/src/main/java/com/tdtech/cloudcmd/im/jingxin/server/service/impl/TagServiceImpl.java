package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupTagVO;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Label;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Tag;
import com.tdtech.cloudcmd.im.jingxin.server.service.LabelService;
import com.tdtech.cloudcmd.im.jingxin.server.service.TagService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.GroupTagMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.LabelMapper;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.TagMapper;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.util.json.JsonException;
import com.tdtech.cloudcmd.util.json.JsonObject;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Slf4j
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private TagMapper tagMapper;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private GroupTagMapper groupTagMapper;
    @Resource
    private StreamBridge streamBridge;
    private static final String GROUP_TAG_MANAGEMENT = "GROUP_TAG_MANAGEMENT_UPDATE";
    private static final String MSG_TOPIC = "cloudcmd-cagent";

    @Override
    public IPage<Tag> getPageList(Integer pageNum, Integer pageSize, String name) {
        // Default page number and page size
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }

        Page<Tag> page = new Page<>(pageNum, pageSize);
        return tagMapper.selectPageByKeywords(page, name);
    }

    @Override
    public boolean saveTag(Tag tag) throws Exception {
        var user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        Long userId = user.getUserId();
        // 1. Validate tag name is not empty
        if (!StringUtils.hasText(tag.getName())) {
            throw new RuntimeException("Tag name cannot be empty");
        }

        // 2. Validate tag name length
        if (tag.getName().length() > 8) {
            throw new RuntimeException("群组标签名称超过8个字符");
        }

        // 3. Validate tag name uniqueness
        Long excludeId = tag.getId(); // Exclude self when editing
        int count = tagMapper.countByName(tag.getName(), excludeId);
        if (count > 0) {
            throw new RuntimeException("群组标签名称重复");
        }
        tag.setGmtCreated(new Date());
        tag.setCreateUserId(userId);
        // 5. Save data
        boolean b = saveOrUpdate(tag);
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_TAG_MANAGEMENT).broadcast()
            .body(GROUP_TAG_MANAGEMENT, GROUP_TAG_MANAGEMENT, tag.getId()).build();
        streamBridge.send(MSG_TOPIC, cagentMqFrame);
        return b;
    }

    @Override
    public int deleteTag(Long id) throws Exception {
        if (id == null || id <= 0) {
            throw new RuntimeException("Invalid tag ID");
        }
        UpdateWrapper<Tag> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id).eq("deleted", 0).set("deleted", "1") //
            .set("gmt_created", new Date());
        int update = tagMapper.update(null, updateWrapper);
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_TAG_MANAGEMENT).broadcast()
            .body(GROUP_TAG_MANAGEMENT, GROUP_TAG_MANAGEMENT, id).build();
        streamBridge.send(MSG_TOPIC, cagentMqFrame);
        return update;
    }

    @Override
    @LogReport(type = OperationTypeEnum.GROUP_LABEL_DELETE)
    public int deleteTag(@LogReportParam Tag tag) throws Exception {
        return deleteTag(tag.getId());
    }

    @Override
    public Tag getTagById(Long id) throws Exception {
        if (id == null || id <= 0) {
            throw new RuntimeException("Invalid tag ID");
        }

        Tag tag = tagMapper.selectById(id);
        if (tag == null || tag.getDeleted() == 1) {
            throw new RuntimeException("Tag does not exist or has been deleted");
        }
        return tag;
    }

    @Override
    public List<Label> getAllList() {
        List<Label> labels = labelMapper.selectAllList(null);
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        List<String> orgIds = user.getImOrgPrivCodes();
        Long userId = user.getUserId();
        List<Long> tagIds = groupTagMapper.selectListByOrgIds(orgIds, userId);
        log.info("getAllList tagIds:{}", tagIds);
        List<Label> labelList = new ArrayList<>();
        for (Label label : labels) {
            if (label.getIsDeleted() != null && label.getIsDeleted().equals(0)) {
                labelList.add(label);
            } else {
                if (CollectionUtils.isNotEmpty(tagIds) && tagIds.contains(label.getId())) {
                    labelList.add(label);
                }
            }
        }
        return labelList;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public R<Boolean> deleteBatchTag(List<Long> ids) {
        var tags = tagMapper.selectBatchByIds(ids);
        if (CollectionUtils.isEmpty(tags)) {
            return R.success();
        }
        try {
            return deleteTagList(tags);
        } catch (Exception e) {
            log.error("删除协同群组标签失败", e);
            throw new BusinessException(e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    @LogReport(type = OperationTypeEnum.GROUP_LABEL_DELETE)
    public R<Boolean> deleteTagList(@LogReportParam List<Tag> tagList) {
        if (CollectionUtils.isEmpty(tagList)) {
            return R.success();
        }
        tagMapper.deleteBatchByIds(tagList);
        for (Tag tag : tagList) {
            var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage(GROUP_TAG_MANAGEMENT).broadcast()
                    .body(GROUP_TAG_MANAGEMENT, GROUP_TAG_MANAGEMENT, tag.getId()).build();
            streamBridge.send(MSG_TOPIC, cagentMqFrame);
        }
        return R.success();
    }

    @Override
    public List<Tag> selectBatchByIds(List<Long> ids) {
        return tagMapper.selectBatchByIds(ids);
    }

    private static List<String> collectAllOrgCodes(JsonArray orgJsonArray) {
        List<String> codeList = new ArrayList<>();
        // 边界处理：空数组直接返回
        if (orgJsonArray == null || orgJsonArray.isEmpty()) {
            return codeList;
        }

        // 1. 遍历当前JsonArray中的每个组织对象
        for (int i = 0; i < orgJsonArray.size(); i++) {
            // 获取当前索引的组织JsonObject（依赖自定义JsonArray的getJSONObject方法）
            JsonObject orgObj = orgJsonArray.getJSONObject(i);
            if (orgObj == null) {
                continue; // 跳过空对象，避免空指针
            }

            // 2. 提取当前组织对象的code（依赖自定义JsonObject的getString方法）
            String currentCode = orgObj.getString("code");
            if (currentCode != null && !currentCode.trim().isEmpty()) {
                codeList.add(currentCode);
            }

            // 3. 递归处理当前组织的children数组（若存在）
            JsonArray childrenArray = null;
            try {
                // 尝试获取children字段（可能不存在，需捕获异常）
                childrenArray = orgObj.getJSONArray("children");
            } catch (JsonException e) {
                // 若children不是JsonArray（或不存在），跳过递归
                continue;
            }
            // 递归收集子组织的code，并合并到主列表
            List<String> childCodes = collectAllOrgCodes(childrenArray);
            codeList.addAll(childCodes);
        }

        return codeList;
    }

    @Override
    public List<GroupTagVO> listTagsByGroups(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return groupTagMapper.listTagsByGroupIds(ids);
    }
}
