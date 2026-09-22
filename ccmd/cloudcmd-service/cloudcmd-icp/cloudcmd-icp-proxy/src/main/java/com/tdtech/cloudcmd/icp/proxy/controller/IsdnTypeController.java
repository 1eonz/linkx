package com.tdtech.cloudcmd.icp.proxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.entity.IsdnType;
import com.tdtech.cloudcmd.icp.proxy.service.CameraService;
import com.tdtech.cloudcmd.icp.proxy.service.IsdnTypeService;
import com.tdtech.cloudcmd.icp.proxy.service.UserService;
import com.tdtech.cloudcmd.icp.proxy.util.AttachmentUtil;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.IsdnTypeUpdateReq;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ISDN类型管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/proxy/icp/v1/isdnType")
@RequiredArgsConstructor
@Tag(name = "ISDN类型管理", description = "ISDN类型信息查询接口")
public class IsdnTypeController {

    private final IsdnTypeService isdnTypeService;
    private final UserService userService;
    private final CameraService cameraService;

    /**
     * 设备图标存储路径
     */
    private static final String DEVICE_ICON_PATH = "/data/linkx/data/device-icon/";
    
    /**
     * 设备图标静态资源访问路径
     */
    private static final String DEVICE_ICON_STATIC_PATH = "/proxy/icp/static/device-icon/";

    @GetMapping
    @Operation(summary = "分页查询ISDN类型", description = "根据条件分页查询ISDN类型信息")
    @Parameters({@Parameter(name = "pageNo", description = "页码", example = "1"),
            @Parameter(name = "pageSize", description = "每页大小", example = "10"),
            @Parameter(name = "category", description = "设备类型"),
            @Parameter(name = "subusercategory", description = "设备子类型"),
            @Parameter(name = "apptype", description = "设备子子类型")})
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<CcmdPage<IsdnType>> selectPage(
            @RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subusercategory", required = false) String subusercategory,
            @RequestParam(value = "apptype", required = false) String apptype,
            @RequestParam(value = "name", required = false) String name) {
        
        CcmdPageParam pageParam = new CcmdPageParam(pageNo, pageSize);
        LambdaQueryWrapper<IsdnType> queryWrapper = Wrappers.lambdaQuery(IsdnType.class)
                .eq(category != null && !category.isBlank(), IsdnType::getCategory, category)
                .eq(subusercategory != null && !subusercategory.isBlank(), IsdnType::getSubusercategory, subusercategory)
                .eq(apptype != null && !apptype.isBlank(), IsdnType::getApptype, apptype)
                .like(name != null && !name.isBlank(), IsdnType::getName, name)
                .orderByDesc(IsdnType::getId);
        
        CcmdPage<IsdnType> page = isdnTypeService.selectPage(pageParam, queryWrapper);
        return R.success(page);
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有ISDN类型", description = "查询所有ISDN类型信息")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<List<IsdnType>> selectAll() {
        List<IsdnType> list = isdnTypeService.selectAll(Wrappers.lambdaQuery(IsdnType.class).orderByDesc(IsdnType::getId));
        return R.success(list);
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询ISDN类型", description = "根据ID查询ISDN类型详情")
    @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(schema = @Schema(implementation = R.class)))
    public R<IsdnType> selectById(@PathVariable Long id) {
        IsdnType isdnType = isdnTypeService.selectById(id);
        return R.success(isdnType);
    }

    @PostMapping
    @Operation(summary = "新增ISDN类型", description = "新增ISDN类型信息")
    @ApiResponse(responseCode = "200", description = "新增成功")
    public R<Void> insert(@RequestBody IsdnType isdnType) {
        isdnTypeService.insert(isdnType);
        return R.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新ISDN类型", description = "更新ISDN类型信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public R<Void> update(@PathVariable Long id, @RequestBody IsdnTypeUpdateReq req) {
        IsdnType isdnType = BeanCopyUtils.copyBean(req, IsdnType::new);
        isdnType.setId(id);
        isdnTypeService.update(isdnType);
        return R.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除ISDN类型", description = "删除ISDN类型信息")
    @ApiResponse(responseCode = "200", description = "删除成功")
    public R<Void> delete(@PathVariable Long id) {
        isdnTypeService.deleteById(id);
        return R.success();
    }


    @PostMapping("/uploadIcon")
    @Operation(summary = "上传设备图标", description = "上传设备类型图标文件，返回文件路径信息")
    @ApiResponse(responseCode = "200", description = "上传成功")
    public R<AttachmentUtil.UploadResult> uploadIcon(
            @Parameter(description = "图标文件", required = true) 
            @RequestParam("file") MultipartFile file) {
        log.info("开始上传设备图标文件: {}", file.getOriginalFilename());
        
        // 使用AttachmentUtil上传文件
        AttachmentUtil.UploadResult result = AttachmentUtil.upload(file, DEVICE_ICON_PATH);
        
        // 设置静态资源访问URL
        String fileUrl = DEVICE_ICON_STATIC_PATH + result.getStorageFileName();
        result.setFileUrl(fileUrl);
        
        log.info("设备图标上传成功: fileId={}, filePath={}, fileUrl={}", 
                result.getFileId(), result.getFilePath(), result.getFileUrl());
        return R.success(result);
    }

    @PostMapping("/upsertType")
    @Operation(summary = "更新IsdnType表", description = "从tb_isdn和tb_camera表获取去重后的category,subusercategory,apptype并同步到tb_isdn_type表")
    @ApiResponse(responseCode = "200", description = "更新成功")
    public R<String> upsertType() {
        List<IsdnType> fromUsers = userService.selectDistinctIsdnType();
        List<IsdnType> fromCameras = cameraService.selectDistinctIsdnType();

        List<IsdnType> isdnTypes = isdnTypeService.mergeIsdnTypeByKey(fromUsers, fromCameras);
        if (isdnTypes.isEmpty()) {
            return R.success("无数据需要同步");
        }
        isdnTypeService.syncIsdnType(isdnTypes);
        return R.success("同步成功，共处理 " + isdnTypes.size() + " 条数据");
    }

    @PutMapping("/{id}/isShow")
    @Operation(summary = "修改ISDN类型展示状态", description = "修改ISDN类型的是否展示状态")
    @ApiResponse(responseCode = "200", description = "修改成功")
    public R<Void> updateIsShow(@PathVariable Long id, @RequestParam Integer isShow) {
        isdnTypeService.updateIsShow(id, isShow);
        return R.success();
    }

}