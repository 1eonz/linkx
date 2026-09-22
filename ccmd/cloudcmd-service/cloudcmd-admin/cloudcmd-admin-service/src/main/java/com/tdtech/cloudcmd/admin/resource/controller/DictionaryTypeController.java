package com.tdtech.cloudcmd.admin.resource.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tdtech.cloudcmd.admin.annotation.OperationAnnotation;
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryType;
import com.tdtech.cloudcmd.admin.resource.service.IDictionaryTypeService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * 数据字典类型表 前端控制器
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Tag(name = "字典，没啥用")
@RestController
@RequestMapping("/admin/v1/dictionaryType")
public class DictionaryTypeController {

    @Autowired
    private IDictionaryTypeService dictionaryTypeService;

    @PostMapping("/list")
    public R getDictionaryTypeListByParam() {
        List<DictionaryType> data = dictionaryTypeService.getDictionaryTypeList();
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_8",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.DictionaryType")
    @PostMapping("/update")
    public R updateCommandCenter(@Validated @RequestBody DictionaryType dictionaryType) {
        String data = dictionaryTypeService.updateDictionaryType(dictionaryType);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_9", bodyKeyValue = "java.lang.Long")
    @PostMapping("/delete")
    public R deleteCommandCenter(@RequestParam(name = "id") Long id) {
        String data = dictionaryTypeService.deleteDictionaryTypeById(id);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_10",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.DictionaryType")
    @PostMapping("/create")
    public R createCommandCenter(@Validated @RequestBody DictionaryType dictionaryType) {
        String data = dictionaryTypeService.createDictionaryType(dictionaryType);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }
}
