package com.tdtech.cloudcmd.base.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.base.api.param.GlobalsParam;
import com.tdtech.cloudcmd.base.config.DisplayTarget;
import com.tdtech.cloudcmd.base.entity.Globals;
import com.tdtech.cloudcmd.base.service.IGlobalsService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.license.LicensePredicate;
import com.tdtech.cloudcmd.license.entity.LicenseItem;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;

/**
 * <p>
 * 全局变量信息表 前端控制器
 * </p>
 *
 * @author zhuangzl
 * @since 2020-06-07
 */
@RestController
@RequestMapping("/base/v1/globals")
public class GlobalsController {

    private static final String NETWORK = "1";// 1：外网，0:内网
    private static final String PREFIX = "NETWORK.";
    @Autowired
    private IGlobalsService globalsService;
    @Autowired
    private LicensePredicate licensePredicate;
    @Autowired
    private DisplayTarget displayTarget;
//    @Autowired
//    private CappSwitchProperties cappSwitchProperties;

    @PostMapping("/getGlobalsValueByName")
    public R getGlobalsValueByName(@RequestBody GlobalsParam param) {
        String value = globalsService.getValueByName(param.getName());
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), value);
    }

    @PostMapping("/getGlobalsList")
    public R getGlobalList(@RequestHeader("X-CloudCmd-Network") String network) {
        UserInfo user = SecurityUtils.getUser();
        List<Globals> globalsList = globalsService.getGlobalsList();
        List<Globals> tenantGlobalsList = null;
        if (user != null && user.getExecutorId() != null) {
            // 已登录
            tenantGlobalsList = globalsService.getTenantGlobalsList(user);
        }
        Map<String, String> map;
        if (StringUtils.equalsIgnoreCase(NETWORK, network)) {
            map = globalsList.stream()
                .filter(globals -> !StringUtils.isNullBlank(globals.getName()) && globals.getName().startsWith(PREFIX))
                .map(globals -> {
                    Globals globalsVo = new Globals();
                    return globalsVo.setValue(globals.getValue()).setName(globals.getName().split("\\.")[1]);
                }).collect(Collectors.toMap(Globals::getName, Globals::getValue, (oldvalue, newvalue) -> newvalue));
        } else {
            map = globalsList.stream().filter(s -> !s.getName().startsWith(PREFIX))
                .collect(Collectors.toMap(Globals::getName, Globals::getValue, (oldvalue, newvalue) -> newvalue));
        }
        map.put("STATION_NAME", I18nUtil.get(map.get("STATION_NAME")));
        map.put("ADDRESS_COLLECT_TYPE", I18nUtil.get(map.get("ADDRESS_COLLECT_TYPE")));
        map.put("ICP_CONTROL", "1");
        map.put(LicenseItem.CONTROL_CENTER_FUNCTION, "1");
        map.put(LicenseItem.ALARM_NOTIFY, "1");
        map.put(LicenseItem.MISSION_DISPATCH, "1");
        map.put(LicenseItem.SUSPECT_TASK, "1");
        map.put(LicenseItem.BODY_CAM_ONLINE_CONTROL, "1");
        map.put("DisplayTarget", String.valueOf(displayTarget.displayTarget));
        map.put(LicenseItem.VIDEO_CONFERENCING_NAME, "1");
//        map.put("CAPP_SWITCH_PDT", cappSwitchProperties.getCAPP_SWITCH_PDT());
//        map.put("CAPP_SWITCH_PINGAO", cappSwitchProperties.getCAPP_SWITCH_PINGAO());
//        map.put("CAPP_SWITCH_DS", cappSwitchProperties.getCAPP_SWITCH_DS());
//        map.put("CAPP_SWITCH_NOTICE", cappSwitchProperties.getCAPP_SWITCH_NOTICE());
//        map.put("CAPP_SWITCH_XUNFEI", cappSwitchProperties.getCAPP_SWITCH_XUNFEI());
//        map.put("CAPP_SWITCH_MAP_PATHPLAN", cappSwitchProperties.getCAPP_SWITCH_MAP_PATHPLAN());
//        map.put("CAPP_SWITCH_DUTY_QUERY", cappSwitchProperties.getCAPP_SWITCH_DUTY_QUERY());
        map.put(LicenseItem.AUTHENTICATION_CAPABILITY_FLAG, "1");
        map.put(LicenseItem.ICS_BASE_ITEM_NAME, "1");
        map.put(LicenseItem.NORTHBOUND_FUNCTION, "1");
        map.put(LicenseItem.ICS_SSF_NUM_NAME, "1");
        map.put(LicenseItem.ICS_VDF_NUM_NAME, "1");
        map.put("LANG_VERSION", I18nUtil.getVersion());
        // 登录后如果有自定义参数直接put
        if (tenantGlobalsList != null) {
            for (Globals globals : tenantGlobalsList) {
                map.put(globals.getName(), globals.getValue());
            }
        }
        String result = JSON.toJSONString(map, false);
        JSONObject object = JSON.parseObject(result);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), object);
    }

    @GetMapping("/serverTime")
    public R<Long> serverTimeStamp() {
        return R.success(System.currentTimeMillis());
    }
}
