package com.tdtech.cloudcmd.admin.resource.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.admin.resource.entity.Carousel;
import com.tdtech.cloudcmd.admin.resource.entity.dto.CarouselDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.CarouselPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.CarouselSaveReqVO;
import com.tdtech.cloudcmd.admin.resource.mapper.CarouselMapper;
import com.tdtech.cloudcmd.admin.resource.service.ICarouselService;
import com.tdtech.cloudcmd.bean.CagentMqFrame;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.msip.aop.LogReport;
import com.tdtech.cloudcmd.msip.aop.LogReportParam;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

import static com.tdtech.cloudcmd.admin.config.WebConfig.UPLOAD_FILE_PATH;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Slf4j
@Service
public class CarouselServiceImpl implements ICarouselService {

    @Resource
    private CarouselMapper carouselMapper;
    @Resource
    private StreamBridge streamBridge;

    @Override
    @LogReport(type = OperationTypeEnum.CAROUSEL_INSERT)
    public Long createCarousel(@LogReportParam(field = "title") CarouselSaveReqVO createReqVO) {
        // 插入
        Carousel carousel = BeanCopyUtils.copyBean(createReqVO, Carousel::new);
        carouselMapper.insert(carousel);
        updateMessage("CREATE", carousel);
        // 返回
        return carousel.getId();
    }

    @Override
    @LogReport(type = OperationTypeEnum.CAROUSEL_UPDATE)
    public void updateCarousel(@LogReportParam(field = "title") CarouselSaveReqVO updateReqVO) {
        // 校验存在
        validateCarouselExists(updateReqVO.getId());
        // 更新
        Carousel updateObj = BeanCopyUtils.copyBean(updateReqVO, Carousel::new);
        log.info("updateObj:{}", updateObj);
        carouselMapper.updateById(updateObj);
        updateMessage("UPDATE", updateObj);
    }

    @Override
    public void deleteCarousel(Long id) {
        // 校验存在
        validateCarouselExists(id);
        // 删除
        carouselMapper.deleteById(id);
        updateMessage("DEL", List.of(id));
    }

    @Override
    @LogReport(type = OperationTypeEnum.CAROUSEL_DELETE)
    public void deleteCarousel(@LogReportParam(field = "title") CarouselDO carousel) {
        // 删除
        carouselMapper.deleteById(carousel.getId());
        updateMessage("DEL", List.of(carousel.getId()));
    }

    @Override
    public void deleteCarouselListByIds(List<Long> ids) {
        // 校验存在
        validateCarouselExists(ids);
        // 删除
        carouselMapper.deleteBatchIds(ids);
        updateMessage("DEL", ids);
    }

    private void validateCarouselExists(List<Long> ids) {
        List<Carousel> list = carouselMapper.selectBatchIds(ids);
        if (CollectionUtils.isEmpty(list) || list.size() != ids.size()) {
            throw new RuntimeException("轮播图不存在");
        }
    }

    private void validateCarouselExists(Long id) {
        if (carouselMapper.selectById(id) == null) {
            throw new RuntimeException("轮播图不存在");
        }
    }

    private void updateMessage(String notifyType, Object payload) {
        var cagentMqFrame = new CagentMqFrame().toBuilder().typeSubSystemMessage("CAROUSEL_UPDATE").broadcast()
            .body("CAROUSEL_UPDATE", notifyType, payload).build();
        streamBridge.send("cloudcmd-cagent", cagentMqFrame);
    }

    @Override
    public CarouselDO getCarousel(Long id) {
        Carousel carousel = carouselMapper.selectById(id);
        return BeanCopyUtils.copyBean(carousel, CarouselDO::new);
    }

    @Override
    public PageResult<CarouselDO> getCarouselPage(CarouselPageReqVO pageReqVO) {

        var page = new Page<Carousel>(pageReqVO.getPageNum(), pageReqVO.getPageSize());
        Page<CarouselDO> carouselDOPage = carouselMapper.selectPage(page, pageReqVO);
        log.info("carouselDOPage:{}", carouselDOPage);
        var pageResult = new PageResult<CarouselDO>();
        pageResult.setPages(carouselDOPage.getPages());
        pageResult.setSize(carouselDOPage.getSize());
        pageResult.setTotal(carouselDOPage.getTotal());
        pageResult.setCurrent(carouselDOPage.getCurrent());
        pageResult.setRecords(carouselDOPage.getRecords());
        return pageResult;
    }

    @Override
    public List<String> uploadBatch(MultipartFile[] multipartFile) throws IOException {
        var result = new LinkedList<String>();
        for (MultipartFile file : multipartFile) {
            var name = file.getOriginalFilename();
            log.info("on batch upload file:{}", name);
            if (name == null || name.isBlank()) {
                log.warn("file unknown:{}", file);
                continue;
            }
            var suffix = fileSuffix(file);
            var code = name.substring(0, name.length() - suffix.length());
            var format = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            var path = Path.of(UPLOAD_FILE_PATH, format, code + suffix);
            Files.createDirectories(path.getParent());
            file.transferTo(path);
            result.add(Path.of("/staticFile", format, code + suffix).toString());
        }
        return result;
    }

    private String fileSuffix(MultipartFile multipartFile) {
        var originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            return "";
        }
    }
}