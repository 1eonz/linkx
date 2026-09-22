package com.tdtech.cloudcmd.admin.resource.controller;

import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.BOOLEAN_FIXVALUE_TYPES;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.CORE_CIRCLE_RADIUS_TYPES;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.DEVICE_RESOLUTION_RATIO;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.DEVICE_RESOLUTION_RATIOS;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.FIXVALUE_TYPES;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.IP_TYPES;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.MAP_TYPE;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.MAP_TYPES;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.RANGE_TYPES;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.REFRESH_OVERSPEED_LIST;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.STATISTICS_REFRESH_TIME;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.STATION_NAME;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.STATION_NAME_CHINESE;
import static com.tdtech.cloudcmd.admin.resource.resourceConstants.Constants.GLOBALS_VALUE_EMPTY;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.admin.annotation.OperationAnnotation;
import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.resource.entity.Globals;
import com.tdtech.cloudcmd.admin.resource.entity.vo.AiDeployConfigVO;
import com.tdtech.cloudcmd.admin.resource.service.IGlobalsService;
import com.tdtech.cloudcmd.admin.util.GlobalsUtils;
import com.tdtech.cloudcmd.admin.util.response.AdminResultCode;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.util.StringUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;

/**
 * <p>
 * 全局变量信息表 前端控制器
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Tag(name = "全局参数")
@RestController
@RequestMapping("/admin/v1/globals")
@Slf4j
public class GlobalsController {
    @DubboReference
    private GlobalsRpcService globalsRpcService;
    @Autowired
    private IGlobalsService globalsService;
    @Autowired
    private ReportUtil reportUtil;

    @PostMapping("/list")
    public R getGlobalsListByParam(@RequestParam(required = false) String keyword) {
        List<Globals> data = globalsService.getGlobalsList(keyword);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @GetMapping("/ai/deploy")
    @Operation(summary = "查询AI分离部署配置")
    public R<AiDeployConfigVO> getAiDeployConfig() {
        return R.success(globalsService.getAiDeployConfig());
    }

    @PutMapping("/ai/deploy")
    @Operation(summary = "更新AI分离部署配置")
    public R<Boolean> updateAiDeployConfig(@Valid @RequestBody AiDeployConfigVO aiDeployConfigVO) {
        globalsService.updateAiDeployConfig(aiDeployConfigVO);
        return R.success(Boolean.TRUE);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_24",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.Globals")
    @PostMapping("/update")
    public R updateGlobals(@Validated @RequestBody Globals globals) {

        Globals old = null;
        try {
            R<Object> checked = checkGlobalsValue(globals);
            if (checked != null) {
                reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_UPDATE,
                        "修改全局参数校验失败");
                return checked;
            }
            R<Object> checkResult = parmCheck(globals);
            if (checkResult != null) {
                reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_UPDATE,
                        "修改全局参数校验失败");
                return checkResult;
            }
            old = globalsService.getById(globals.getId());
            if (Objects.isNull(old)) {
                reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_UPDATE,
                        "修改全局参数：[id=" + globals.getId() + "]不存在");
                return R.failure("全局参数不存在");
            }
            boolean isEnableOperation = old.getStatus() == 1 && globals.getStatus() == 0;
            if (isEnableOperation) {
                globalsService.enableGlobals(globals);
            } else {
                globalsService.updateGlobals(globals);
            }
        } catch (AdminException exception) {
            String name = Objects.nonNull(old) ? old.getRemark() : "";
            reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_UPDATE,
                    "修改全局参数" + name + "：" + exception.getMessage());
            return R.failure(exception.getCode(), exception.getMessage());
        } catch (Exception e) {
            log.error("globals update error", e);
            String name = Objects.nonNull(old) ? old.getRemark() : "";
            reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_UPDATE,
                    "修改全局参数\"" + name);
            return R.failure();
        }
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_25", bodyKeyValue = "java.lang.Long")
    @PostMapping("/delete")
    public R deleteGlobals(@RequestParam(name = "id") Long id) {
        Globals old = globalsService.getById(id);
        if (Objects.isNull(old)) {
                reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_DELETE,
                        "删除全局参数：[id=" + id + "]不存在");
            return R.success(ResponseCodeEnum.FAILURE.getCode(), I18nUtil.get(ResponseCodeEnum.FAILURE.getMsg()));
        }
        String data = globalsService.deleteGlobals(old);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_26",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.Globals")
    @PostMapping("/create")
    public R createGlobals(@Validated @RequestBody Globals globals) {

        try {
            R<Object> checked = checkGlobalsValue(globals);
            if (checked != null) {
                reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_INSERT,
                        "新增全局参数校验失败");
                return checked;
            }
            globalsService.createGlobals(globals);
            reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_INSERT,
                    "新增了全局参数\"" + globals.getRemark() + "\"", MSIPConstant.OPERATION_SUCCESS);
        } catch (AdminException exception) {
            reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_INSERT,
                    "新增全局参数" + globals.getRemark() + "：" + exception.getMessage());
            return R.failure(exception.getCode(), exception.getMessage());
        } catch (Exception e) {
            log.error("globals create error", e);
            reportUtil.saveOperationLog(OperationTypeEnum.GLOBAL_CONFIG_INSERT,
                    "新增全局参数\"" + globals.getRemark());
            return R.failure();
        }
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()));
    }

    private R<Object> checkGlobalsValue(Globals globals) {
        if (globals == null || globals.getName() == null) {
            return R.failure(ResponseCodeEnum.COMMON_ERROR_182.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_182.getMsg()));
        }
        if (!GLOBALS_VALUE_EMPTY.contains(globals.getName()) && StringUtils.isEmpty(globals.getValue())) {
            return R.failure(ResponseCodeEnum.COMMON_ERROR_182.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_182.getMsg()));
        }
        // value改为text类型，非空，加默认值防止入库失败
        if (StringUtils.isEmpty(globals.getValue())) {
            globals.setValue("");
        }
        return null;
    }

    private R<Object> parmCheck(Globals globals) {
        String globalsName = globals.getName();
        String globalsValue = globals.getValue();
        try {
            if (IP_TYPES.contains(globalsName) && !StringUtils.ipCheck(globalsValue)) {// ip校验
                return R.failure(ResponseCodeEnum.COMMON_ERROR_135.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_135.getMsg()));
            }
            if (globalsName.equals(MAP_TYPE) && !MAP_TYPES.contains(globalsValue)) {// 地图类型校验
                return R.failure(ResponseCodeEnum.COMMON_ERROR_136.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_136.getMsg()));
            }
            if (DEVICE_RESOLUTION_RATIO.equals(globalsName) && !DEVICE_RESOLUTION_RATIOS.contains(globalsValue)) {
                return R.failure(ResponseCodeEnum.COMMON_ERROR_137.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_137.getMsg()));
            }
            if (RANGE_TYPES.contains(globalsName) && !GlobalsUtils.rangeCheck(globalsName, globalsValue)) {
                return R.failure(ResponseCodeEnum.COMMON_ERROR_137.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_137.getMsg()));
            }

            if (FIXVALUE_TYPES.contains(globalsName) && !GlobalsUtils.fixValueCheck(globalsName, globalsValue)) {
                return R.failure(ResponseCodeEnum.COMMON_ERROR_137.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_137.getMsg()));
            }

            if (BOOLEAN_FIXVALUE_TYPES.contains(globalsName) && !GlobalsUtils.fixBooleanValueCheck(globalsValue)) {
                return R.failure(ResponseCodeEnum.COMMON_ERROR_137.getCode(),
                    I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_137.getMsg()));
            }
            if (STATION_NAME.equals(globalsName)) {
                String regex = "[\u4e00-\u9fa5]+";
                // 判断市中文还是英文
                if ((globalsValue.matches(regex)
                    && !GlobalsUtils.rangeCheck(STATION_NAME_CHINESE, String.valueOf(globalsValue.length())))
                    || (!globalsValue.matches(regex)
                        && !GlobalsUtils.rangeCheck(globalsName, String.valueOf(globalsValue.length())))) {
                    return R.failure(ResponseCodeEnum.COMMON_ERROR_137.getCode(),
                        I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_137.getMsg()));
                }
            }
            if(REFRESH_OVERSPEED_LIST.equals(globalsName) && (Integer.parseInt(globalsValue) < 10 || Integer.parseInt(globalsValue) > 30)){
                return R.failure(ResponseCodeEnum.COMMON_ERROR_141.getCode(),
                        I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_141.getMsg()));
            }

            if(STATISTICS_REFRESH_TIME.equals(globalsName) && (Integer.parseInt(globalsValue) < 10 || Integer.parseInt(globalsValue) > 60)){
                return R.failure(ResponseCodeEnum.COMMON_ERROR_141.getCode(),
                        I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_141.getMsg()));
            }
            if (CORE_CIRCLE_RADIUS_TYPES.contains(globalsName) && !GlobalsUtils.rangeCheckDouble(globalsValue)){
                return R.failure(ResponseCodeEnum.COMMON_ERROR_137.getCode(),
                        I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_137.getMsg()));
            }
            if (CORE_CIRCLE_RADIUS_TYPES.contains(globalsName)){
                List<String> collect = CORE_CIRCLE_RADIUS_TYPES.stream().filter(
                        coreCircleRadiusType -> !coreCircleRadiusType.equals(globalsName)
                ).collect(Collectors.toList());
                double value = Double.parseDouble(globalsValue);
                String formatValue = String.format("%.2f", value);
                for (String coreCircleRadius : collect){
                    String radius = globalsRpcService.getGlobalsValueByName(coreCircleRadius);
                    log.info("core Circle Radius other:{} now:{}",radius, formatValue);
                    if (formatValue.equals(radius)){
                        return R.failure(ResponseCodeEnum.COMMON_ERROR_142.getCode(),
                                I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_142.getMsg()));
                    }
                }
                globals.setValue(formatValue);
            }
        } catch (Exception e) {
            return R.failure(AdminResultCode.PARAM_TYPE_BIND_ERROR.code(),
                I18nUtil.get(AdminResultCode.PARAM_TYPE_BIND_ERROR.message()));
        }
        return null;
    }

}