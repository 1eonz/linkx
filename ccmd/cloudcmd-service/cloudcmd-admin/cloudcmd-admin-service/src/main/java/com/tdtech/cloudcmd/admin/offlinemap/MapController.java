package com.tdtech.cloudcmd.admin.offlinemap;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tdtech.cloudcmd.admin.offlinemap.repo.Division;
import com.tdtech.cloudcmd.admin.offlinemap.repo.Geo;
import com.tdtech.cloudcmd.admin.offlinemap.repo.OfMap;
import com.tdtech.cloudcmd.admin.offlinemap.vo.BaseMapVo;
import com.tdtech.cloudcmd.admin.offlinemap.vo.GeoCo;
import com.tdtech.cloudcmd.admin.offlinemap.vo.MapCo;
import com.tdtech.cloudcmd.admin.offlinemap.vo.MapQo;
import com.tdtech.cloudcmd.bean.R;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 地图管理（包括地图和底图）前端控制器
 * </p>
 *
 * @author zht
 * @since 2024-06-18
 */
@Slf4j
@RestController
@RequestMapping("/admin/v1/map")
public class MapController {
    @Resource
    private MapService mapService;
    @Resource
    private OfflineMapProxy offlineMapProxy;
    @Resource
    private DivisionService divisionService;

    /**
     * 创建地图
     *
     */
    @PostMapping("/createMap")
    public R createMap(@RequestBody @Validated MapCo mapCo) {
        log.info("createMapmap Dto:{}", mapCo);
        mapService.createMap(mapCo);
        return R.success();
    }

    /**
     * 删除地图
     *
     */
    @PostMapping("/deleteMap")
    public R deleteMap(@RequestParam Long id) {
        log.info("deleteMap id:{}", id);
        mapService.deleteMap(id);
        return R.success();
    }

    /**
     * 修改地图
     *
     */
    @PostMapping("/updateMap")
    public R updateMap(@RequestBody @Validated MapCo mapCo) {
        log.info("updateMapmap Dto:{}", mapCo);
        mapService.updateMap(mapCo);
        return R.success();
    }

    /**
     * 分页查询地图
     *
     */
    @PostMapping("/selectPageMap")
    public R selectPageMap(@RequestBody MapQo mapQo) {
        log.info("selectPageMap Vo:{}", mapQo);
        IPage<OfMap> mapList = mapService.selectPageMap(mapQo);
        return R.success(mapList);
    }

    /**
     * 上传地图图标
     *
     */
    @Operation(description = "上传地图图标")
    @PostMapping(value = "/uploadMapIcon", consumes = "multipart/form-data")
    public R<?> uploadMapIcon(@RequestParam("file") MultipartFile multipartFile) {
        log.info("uploadMapIcon multipartFile:{}", multipartFile);
        String umi = mapService.uploadMapIcon(multipartFile);
        return R.success(umi);
    }

    /**
     * 上传底图
     *
     */
    @Operation(description = "上传底图")
    @PostMapping(value = "/uploadBaseMap", consumes = "multipart/form-data")
    public R<?> uploadBaseMap(@RequestParam("file") MultipartFile multipartFile) {
        log.info("uploadBaseMap multipartFile:{}", multipartFile);
        String ubm = offlineMapProxy.uploadBaseMap(multipartFile);
        return R.success(ubm);
    }

    /**
     * 分页查询底图
     *
     */
    @PostMapping("/selectPageBaseMap")
    public R selectPageBaseMap(@RequestBody @Validated BaseMapVo baseMapVo) {
        log.info("selectPageBaseMap Vo:{}", baseMapVo);
        var basemapList = offlineMapProxy.selectPageBaseMap(baseMapVo);
        return R.success(basemapList);
    }

    /**
     * 删除底图
     *
     */
    @PostMapping("/deleteBaseMap")
    public R deleteBaseMap(@RequestParam Long id) throws IOException {
        log.info("deleteBaseMap id:{}", id);
        offlineMapProxy.deleteBaseMap(id);
        return R.success();
    }

    /**
     * 列表查询地图激活
     *
     */
    @PostMapping("/selectListMap")
    public R selectListMap(@RequestBody MapQo mapQo) {
        log.info("selectListMap Vo:{}", mapQo);
        List<OfMap> mapList = mapService.selectListMap(mapQo);
        return R.success(mapList);
    }

    /**
     * 地图数据更新接口
     *
     */
    @PostMapping("/updateGeo")
    public R updateGeo(@RequestBody @Validated GeoCo geoDto) {
        log.info("updateGeo Dto:{}", geoDto);
        mapService.updateGeo(geoDto);
        return R.success();
    }

    /**
     * 地图数据id查询
     *
     */
    @PostMapping("/selectGeo")
    public R selectGeo(@RequestBody GeoCo geoDto) {
        log.info("selectGeo id:{}", geoDto.getId());
        Geo geo = mapService.selectGeo(geoDto.getId());
        return R.success(geo);
    }

    /**
     * 地图数据列表查询
     *
     */
    @PostMapping("/selectListGeo")
    public R selectListGeo(@RequestBody GeoCo geoDto) {
        log.info("type:{}", geoDto.getType());
        Set<String> geoList = mapService.selectListGeo(geoDto.getType());
        return R.success(geoList);
    }

    /**
     * 行政区划列表查询
     *
     */
    @PostMapping("/selectDivision")
    public R selectDivision() {
        Division division = divisionService.selectGeo();
        return R.success(division);
    }

    /**
     * 初始化并创建底图数据
     *
     */
    @PostMapping("/initBaseMap")
    public R initBaseMap() throws IOException {
        offlineMapProxy.initBaseMap();
        return R.success();
    }

    /**
     * 导出行政区划
     *
     */
    @PostMapping("/updateDivision")
    public ResponseEntity<Map<String, Object>> updateDivision(@RequestBody GeoCo geoDto) {
        Map<String, Object> result = offlineMapProxy.updateDivision(geoDto.getNodeId()); // 调用更新方法
        return ResponseEntity.ok(result); // 返回结果
    }
}
