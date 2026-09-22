package com.tdtech.cloudcmd.im.jingxin.server.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.im.jingxin.server.entity.DutyType;
import com.tdtech.cloudcmd.im.jingxin.server.service.DutyTypeService;
import com.tdtech.cloudcmd.im.jingxin.server.service.mapper.DutyTypeMapper;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DutyTypeServiceImpl extends ServiceImpl<DutyTypeMapper, DutyType> implements DutyTypeService {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_NAME_LENGTH = 255;

    private static final List<Long> DEFAULT_TYPES = Arrays.asList(0L);

    @Resource
    private DutyTypeMapper dutyTypeMapper;

    @Override
    public IPage<DutyType> getPageList(Integer pageNum, Integer pageSize, String name) {
        if (pageNum == null || pageNum <= 0) {
            pageNum = DEFAULT_PAGE_NUM;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        return dutyTypeMapper.selectPageByKeywords(new Page<>(pageNum, pageSize), name);
    }

    @Override
    public DutyType getDutyTypeByType(Long type) {
        if (type == null || type < 0) {
            throw new BusinessException("排班类型标识不能为空");
        }
        DutyType dutyType = getById(type);
        if (Objects.isNull(dutyType)) {
            throw new BusinessException("排班类型不存在");
        }
        return dutyType;
    }

    @Override
    public boolean saveDutyType(DutyType dutyType) {
        validateDutyType(dutyType);
        DutyType old = getById(dutyType.getType());
        if (dutyTypeMapper.countByName(dutyType.getName(), dutyType.getType()) > 0) {
            throw new BusinessException("排班类型名称已存在");
        }

        if (Objects.isNull(old)) {
            checkDefaultTypeCreatable(dutyType.getType());
            dutyType.setCreateUserId(getCurrentUserId());
            dutyType.setGmtCreated(new Date());
        } else {
            checkDefaultTypeEditable(dutyType.getType());
            dutyType.setCreateUserId(old.getCreateUserId());
            dutyType.setGmtCreated(old.getGmtCreated());
        }
        return saveOrUpdate(dutyType);
    }

    @Override
    public boolean deleteDutyType(Long type) {
        DutyType old = getDutyTypeByType(type);
        checkDefaultTypeEditable(old.getType());
        if (dutyTypeMapper.countCurrentOrFutureScheduleByType(old.getType()) > 0) {
            throw new BusinessException("排班类型已被当前及未来排班信息引用，不能删除");
        }
        return removeById(type);
    }

    @Override
    public List<DutyType> getAllList() {
        return dutyTypeMapper.selectAllList();
    }

    @Override
    public Map<Long, DutyType> getTypeMap() {
        return getAllList().stream()
            .collect(Collectors.toMap(DutyType::getType, Function.identity(), (oldValue, newValue) -> oldValue));
    }

    private void validateDutyType(DutyType dutyType) {
        if (Objects.isNull(dutyType)) {
            throw new BusinessException("排班类型不能为空");
        }
        if (StringUtils.isBlank(dutyType.getName())) {
            throw new BusinessException("排班类型名称不能为空");
        }
        if (dutyType.getName().length() > MAX_NAME_LENGTH) {
            throw new BusinessException("排班类型名称不能超过255个字符");
        }
    }

    private void checkDefaultTypeEditable(Long type) {
/*        if (DEFAULT_TYPES.contains(type)) {
            throw new BusinessException("默认排班类型不允许修改或删除");
        }*/

        // 根据se的要求就先不限制了 但是不知道后面会不会调整 先放着
    }

    private void checkDefaultTypeCreatable(Long type) {
/*        if (DEFAULT_TYPES.contains(type)) {
            throw new BusinessException("默认排班类型由系统初始化，不允许手工创建");
        }*/
        // 根据se的要求就先不限制了 但是不知道后面会不会调整 先放着
    }

    private Long getCurrentUserId() {
        UserInfo user = SecurityUtils.getUser();
        if (Objects.isNull(user)) {
            throw new SecurityUtils.UnAuthException("access token invalid");
        }
        return user.getUserId();
    }
}
