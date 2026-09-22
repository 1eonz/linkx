package com.tdtech.cloudcmd.base.controller;

import java.util.List;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.base.api.param.ItemDto;
import com.tdtech.cloudcmd.base.api.param.ItemQueryParam;
import com.tdtech.cloudcmd.base.api.service.DictionaryItemRpcService;
import com.tdtech.cloudcmd.base.service.IDictionaryItemService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code） 前端控制器
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@RestController
@RequestMapping("/base/v1/dictionaryItem")
public class DictionaryItemController {

    @Autowired
    private IDictionaryItemService dictionaryItemService;
    @DubboReference
    private DictionaryItemRpcService dictionaryItemRpcService;

    @PostMapping("/getItemListByType")
    public R getDictionaryItemListByTypeCode(@RequestBody ItemQueryParam param) {
        List<ItemDto> itemList = dictionaryItemRpcService.getItemListByType(param.getType());
        return R.success(itemList);
    }

    @PostMapping("/getItemNameByParam")
    public R getValue(@RequestBody ItemQueryParam param) {
        String name = dictionaryItemService.getItemNameByCode(param.getType(), param.getValue());
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), name);
    }
}
