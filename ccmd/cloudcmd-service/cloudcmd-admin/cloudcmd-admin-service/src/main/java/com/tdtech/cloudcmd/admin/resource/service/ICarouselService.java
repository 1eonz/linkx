package com.tdtech.cloudcmd.admin.resource.service;

import java.io.IOException;
import java.util.List;

import javax.validation.Valid;

import org.springframework.web.multipart.MultipartFile;

import com.tdtech.cloudcmd.admin.resource.entity.dto.CarouselDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.CarouselPageReqVO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.CarouselSaveReqVO;
import com.tdtech.cloudcmd.bean.PageResult;

/**
 * @author lsc
 * @date 2025/7/14
 **/
public interface ICarouselService {

    /**
     * 创建轮播图
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCarousel(@Valid CarouselSaveReqVO createReqVO);

    /**
     * 更新轮播图
     *
     * @param updateReqVO 更新信息
     */
    void updateCarousel(@Valid CarouselSaveReqVO updateReqVO);

    /**
     * 删除轮播图
     *
     * @param id 编号
     */
    void deleteCarousel(Long id);

    void deleteCarousel(CarouselDO carousel);

    /**
     * 批量删除轮播图
     *
     * @param ids 编号
     */
    void deleteCarouselListByIds(List<Long> ids);

    /**
     * 获得轮播图
     *
     * @param id 编号
     * @return 轮播图
     */
    CarouselDO getCarousel(Long id);

    /**
     * 获得轮播图分页
     *
     * @param pageReqVO 分页查询
     * @return 轮播图分页
     */
    PageResult<CarouselDO> getCarouselPage(CarouselPageReqVO pageReqVO);

    List<String> uploadBatch(MultipartFile[] multipartFile) throws IOException;
}
