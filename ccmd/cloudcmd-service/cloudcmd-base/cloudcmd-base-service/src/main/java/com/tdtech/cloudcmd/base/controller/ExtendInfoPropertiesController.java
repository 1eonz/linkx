package com.tdtech.cloudcmd.base.controller;

import java.util.List;
import java.util.Map;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesDto;
import com.tdtech.cloudcmd.base.api.param.ExtendInfoPropertiesQueryParam;
import com.tdtech.cloudcmd.base.api.service.ExtendInfoPropertiesRpcService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;

/**
 * <p>
 * 扩展信息属性定义，通过反射实现扩展类 前端控制器
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@RestController
@RequestMapping("/base/v1/extendInfoProperties")
public class ExtendInfoPropertiesController {

    @DubboReference
    private ExtendInfoPropertiesRpcService extendInfoPropertiesRpcService;

    @PostMapping("/getExtendInfoPropertiesListByCode")
    public R getExtendInfoPropertiesListByCode(@RequestBody ExtendInfoPropertiesQueryParam param) {
        List<ExtendInfoPropertiesDto> extendInfoPropertiesList =
            extendInfoPropertiesRpcService.getExtendInfoPropertiesListByCode(param.getCode());
        return R.success(extendInfoPropertiesList);
    }

    @PostMapping("/getExtendInfoPropertiesLabelByParam")
    public R getExtendInfoPropertiesLabelByParam(@RequestBody ExtendInfoPropertiesQueryParam param) {
        String label = extendInfoPropertiesRpcService.getExtendInfoPropertiesLabelByParam(param);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), label);
    }

    @PostMapping("/getExtendInfoPropertiesList")
    public R getExtendInfoPropertiesList() {
        Map<String, List<ExtendInfoPropertiesDto>> map = extendInfoPropertiesRpcService.getExtendInfoPropertiesList();
        return R.success(map);
    }

}
