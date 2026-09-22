package com.tdtech.cloudcmd.im.jingxin.server.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.im.jingxin.client.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.entity.*;
import com.tdtech.cloudcmd.im.jingxin.server.service.CollaborationPostService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IOrganizationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.IUserCoopSharedService;
import com.tdtech.cloudcmd.im.jingxin.server.service.PoliceTicketTypeService;
import com.tdtech.cloudcmd.im.jingxin.server.service.impl.ImService;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.nodedispatch.NodeDispatchClient;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Tag(name = "协同岗", description = "协同岗管理相关接口")
@Slf4j
@RestController
@RequestMapping("/collaboration/v1/post")
@RequiredArgsConstructor
public class CollaborationPostController {

    private static final String FILE_PATH = "/home/linkx/im/";
    private static final String FILE_PATH_URL = "/collaboration/static/";
    private static final String DEFAULT_NAME = "default.png";
    @Resource
    private CollaborationPostService collaborationPostService;
    @Resource
    private PoliceTicketTypeService policeTicketTypeService;
    @Resource
    private IOrganizationService organizationService;
    @Resource
    private ImHttpClient imHttpClient;
    @Resource
    private ImService imService;
    @Resource
    private IdWorker idWorker;
    @Resource(name = "collaborationPostExecutorService")
    private TaskExecutor taskExecutor;
    @Resource
    private FileUtil fileUtil;
    @Resource
    private ReportUtil reportUtil;
    @Resource
    private NodeDispatchClient nodeDispatchClient;
    @Resource
    private IUserCoopSharedService userCoopSharedService;

    @GetMapping("/page")
    @Operation(summary = "分页查询协同岗",
            description = "接口路径：GET /collaboration/post/page。根据条件分页查询协同岗信息；可通过 selectionType、selectionId 返回当前上下文下协同岗是否已选择。")
    public Page<?> getPage(@Parameter(description = "页码，默认为1") @RequestParam(defaultValue = "1") int pageNum,
                           @Parameter(description = "每页大小，默认为10") @RequestParam(defaultValue = "10") int pageSize,
                           @Parameter(description = "岗位名称") @RequestParam(required = false) String postName,
                           @Parameter(description = "组织名称") @RequestParam(required = false) String orgName,
                           @Parameter(description = "组织ID") @RequestParam(required = false) Long orgId,
                           @Parameter(description = "关联用户名称") @RequestParam(required = false) String relatedUserNames,
                           @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
                           @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
                           @Parameter(description = "类型，0 普通 1 1：14E") @RequestParam(required = false) Integer type,
                           @Parameter(description = "选择上下文类型：coopLevel-协同岗层级；functionalDepartment-职能分类。传入后返回 selected 字段") @RequestParam(required = false) String selectionType,
                           @Parameter(description = "选择上下文ID，和 selectionType 配套使用，当前 coopLevel/functionalDepartment 类型传对应节点ID") @RequestParam(required = false) String selectionId) {
        return pages(pageNum, pageSize, postName, orgName, orgId, relatedUserNames, startTime, endTime, type, selectionType, selectionId);
    }

    @GetMapping("/pageList")
    @Operation(summary = "分页查询协同岗",
            description = "接口路径：GET /collaboration/post/pageList。根据条件分页查询协同岗信息；可通过 selectionType、selectionId 返回当前上下文下协同岗是否已选择。")
    public R<Page<?>> getPageList(@Parameter(description = "页码，默认为1") @RequestParam(defaultValue = "1") int pageNum,
                                  @Parameter(description = "每页大小，默认为10") @RequestParam(defaultValue = "10") int pageSize,
                                  @Parameter(description = "岗位名称") @RequestParam(required = false) String postName,
                                  @Parameter(description = "组织名称") @RequestParam(required = false) String orgName,
                                  @Parameter(description = "组织ID") @RequestParam(required = false) Long orgId,
                                  @Parameter(description = "关联用户名称") @RequestParam(required = false) String relatedUserNames,
                                  @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
                                  @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
                                  @Parameter(description = "类型，0 普通 1 1：14E") @RequestParam(required = false) Integer type,
                                  @Parameter(description = "选择上下文类型：coopLevel-协同岗层级；functionalDepartment-职能分类。传入后返回 selected 字段") @RequestParam(required = false) String selectionType,
                                  @Parameter(description = "选择上下文ID，和 selectionType 配套使用，当前 coopLevel/functionalDepartment 类型传对应节点ID") @RequestParam(required = false) String selectionId) {
        return R.success(pages(pageNum, pageSize, postName, orgName, orgId, relatedUserNames, startTime, endTime, type, selectionType, selectionId));
    }

    @GetMapping("/queryByName")
    @Operation(summary = "根据名称查询协同岗", description = "根据岗位名称查询协同岗信息")
    public R<List<CollaborationPost>> queryByName(
            @Parameter(description = "岗位名称", required = true) @RequestParam String name) {
        List<CollaborationPost> list = collaborationPostService.queryByName(name);
        return R.success(list);
    }

    @GetMapping("/queryByUserId")
    @Operation(summary = "根据用户id查询协同岗", description = "根据用户id查询协同岗信息")
    public R<List<CooperationUserVO>> queryByUserId(
            @Parameter(description = "userId") @RequestParam(value = "userId", required = false) String userId) {
        List<CooperationUserVO> list = collaborationPostService.queryByUserId(userId);
        return R.success(list);
    }

    @PostMapping("/query/batch")
    @Operation(summary = "根据idList批量查询协同岗", description = "根据idList批量查询协同岗信息")
    public R<List<CollaborationPost>> queryBatch(
            @Parameter(description = "协同岗id数组", required = true) @RequestBody List<Long> ids) {
        List<CollaborationPost> list = collaborationPostService.findBatchContainsDeleted(ids);
        return R.success(list);
    }

    @GetMapping("/export")
    @Operation(summary = "导出协同岗数据", description = "将协同岗数据导出为Excel文件")
    public void exportToExcel(@Parameter(description = "文件名", required = true) @RequestParam String fileName,
                              @Parameter(description = "岗位名称") @RequestParam(required = false) String name,
                              @Parameter(description = "组织名称") @RequestParam(required = false) String orgName,
                              @Parameter(description = "组织ID") @RequestParam(required = false) String orgId,
                              @Parameter(description = "关联用户名称") @RequestParam(required = false) String relatedUserNames,
                              @Parameter(description = "开始时间") @RequestParam(required = false) String startTime,
                              @Parameter(description = "结束时间") @RequestParam(required = false) String endTime,
                              HttpServletResponse response) throws IOException {
        collaborationPostService.exportToExcel(fileName, name, orgName, orgId, relatedUserNames, startTime, endTime,
                response);
    }

    @PostMapping("/save")
    @Operation(summary = "保存协同岗", description = "创建新的协同岗信息")
    @LogReport(type = OperationTypeEnum.COLLABORATION_POST_INSERT)
    public R<Boolean> save(@Valid @RequestBody @LogReportParam(field = "postName") CollaborationPostVO reqVO) {
        // 参数校验由 @Valid 自动完成
        CollaborationPost collaborationPost = BeanCopyUtils.copyBean(reqVO, CollaborationPost::new);
        if (collaborationPost.getUids().size() != collaborationPost.getUnames().size()) {
            throw new BusinessException("参数错误");
        }
        // 检查协同岗绑定人员
        collaborationPostService.checkPostParam(collaborationPost);

        UserInfo user = SecurityUtils.getUser();
        if (Objects.nonNull(user)) {
            collaborationPost.setOperatorId(user.getUserId());
            collaborationPost.setOperatorName(user.getUserName());
        } else {
            collaborationPost.setOperatorName("ADMIN");
        }
        collaborationPost.setOperationType(1);

        // 如果没有图标，上传默认图标
        if (StringUtils.isEmpty(collaborationPost.getFileId())) {
            File file = new File(FILE_PATH + DEFAULT_NAME);
            String image = imHttpClient.uploadFile(file, file.getName(), "image");
            collaborationPost.setFileId(image);
            collaborationPost.setIconUrl(FILE_PATH_URL + DEFAULT_NAME);
        }
        UserCreateRequestBody userCreateRequestBody = new UserCreateRequestBody();

        UserReq userReq = new UserReq();
        // 处理图标上传
        userReq.setAvatar(collaborationPost.getFileId());
        userReq.setName(collaborationPost.getPostName());
        userReq.setCategory(1);
        String[] split = collaborationPost.getRelatedUserIds().split(",");
        List<Long> longList = Arrays.stream(split).parallel() // 启用并行流
                .map(Long::parseLong).collect(Collectors.toList());
        userReq.setBindUserIds(longList);
        userReq.setDepartmentId(collaborationPost.getOrgId());
        UserPolicyReq userPolicyReq;
        if (reqVO.getType() != null && reqVO.getType() == 1) {
            userPolicyReq = new UserPolicyReq();
            userPolicyReq.setType(1);
            userPolicyReq.setWhiteUserIds(imHttpClient.getToken().getProxyUser().getId() + "");
        } else {
            userPolicyReq = new UserPolicyReq();
            var departmentId = imHttpClient.getToken().getDepartment().getDepartmentId();
            userPolicyReq.setType(departmentId == null ? 0 : 1);
            userPolicyReq.setWhiteDepartmentIds(departmentId == null ? null : (departmentId + ""));
        }
        userReq.setPolicy(userPolicyReq);
        userCreateRequestBody.setUserReq(userReq);
        try {
//            Long id = imHttpClient.insertCollborationUser(userCreateRequestBody);
//            collaborationPost.setId(id);
//            // 新增协同岗获取id
//            // 新增成功后，将id绑定到协同岗实体中
//            collaborationPostService.save(collaborationPost);
            collaborationPostService.savePost(userCreateRequestBody, collaborationPost);
            log.info("save post: {}", collaborationPost);
            var typeIds = reqVO.getTypeIds();
            if (typeIds != null && !typeIds.isEmpty()) {
                policeTicketTypeService.bindPost(collaborationPost.getId(), typeIds);
            }
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
        return R.success(true);
    }

    @PostMapping("/upload/icon")
    @Operation(summary = "上传图标", description = "上传协同岗图标文件")
    public R uploadIcon(@Parameter(description = "图标文件", required = true) @RequestParam("file") MultipartFile file)
            throws IOException {
        // 将 MultipartFile 转换为 File
        String suffix = fileSuffix(file);
        String name = idWorker.nextId() + suffix;
        Path filePath = Path.of(FILE_PATH, name);
        file.transferTo(filePath.toFile());
        // 构建目标文件路径

        try {
            // 调用服务层上传文件
            String image = imHttpClient.uploadFile(filePath.toFile(), file.getOriginalFilename(), "image");

            IconVO iconVO = new IconVO();
            iconVO.setFileId(image);
            iconVO.setIconUrl(FILE_PATH_URL + name);
            return R.success(iconVO);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.failure(e.getMessage());
        }
    }

    private String fileSuffix(MultipartFile multipartFile) {
        var originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            return "";
        }
    }

    @PostMapping("/update")
    @Operation(summary = "更新协同岗", description = "更新协同岗信息")
    @LogReport(type = OperationTypeEnum.COLLABORATION_POST_UPDATE)
    public R<Boolean> update(@Valid @RequestBody @LogReportParam(field = "postName") CollaborationPostVO reqVO) {
        CollaborationPost collaborationPost = BeanCopyUtils.copyBean(reqVO, CollaborationPost::new);
        if (collaborationPost.getUids().size() != collaborationPost.getUnames().size()) {
            throw new BusinessException("参数错误");
        }

        collaborationPost.setId(Long.parseLong(reqVO.getId()));
        // 检查协同岗绑定人员，因为接收参数之前被定义成string了，导致，丢失了id，这里放在赋值id之后校验
        collaborationPostService.checkPostParam(collaborationPost);

        UserInfo user = SecurityUtils.getUser();
        if (Objects.nonNull(user)) {
            collaborationPost.setOperatorId(user.getUserId());
            collaborationPost.setOperatorName(user.getUserName());
        } else {
            collaborationPost.setOperatorName("ADMIN");
        }
        collaborationPost.setOperationType(2);
        UserCreateRequestBody userCreateRequestBody = new UserCreateRequestBody();

        UserReq userReq = new UserReq();
        // 处理图标上传
        userReq.setAvatar(collaborationPost.getFileId());
        userReq.setName(collaborationPost.getPostName());
        userReq.setCategory(1);
        UserPolicyReq userPolicyReq;
        if (reqVO.getType() != null && reqVO.getType() == 1) {
            userPolicyReq = new UserPolicyReq();
            userPolicyReq.setType(1);
            userPolicyReq.setWhiteUserIds(imHttpClient.getToken().getProxyUser().getId() + "");
        } else {
            userPolicyReq = new UserPolicyReq();
            var departmentId = imHttpClient.getToken().getDepartment().getDepartmentId();
            userPolicyReq.setType(departmentId == null ? 0 : 1);
            userPolicyReq.setWhiteDepartmentIds(departmentId == null ? null : (departmentId + ""));
        }
        userReq.setDepartmentId(collaborationPost.getOrgId());
        userReq.setPolicy(userPolicyReq);
        String[] split = collaborationPost.getRelatedUserIds().split(",");
        List<Long> longList = Arrays.stream(split).parallel() // 启用并行流
                .map(Long::parseLong).collect(Collectors.toList());
        userReq.setBindUserIds(longList);
        userCreateRequestBody.setUserReq(userReq);
        try {
            imHttpClient.updateCollborationUser(userCreateRequestBody, reqVO.getId());
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
        var typeIds = reqVO.getTypeIds();
        collaborationPostService.update(collaborationPost, typeIds);
        if (typeIds != null && !typeIds.isEmpty()) {
            policeTicketTypeService.bindPost(Long.parseLong(reqVO.getId()), typeIds);
        } else {
            policeTicketTypeService.deleteBinding(Long.parseLong(reqVO.getId()));
        }
        return R.success();
    }

    @GetMapping("/detail")
    public R<CollaborationPost> detail(@RequestParam(name = "id") Long id) {
        CollaborationPost post = collaborationPostService.getById(id);
        return R.success(post);
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除协同岗", description = "根据ID删除协同岗信息")
    public R<Boolean> delete(@Parameter(description = "协同岗ID", required = true) @PathVariable Long id) {
        CollaborationPost collaborationPost  = collaborationPostService.getById(id);
        if (Objects.nonNull(collaborationPost)) {
            collaborationPostService.delete(collaborationPost);
            policeTicketTypeService.deleteBinding(id);
        } else {
            reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_POST_DELETE, "删除协同岗：[id=" + id + "]不存在");
            return R.failure("协同岗不存在");
        }
        try {
            imHttpClient.deleteCollborationUser(id);
        } catch (Exception e) {
            log.error("删除协同岗失败", e);
            String name = collaborationPost.getPostName();
            reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_POST_DELETE, "删除协同岗" + name);
            return R.failure(e.getMessage());
        }
        return R.success(true);
    }

    /**
     * 从im同步协同管理岗记录
     *
     * @return 成功同步入库条数
     */
    @GetMapping("/syncPostFromIm")
    @Operation(summary = "同步协同岗", description = "从IM系统同步协同岗记录")
    public R<Integer> syncPostFromIm() {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        taskExecutor.execute(() -> {
            collaborationPostService.syncPostFromIm(user);
        });
        return R.success();
    }

    @GetMapping("/getProcess")
    @Operation(summary = "同步协同岗进度", description = "确认同步协同岗进度是否完成")
    public R getProcess() {
        return R.success(collaborationPostService.getProcess());
    }

    /**
     * 获取im同步状态
     *
     * @return 成功同步入库条数
     */
    @GetMapping("/getImSyncStatus")
    @Operation(summary = "获取IM同步状态", description = "检查IM系统同步状态")
    public R<Boolean> getImSyncStatus() {
        return R.success(collaborationPostService.getImSyncStatus());
    }

    @DeleteMapping("/deleteBatch")
    @Operation(summary = "批量删除协同岗", description = "批量删除协同岗信息")
    public R<Boolean> deleteBatch(
            @Parameter(description = "协同岗ID列表", required = true) @RequestBody List<Long> ids) {
        List<CollaborationPost> dataList = collaborationPostService.getByIds(ids);
        if (CollectionUtils.isEmpty(dataList)) {
             reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_POST_DELETE, "批量删除协同岗：[ids=" + ids + "]不存在");
            return R.failure("协同岗不存在");
        }
        collaborationPostService.deleteBatchPost(dataList);

        try {
            for (Long id : ids) {
                imHttpClient.deleteCollborationUser(id);
            }
        } catch (Exception e) {
            log.error("批量删除协同岗失败", e);
            String names = CollectionUtils.isNotEmpty(dataList)
                    ? dataList.stream().map(CollaborationPost::getPostName).collect(java.util.stream.Collectors.joining("、")) : "";
            String logContent = names.isEmpty()
                    ? "批量删除协同岗[ids=" + ids + "]"
                    : "批量删除协同岗" + names;
            reportUtil.saveOperationLog(OperationTypeEnum.COLLABORATION_POST_DELETE, logContent);
            return R.failure(e.getMessage());
        }
        return R.success(true);
    }

    @GetMapping("/downloadIcon")
    @Operation(summary = "下载图标", description = "根据图标URL下载图标文件")
    public Map<String, String> downloadIcon(
            @Parameter(description = "图标URL", required = true) @RequestParam String iconUrl) {
        try {
            // 调用 ImHttpClient 的 downloadIcon 方法获取文件流
            var fileBytes = imHttpClient.downloadIcon(iconUrl);

            // 将字节数组转换为 Base64 编码字符串
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);

            // 返回 Base64 编码字符串
            return Map.of("image", "data:image/png;base64," + base64Image);
        } catch (Exception e) {
            log.error("Failed to download icon: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to download icon", e);
        }
    }

    @GetMapping("/queryUser")
    @Operation(summary = "查询用户", description = "根据条件查询用户信息")
    public R queryUser(@Parameter(description = "组织代码", required = true) @RequestParam("code") String code,
                       @Parameter(description = "是否包含子级") @RequestParam(name = "includeChildren",
                               required = false) Integer includeChildren,
                       @Parameter(description = "关键字") @RequestParam(name = "keywords", required = false) String keywords,
                       @Parameter(description = "用户名") @RequestParam(name = "name", required = false) String name) {
        log.info("code:{}", code);
        return R.success(collaborationPostService.queryUser(code, includeChildren, keywords, name));
    }

    @GetMapping("/queryUserByPage")
    @Operation(summary = "分页查询用户", description = "根据条件分页查询用户信息")
    public R queryUserByPage(
            @Parameter(description = "组织代码") @RequestParam(value = "code", required = false) String code,
            @Parameter(description = "协同岗类型") @RequestParam(value = "type", required = false) Integer type,
            @Parameter(description = "页码", required = true) @RequestParam("pageNum") Integer pageNum,
            @Parameter(description = "每页大小", required = true) @RequestParam("pageSize") Integer pageSize,
            @Parameter(description = "是否包含子级") @RequestParam(name = "includeChildren",
                    required = false) Integer includeChildren,
            @Parameter(description = "关键字") @RequestParam(name = "keywords", required = false) String keywords,
            @Parameter(description = "用户名") @RequestParam(name = "name", required = false) String name) {
        log.info("queryUserByPage code: {}, type: {}, pageNum: {}, pageSize: {}", code, type, pageNum, pageSize);
        return R.success(collaborationPostService.queryUserByPage(code, type, pageNum, pageSize, includeChildren, keywords, name));
    }

    @GetMapping("/queryDepartment")
    @Operation(summary = "查询部门", description = "根据条件查询部门信息，携带 peerId 时查询对端节点组织树")
    public R queryDepartment(@Parameter(description = "父级组织代码") @RequestParam(required = false,
                                     value = "parentCode") String parentCode,
                             @Parameter(description = "父级ID") @RequestParam(required = false, value = "parentId") String parentId,
                             @Parameter(description = "目标节点 peerId，传入则查询对端节点组织树") @RequestParam(required = false, value = "peerId") String peerId) {
        if (StringUtils.isNotBlank(peerId)) {
            // 跨节点查询：通过 linkx-node dispatch 透传到对端
            Map<String, String> params = new HashMap<>(2);
            if (parentCode != null) params.put("parentCode", parentCode);
            if (parentId != null) params.put("parentId", parentId);
            return nodeDispatchClient.dispatchAndParse(
                    peerId, "/collaboration/v1/post/queryDepartment", params, R.class);
        }
        return R.success(imService.queryDepartment(parentCode, parentId));
    }

    @GetMapping("/queryUserByGroupId")
    @Operation(summary = "根据群组ID查询用户", description = "根据群组ID查询用户信息")
    public R queryUserByGroupId(
            @Parameter(description = "群组ID") @RequestParam(required = false, value = "groupId") Long groupId) {
        return R.success(imService.queryUserByGroupId(groupId));
    }

    @GetMapping("/queryUserByIdCard")
    @Operation(summary = "根据身份证号查询用户", description = "根据身份证号码查询用户信息")
    public R queryUserByIdCard(
            @Parameter(description = "身份证号", required = true) @RequestParam("idCard") String idCard) {
        log.info("idCard:{}", idCard);
        return R.success(imService.userPageByIdCard(idCard));
    }

    @GetMapping("/officialAccounts/page")
    @Operation(summary = "分页查询公众号", description = "分页查询公众号信息")
    public R queryOfficialAccountsPage(
            @Parameter(description = "页码，默认为1") @RequestParam(defaultValue = "1", required = false) Integer pageNum,
            @Parameter(description = "每页大小，默认为10") @RequestParam(defaultValue = "10",
                    required = false) Integer pageSize,
            @Parameter(description = "分类") @RequestParam(required = false) Integer category,
            @Parameter(description = "名称") @RequestParam(required = false) String name) {
        return R.success(imHttpClient.queryOfficialAccountsPage(pageNum, pageSize, category, name));
    }

    @GetMapping("/articles/page")
    @Operation(summary = "分页查询文章", description = "分页查询公众号文章信息")
    public R queryArticlesPage(
            @Parameter(description = "页码，默认为1") @RequestParam(defaultValue = "1", required = false) Integer pageNum,
            @Parameter(description = "每页大小，默认为10") @RequestParam(defaultValue = "10",
                    required = false) Integer pageSize,
            @Parameter(description = "标题") @RequestParam(required = false) String title,
            @Parameter(description = "公众号ID", required = true) @RequestParam(required = true) Long officialAccountId,
            @Parameter(description = "是否发布") @RequestParam(required = false) Boolean isPublish,
            @Parameter(description = "开始时间") @RequestParam(required = false) Long beginTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) Long endTime,
            @Parameter(description = "是否删除") @RequestParam(required = false) Boolean isDel) {
        return R.success(
                imHttpClient.queryArticlesPage(pageNum, pageSize, title, officialAccountId, isPublish, beginTime, endTime,
                        isDel));
    }

    @PostMapping("/queryUser/batch")
    @Operation(summary = "批量查询用户信息", description = "批量查询用户信息")
    public R<UserGetVo> queryUserBatch(@RequestBody List<String> userIdList) {
        if (CollectionUtils.isEmpty(userIdList)) {
            return R.success();
        }
        List<ImUser> users = collaborationPostService.getUserList(userIdList);
        UserGetVo userGetVo;
        if (CollectionUtils.isEmpty(users)) {
            userGetVo = imHttpClient.userPage(null, String.join(",", userIdList));
        } else {
            userGetVo = new UserGetVo();
            userGetVo.setResults(users);
        }
        if (Objects.nonNull(userGetVo)) {
            List<ImUser> results = userGetVo.getResults();
            if (CollectionUtils.isNotEmpty(results)) {
                results.forEach(user -> {
                    String path = fileUtil.downloadSaveIcon(imHttpClient, user.getAvatar());
                    user.setAvatar(path);
                });
            }
        }
        return R.success(userGetVo);
    }

    private Page<?> pages(int pageNum, int pageSize, String postName, String orgName,
                          Long orgId, String relatedUserNames, String startTime, String endTime, Integer type,
                          String selectionType, String selectionId) {
        Page<? extends CollaborationPost> page =
                collaborationPostService.getPage(pageNum, pageSize, postName, orgName, orgId, relatedUserNames, startTime,
                        endTime, type);
        var records = page.getRecords();
        if (records == null || records.isEmpty()) {
            return page;
        }
        List<CollaborationPostPageVO> newRecords = BeanCopyUtils.copyList(records, CollaborationPostPageVO::new);
        List<Long> postIds = newRecords.stream().map(CollaborationPost::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Set<Long> selectedPostIds = collaborationPostService.selectedPostIds(postIds, selectionType, selectionId);
        // 批量查询已分享到对端节点的协同岗ID集合，用于填充 isShared 字段
        Set<Long> sharedCoopUserIds = userCoopSharedService.listSharedCoopUserIds(postIds);
        log.info("selectionType is {},selectionId is : {}, postIds is : {}, selectedPostIds is {}",
                selectionType, selectionId,
                postIds.stream().map(String::valueOf).collect(Collectors.joining(",")),
                selectedPostIds.stream().map(String::valueOf).collect(Collectors.joining(","))
        );
        for (var newRecord : newRecords) {
            var types = policeTicketTypeService.findAll(new PoliceTicketTypeQO().setPostId(List.of(newRecord.getId())));
            newRecord.setPoliceTicketTypes(types);
            newRecord.setOrgName(organizationService.findOneById(newRecord.getOrgId()).getName());
            newRecord.setSelected(selectedPostIds.contains(newRecord.getId()));
            newRecord.setIsShared(sharedCoopUserIds.contains(newRecord.getId()) ? 1 : 0);
        }
        Page<CollaborationPostPageVO> newPage = new Page<>();
        newPage.setCurrent(page.getCurrent());
        newPage.setSize(page.getSize());
        newPage.setTotal(page.getTotal());
        newPage.setPages(page.getPages());
        newPage.setRecords(newRecords);
        return newPage;
    }
}