package com.tdtech.cloudcmd.admin.resource.controller;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

/**
 * <p>
 * msip授权 前端控制器
 * </p>
 *
 * @author ly
 * @since 2025-11-24
 */
@Tag(name = "msip授权")
@RestController
@RequestMapping("/admin/v1/msip/license")
@Slf4j
public class MSIPLicenseController {
    @Resource
    private LicenseUtil licenseUtil;

    @GetMapping("/info")
    @Operation(summary = "获取MSIP授权信息", description = "获取MSIP平台授权license信息")
    public R<Map<String, String>> getLicense() {
        Map<String, String> data = licenseUtil.getMSIPLicenseInfoMap();
        return R.success(data);
    }

}
