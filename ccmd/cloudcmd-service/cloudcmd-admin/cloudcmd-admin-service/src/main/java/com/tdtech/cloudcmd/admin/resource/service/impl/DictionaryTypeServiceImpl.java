package com.tdtech.cloudcmd.admin.resource.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryItem;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryType;
import com.tdtech.cloudcmd.admin.resource.mapper.DictionaryItemMapper;
import com.tdtech.cloudcmd.admin.resource.mapper.DictionaryTypeMapper;
import com.tdtech.cloudcmd.admin.resource.service.IDictionaryTypeService;
import com.tdtech.cloudcmd.util.IdWorker;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 数据字典类型表 服务实现类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Service
@Slf4j
public class DictionaryTypeServiceImpl extends ServiceImpl<DictionaryTypeMapper, DictionaryType>
    implements IDictionaryTypeService {

    @Autowired
    private DictionaryTypeMapper dictionaryTypeMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private DictionaryItemMapper dictionaryItemMapper;

    @Override
    public List<DictionaryType> getDictionaryTypeList() {
        return dictionaryTypeMapper.selectList(null);
    }

    @Override
    public DictionaryType getDictionaryTypeByTypeCode(String typeCode) {
        QueryWrapper<DictionaryType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DictionaryType.CODE, typeCode);
        return dictionaryTypeMapper.selectOne(queryWrapper);
    }

    @Override
    public String createDictionaryType(DictionaryType dictionaryType) {
        QueryWrapper<DictionaryType> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DictionaryType.CODE, dictionaryType.getCode());
        var count = dictionaryTypeMapper.selectCount(queryWrapper);
        if (count > 1) {
            return "error";
        }
        long id = idWorker.nextId();
        dictionaryType.setId(id);
        int i = dictionaryTypeMapper.insert(dictionaryType);
        return i > 0 ? "success" : "error";
    }

    @Override
    public String updateDictionaryType(DictionaryType dictionaryType) {
        int i = dictionaryTypeMapper.updateById(dictionaryType);
        return i > 0 ? "success" : "error";
    }

    @Override
    public String deleteDictionaryTypeById(Long id) {
        int i = dictionaryTypeMapper.deleteById(id);
        QueryWrapper<DictionaryItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DictionaryItem.TYPE_ID, id);
        dictionaryItemMapper.delete(queryWrapper);
        return i > 0 ? "success" : "error";
    }

    @Override
    public void batchDeleteDictionaryTypeByIds(List<Long> dictionaryTypeIds) {
        dictionaryTypeMapper.deleteBatchIds(dictionaryTypeIds);
    }

    @Override
    public void batchInsertDictionaryType(List<DictionaryType> dictionaryTypeList) {
        dictionaryTypeMapper.batchInsertDictionaryType(dictionaryTypeList);
    }
}
