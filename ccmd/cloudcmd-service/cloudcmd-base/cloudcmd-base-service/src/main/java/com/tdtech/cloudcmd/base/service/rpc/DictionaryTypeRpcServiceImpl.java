package com.tdtech.cloudcmd.base.service.rpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import com.tdtech.cloudcmd.base.api.param.ItemDto;
import com.tdtech.cloudcmd.base.api.param.TypeDto;
import com.tdtech.cloudcmd.base.api.service.DictionaryTypeRpcService;
import com.tdtech.cloudcmd.base.entity.DictionaryItem;
import com.tdtech.cloudcmd.base.entity.DictionaryType;
import com.tdtech.cloudcmd.base.service.IDictionaryItemService;
import com.tdtech.cloudcmd.base.service.IDictionaryTypeService;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.util.ListUtils;

/**
 * @author mWX556161
 * @date 2020/6/23 10:22
 */
@DubboService
public class DictionaryTypeRpcServiceImpl implements DictionaryTypeRpcService {
    @Autowired
    private IDictionaryTypeService dictionaryTypeService;
    @Autowired
    private IDictionaryItemService dictionaryItemService;

    @Override
    public List<TypeDto> getDictionaryTypeDtoList() {
        List<DictionaryType> dictionaryTypeList = dictionaryTypeService.getDictionaryTypeList();
        List<DictionaryItem> dictionaryItemList = dictionaryItemService.getDictionaryItemList();
        dictionaryTypeList.stream().forEach(v -> {
            v.setName(I18nUtil.get(v.getName()));
        });
        dictionaryItemList.stream().forEach(v -> {
            v.setName(I18nUtil.get(v.getName()));
        });
        Map<Long, List<DictionaryItem>> itemMap =
            dictionaryItemList.stream().collect(Collectors.groupingBy(DictionaryItem::getTypeId));
        List<TypeDto> typeDtoList = new ArrayList<>();
        for (DictionaryType dictionaryType : dictionaryTypeList) {
            TypeDto typeDto = new TypeDto();
            typeDto.setCode(dictionaryType.getCode());
            typeDto.setName(dictionaryType.getName());
            List<DictionaryItem> itemList = itemMap.get(dictionaryType.getId());
            if (ListUtils.isNotBlankList(itemList)) {
                List<ItemDto> itemDtos = new ArrayList<>();
                for (DictionaryItem item : itemList) {
                    ItemDto itemDto = new ItemDto();
                    itemDto.setName(item.getName());
                    itemDto.setIsDefault(item.getIsDefault());
                    itemDto.setValue(item.getValue());
                    itemDtos.add(itemDto);
                }
                typeDto.setItemList(itemDtos);
            } else {
                // 设置为[]避免为null 前端异常
                typeDto.setItemList(new ArrayList<>());
            }
            typeDtoList.add(typeDto);
        }
        return typeDtoList;
    }
}
