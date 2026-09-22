package com.tdtech.cloudcmd.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Properties;

import org.springframework.util.CollectionUtils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 版本信息工具类。
 * 优先从 jar 同目录下的 version.ini 读取版本信息；
 * 若 version.ini 不存在，回退取 MANIFEST.MF 的 Implementation-Version（即 pom.xml 版本号）；
 * 若仍取不到，回退返回 defaultVersion。
 */
@Data
@Slf4j
public class VersionInfo {
    private static final String VERSION_FILE = "version.ini";
    private static final String VERSION = "Version";
    private static final String SPLIT = "_";
    private static final String DEFAULT_VERSION = "1.0.0-SNAPSHOT";

    public static HashMap<String, String> getVersionInfo() {
        return getVersionInfo(DEFAULT_VERSION);
    }

    public static HashMap<String, String> getVersionInfo(String fallbackVersion) {
        HashMap<String, String> versionInfo = new HashMap<>();
        String filePath = getVersionFilePath();
        log.info("VersionInfo:versionFile:{}", filePath);

        Properties prop = new Properties();
        try (InputStream ins = new FileInputStream(filePath)) {
            prop.load(ins);
            CollectionUtils.mergePropertiesIntoMap(prop, versionInfo);
        } catch (FileNotFoundException e) {
            log.warn("VersionInfo:file not found, fallback to MANIFEST.MF.");
        } catch (IOException e) {
            log.error("VersionInfo:IOException.", e);
        }

        String version = resolveVersion(versionInfo.get(VERSION), fallbackVersion);
        versionInfo.put(VERSION, version);
        log.info("VersionInfo:version:{}", version);
        return versionInfo;
    }

    public static String getVersion() {
        return getVersion(DEFAULT_VERSION);
    }

    public static String getVersion(String fallbackVersion) {
        return getVersionInfo(fallbackVersion).get(VERSION);
    }

    private static String resolveVersion(String iniVersion, String fallbackVersion) {
        if (iniVersion != null && !iniVersion.isEmpty()) {
            int idx = iniVersion.indexOf(SPLIT);
            return idx > 0 ? iniVersion.substring(0, idx) : iniVersion;
        }
        String manifestVersion = VersionInfo.class.getPackage().getImplementationVersion();
        if (manifestVersion != null && !manifestVersion.isEmpty()) {
            return manifestVersion;
        }
        return fallbackVersion != null ? fallbackVersion : DEFAULT_VERSION;
    }

    public static String getVersionFilePath() {
        String envPath = System.getenv("VERSION_INI_PATH");
        if (envPath != null && !envPath.isEmpty()) {
            return envPath;
        }
        File jarFile = getJarFile();
        if (jarFile == null) {
            return VERSION_FILE;
        }
        File parent = jarFile.getParentFile();
        if (parent == null) {
            return VERSION_FILE;
        }
        // 使用规范化路径（消除符号链接与 . / ..），再拼接版本文件，避免路径歧义
        try {
            return parent.getCanonicalPath() + File.separator + VERSION_FILE;
        } catch (IOException e) {
            log.warn("VersionInfo:canonical path resolve failed, fallback to absolute path.", e);
            return parent.getAbsolutePath() + File.separator + VERSION_FILE;
        }
    }

    private static File getJarFile() {
        try {
            ProtectionDomain pd = VersionInfo.class.getProtectionDomain();
            if (pd == null) {
                return null;
            }
            CodeSource cs = pd.getCodeSource();
            if (cs == null) {
                return null;
            }
            URL location = cs.getLocation();
            if (location == null) {
                return null;
            }
            try {
                return new File(location.toURI());
            } catch (IllegalArgumentException e) {
                log.warn("VersionInfo:location is not a hierarchical URI, fallback to default. location={}", location);
                return null;
            }
        } catch (URISyntaxException e) {
            log.warn("VersionInfo:resolve jar location failed.", e);
            return null;
        }
    }
}
