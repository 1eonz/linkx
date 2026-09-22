package com.tdtech.cloudcmd.admin.offlinemap;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.config.BaseMapPersistConfigurationProperties;
import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.offlinemap.repo.Geo;
import com.tdtech.cloudcmd.admin.offlinemap.repo.GeoMapper;
import com.tdtech.cloudcmd.admin.offlinemap.repo.MapMapper;
import com.tdtech.cloudcmd.admin.offlinemap.repo.OfMap;
import com.tdtech.cloudcmd.admin.offlinemap.vo.GeoCo;
import com.tdtech.cloudcmd.admin.offlinemap.vo.MapCo;
import com.tdtech.cloudcmd.admin.offlinemap.vo.MapQo;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.tdtech.cloudcmd.admin.exception.AdminErrorEnum.COMMON_ERROR_563;

/**
 * @author Zht
 */
@Slf4j
@Service
public class MapService extends ServiceImpl<MapMapper, OfMap> {
    @Resource
    private GeoMapper geoMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private BaseMapPersistConfigurationProperties baseMapProperties;

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    public void createMap(MapCo mapCo) {
        mapCo.setId(idWorker.nextId());
        OfMap map = BeanCopyUtils.copyBean(mapCo, OfMap::new);
        map.setCreated(new Date());
        log.info("map:{}", map);
        var totalmaps = super.baseMapper.selectCount(null);
        log.info("totalmaps:{}", totalmaps);
        //判断数量是否大于最大支持数
        if (globalsRpcService.getGlobalsValueByName("MAP_ACTIVE_MAX_COUNT") != null) {
            if (totalmaps >= Long.parseLong(globalsRpcService.getGlobalsValueByName("MAP_ACTIVE_MAX_COUNT"))) {
                log.info("MAP_ACTIVE_MAX_COUNT:{}",
                    Integer.parseInt(globalsRpcService.getGlobalsValueByName("MAP_ACTIVE_MAX_COUNT")));
                throw new AdminException(COMMON_ERROR_563.getCode(), I18nUtil.get(COMMON_ERROR_563.getMsg()));
            }
        } else {
            throw new AdminException("MAP_ACTIVE_MAX_COUNT is null");
        }

        super.baseMapper.insert(map);
    }

    public void deleteMap(Long id) {
        OfMap map = super.baseMapper.selectById(id);
        // 获取最后一个`/`后的字符串，即文件名部分
        String mapIcon = map.getIcon();
        String icon = mapIcon.substring(mapIcon.lastIndexOf('/') + 1);
        var iconPath = Path.of(baseMapProperties.getMapiconDir(), icon);
        try {
            Files.delete(iconPath);
        } catch (Exception e) {
            log.error("delete Error:{}", e.getMessage());
        }
        super.baseMapper.deleteById(id);
    }

    public void updateMap(MapCo mapCo) {
        UpdateWrapper<OfMap> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", mapCo.getId());
        OfMap map = BeanCopyUtils.copyBean(mapCo, OfMap::new);
        super.baseMapper.update(map, updateWrapper);
    }

    public IPage<OfMap> selectPageMap(MapQo mapQo) {
        Integer pageNo = mapQo.getPageNo();
        Integer pageSize = mapQo.getPageSize();
        //若未定义每页显示数据条数，则默认为10个每页
        if (mapQo.getPageNo() == null) {
            pageNo = 1;
        }
        if (mapQo.getPageSize() == null) {
            pageSize = 10;
        }
        Page<OfMap> MapPage = new Page<>(pageNo, pageSize);

        return super.baseMapper.selectPage(MapPage, null);
    }

    public String uploadMapIcon(MultipartFile multipartFile) {
        String suffix = fileSuffix(multipartFile);
        String fileName = idWorker.nextId() + suffix;
        var filePath = Path.of(baseMapProperties.getMapiconDir(), fileName);
        try {
            Files.createDirectories(filePath.getParent());
            multipartFile.transferTo(filePath);
        } catch (Exception e) {
            throw new AdminException("The file name cannot contain Chinese characters");
        }
        return baseMapProperties.getMapiconUriPrefix() + fileName;
    }

    public List<OfMap> selectListMap(MapQo mapQo) {
        QueryWrapper<OfMap> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("activation", mapQo.getActivation());
        return super.baseMapper.selectList(queryWrapper);
    }

    public void updateGeo(GeoCo geoDto) {
        geoDto.setId(idWorker.nextId());
        geoDto.setCreated(new Date());
        Geo geo = BeanCopyUtils.copyBean(geoDto, Geo::new);
        log.info("geo:{}", geo);
        geoMapper.insert(geo);
    }

    public Geo selectGeo(Long id) {
        if (id != null) {
            return geoMapper.selectById(id);
        } else {
            QueryWrapper<Geo> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByDesc("created").last("LIMIT 1");
            return geoMapper.selectOne(queryWrapper);
        }
    }

    public Set<String> selectListGeo(String type) {
        QueryWrapper<Geo> queryWrapper = new QueryWrapper<>();
        Set<String> geoset = new HashSet<>();
        // 根据不同类型设置查询条件
        if ("geocode".equals(type)) {
            queryWrapper.select("geocode");
        } else if ("inversecode".equals(type)) {
            queryWrapper.select("inversecode");
        } else if ("poi".equals(type)) {
            queryWrapper.select("poi");
        } else {
            log.warn("不支持的类型: {}", type);
            return geoset; // 返回空集合
        }
        // 从数据库查询
        List<Geo> geoList = geoMapper.selectList(queryWrapper);
        // 将查询结果添加到 Set 中，实现去重
        for (Geo geo : geoList) {
            if ("geocode".equals(type) && geo.getGeocode() != null) {
                geoset.add(geo.getGeocode());
            } else if ("inversecode".equals(type) && geo.getInversecode() != null) {
                geoset.add(geo.getInversecode());
            } else if ("poi".equals(type) && geo.getPoi() != null) {
                geoset.add(geo.getPoi());
            }
        }
        return geoset; // 返回包含去重结果的 Set
    }

    //获取文件后缀
    private String fileSuffix(MultipartFile multipartFile) {
        var originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            return "";
        }
    }
}
