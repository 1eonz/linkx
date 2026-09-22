package com.tdtech.cloudcmd.admin.resource.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.tdtech.cloudcmd.util.IdWorker;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tdtech.cloudcmd.admin.resource.entity.SystemLayoutSection;
import com.tdtech.cloudcmd.admin.resource.mapper.SystemLayoutSectionMapper;
import com.tdtech.cloudcmd.admin.resource.service.ISystemLayoutSectionService;
import com.tdtech.cloudcmd.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 系统布局板块 Service 实现类
 *
 * @author: S063874
 * @date: 2026-03-10 14:08
 */
@Slf4j
@Service
public class SystemLayoutSectionServiceImpl implements ISystemLayoutSectionService {

    private static final List<Integer> TYPE_LIST = List.of(1, 2, 3, 6);

    @Resource
    private SystemLayoutSectionMapper systemLayoutSectionMapper;

    @Resource
    IdWorker idWorker;
    @Override
    public Boolean createSystemLayoutSection(SystemLayoutSection systemLayoutSection) {

        synchronized (this){
            if(TYPE_LIST.contains(systemLayoutSection.getType())){
                boolean b = systemLayoutSectionMapper.existsByType(systemLayoutSection.getType());
                if(b){
                    return false;
                }
            }
            systemLayoutSection.setId(idWorker.nextId());
            systemLayoutSection.setGmtCreateTime(new Date());
            systemLayoutSection.setGmtLastModified(new Date());
            systemLayoutSection.setDeleted(0);
            systemLayoutSectionMapper.insert(systemLayoutSection);
        }
        return true;
    }

    @Override
    public Boolean updateSystemLayoutSection(SystemLayoutSection systemLayoutSection) {
        // 更新
        synchronized (this){
            Integer type = systemLayoutSection.getType();
            if(type != null && TYPE_LIST.contains(systemLayoutSection.getType())){
                boolean b = systemLayoutSectionMapper.existsByTypeAndId(systemLayoutSection.getType(), systemLayoutSection.getId());
                if(b){
                    return false;
                }
            }
            systemLayoutSection.setGmtLastModified(new Date());
            systemLayoutSectionMapper.updateById(systemLayoutSection);
            return true;
        }
    }

    @Override
    public void deleteSystemLayoutSection(Long id) {
        // 校验存在
        validateSystemLayoutSectionExists(id);
        // 删除
        systemLayoutSectionMapper.deleteById(id);
    }

    @Override
    public void deleteSystemLayoutSectionByIds(List<Long> ids) {
        // 校验存在
        validateSystemLayoutSectionExists(ids);
        // 删除
        systemLayoutSectionMapper.deleteBatchIds(ids);
    }

    @Override
    public SystemLayoutSection getSystemLayoutSectionById(Long id) {
        return systemLayoutSectionMapper.selectById(id);
    }

    @Override
    public List<SystemLayoutSection> listSystemLayoutSection() {
        LambdaQueryWrapper<SystemLayoutSection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemLayoutSection::getDeleted, 0)
                .orderByAsc(SystemLayoutSection::getSort);
        return systemLayoutSectionMapper.selectList(queryWrapper);
    }

    @Override
    public List<SystemLayoutSection> listShowSystemLayoutSection() {
        LambdaQueryWrapper<SystemLayoutSection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemLayoutSection::getDeleted, 0)
                .eq(SystemLayoutSection::getShow, 1)
                .orderByAsc(SystemLayoutSection::getSort);
        return systemLayoutSectionMapper.selectList(queryWrapper);
    }

    @Override
    public List<SystemLayoutSection> listByType(Integer type) {
        LambdaQueryWrapper<SystemLayoutSection> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemLayoutSection::getType, type)
                .eq(SystemLayoutSection::getDeleted, 0)
                .orderByAsc(SystemLayoutSection::getSort);
        return systemLayoutSectionMapper.selectList(queryWrapper);
    }

    @Override
    public boolean existSectionsByType(Integer type, Long id) {
        if(id == null){
            return systemLayoutSectionMapper.existsByType(type);
        }else{
            return systemLayoutSectionMapper.existsByTypeAndId(type, id);
        }
    }

    /**
     * 校验系统布局板块是否存在
     *
     * @param id 编号
     */
    private void validateSystemLayoutSectionExists(Long id) {
        if (systemLayoutSectionMapper.selectById(id) == null) {
            throw new RuntimeException("系统布局板块不存在");
        }
    }

    /**
     * 校验系统布局板块是否存在
     *
     * @param ids 编号列表
     */
    private void validateSystemLayoutSectionExists(List<Long> ids) {
        List<SystemLayoutSection> list = systemLayoutSectionMapper.selectBatchIds(ids);
        if (CollectionUtils.isEmpty(list) || list.size() != ids.size()) {
            throw new RuntimeException("系统布局板块不存在");
        }
    }
}