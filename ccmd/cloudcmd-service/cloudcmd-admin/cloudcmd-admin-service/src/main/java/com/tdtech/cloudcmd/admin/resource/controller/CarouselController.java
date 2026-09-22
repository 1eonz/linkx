package com.tdtech.cloudcmd.admin.resource.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.tdtech.cloudcmd.admin.resource.entity.dto.CarouselDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.CarouselPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.CarouselSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.service.ICarouselService;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Slf4j
@Tag(name = "轮播图")
@RestController
@RequestMapping("/admin/v1/content/carousel")
@Validated
public class CarouselController {

    @Resource
    private ICarouselService carouselService;

    @Resource
    private ReportUtil reportUtil;

    @PostMapping("/create")
    @Operation(summary = "创建轮播图")
    public R<Long> createCarousel(@Valid @RequestBody CarouselSaveReqVO createReqVO) {
        return R.success(carouselService.createCarousel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新轮播图")
    public R<Boolean> updateCarousel(@Valid @RequestBody CarouselSaveReqVO updateReqVO) {
        carouselService.updateCarousel(updateReqVO);
        return R.success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除轮播图")
    @Parameter(name = "id", description = "编号", required = true)
    public R<Boolean> deleteCarousel(@RequestParam("id") Long id) {
        CarouselDO carousel = carouselService.getCarousel(id);
        if (Objects.isNull(carousel)) {
            reportUtil.saveOperationLog(OperationTypeEnum.CAROUSEL_DELETE,
                    "删除咨询轮播图：[id=" + id + "]不存在");
            return R.failure("轮播图不存在");
        }
        carouselService.deleteCarousel(carousel);
        return R.success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号", required = true)
    @Operation(summary = "批量删除轮播图")
    public R<Boolean> deleteCarouselList(@RequestParam("ids") List<Long> ids) {
        try {
            carouselService.deleteCarouselListByIds(ids);
            reportUtil.saveOperationLog(OperationTypeEnum.CAROUSEL_DELETE, "删除了咨询轮播图", MSIPConstant.OPERATION_SUCCESS);
        } catch (Exception ex) {
            reportUtil.saveOperationLog(OperationTypeEnum.CAROUSEL_DELETE,
                    "删除咨询轮播图：" + ex.getMessage());
            return R.failure("删除咨询轮播图失败");
        }

        return R.success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得轮播图")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public R<CarouselDO> getCarousel(@RequestParam("id") Long id) {
        return R.success(carouselService.getCarousel(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得轮播图分页")
    public R<PageResult<CarouselDO>> getCarouselPage(@Valid CarouselPageReqVO pageReqVO) {
        return R.success(carouselService.getCarouselPage(pageReqVO));
    }

    @Operation(description = "批量上传图片")
    @PostMapping(value = "uploadBatch", consumes = "multipart/form-data")
    public R<?> uploadBatch(@RequestParam(name = "file") MultipartFile[] multipartFiles) throws IOException {
        // 参数非空校验
        if (multipartFiles == null) {
            log.error("上传文件参数为null");
            return R.failure("上传文件参数不能为空");
        }

        log.info("on upload batch file size:{}", multipartFiles.length);

        // 检查文件数组长度
        if (multipartFiles.length == 0) {
            log.warn("未选择任何文件进行上传");
            return R.failure("请选择至少一个文件进行上传");
        }

        // 检查每个文件是否有效
        for (MultipartFile file : multipartFiles) {
            if (file == null || file.isEmpty()) {
                log.error("上传的文件中包含空文件");
                return R.failure("上传的文件中包含无效文件");
            }
        }
        log.info("on upload batch file size:{}", multipartFiles.length);
        var result = carouselService.uploadBatch(multipartFiles);
        return R.success(result);
    }

}