package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Label;
import com.tdtech.cloudcmd.im.jingxin.server.entity.Tag;
import com.tdtech.cloudcmd.im.jingxin.server.service.LabelService;
import com.tdtech.cloudcmd.im.jingxin.server.service.TagService;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author ChinasoftPortal
 * @date 2025/9/10
 * @Describe：
 */
@Slf4j
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "协同群组标签")
@RequestMapping("/collaboration/v1/tags") // 接口统一前缀
@RequiredArgsConstructor
public class TagController {

    @Resource
    private TagService tagService;

    @Resource
    private LabelService labelService;

    @Resource
    private ReportUtil reportUtil;

    /**
     * Get paginated tag list
     *
     * @param pageNum  page number
     * @param pageSize page size
     * @param name     search keywords
     * @return paginated result
     */
    @GetMapping("/page")
    public R<IPage<Label>> getPageList(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String name) {
        IPage<Label> labelVOs = labelService.getLabelVOs(pageNum, pageSize, name);
        return R.success(labelVOs);
    }

    /**
     * Get tag details by ID
     *
     * @param id tag ID
     * @return tag details
     * @throws Exception if tag does not exist
     */
    @GetMapping("/{id}")
    public R<Tag> getTagById(@PathVariable Long id) throws Exception {
        Tag tag = tagService.getTagById(id);
        return R.success(tag);
    }

    /**
     * Create new tag
     *
     * @param tag tag entity
     * @return operation result
     * @throws Exception if validation fails
     */
    @PostMapping
    @LogReport(type = OperationTypeEnum.GROUP_LABEL_INSERT)
    public R<Boolean> createTag(@RequestBody @LogReportParam Tag tag) throws Exception {

        try {
            return R.success(tagService.saveTag(tag));
        } catch (Exception e) {
            log.error("保存协同群组标签失败", e);
            return R.failure(e.getMessage());
        }
    }

    /**
     * Update existing tag
     *
     * @param id  tag ID
     * @param tag tag entity with updated data
     * @return operation result
     * @throws Exception if validation fails
     */
    @PutMapping("/{id}")
    @LogReport(type = OperationTypeEnum.GROUP_LABEL_UPDATE)
    public R<Boolean> updateTag(@PathVariable Long id, @RequestBody @LogReportParam Tag tag) throws Exception {
        try {
            tag.setId(id);
            boolean result = tagService.saveTag(tag);
            return R.success(result);
        } catch (Exception e) {
            log.error("更新协同群组标签失败", e);
            return R.failure(e.getMessage());
        }
    }

    /**
     * Delete tag by ID
     *
     * @param id tag ID
     * @return operation result
     * @throws Exception if tag is in use
     */
    @DeleteMapping("/{id}")
    public R<Boolean> deleteTag(@PathVariable Long id) throws Exception {
        try {
            Tag old = tagService.getById(id);
            if (Objects.isNull(old)) {
                reportUtil.saveOperationLog(OperationTypeEnum.GROUP_LABEL_DELETE, "删除群组标签：[id=" + id + "]不存在");
                return R.failure("标签不存在");
            }
            tagService.deleteTag(old);
            return R.success();
        } catch (Exception e) {
            log.error("删除协同群组标签失败", e);
            return R.failure(e.getMessage());
        }
    }

    /**
     * Delete tag by IDs
     *
     * @param ids tag IDs
     * @return operation result
     */
    @DeleteMapping("/delete/list")
    public R<Boolean> deleteTag(@Parameter(description = "标签ID列表", required = true) @RequestBody List<Long> ids) {
        var tags = tagService.selectBatchByIds(ids);
        if (CollectionUtils.isEmpty(tags)) {
            reportUtil.saveOperationLog(OperationTypeEnum.GROUP_LABEL_DELETE, "删除群组标签：[ids=" + ids + "]不存在");
            return R.failure("标签不存在");
        }
        try {
            return tagService.deleteTagList(tags);
        } catch (Exception e) {
            log.error("删除协同群组标签失败", e);
            return R.failure(e.getMessage());
        }
    }

    @GetMapping("/all")
    public R<List<Label>> getPageList(@RequestParam(required = false) String userId) {
        return R.success(tagService.getAllList());
    }
}