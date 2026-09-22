package com.tdtech.cloudcmd.im.jingxin.server.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImDepartment;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DepartmentLocation;
import com.tdtech.cloudcmd.im.jingxin.server.service.DepartmentLocationService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.DepartmentLocationMapper;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.util.ListUtils;
import com.tdtech.cloudcmd.util.json.JsonArray;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.im.jingxin.server.service.impl.GroupExtendsServiceImpl.collectAllOrgCodes;

/**
 * <p>
 * 部门位置信息表 服务实现类
 * </p>
 *
 * @since 2025-08-13
 */
@Service
public class DepartmentLocationServiceImpl extends ServiceImpl<DepartmentLocationMapper, DepartmentLocation> implements DepartmentLocationService {

    @Resource
    private DepartmentLocationMapper departmentLocationMapper;
    // 经纬度验证正则表达式
    private static final String LOCATION_REGEX =
            "^(-?((180(\\.\\d{1,18})?)|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d{1,18})?))," +
                    "(-?((90(\\.\\d{1,18})?)|([1-8]?\\d)(\\.\\d{1,18})?))$";
    private static final Pattern LOCATION_PATTERN = Pattern.compile(LOCATION_REGEX);
    @Resource
    private ImService imService;

    @Resource
    private OrganizationDiversionService organizationDiversionService;

    @Override
    public IPage<DepartmentLocation> getPageList(Integer pageNum, Integer pageSize, String departmentName) {
        // 默认页码和每页条数
        if (pageNum == null || pageNum <= 0) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 10;
        }
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        List<String> orgIds = user.getImOrgPrivCodes();
        String organizationCode = user.getOrganizationCode();
        List<ImDepartment> imDepartments = organizationDiversionService.queryDepartmentForList(organizationCode);
        if(ListUtils.isBlankList(orgIds)){
            orgIds = imDepartments.stream().map(ImDepartment::getCode).collect(Collectors.toList());
        }
        Page<DepartmentLocation> page = new Page<>(pageNum, pageSize);
        return departmentLocationMapper.selectPageByDepartmentName(page, departmentName,orgIds);
    }

    @Override
    public boolean saveDepartmentLocation(DepartmentLocation departmentLocation) throws Exception {
        // 1. 验证部门编码唯一性
        Long excludeId = departmentLocation.getId(); // 编辑时排除自身
        int count = departmentLocationMapper.countByDepartmentId(departmentLocation.getDepartmentCode(), excludeId);
        if (count > 0) {
            throw new Exception("已存在部门位置");
        }

        // 2. 验证经纬度格式
        String location = departmentLocation.getLocation();
        if (location != null && !location.isEmpty()) {
            // 验证字符长度
            if (location.length() > 25) {
                throw new Exception("经纬度格式错误，长度不能超过25个字符");
            }

            // 验证格式
            Matcher matcher = LOCATION_PATTERN.matcher(location);
            if (!matcher.matches()) {
                throw new Exception("经纬度格式错误，正确格式：经度,纬度，经度(-180~180)，纬度(-90~90)，小数点后最多18位");
            }
        }
        // 3. 保存数据
        return saveOrUpdate(departmentLocation);
    }

    @Override
    @LogReport(type = OperationTypeEnum.LOCATION_INSERT)
    public boolean saveLocation(@LogReportParam(field = "departmentName") DepartmentLocation departmentLocation) throws Exception {
        return saveDepartmentLocation(departmentLocation);
    }

    @Override
    @LogReport(type = OperationTypeEnum.LOCATION_UPDATE)
    public boolean updateLocation(@LogReportParam(field = "departmentName") DepartmentLocation departmentLocation) throws Exception {
        return saveDepartmentLocation(departmentLocation);
    }

    @Override
    public boolean deleteDepartmentLocation(Long id) {
        return removeById(id);
    }

    @Override
    @LogReport(type = OperationTypeEnum.LOCATION_DELETE)
    public boolean deleteDepartmentLocation(@LogReportParam(field = "departmentName") DepartmentLocation departmentLocation) {
        return deleteDepartmentLocation(departmentLocation.getId());
    }

    @Override
    public DepartmentLocation findByDeptCode(String deptCode) {
        LambdaQueryWrapper<DepartmentLocation> lambdaWrapper = new LambdaQueryWrapper<>();
        lambdaWrapper.eq(DepartmentLocation::getDepartmentCode, deptCode);
        return departmentLocationMapper.selectOne(lambdaWrapper);
    }
}
