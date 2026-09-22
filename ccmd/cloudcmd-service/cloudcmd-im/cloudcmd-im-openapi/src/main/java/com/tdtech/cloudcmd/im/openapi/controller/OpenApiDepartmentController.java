package com.tdtech.cloudcmd.im.openapi.controller;

import com.tdtech.cloudcmd.auth.dto.ImUserDto;
import com.tdtech.cloudcmd.auth.service.IIMUserRPCService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.jingxin.api.DepartmentRpcApi;
import com.tdtech.cloudcmd.im.jingxin.api.entity.department.OrganizationVO;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImPage;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth;
import com.tdtech.cloudcmd.im.openapi.controller.constant.BusinessScopeEnum;
import com.tdtech.cloudcmd.msip.aop.Licensed;
import com.tdtech.cloudcmd.msip.aop.RequestLimit;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(name = "组织部门", description = "组织部门相关接口")
@RestController
@RequestMapping("/openapi/v1/im/departments")
@OpenApiOauth(BusinessScopeEnum.THIRD_PARTY_APPLICATION)
@RequestLimit(business = "组织部门")
@Licensed(module = LicenseEnum.NORTHBOUND_DATA)
@Validated
public class OpenApiDepartmentController {

    @DubboReference
    private DepartmentRpcApi departmentRpcApi;

    @DubboReference
    private IIMUserRPCService imUserRPCService;

    @Operation(summary = "获取警信组织部门信息", description = "分页查询警信组织部门信息")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "成功返回部门信息",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @GetMapping("/page")
    public R<ImPage<ImDepartment>> listDepartments(
        @Parameter(description = "组织部门ID。为空则查询当前局点的所有根部门")
        @RequestParam(name = "departmentId", required = false) String departmentId,
        @Parameter(description = "页码。默认：1")
        @RequestParam(name = "page", required = false, defaultValue = "1") @Min(1) Integer page,
        @Parameter(description = "分页每页的条目。默认：10")
        @RequestParam(name = "pageSize", required = false, defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
        @Parameter(description = "部门名称关键字")
        @RequestParam(name = "keywords", required = false) String keywords) {
        Long parentId = parseDepartmentId(departmentId);
        if (StringUtils.isNotBlank(departmentId) && parentId == null) {
            return R.failure("部门ID格式错误");
        }
        PageResult<OrganizationVO> dataPage = departmentRpcApi.page(parentId, trimToNull(keywords), page, pageSize);
        return R.success(toDepartmentPage(dataPage));
    }

    @Operation(summary = "获取局点的警信根组织部门", description = "获取当前局点的警信根组织部门")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @ApiResponse(responseCode = "200", description = "成功返回根组织部门",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @GetMapping("/root")
    public R<ImDepartment> rootDepartment() {
        OrganizationVO root = departmentRpcApi.findRoot();
        if (root == null) {
            return R.failure("未获取到根组织部门");
        }
        return R.success(toDepartment(root));
    }

    @Operation(summary = "获取指定警信组织部门的用户信息", description = "分页查询指定警信组织部门的用户信息")
    @Parameter(name = "Authorization", description = "用户认证Token", required = true, in = ParameterIn.HEADER)
    @Parameter(name = "departmentId", description = "组织部门ID", required = true, in = ParameterIn.PATH)
    @ApiResponse(responseCode = "200", description = "成功返回用户信息",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = R.class)))
    @GetMapping("/{departmentId}/users/page")
    public R<ImPage<ImUser>> listDepartmentUsers(
        @PathVariable("departmentId") String departmentId,
        @Parameter(description = "页码。默认：1")
        @RequestParam(name = "page", required = false, defaultValue = "1") @Min(1) Integer page,
        @Parameter(description = "分页每页的条目。默认：10")
        @RequestParam(name = "pageSize", required = false, defaultValue = "10") @Min(1) @Max(100) Integer pageSize,
        @Parameter(description = "关键字")
        @RequestParam(name = "keywords", required = false) String keywords) {
        Long deptId = parseDepartmentId(departmentId);
        if (deptId == null) {
            return R.failure("部门ID格式错误");
        }
        PageResult<ImUserDto> dataPage =
            imUserRPCService.pageByDepartmentIds(Collections.singletonList(deptId), trimToNull(keywords), page,
                pageSize);
        return R.success(toUserPage(dataPage));
    }

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }

    private Long parseDepartmentId(String departmentId) {
        String value = trimToNull(departmentId);
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ImPage<ImDepartment> toDepartmentPage(PageResult<OrganizationVO> dataPage) {
        ImPage<ImDepartment> result = new ImPage<>();
        result.setCurrent(toInt(dataPage.getCurrent()));
        result.setPageNo(toInt(dataPage.getCurrent()));
        result.setSize(toInt(dataPage.getSize()));
        result.setPageSize(toInt(dataPage.getSize()));
        result.setTotal(toInt(dataPage.getTotal()));
        result.setTotalCount(toInt(dataPage.getTotal()));
        result.setRecords(dataPage.getRecords().stream().map(this::toDepartment).collect(Collectors.toList()));
        return result;
    }

    private ImDepartment toDepartment(OrganizationVO organization) {
        ImDepartment department = new ImDepartment();
        department.setId(organization.getId());
        department.setCode(organization.getCode());
        department.setName(organization.getName());
        department.setShortName(organization.getShortName());
        department.setParentId(organization.getParentId());
        department.setParentCode(organization.getParentCode());
        department.setParentName(organization.getParentName());
        department.setSort(organization.getSort());
        department.setFullPath(organization.getFullPath());
        department.setFullPathCode(organization.getFullPathCode());
        department.setFullPathName(organization.getFullPathName());
        department.setGmtCreated(organization.getGmtCreated() == null ? null : organization.getGmtCreated().getTime());
        department.setGmtModified(
            organization.getGmtModified() == null ? null : organization.getGmtModified().getTime());
        return department;
    }

    private ImPage<ImUser> toUserPage(PageResult<ImUserDto> dataPage) {
        ImPage<ImUser> result = new ImPage<>();
        result.setCurrent(toInt(dataPage.getCurrent()));
        result.setPageNo(toInt(dataPage.getCurrent()));
        result.setSize(toInt(dataPage.getSize()));
        result.setPageSize(toInt(dataPage.getSize()));
        result.setTotal(toInt(dataPage.getTotal()));
        result.setTotalCount(toInt(dataPage.getTotal()));
        result.setRecords(dataPage.getRecords().stream().map(this::toUser).collect(Collectors.toList()));
        return result;
    }

    private ImUser toUser(ImUserDto userDto) {
        ImUser user = new ImUser();
        user.setId(userDto.getId());
        user.setCode(userDto.getCode());
        user.setName(userDto.getName());
        user.setAvatar(userDto.getAvatar());
        user.setGender(userDto.getGender());
        user.setMobile(userDto.getMobile());
        user.setEmail(userDto.getEmail());
        user.setIsdn(userDto.getIsdn());
        user.setIdCard(userDto.getIdCard());
        user.setDistrict(userDto.getDistrict());
        user.setDirectLeaderId(userDto.getDirectLeaderId());
        user.setDirectLeaderName(userDto.getDirectLeaderName());
        user.setUserDepartments(Collections.singletonList(toUserDepartment(userDto)));
        return user;
    }

    private ImUser.UserDepartment toUserDepartment(ImUserDto userDto) {
        ImUser.UserDepartment department = new ImUser.UserDepartment();
        department.setId(userDto.getDepartmentId());
        department.setDepartmentCode(userDto.getDepartmentCode());
        department.setDepartmentName(userDto.getDepartmentName());
        department.setIsPrimary(true);
        return department;
    }

    private Integer toInt(Long value) {
        return Objects.isNull(value) ? 0 : Math.toIntExact(value);
    }
}
