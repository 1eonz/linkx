package com.tdtech.cloudcmd.base.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.common.util.VersionInfo;

/**
 * @author dl272038
 * @date 2022/5/19 14:55
 */
@RestController
@RequestMapping("/base/v1/version")
public class BaseInfoController {
    @GetMapping
    public R getICSVersionInfo() {
        return R.success(VersionInfo.getVersionInfo());
    }
}
