package com.tdtech.cloudcmd.base.controller;

import java.util.List;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.base.api.param.TypeDto;
import com.tdtech.cloudcmd.base.api.service.DictionaryTypeRpcService;
import com.tdtech.cloudcmd.bean.R;

/**
 * @author mWX556161
 * @date 2020/6/23 10:40
 */
@RestController
@RequestMapping("/base/v1/dictionaryType")
public class DictionaryTypeController {

    @DubboReference
    private DictionaryTypeRpcService dictionaryTypeRpcService;

    @PostMapping("/getTypeList")
    public R getDictionaryItemListByTypeCode() {
        List<TypeDto> typeDtoList = dictionaryTypeRpcService.getDictionaryTypeDtoList();
        return R.success(typeDtoList);
    }
}
