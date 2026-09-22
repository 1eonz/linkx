package com.tdtech.cloudcmd.base.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.base.api.param.DictionaryDto;
import com.tdtech.cloudcmd.base.entity.DictionaryItem;
import com.tdtech.cloudcmd.base.entity.DictionaryType;
import com.tdtech.cloudcmd.base.mapper.DictionaryItemMapper;
import com.tdtech.cloudcmd.base.service.IDictionaryItemService;
import com.tdtech.cloudcmd.base.service.IDictionaryTypeService;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code） 服务实现类
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@Service
@Slf4j
public class DictionaryItemServiceImpl extends ServiceImpl<DictionaryItemMapper, DictionaryItem>
    implements IDictionaryItemService {

    @Autowired
    DictionaryItemMapper dictionaryItemMapper;
    @Autowired
    IDictionaryTypeService dictionaryTypeService;

    @Override
    public List<DictionaryItem> getDictionaryItemListByTypeId(Long typeId) {
        QueryWrapper<DictionaryItem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(DictionaryItem.TYPE_ID, typeId);
        return dictionaryItemMapper.selectList(queryWrapper);
    }

    @Override
    public Map<String, List<DictionaryItem>> getDictionaryItemMap() {
        List<DictionaryType> dictionaryTypeList = dictionaryTypeService.getDictionaryTypeList();
        List<DictionaryItem> dictionaryItems = dictionaryItemMapper.selectList(null);
        Map<Long, List<DictionaryItem>> itemMap =
            dictionaryItems.stream().collect(Collectors.groupingBy(DictionaryItem::getTypeId));
        Map<String, List<DictionaryItem>> map = new HashMap<>();
        for (DictionaryType dictionaryType : dictionaryTypeList) {
            map.put(dictionaryType.getCode(), itemMap.get(dictionaryType.getId()));
        }
        return map;
    }

    @Override
    public List<DictionaryItem> getDictionaryItemList() {
        return dictionaryItemMapper.selectList(null);
    }

    @Override
    public String getItemNameByCode(String type, String value) {
        String name = null;
        DictionaryItem item = dictionaryItemMapper.getDictionaryItemByTypeAndValue(type, value);
        if (item != null) {
            name = item.getName();
        }
        return name;
    }

    @Override
    public List<DictionaryItem> getDictionaryItemByTypeCode(String code) {
        return dictionaryItemMapper.getDictionaryItemListByTypeCode(code);
    }

    @Override
    public DictionaryDto queryIconFromDictionary(Long typeId, String dicType) {
        DictionaryItem dictionaryItem =
            dictionaryItemMapper.getDictionaryItemByTypeAndValue(dicType, Long.toString(typeId));
        String dicItemJSONStr = JSONObject.toJSONString(dictionaryItem);
        DictionaryDto dictionaryDto = JSONObject.parseObject(dicItemJSONStr, DictionaryDto.class);
        return dictionaryDto;
    }
}
