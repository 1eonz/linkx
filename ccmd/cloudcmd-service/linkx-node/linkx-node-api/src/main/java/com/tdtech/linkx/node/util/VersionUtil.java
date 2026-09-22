package com.tdtech.linkx.node.util;

import com.tdtech.cloudcmd.common.util.VersionInfo;
import org.apache.commons.lang3.StringUtils;

public class VersionUtil {

    public static String getProjectVersion() {
        return VersionInfo.getVersion();
    }

    /**
     * 比较两个版本号是否一致
     *
     * @param localVersion  本机版本号
     * @param remoteVersion 对端版本号
     * @return true=一致，false=不一致
     */
    public static boolean isSame(String localVersion, String remoteVersion) {
        if (StringUtils.isBlank(localVersion) || StringUtils.isBlank(remoteVersion)) {
            return false;
        }
        return localVersion.equals(remoteVersion);
    }
}
