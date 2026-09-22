package com.tdtech.cloudcmd.msip.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.tdtech.cloudcmd.base.api.service.GlobalsRpcService;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.entity.MSIPLicense;
import com.tdtech.cloudcmd.msip.entity.MSIPLicenseItem;
import com.tdtech.cloudcmd.msip.entity.MSIPResponse;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.redis.RedisUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import com.tdtech.cloudcmd.util.StringUtils;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URI;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class LicenseUtil {

    private static final String MSIP_HOST = "MSIP_HOST";

    private final Cache<String, String> cache =
            CacheBuilder.newBuilder().expireAfterWrite(10L, TimeUnit.SECONDS).build();

    @DubboReference
    private GlobalsRpcService globalsRpcService;

    @Resource
    private HttpClient httpClient;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private RedisLockFactory redisLockFactory;

    private String getMSIPHost() {
        return getConfig(MSIP_HOST);
    }


    @SneakyThrows
    private String getConfig(String key) {
        return globalsRpcService.getGlobalsValueByName(key);
//        return cache.get(key, () -> globalsRpcService.getGlobalsValueByName(key));
    }

    /**
     * 从MSIP平台获取license
     */
    @SneakyThrows
    private MSIPLicense getMSIPLicenseFromMSIP() {
        URI uri = new URI(getMSIPHost() + "/msip/v1/license/licenses?productName=LINKX");
        MSIPResponse<List<MSIPLicense>> resp = httpClient.getJson(uri, null, new TypeReference<>(){});
        log.info("getMSIPLicense uri: {}, resp: {}", uri, resp);
        // msip只会返回一个
        if (Objects.nonNull(resp) && CollectionUtils.isNotEmpty(resp.getDatas())) {
            return resp.getDatas().get(0);
        }
        return null;
    }

    private MSIPLicense getAndSetMSIPLicense() {
        MSIPLicense msipLicense = redisUtil.get(MSIPConstant.LICENSE_KEY, MSIPLicense.class);
        if (Objects.nonNull(msipLicense)) {
            return msipLicense;
        }
        var redisLock = redisLockFactory.newRedisLock(MSIPConstant.LICENSE_LOCK_KEY, Duration.ofSeconds(10L));
        if (!redisLock.tryLock(30L, TimeUnit.SECONDS)) {
            log.error("getMSIPLicense lock timeout");
            return null;
        }
        try {
            // double check lock
            msipLicense = redisUtil.get(MSIPConstant.LICENSE_KEY, MSIPLicense.class);
            if (Objects.nonNull(msipLicense)) {
                return msipLicense;
            }
            msipLicense = getMSIPLicenseFromMSIP();
            if (Objects.isNull(msipLicense)) {
                return null;
            }
            Date expireDate = DateFormatUtil.parseDate(msipLicense.getExpireDate());
            Date now = new Date();
            long millis = Math.abs(expireDate.getTime() - now.getTime());
//            redisUtil.set(MSIPConstant.LICENSE_KEY, msipLicense, Duration.ofSeconds(millis / 2));
            redisUtil.set(MSIPConstant.LICENSE_KEY, msipLicense, Duration.ofMinutes(1));
            return msipLicense;
        } catch (Exception e) {
            // block on fail
            log.error("getMSIPLicense error", e);
            return null;
        } finally {
            redisLock.unlock();
        }
    }

    public Map<String, String> getMSIPLicenseItemMap() {
        MSIPLicense msipLicense = getAndSetMSIPLicense();
        if(Objects.isNull(msipLicense)) {
            log.error("getMSIPLicenseItemMap msipLicense is null");
            return new HashMap<>();
        }
        boolean available = msipLicense.available();
        if (!available) {
            log.error("getMSIPLicenseItemMap available is false");
            return new HashMap<>();
        }
        return convert2Map(msipLicense);
    }

    public Map<String, String> getMSIPLicenseInfoMap() {
        MSIPLicense msipLicense = getAndSetMSIPLicense();
        if(Objects.isNull(msipLicense)) {
            log.error("getMSIPLicenseInfoMap msipLicense is null");
            return new HashMap<>();
        }
        // 把证书状态放进map，简单处理，给3端去判断证书的状态
        Map<String, String> map = convert2Map(msipLicense);
        map.put("status", msipLicense.getStatus());
        map.put("expireDate", msipLicense.getExpireDate());
        return map;
    }

    private Map<String, String> convert2Map(MSIPLicense msipLicense) {
        List<MSIPLicenseItem> itemList = msipLicense.getItemList();
        if (CollectionUtils.isEmpty(itemList)) {
            return new HashMap<>();
        }
        return itemList.stream().collect(Collectors.toMap(MSIPLicenseItem::getLicenseControlItem, MSIPLicenseItem::getResNum));
    }


    /**
     * 判断功能项是否可用
     * @param itemCode
     * @return
     */
    public boolean availableByCode(String itemCode) {
        Map<String, String> msipLicenseItemMap = getMSIPLicenseItemMap();
        String value = msipLicenseItemMap.get(itemCode);
        return StringUtils.isNotBlank(value) && MSIPConstant.AVAILABLE.equals(value);
    }

    public String getValue(String itemCode) {
        Map<String, String> msipLicenseItemMap = getMSIPLicenseItemMap();
        return msipLicenseItemMap.get(itemCode);
    }
}
