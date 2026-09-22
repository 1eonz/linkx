package com.tdtech.cloudcmd.base.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.base.api.param.DictionaryDto;
import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesDto;
import com.tdtech.cloudcmd.base.api.param.ItemDto;
import com.tdtech.cloudcmd.base.entity.DictionaryItem;
import com.tdtech.cloudcmd.base.entity.ExtendInfoProperties;
import com.tdtech.cloudcmd.base.entity.Globals;
import com.tdtech.cloudcmd.base.service.ICacheService;
import com.tdtech.cloudcmd.base.service.IDictionaryItemService;
import com.tdtech.cloudcmd.base.service.IExtendInfoPropertiesService;
import com.tdtech.cloudcmd.base.service.IGlobalsService;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.ListUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tdtech.cloudcmd.base.api.param.TypeKey.BASE_CACHE_IS_INIT;
import static com.tdtech.cloudcmd.base.api.param.TypeKey.EXTEND_PROPERTIES_LIST_KEY;
import static com.tdtech.cloudcmd.base.api.param.TypeKey.EXTEND_PROPERTIES_VALUE_KEY;
import static com.tdtech.cloudcmd.base.api.param.TypeKey.GLOBALS_REDIS_KEY;
import static com.tdtech.cloudcmd.base.api.param.TypeKey.ITEM_REDIS_KEY;
import static com.tdtech.cloudcmd.base.api.param.TypeKey.TYPE_REDIS_KEY;

/**
 * @author mWX556161
 * @date 2020/6/16 13:35
 */
@Service
@Slf4j
public class CacheServiceImpl implements ICacheService {

    // im服务器ip、端口配置
    private List<String> IM_SERVER_ADDRESS_CONFIG_LIST = Arrays.asList("IM_ADDRESS_HTTP", "IM_ADDRESS_WS");

    // im服务器账号密码配置
    private List<String> IM_SERVER_ACCOUNT_PASSWORD_CONFIG_LIST = Arrays.asList("IM_CLI_ID", "IM_CLI_SEC");

    @Autowired
    private IDictionaryItemService dictionaryItemService;
    @Autowired
    private IGlobalsService globalsService;
    @Autowired
    private IExtendInfoPropertiesService extendInfoPropertiesService;
    @Autowired
    private RedisUtil redisUtil;

    @Resource
    private ReportUtil reportUtil;

    @PostConstruct
    @Override
    public void init() {
        log.info("init base cache");
        redisUtil.set(BASE_CACHE_IS_INIT, "1");
        this.initGlobals();
        this.initDictionary();
        this.initExtendInfoProperties();
    }

    /**
     * 每个20秒检测redis是否正常
     */
    @Scheduled(fixedRate = 20000)
    public void cacheHeart() {
        try {
            String value = redisUtil.get(BASE_CACHE_IS_INIT, String.class);
            if (StringUtils.isNullBlank(value)) {
                this.init();
            }
        } catch (Exception e) {
            log.error("cacheHeart redis error", e);
            this.init();
        }
    }

    /**
     * 每个60秒检测im基础配置是否存在
     */
    @Scheduled(fixedRate = 60000)
    public void checkBaseConfigAndAlarm() {
        // im服务器ip地址，端口没有配置
        int alreadyConfigIMServerSize = IM_SERVER_ADDRESS_CONFIG_LIST.stream().filter(code -> StringUtils.isNotBlank(this.getGlobalsValue(code))).collect(Collectors.toList()).size();
        boolean reportAlarmIMServerAddress = alreadyConfigIMServerSize < IM_SERVER_ADDRESS_CONFIG_LIST.size();
        if (reportAlarmIMServerAddress) {
            reportAlarm(AlarmTemplateZhEnum.IM_SERVER_IP_ADDRESS_IS_NOT_CONFIGURED);
            reportAlarm(AlarmTemplateZhEnum.IM_SERVER_PORT_IS_NOT_CONFIGURED);
        } else {
            clearAlarm(AlarmTemplateZhEnum.IM_SERVER_IP_ADDRESS_IS_NOT_CONFIGURED);
            clearAlarm(AlarmTemplateZhEnum.IM_SERVER_PORT_IS_NOT_CONFIGURED);
        }

        // im服务器账号密码没有配置
        int alreadyConfigIMAccountSize = IM_SERVER_ACCOUNT_PASSWORD_CONFIG_LIST.stream().filter(code -> StringUtils.isNotBlank(this.getGlobalsValue(code))).collect(Collectors.toList()).size();
        boolean reportAlarmIMAccount = alreadyConfigIMAccountSize < IM_SERVER_ACCOUNT_PASSWORD_CONFIG_LIST.size();
        if (reportAlarmIMAccount) {
            reportAlarm(AlarmTemplateZhEnum.IM_SERVER_ACCOUNT_AND_PASSWORD_IS_NOT_CONFIGURED);
        } else {
            clearAlarm(AlarmTemplateZhEnum.IM_SERVER_ACCOUNT_AND_PASSWORD_IS_NOT_CONFIGURED);
        }

    }

    private void reportAlarm(AlarmTemplateZhEnum alarmTemplateZhEnum) {
        reportUtil.saveAlarm2MSIP(alarmTemplateZhEnum);
    }

    private void clearAlarm(AlarmTemplateZhEnum alarmTemplateZhEnum) {
        reportUtil.clearAlarm2MSIP(alarmTemplateZhEnum);
    }

    @Override
    public void initDictionary() {
        Map<String, List<DictionaryItem>> dictionaryItemMap = dictionaryItemService.getDictionaryItemMap();
        for (String code : dictionaryItemMap.keySet()) {
            redisUtil.del(TYPE_REDIS_KEY.replace("{type}", code));
            List<DictionaryItem> list = dictionaryItemMap.get(code);
            if (ListUtils.isNotBlankList(list)) {
                for (DictionaryItem item : list) {
                    ItemDto itemDto = new ItemDto();
                    String name = item.getName();
                    String value = item.getValue();
                    itemDto.setValue(value);
                    itemDto.setName(name);
                    itemDto.setIsDefault(item.getIsDefault());
                    if (item.getStatus() == 0) {
                        redisUtil.set(ITEM_REDIS_KEY.replace("{type}", code).replace("{value}", value), name);
                        redisUtil.sAdd(TYPE_REDIS_KEY.replace("{type}", code), itemDto);
                    } else {
                        redisUtil.del(ITEM_REDIS_KEY.replace("{type}", code).replace("{value}", value));
                    }
                }
            }
        }
    }

    @Override
    public void initExtendInfoProperties() {
        Map<String, List<ExtendInfoProperties>> extendInfoPropertiesMap =
            extendInfoPropertiesService.getExtendInfoPropertiesMap();
        for (String code : extendInfoPropertiesMap.keySet()) {
            redisUtil.del(EXTEND_PROPERTIES_LIST_KEY.replace("{code}", code));
            List<ExtendInfoProperties> list = extendInfoPropertiesMap.get(code);
            for (ExtendInfoProperties item : list) {
                ExtendInfoPropertiesDto param = new ExtendInfoPropertiesDto();
                String name = item.getName();
                String label = item.getLabel();
                param.setName(name);
                param.setLabel(label);
                if (item.getStatus() == 0) {
                    redisUtil.set(EXTEND_PROPERTIES_VALUE_KEY.replace("{code}", code).replace("{name}", name), label);
                    redisUtil.sAdd(EXTEND_PROPERTIES_LIST_KEY.replace("{code}", code), param);
                } else {
                    redisUtil.del(EXTEND_PROPERTIES_VALUE_KEY.replace("{code}", code).replace("{name}", name));
                }
            }
        }
    }

    @Override
    public void initGlobals() {
        List<Globals> globalsList = globalsService.getGlobalsList();
        for (Globals globals : globalsList) {
            if (globals.getStatus() == 0) {
                redisUtil.set(GLOBALS_REDIS_KEY.replace("{name}", globals.getName()), globals.getValue());
            } else {
                redisUtil.del(GLOBALS_REDIS_KEY.replace("{name}", globals.getName()));
            }
        }
    }

    @Override
    public List<ItemDto> getItemListByTypeCode(String typeCode) {
        List<ItemDto> list = new ArrayList();
        Set<String> set = redisUtil.sMembers(TYPE_REDIS_KEY.replace("{type}", typeCode), String.class);
        if (set == null || set.size() == 0) {
            List<DictionaryItem> dictionaryItemList = dictionaryItemService.getDictionaryItemByTypeCode(typeCode);
            List<ItemDto> itemDtoList = new ArrayList<>();
            for (DictionaryItem dictionaryItem : dictionaryItemList) {
                ItemDto itemDto = new ItemDto();
                itemDto.setValue(dictionaryItem.getValue());
                itemDto.setIsDefault(dictionaryItem.getIsDefault());
                 itemDto.setName(I18nUtil.get(dictionaryItem.getName()));
                itemDtoList.add(itemDto);
                redisUtil.sAdd(TYPE_REDIS_KEY.replace("{type}", typeCode), JSONObject.toJSONString(itemDto));
            }
            list = itemDtoList;
        } else {
            for (String o : set) {
                ItemDto itemDto = JSONObject.parseObject(o, ItemDto.class);
                list.add(itemDto);
            }
        }
        list.forEach(itemDto -> {
            itemDto.setName(I18nUtil.get(itemDto.getName()));
        });
        return list;
    }

    @Override
    public String getItemName(String type, String value) {
        String name = redisUtil.get(ITEM_REDIS_KEY.replace("{type}", type).replace("{value}", value), String.class);
        if (StringUtils.isNullBlank(name)) {
            name = dictionaryItemService.getItemNameByCode(type, value);
            if (!StringUtils.isNullBlank(name)) {
                redisUtil.set(ITEM_REDIS_KEY.replace("{type}", type).replace("{value}", value), name);
            } else {
                log.error("getItemName db cat not get value,type={},value={}", type, value);
            }
        }
        return name;
    }

    @Override
    public String getGlobalsValue(String name) {
        String value = redisUtil.get(GLOBALS_REDIS_KEY.replace("{name}", name), String.class);
        if (StringUtils.isNullBlank(value)) {
            value = globalsService.getValueByName(name);
            if (!StringUtils.isNullBlank(value)) {
                redisUtil.set(GLOBALS_REDIS_KEY.replace("{name}", name), value);
            } else {
                log.error("getGlobalsValue db cat not get value,name={}", name);
            }
        }
        return value;
    }

    @Override
    public List<ExtendInfoPropertiesDto> getPropertiesListByCode(String code) {
        List<ExtendInfoPropertiesDto> list = new ArrayList<>();
        Set<ExtendInfoPropertiesDto> set =
            redisUtil.sMembers(EXTEND_PROPERTIES_LIST_KEY.replace("{code}", code), ExtendInfoPropertiesDto.class);
        if (set == null || set.size() == 0) {
            List<ExtendInfoProperties> extendInfoPropertiesList =
                extendInfoPropertiesService.getExtendInfoPropertiesListByCode(code);
            List<ExtendInfoPropertiesDto> extendInfoPropertiesDtoList = new ArrayList<>();
            for (ExtendInfoProperties extendInfoProperties : extendInfoPropertiesList) {
                ExtendInfoPropertiesDto extendInfoPropertiesDto = new ExtendInfoPropertiesDto();
                extendInfoPropertiesDto.setLabel(extendInfoProperties.getLabel());
                extendInfoPropertiesDto.setName(extendInfoProperties.getName());
                extendInfoPropertiesDtoList.add(extendInfoPropertiesDto);
                redisUtil.sAdd(EXTEND_PROPERTIES_LIST_KEY.replace("{code}", code),
                    JSONObject.toJSONString(extendInfoPropertiesDto));
            }
            list = extendInfoPropertiesDtoList;
        } else {
            list.addAll(set);
        }
        return list;
    }

    @Override
    public String getPropertiesLabel(String code, String name) {
        String label =
            redisUtil.get(EXTEND_PROPERTIES_VALUE_KEY.replace("{code}", code).replace("{name}", name), String.class);
        if (StringUtils.isNullBlank(label)) {
            ExtendInfoProperties extendInfoProperties =
                extendInfoPropertiesService.getExtendInfoPropertiesLabelByCodeAndName(code, name);
            label = extendInfoProperties.getLabel();
            if (!StringUtils.isNullBlank(label)) {
                redisUtil.set(EXTEND_PROPERTIES_VALUE_KEY.replace("{code}", code).replace("{name}", name), label);
            }
        }
        return label;
    }

    @Override
    public DictionaryDto queryIconFromDictionary(Long typeId, String dicType) {
        return dictionaryItemService.queryIconFromDictionary(typeId, dicType);
    }
}
