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
import com.tdtech.cloudcmd.admin.resource.entity.DictionaryItem;
import com.tdtech.cloudcmd.admin.resource.entity.dto.DictionaryItemDto;
import com.tdtech.cloudcmd.admin.resource.service.IDictionaryItemService;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * <p>
 * 数据字典项表（获取字典项通过字典类型code） 前端控制器
 * </p>
 *
 * @author mWX556161
 * @since 2020-07-16
 */
@Tag(name = "字典，没啥用")
@RestController
@RequestMapping("/admin/v1/dictionaryItem")
public class DictionaryItemController {
    @Autowired
    private IDictionaryItemService dictionaryItemService;

    @PostMapping("/getListByTypeCode")
    public R getDictionaryItemListByParam(@RequestParam("typeCode") String typeCode) {
        List<DictionaryItem> data = dictionaryItemService.getDictionaryItemListByType(typeCode);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_5",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.dto.DictionaryItemDto")
    @PostMapping("/update")
    public R updateCommandCenter(@Validated @RequestBody DictionaryItemDto dictionaryItemDto) {
        String data = dictionaryItemService.updateDictionaryItem(dictionaryItemDto);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_6", bodyKeyValue = "java.lang.Long")
    @PostMapping("/delete")
    public R deleteCommandCenter(@RequestParam(name = "id") Long id) {
        String data = dictionaryItemService.deleteDictionaryItemById(id);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }

    @OperationAnnotation(value = "OPERATION_ANNOTATION_7",
        bodyKeyValue = "com.tdtech.cloudcmd.admin.resource.entity.dto.DictionaryItemDto")
    @PostMapping("/create")
    public R createCommandCenter(@Validated @RequestBody DictionaryItemDto dictionaryItemDto) {
        String data = dictionaryItemService.createDictionaryItem(dictionaryItemDto);
        return R.success(ResponseCodeEnum.SUCCESS.getCode(), I18nUtil.get(ResponseCodeEnum.SUCCESS.getMsg()), data);
    }
}
