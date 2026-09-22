package com.tdtech.cloudcmd.icp.proxy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.CameraVO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.DepartmentQO;
import com.tdtech.cloudcmd.icp.proxy.controller.vo.UserVO;
import com.tdtech.cloudcmd.icp.proxy.entity.*;
import com.tdtech.cloudcmd.icp.proxy.service.*;
import com.tdtech.cloudcmd.mysql.entity.CcmdPage;
import com.tdtech.cloudcmd.mysql.entity.CcmdPageParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/proxy/icp/v1")
@RequiredArgsConstructor
@Tag(name = "h5查询接口", description = "h5查询icp信息接口")
public class IcpProxyController {

    private final DepartmentService departmentService;
    private final UserService userService;
    private final CameraLevelService cameraLevelService;
    private final CameraService cameraService;
    private final IsdnTypeService isdnTypeService;

    @GetMapping("/depts/page")
    @Operation(summary = "分页查询部门", description = "根据条件分页查询部门信息")
    public R<CcmdPage<Department>> selectPage(DepartmentQO qo) {
        var page = departmentService.selectPage(qo, Wrappers.lambdaQuery(Department.class)
                .and(qo.getKeywords() != null && !qo.getKeywords().isBlank(), wrapper -> wrapper
                        .like(Department::getDepartmentname, qo.getKeywords())
                        .or()
                        .like(Department::getDepartmentid, qo.getKeywords())));
        return R.success(page);
    }

    @GetMapping("/depts/{deptId}/children")
    @Operation(summary = "查询所有下级组织列表", description = "根据组织ID获取所有下级组织列表（包括子组织、孙子组织等）")
    public R<List<Department>> selectDescendants(@PathVariable("deptId") Long deptId) {
        Department currentDept = departmentService.selectOne(Wrappers.lambdaQuery(Department.class)
                .eq(Department::getDepartmentid, deptId));
        
        if (currentDept == null) {
            return R.success(new ArrayList<>());
        }

        // 排除当前组织
        var list = departmentService.selectAll(Wrappers.lambdaQuery(Department.class)
                .likeRight(Department::getDepartmentIdPath, currentDept.getDepartmentIdPath() + ":"));

        return R.success(list);
    }

    @GetMapping("/depts/{deptId}")
    @Operation(summary = "查询组织详情", description = "根据组织ID获取组织详情")
    public R<Department> selectOne(@PathVariable Long deptId) {
        Department department = departmentService.selectOne(Wrappers.lambdaQuery(Department.class)
                .eq(Department::getId, deptId));
        return R.success(department);
    }

    @GetMapping("/depts/{deptId}/isdns/")
    @Operation(summary = "分页查询组织部门下的用户列表", description = "根据组织ID获取组织下所有用户")
    public R<CcmdPage<UserVO>> selectUserByDeptId(@PathVariable Long deptId,
                                                @RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
                                                @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                                @RequestParam(value = "keywords", required = false) String keywords) {
        Department currentDept = departmentService.selectOne(Wrappers.lambdaQuery(Department.class)
                .eq(Department::getId, deptId));
        
        if (currentDept == null) {
            return R.success(CcmdPage.empty(pageNo, pageSize));
        }

        List<Department> allDepartments = departmentService.selectAll(Wrappers.lambdaQuery(Department.class)
                .likeRight(Department::getDepartmentIdPath, currentDept.getDepartmentIdPath()));

        List<String> departmentIds = allDepartments.stream()
                .map(Department::getDepartmentid)
                .collect(Collectors.toList());

        CcmdPageParam pageParam = new CcmdPageParam(pageNo, pageSize);
        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .in(User::getDepartmentid, departmentIds);

        if (keywords != null && !keywords.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getName, keywords)
                    .or()
                    .like(User::getIsdn, keywords)
                    .or()
                    .like(User::getAlias, keywords));
        }

        queryWrapper.orderByDesc(User::getIsdn);
        
        CcmdPage<User> pageResult = userService.selectPage(pageParam, queryWrapper);
        
        // 获取 IsdnType 映射
        Map<String, IsdnType> isdnTypeMap = isdnTypeService.getIsdnTypeMap();
        
        // 转换为 UserVO 并填充 icon 和 iconUri
        List<UserVO> voList = pageResult.getRecords().stream().map(user -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            String key = isdnTypeService.buildIsdnTypeKey(user.getCategory(), user.getSubusercategory(), user.getApptype());
            IsdnType type = isdnTypeMap.get(key);
            if (type != null) {
                vo.setIcon(type.getIcon());
                vo.setIconUri(type.getIconUri());
                vo.setIsShow(type.getIsShow());
                vo.setIsdnTypeId(type.getId());
            }
            return vo;
        }).collect(Collectors.toList());
        
        CcmdPage<UserVO> result = new CcmdPage<>();
        result.setRecords(voList);
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getPageNum());
        result.setPageSize(pageResult.getPageSize());
        
        return R.success(result);
    }

    @GetMapping("/isdns/{isdn}")
    @Operation(summary = "根据isdn查询用户详情", description = "根据isdn查询用户详情")
    public R<User> selectUserByIsdn(@PathVariable String isdn) {
        User user = userService.selectOne(Wrappers.lambdaQuery(User.class)
                .eq(User::getIsdn, isdn));
        return R.success(user);
    }


    @GetMapping("/levels/page")
    @Operation(summary = "分页查询层级", description = "根据条件分页查询层级列表")
    public R<CcmdPage<CameraLevel>> selectAllLevels(@RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
                                                  @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                                  @RequestParam(value = "keywords", required = false) String keywords) {
        CcmdPageParam pageParam = new CcmdPageParam(pageNo, pageSize);
        LambdaQueryWrapper<CameraLevel> queryWrapper = Wrappers.lambdaQuery(CameraLevel.class);
        if (keywords != null && !keywords.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(CameraLevel::getNodeName, keywords)
                    .or()
                    .like(CameraLevel::getLevelNumber, keywords));
        }
        queryWrapper.orderByDesc(CameraLevel::getLevelNumber);
        CcmdPage<CameraLevel> pageResult = cameraLevelService.selectPage(pageParam, queryWrapper);
        return R.success(pageResult);
    }

    @GetMapping("/levels/{levelId}/children")
    @Operation(summary = "查询层级下的所有子层级", description = "根据层级ID获取所有子层级")
    public R<List<CameraLevel>> selectChildrenLevels(@PathVariable Long levelId) {
        CameraLevel cameraLevel = cameraLevelService.selectone(Wrappers.lambdaQuery(CameraLevel.class)
                .eq(CameraLevel::getLevelNumber, levelId));
        if (cameraLevel == null) {
            return R.success(new ArrayList<>());
        }

        // 排除当前组织
        var list = cameraLevelService.selectAll(Wrappers.lambdaQuery(CameraLevel.class)
                .likeRight(CameraLevel::getLevelNumberPath, cameraLevel.getLevelNumberPath() + ":"));
        return R.success(list);
    }

    @GetMapping("/levels/{levelId}")
    @Operation(summary = "查询层级详情", description = "根据层级ID获取层级详情")
    public R<CameraLevel> selectOneLevel(@PathVariable Long levelId) {
        CameraLevel cameraLevel = cameraLevelService.selectone(Wrappers.lambdaQuery(CameraLevel.class)
                .eq(CameraLevel::getLevelNumber, levelId));
        return R.success(cameraLevel);
    }

    @GetMapping("levels/{levelId}/isdns/")
    @Operation(summary = "分页查询层级下的摄像头列表", description = "根据层级ID获取层级下所有摄像头")
    public R<CcmdPage<CameraVO>> selectCameraByLevelId(@PathVariable Long levelId,
                                                       @RequestParam(value = "pageNo", defaultValue = "1") Long pageNo,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                                       @RequestParam(value = "keywords", required = false) String keywords) {
        CameraLevel currentLevel = cameraLevelService.selectone(Wrappers.lambdaQuery(CameraLevel.class)
                .eq(CameraLevel::getLevelNumber, levelId));

        if (currentLevel == null) {
            return R.success(CcmdPage.empty(pageNo, pageSize));
        }

        List<CameraLevel> allLevels = cameraLevelService.selectAll(Wrappers.lambdaQuery(CameraLevel.class)
                .likeRight(CameraLevel::getLevelNumberPath, currentLevel.getLevelNumberPath()));

        List<String> levelNumbers = allLevels.stream()
                .map(CameraLevel::getLevelNumber)
                .collect(Collectors.toList());

        CcmdPageParam pageParam = new CcmdPageParam(pageNo, pageSize);
        LambdaQueryWrapper<Camera> queryWrapper = Wrappers.lambdaQuery(Camera.class)
                .in(Camera::getLevelNumber, levelNumbers);

        if (keywords != null && !keywords.trim().isEmpty()) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Camera::getName, keywords)
                    .or()
                    .like(Camera::getIsdn, keywords));
        }

        queryWrapper.orderByDesc(Camera::getIsdn);

        CcmdPage<Camera> pageResult = cameraService.selectPage(pageParam, queryWrapper);

        // 获取 IsdnType 映射
        Map<String, IsdnType> isdnTypeMap = isdnTypeService.getIsdnTypeMap();

        // 转换为 UserVO 并填充 icon 和 iconUri
        List<CameraVO> voList = pageResult.getRecords().stream().map(camera -> {
            CameraVO vo = new CameraVO();
            BeanUtils.copyProperties(camera, vo);
            String key = isdnTypeService.buildIsdnTypeKey(camera.getCategory(), camera.getSubusercategory(), camera.getApptype());
            IsdnType type = isdnTypeMap.get(key);
            if (type != null) {
                vo.setIcon(type.getIcon());
                vo.setIconUri(type.getIconUri());
                vo.setIsShow(type.getIsShow());
                vo.setIsdnTypeId(type.getId());
            }
            return vo;
        }).collect(Collectors.toList());

        CcmdPage<CameraVO> result = new CcmdPage<>();
        result.setRecords(voList);
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getPageNum());
        result.setPageSize(pageResult.getPageSize());
        return R.success(result);
    }

}