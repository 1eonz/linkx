package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.CreateGroupCO;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.GroupCreateSourceEnum;
import com.tdtech.cloudcmd.im.jingxin.api.entity.group.LabelVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.client.entity.UserIDNameInfo;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.LabelService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lsc
 * @date 2025/7/15
 **/
@Slf4j
@Tag(name = "标签", description = "标签管理相关接口")
@RestController
@RequestMapping("/collaboration/v1/label")
public class LabelController {

    @Resource
    private LabelService labelService;
    @Resource
    private IdWorker idWorker;
    @Resource
    private ReportUtil reportUtil;

    @Resource
    private CollaborationPostService postService;
    @Resource
    private ImService imService;
    @Resource
    private IOrganizationService organizationService;

    /**
     * 查询所有标签
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有标签", description = "根据名称模糊查询所有标签")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LabelVO.class)))
    public R<List<LabelVO>> listAllLabels(
            @Parameter(description = "标签名称（可选）") @RequestParam(required = false) String name,
            @Parameter(description = "标签作用域") @RequestParam(required = false) Integer scope) {
        List<LabelVO> labelVOs = labelService.getLabelVOs(name, scope, 0);
        return R.success(labelVOs);
    }

    /**
     * 根据层级查询标签
     */
    @GetMapping("/listByLevel")
    @Operation(summary = "根据层级查询标签", description = "根据标签层级查询标签列表")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Label.class)))
    public R<List<Label>> listLabelsByLevel(
            @Parameter(description = "标签层级", required = true) @RequestParam Integer level) {
        List<Label> labels = labelService.getLabelsByLevel(level);
        return R.success(labels);
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "获取标签详情", description = "根据标签ID获取标签详细信息")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LabelDetailVO.class)))
    public R<LabelDetailVO> detail(@Parameter(description = "标签ID", required = true) @PathVariable("id") Long id,
                                   @RequestParam(name = "idCard", required = false) String idCard
    ) {
        var byId = labelService.getById(id);
        if (byId == null) {
            return R.success();
        }
        var labelDetailVO = BeanCopyUtils.copyBean(byId, LabelDetailVO::new);
        var labelBindings = labelService.bindingsByLabel(id);
        var labelBindingUsers = labelService.bindingsUserByLabel(id);
        if (CollectionUtils.isNotEmpty(labelBindings)) {
            var postIds = labelBindings.stream().map(LabelBinding::getPostIds).map(ids -> {
                        if (ids == null || ids.isEmpty()) {
                            return Collections.<Long>emptyList();
                        } else {
                            return ids;
                        }
                    })
                    .flatMap(Collection::stream).collect(Collectors.toList());
            List<CollaborationPost> posts;
            if (!postIds.isEmpty()) {
                posts = postService.getByIds(postIds);
            } else {
                posts = Collections.emptyList();
            }
            List<Long> imDepartIds = getImDepartIds(idCard);
            Map<Long, LabelCO.Binding> lvos;
            if (CollectionUtils.isNotEmpty(imDepartIds)) {
                lvos = labelBindings.stream().filter(e -> imDepartIds.contains(e.getDepartmentId()))
                        .collect(
                                Collectors.toMap(LabelBinding::getDepartmentId, a -> LabelCO.createBindingView(a, posts, organizationService), (a, b) -> a));
            } else {
                lvos = labelBindings.stream().collect(
                        Collectors.toMap(LabelBinding::getDepartmentId, a -> LabelCO.createBindingView(a, posts, organizationService), (a, b) -> a));
            }
            labelDetailVO.setOrgCollaborations(lvos);
        }

        if(CollectionUtils.isNotEmpty(labelBindingUsers)){
            Map<Long, LabelCO.BindingUser> lvos = new HashMap<>();
            List<Long> allUserIds = labelBindingUsers.stream()
                    .filter(binding -> CollectionUtils.isNotEmpty(binding.getUserIds()))
                    .flatMap(binding -> binding.getUserIds().stream())
                    .distinct()
                    .collect(Collectors.toList());
            List<UserIDNameInfo> userIDNameInfos = labelService.queryUserDetailsByUserIds(allUserIds);
            Map<Long, String> idToNameMap = userIDNameInfos.stream().collect(Collectors.toMap(UserIDNameInfo::getId, UserIDNameInfo::getName));

            labelBindingUsers.forEach(e -> {
                List<Long> userIds = e.getUserIds();
                if (CollectionUtils.isNotEmpty(userIds)) {
                    Long departmentId = e.getDepartmentId();
                    String departmentName = e.getDepartmentName();
                    LabelCO.BindingUser bindingUser = new LabelCO.BindingUser();
                    bindingUser.setOrgName(departmentName);
                    List<ImUser> imUsers = userIds.stream().map(userId -> {
                        ImUser imUser = new ImUser();
                        imUser.setId(userId);
                        imUser.setName(idToNameMap.get(userId));
                        return imUser;
                    }).collect(Collectors.toList());
                    bindingUser.setUserIds(imUsers);
                    lvos.put(departmentId, bindingUser);
                }
            });

            labelDetailVO.setOrgUsers(lvos);
        }
        return R.success(labelDetailVO);
    }

    @Nullable
    private List<Long> getImDepartIds(String idCard) {
        List<Long> imDepartIds;
        if (StringUtils.isNotEmpty(idCard)) {
            // 如果idCard不为空，表示是im用户登录，需要判断部门的层级
            ImUser imUser = imService.userPageByIdCard(idCard);
            List<ImUser.UserDepartment> userDepartments = imUser.getUserDepartments();
            if (CollectionUtils.isNotEmpty(userDepartments)) {
                Optional<ImUser.UserDepartment> first = userDepartments.stream().filter(ImUser.UserDepartment::getIsPrimary).findFirst();
                if (first.isPresent()) {
                    ImUser.UserDepartment userDepartment = first.get();
                    List<ImDepartment> imDepartments = imService.queryDepartmentForList(userDepartment.getDepartmentCode());
                    imDepartIds = imDepartments.stream().map(ImDepartment::getId).collect(Collectors.toList());
                } else {
                    imDepartIds = null;
                }
            } else {
                imDepartIds = null;
            }
        } else {
            imDepartIds = null;
        }
        return imDepartIds;
    }

    /**
     * 新增标签
     */
    @PostMapping("/save")
    @Operation(summary = "保存标签", description = "新增或更新标签信息")
    @ApiResponse(responseCode = "200", description = "成功")
    @ApiResponse(responseCode = "400", description = "参数错误或标签已存在")
    public R<Boolean> saveLabel(@Parameter(description = "标签信息", required = true) @RequestBody LabelCO label) {
        boolean isCreate = label.getId() == null;
        OperationTypeEnum operationType = isCreate
                ? OperationTypeEnum.COLLABORATION_LABEL_INSERT
                : OperationTypeEnum.COLLABORATION_LABEL_UPDATE;
        //check duplicated
        List<Label> labels = labelService.getLableByName(label.getName());
        if (CollectionUtils.isNotEmpty(labels)) {
            // 移除同名同id的标签，若移除成功且协同岗为空则设置isCancel
            boolean removed = labels.removeIf(l -> label.getId() != null && Objects.equals(l.getId(), label.getId()));
            if (removed && (label.getOrgCollaborations() == null || label.getOrgCollaborations().isEmpty())) {
                label.setIsCancel(1);
            }
            if (!labels.isEmpty()) {
                reportUtil.saveOperationLog(operationType, "标签：" + label.getName() + "，已存在");
                return R.failure("标签：" + label.getName() + "，已存在");
            }
        }
        //set id for create
        if (isCreate) {
            label.setId(idWorker.nextId());
            labelService.saveLabel(label);
        } else {
            labelService.updateLabel(label);
        }
        return R.success();
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除标签", description = "根据标签ID删除标签")
    @ApiResponse(responseCode = "200", description = "成功")
    @ApiResponse(responseCode = "400", description = "存在下级标签无法删除")
    public R<Boolean> deleteLabel(@Parameter(description = "标签ID", required = true) @PathVariable Long id) {
        Label old = labelService.getById(id);
        if (Objects.isNull(old)) {
            reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_LABEL_DELETE, "删除标签：[id=" + id + "]不存在");
            return R.failure("标签不存在");
        }
        List<Label> childrenLabels = labelService.getChildrenLabels(id, null);
        if (childrenLabels != null && !childrenLabels.isEmpty()) {
            reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_LABEL_DELETE,
                    "删除标签：" + old.getName() + "，存在下级标签");
            return R.failure("存在下级标签，请先删除下级标签！");
        }
        var labelBindings = labelService.bindingsByLabel(id);
        if (labelBindings != null && !labelBindings.isEmpty()) {
            reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_LABEL_DELETE,
                    "删除标签：" + old.getName() + "，标签已绑定协同岗");
            return R.failure("标签已绑定协同岗，请先删除绑定！");
        }
        List<LabelBindingUser> LabelBindingUsers = labelService.bindingsUserByLabel(id);
        if (CollectionUtils.isNotEmpty(LabelBindingUsers)) {
            reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_LABEL_DELETE,
                    "删除标签：" + old.getName() + "，标签已绑定人员");
            return R.failure("标签已绑定人员，请先删除绑定！");
        }
        labelService.delete(old);
        return R.success();
    }

    @DeleteMapping("/delete/list")
    @Operation(summary = "批量删除标签", description = "批量根据标签ID删除标签")
    @ApiResponse(responseCode = "200", description = "成功")
    @ApiResponse(responseCode = "400", description = "存在下级标签无法删除")
    public R<Boolean> deleteLabels(@Parameter(description = "标签ID列表", required = true) @RequestBody List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return R.success(true);
        }
        List<Label> labels = labelService.selectAllByIds(ids);
        return labelService.deleteBatchLabels(labels);
    }

    /**
     * 一键建群
     */
    @PostMapping("/createGroup")
    @Operation(summary = "一键建群", description = "根据标签创建群组")
    @ApiResponse(responseCode = "200", description = "成功")
    @LogReport(type = OperationTypeEnum.COLLABORATION_GROUP_INSERT)
    public R createGroup(
            @Parameter(description = "建群参数", required = true) @Validated @RequestBody @LogReportParam(field = "departmentName") CreateGroupCO createGroupVO) {
        createGroupVO.setSource(GroupCreateSourceEnum.ONE_KEY.getValue());
        return R.success(labelService.createGroup(createGroupVO));
    }

    /**
     * 更新群组经纬度
     */
    @PostMapping("/update/location")
    @Operation(summary = "更新群组位置", description = "更新群组的经纬度信息")
    @ApiResponse(responseCode = "200", description = "成功")
    public R<Boolean> updateGroupLocation(
            @Parameter(description = "群组位置信息", required = true) @RequestBody GroupLocationVO groupLocationVO) {
        labelService.updateGroupLocation(groupLocationVO);
        return R.success();
    }
    @PostMapping("/binding/user")
    @Operation(summary = "标签绑定人员", description = "标签绑定人员")
    @ApiResponse(responseCode = "200", description = "成功")
    public R<Boolean> bindingUser(
            @Parameter(description = "群组位置信息", required = true) @RequestBody LabelCO label) {
        labelService.bindingUser(label);
        return R.success(true);
    }

    @GetMapping("/binding/user")
    @Operation(summary = "根据标签查询关联人员", description = "标签绑定人员")
    @ApiResponse(responseCode = "200", description = "成功")
    public R<List<UserIDNameInfo>> getBindingUser(
            @Parameter(description = "标签id", required = true) @RequestParam Long id,
            @Parameter(description = "部门id", required = false) @RequestParam(required = false) Long departmentId) {
        return R.success(labelService.getBindingUser(id, departmentId));
    }

    @GetMapping("/all")
    @Operation(summary = "查询所有标签", description = "根据名称模糊查询所有标签")
    @ApiResponse(responseCode = "200", description = "成功",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = LabelVO.class)))
    public R<List<Label>> listAllLabels(
            @Parameter(description = "标签作用域") @RequestParam(required = false) Integer scope) {
        List<Label> labelVOs = labelService.getAllLabel(scope);
        return R.success(labelVOs);
    }
}