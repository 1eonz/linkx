package com.tdtech.cloudcmd.admin.resource.service.impl;

import java.util.List;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryItem;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryType;
import com.tdtech.cloudcmd.admin.resource.entity.dto.DictionaryItemDto;
import com.tdtech.cloudcmd.admin.resource.mapper.DictionaryItemMapper;
import com.tdtech.cloudcmd.admin.resource.service.IDictionaryItemService;
import com.tdtech.cloudcmd.admin.resource.service.IDictionaryTypeService;
import com.tdtech.cloudcmd.base.api.service.CacheRpcService;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.ListUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code） 服务实现类
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Service
@Slf4j
public class DictionaryItemServiceImpl extends ServiceImpl<DictionaryItemMapper, DictionaryItem>
    implements IDictionaryItemService {

    @Autowired
    private DictionaryItemMapper dictionaryItemMapper;
    @Autowired
    private IdWorker idWorker;
    @Autowired
    private IDictionaryTypeService dictionaryTypeService;
    @DubboReference
    private CacheRpcService cacheRpcService;
    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @Override
    public List<DictionaryItem> getDictionaryItemListByType(String typeCode) {
        Integer displayTarget = globalsRpcService.getDisplayTarget();
        return dictionaryItemMapper.getDictionaryItemListByTypeCode(typeCode,displayTarget);
    }

    @Override
    public List<DictionaryItem> getDictionaryItemList() {
        return dictionaryItemMapper.selectList(null);
    }

    @Override
    public String createDictionaryItem(DictionaryItemDto dictionaryItemDto) {
        DictionaryItem dictionaryItem = new DictionaryItem();
        dictionaryItem.setName(dictionaryItemDto.getName());
        dictionaryItem.setValue(dictionaryItemDto.getValue());
        DictionaryType dictionaryType =
            dictionaryTypeService.getDictionaryTypeByTypeCode(dictionaryItemDto.getTypeCode());
        if (dictionaryType == null) {
            return "error";
        } else {
            dictionaryItem.setTypeId(dictionaryType.getId());
        }
        QueryWrapper<DictionaryItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DictionaryItem.NAME, dictionaryItemDto.getName());
        queryWrapper.eq(DictionaryItem.TYPE_ID, dictionaryType.getId());
        queryWrapper.eq(DictionaryItem.VALUE, dictionaryItemDto.getValue());
        var count = dictionaryItemMapper.selectCount(queryWrapper);
        if (count >= 1) {
            return "error";
        }
        long id = idWorker.nextId();
        dictionaryItem.setId(id);
        int i = dictionaryItemMapper.insert(dictionaryItem);
        cacheRpcService.initDictionary();
        return i > 0 ? "success" : "error";
    }

    @Override
    public String updateDictionaryItem(DictionaryItemDto dictionaryItemDto) {
        DictionaryItem dictionaryItem = new DictionaryItem();
        DictionaryType dictionaryType =
            dictionaryTypeService.getDictionaryTypeByTypeCode(dictionaryItemDto.getTypeCode());
        if (dictionaryType == null) {
            return "error";
        } else {
            if (!dictionaryType.getId().equals(dictionaryItemDto.getTypeId())) {
                dictionaryItem.setTypeId(dictionaryType.getId());
            }
        }
        QueryWrapper<DictionaryItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DictionaryItem.NAME, dictionaryItemDto.getName());
        queryWrapper.eq(DictionaryItem.TYPE_ID, dictionaryType.getId());
        queryWrapper.eq(DictionaryItem.VALUE, dictionaryItemDto.getValue());
        List<DictionaryItem> dictionaryItemList = dictionaryItemMapper.selectList(queryWrapper);
        if (ListUtils.isNotBlankList(dictionaryItemList)) {
            if (dictionaryItemList.size() > 1) {
                return "error";
            } else if (dictionaryItemList.size() == 1) {
                DictionaryItem dictionaryItem1 = dictionaryItemList.get(0);
                if (!dictionaryItem1.getId().equals(dictionaryItemDto.getId())) {
                    return "error";
                }
            }
        }
        dictionaryItem.setId(dictionaryItemDto.getId());
        dictionaryItem.setName(dictionaryItemDto.getName());
        dictionaryItem.setValue(dictionaryItemDto.getValue());
        int i = dictionaryItemMapper.updateById(dictionaryItem);
        cacheRpcService.initDictionary();
        return i > 0 ? "success" : "error";
    }

    @Override
    public String deleteDictionaryItemById(Long id) {
        int i = dictionaryItemMapper.deleteById(id);
        cacheRpcService.initDictionary();
        return i > 0 ? "success" : "error";
    }

    @Override
    public void batchDeleteDictionaryItemByIds(List<Long> dictionaryItemIds) {
        dictionaryItemMapper.deleteBatchIds(dictionaryItemIds);
    }

    @Override
    public void batchInsertDictionaryItem(List<DictionaryItem> dictionaryItemList) {
        dictionaryItemMapper.batchInsertDictionaryItem(dictionaryItemList);
    }
}
