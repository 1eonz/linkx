package com.tdtech.cloudcmd.admin.resource.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tdtech.cloudcmd.admin.resource.entity.Carousel;
import com.tdtech.cloudcmd.admin.resource.entity.dto.CarouselDO;
import com.tdtech.cloudcmd.admin.resource.entity.vo.CarouselPageReqVO;

/**
 * @author lsc
 * @date 2025/7/14
 **/
@Mapper
public interface CarouselMapper extends BaseMapper<Carousel> {

     Page<CarouselDO> selectPage(Page<Carousel> page, @Param("reqVO") CarouselPageReqVO reqVO);
}
