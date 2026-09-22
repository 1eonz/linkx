package com.tdtech.cloudcmd.admin.offlinemap;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tdtech.cloudcmd.admin.config.BaseMapPersistConfigurationProperties;
import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.admin.offlinemap.repo.BaseMap;
import com.tdtech.cloudcmd.admin.offlinemap.repo.BaseMapMapper;
import com.tdtech.cloudcmd.admin.offlinemap.repo.Division;
import com.tdtech.cloudcmd.admin.offlinemap.repo.DivisionMapper;
import com.tdtech.cloudcmd.admin.offlinemap.repo.Geo;
import com.tdtech.cloudcmd.admin.offlinemap.repo.GeoMapper;
import com.tdtech.cloudcmd.admin.offlinemap.vo.BaseMapVo;
import com.tdtech.cloudcmd.admin.resource.entity.dto.BaseMapDto;
import com.tdtech.cloudcmd.admin.resource.entity.dto.DivisionDto;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.util.BeanCopyUtils;
import com.tdtech.cloudcmd.util.IdWorker;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

@Service
@Slf4j
public class OfflineMapProxy extends ServiceImpl<BaseMapMapper, BaseMap> {

    private static final String OFFLINE_MAP_URL = "http://mapbox:10101";

    @Resource
    private DivisionMapper divisionMapper;
    @Resource
    private GeoMapper geoMapper;
    @Resource
    private IdWorker idWorker;
    @Resource
    private BaseMapPersistConfigurationProperties baseMapProperties;

    @SneakyThrows(IOException.class)
    public String uploadBaseMap(MultipartFile multipartFile) {
        String fileName = multipartFile.getOriginalFilename();
        log.info("basetiles:{}", fileName);
        var filePath = Path.of(baseMapProperties.getBasemapDir(), fileName);
        log.info("filePath:{}", filePath);
        String suffix = fileSuffix(multipartFile);
        log.info("suffix:{}", suffix);
        var totalmaps = divisionMapper.selectCount(null);
        if (suffix.equals(".geojson")) {
            var osmbPath = Path.of(baseMapProperties.getDivisionDir(), fileName);

            //判断是否已存在一个行政区划文件
            if (totalmaps > 0) {
                try (Stream<Path> stream = Files.list(Path.of(baseMapProperties.getDivisionDir()))) {
                    // 遍历目录下的所有文件并删除
                    stream.forEach(file -> {
                        try {
                            Files.delete(file);
                        } catch (IOException e) {
                            e.printStackTrace(); // 处理删除文件时的异常
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace(); // 处理列出文件时的异常
                }
            }
            deleteAllDivisions();

            Files.createDirectories(osmbPath.getParent());
            multipartFile.transferTo(osmbPath);

            DivisionDto divisionDto = new DivisionDto();
            divisionDto.setId(idWorker.nextId());
            divisionDto.setName(fileName);
            divisionDto.setAddress(String.valueOf(osmbPath));
            createDivision(divisionDto);
            return baseMapProperties.getDivisionUriPrefix() + fileName;
        } else {

            // 删除已存在的文件
            if (selectCountBaseMap(fileName) > 0) {
                log.info("CountBaseMap:{}", selectCountBaseMap(fileName));
                delete(fileName);
            }
            Files.createDirectories(filePath.getParent());
            multipartFile.transferTo(filePath);
            // 获取文件大小，MB为单位，精确小数点后两位
            String fileSize = formatFileSize(multipartFile.getSize());
            // 保存底库信息
            BaseMapDto baseMapDto = new BaseMapDto();
            baseMapDto.setId(idWorker.nextId());
            baseMapDto.setName(fileName);
            baseMapDto.setSize(fileSize);
            log.info("basediku:{}", baseMapDto);

            // 瓦片地址
            String tiles = "/map/v1/api/tilesets/" + fileName + "/{z}/{x}/{-y}." + fileFormat(fileName);
            baseMapDto.setTiles(tiles);
            log.info("basetiles:{}", tiles);
            createBaseMap(baseMapDto);
            log.info("baseMapDto:{}", baseMapDto);

            return baseMapProperties.getBasemapUriPrefix() + fileName;
        }
    }

    public IPage<BaseMap> selectPageBaseMap(BaseMapVo baseMapVo) {
        Integer pageNo = baseMapVo.getPageNo();
        Integer pageSize = baseMapVo.getPageSize();
        // 若未定义每页显示数据条数，则默认为10个每页
        if (baseMapVo.getPageNo() == null) {
            pageNo = 1;
        }
        if (baseMapVo.getPageSize() == null) {
            pageSize = 10;
        }
        Page<BaseMap> BaseMapPage = new Page<>(pageNo, pageSize);

        return super.baseMapper.selectPage(BaseMapPage, null);
    }

    public void createDivision(DivisionDto divisionDto) {
        Division division = BeanCopyUtils.copyBean(divisionDto, Division::new);
        divisionMapper.insert(division);
    }

    public void createBaseMap(BaseMapDto baseMapDto) {
        BaseMap baseMap = BeanCopyUtils.copyBean(baseMapDto, BaseMap::new);
        super.baseMapper.insert(baseMap);
    }

    public void deleteBaseMap(Long id) {
        BaseMap baseMap = super.baseMapper.selectById(id);
        String fileName = baseMap.getName();
        var filePath = Path.of(baseMapProperties.getBasemapDir(), fileName);
        try {
            Files.delete(filePath);
            try {
                // 发送异步请求到目标URL
                URL url = new URL(OFFLINE_MAP_URL + "/map/v1/initData");
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("GET");
                log.info("url:{}", url);
                // 设置必要的请求头或参数
                int responseCode = con.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 处理响应
                    // 例如，读取响应内容或处理响应数据
                } else {
                    // 处理错误情况
                }
            } catch (Exception e) {
                // 处理异常
                e.printStackTrace();
            }
        } catch (Exception e) {
            log.error("delete Error:{}", e.getMessage());
        }
        super.baseMapper.deleteById(id);
    }

    public void initBaseMap() throws IOException {
        try {
            log.info("初始化...");
            // 发送异步请求到目标URL
            URL url = new URL(OFFLINE_MAP_URL + "/map/v1/initData");
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("GET");
            log.info("url:{}", url);
            // 设置必要的请求头或参数
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 处理响应
                // 例如，读取响应内容或处理响应数据
            } else {
                // 处理错误情况

            }
        } catch (Exception e) {
            // 处理异常
            log.info(e.getMessage());
            throw new AdminException(I18nUtil.get("AdminException_ERROR_Initialization"));
        }
        // 指定文件夹的路径
        String folderPath = "/home/ics/offlinemap/data/tilesets/";
        String osmbPath = "/home/ics/offlinemap/data/OSMB/";
        String poiPath = "/home/ics/offlinemap/data/poi/";
        // 创建一个File对象，表示该文件夹
        File folder = new File(folderPath);
        File osmb = new File(osmbPath);
        File poi = new File(poiPath);
        // 检查该File对象是否表示一个存在的文件夹
        if (folder.isDirectory()) {
            // 获取文件夹下的所有文件
            File[] files = folder.listFiles();
            // 遍历文件夹下的所有文件
            if (files != null) {
                // 删除全部数据
                super.baseMapper.delete(null);
                geoMapper.delete(null);
                for (File file : files) {
                    String suffix = file.getName().substring(file.getName().lastIndexOf("."));
                    log.info("tilesets suffix:{}", suffix);
                    if (!suffix.equals(".idx") && !suffix.equals(".idx-journal")) {
                        log.info("File:{}", file.getName());
                        // 保存底库信息
                        BaseMapDto baseMapDto = new BaseMapDto();
                        baseMapDto.setId(idWorker.nextId());
                        String fileSize = formatFileSize(file.length());
                        baseMapDto.setName(file.getName());
                        baseMapDto.setSize(fileSize);
                        // 获取文件创建时间
                        FileTime creationTime =
                            (FileTime)Files.getAttribute(Path.of(file.getCanonicalPath()), "creationTime");
                        Date createdDate = new Date(creationTime.toMillis());
                        baseMapDto.setCreated(createdDate);
                        // 瓦片地址
                        String tiles = "/map/v1/api/tilesets/" + file.getName() + "/{z}/{x}/{-y}." + fileFormat(
                            file.getName());
                        log.info("baseMapDto tiles:{}", tiles);
                        baseMapDto.setTiles(tiles);
                        createBaseMap(baseMapDto);
                        log.info("baseMapDto:{}", baseMapDto);
                        // 保存底库信息
                        Geo geo = new Geo();
                        geo.setId(idWorker.nextId());
                        geo.setGeocode("/api/geocode/geo");
                        geo.setInversecode("/api/geocode/regeo");
                        geo.setPoi("/api/poi/" + file.getName());
                        geo.setCreated(createdDate);
                        geoMapper.insert(geo);
                        log.info("geo:{}", geo);
                    }
                }
            }
        } else {
            throw new AdminException("No tilesets");
        }
        if (osmb.isDirectory()) {
            // 获取文件夹下的所有文件
            File[] files = osmb.listFiles();
            // 遍历文件夹下的所有文件
            if (files != null) {
                // 删除全部数据
                divisionMapper.delete(null);
                for (File file : files) {
                    var filePath = Path.of(baseMapProperties.getDivisionDir(), file.getName());
                    String suffix = file.getName().substring(file.getName().lastIndexOf("."));
                    log.info("osmb suffix:{}", suffix);
                    if (suffix.equals(".geojson")) {
                        log.info("File:{}", file.getName());
                        // 保存底库信息
                        DivisionDto division = new DivisionDto();
                        division.setId(idWorker.nextId());
                        division.setName(file.getName());
                        division.setAddress(String.valueOf(filePath));
                        createDivision(division);
                        log.info("division:{}", division);
                    }
                }
            }
        }
        if (poi.isDirectory()) {
            // 获取文件夹下的所有文件
            File[] files = poi.listFiles();
            // 遍历文件夹下的所有文件
            if (files != null) {
                for (File file : files) {
                    String suffix = file.getName().substring(file.getName().lastIndexOf("."));
                    log.info("poi suffix:{}", suffix);
                    if (suffix.equals(".poi")) {
                        log.info("File:{}", file.getName());
                        // 保存底库信息
                        Geo geo = new Geo();
                        geo.setId(idWorker.nextId());
                        geo.setGeocode("/api/geocode/geo");
                        geo.setInversecode("/api/geocode/regeo");
                        geo.setPoi("/api/poi/" + file.getName());
                        geo.setCreated(new Date());
                        geoMapper.insert(geo);
                        log.info("geo:{}", geo);
                    }
                }
            }
        }
    }

    public Map<String, Object> updateDivision(Long nodeId) {
        try {
            log.info("导出行政区划...");

            // 创建 RestTemplate 实例
            RestTemplate restTemplate = new RestTemplate();
            String url = String.format(OFFLINE_MAP_URL + "/map/v1/api/geo/admins/nodes?nodeId=%d", nodeId);
            // 使用 ParameterizedTypeReference 指定返回类型
            // 泛型不能删openjdk有bug
            ResponseEntity<Map<String, Object>> responseEntity =
                restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<Map<String, Object>>() {
                });

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                // 返回接口的响应结果
                return responseEntity.getBody();
            } else {
                log.error("请求失败，响应码: {}", responseEntity.getStatusCode());
                throw new AdminException("请求失败");
            }
        } catch (Exception e) {
            log.error("更新失败: {}", e.getMessage());
            throw new AdminException(I18nUtil.get("AdminException_ERROR_Initialization"));
        }
    }

    public Integer selectCountBaseMap(String fileName) {
        QueryWrapper<BaseMap> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", fileName);
        log.info("selectCountBaseMap:{}", fileName);
        return super.baseMapper.selectCount(queryWrapper).intValue();
    }

    public Integer delete(String fileName) {
        QueryWrapper<BaseMap> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", fileName);
        log.info("delete:{}", fileName);
        return super.baseMapper.delete(queryWrapper);
    }

    public Integer deleteAllDivisions() {
        log.info("Deleting all divisions");
        return divisionMapper.delete(null); // 传入 null 删除所有记录
    }

    // 文件大小单位转换
    public static String formatFileSize(Long size) {
        String sizeName;
        if (1024 * 1024 > size && size >= 1024) {
            sizeName = String.format("%.2f", size.doubleValue() / 1024) + "KB";
        } else if (1024 * 1024 * 1024 > size && size >= 1024 * 1024) {
            sizeName = String.format("%.2f", size.doubleValue() / (1024 * 1024)) + "MB";
        } else if (size >= 1024 * 1024 * 1024) {
            sizeName = String.format("%.2f", size.doubleValue() / (1024 * 1024 * 1024)) + "GB";
        } else {
            sizeName = size + "B";
        }
        return sizeName;
    }

    // 获取文件后缀
    private String fileSuffix(MultipartFile multipartFile) {
        var originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            return "";
        }
    }

    public StringBuilder fileFormat(String fileName) {
        StringBuilder response = new StringBuilder();
        try {
            // 发送请求到目标URL
            URL url = new URL(OFFLINE_MAP_URL + "/map/v1/jdbcUtil?file=" + fileName);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            log.info("url:{}", url);
            // 设置必要的请求头或参数
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应内容
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                }
            } else {
                throw new AdminException("Get fileFormat error");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("fileFormat:{}", response);
        return response;
    }

}