package com.tdtech.cloudcmd.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MIME 类型工具类
 * <p>
 * 纯 JDK 实现的文件类型识别，不依赖 Apache Tika，避免 tika-core 与 commons-io 版本冲突。
 * <p>
 * 检测策略：
 * <ol>
 *   <li>扩展名映射表：覆盖 IM 场景常见格式（图片/音视频/文档/压缩包）；</li>
 *   <li>Magic Number 兜底：通过文件头字节判断常见图片/压缩包/文档类型；</li>
 *   <li>客户端声明值兜底：调用方提供的 Content-Type 作为最后兜底。</li>
 * </ol>
 * 设计上保持与历史 Tika 版本一致的方法签名和行为契约，调用方无需改动。
 *
 * @author cloudcmd
 */
@Slf4j
public final class MimeTypeUtils {

    /**
     * 默认 MIME 类型：未知或检测失败时返回
     */
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    /**
     * 扩展名 -> MIME 映射（小写，无点号）
     * 覆盖 IM 场景常见格式，未覆盖的类型走 magic number / 兜底逻辑
     */
    private static final Map<String, String> EXT_MIME_MAP;

    /**
     * MIME -> 扩展名映射（用于反向查询，取每种 MIME 的代表性扩展名）
     */
    private static final Map<String, String> MIME_EXT_MAP;

    /**
     * Magic Number 规则：{前缀字节数组, 对应 MIME}
     * 用于基于文件内容的兜底检测，覆盖常见图片/压缩包/文档格式
     */
    private static final MagicRule[] MAGIC_RULES;

    static {
        Map<String, String> extMap = new LinkedHashMap<>();
        // 图片
        extMap.put("jpg", "image/jpeg");
        extMap.put("jpeg", "image/jpeg");
        extMap.put("png", "image/png");
        extMap.put("gif", "image/gif");
        extMap.put("bmp", "image/bmp");
        extMap.put("webp", "image/webp");
        extMap.put("ico", "image/x-icon");
        extMap.put("tiff", "image/tiff");
        extMap.put("tif", "image/tiff");
        extMap.put("svg", "image/svg+xml");
        // 音频
        extMap.put("mp3", "audio/mpeg");
        extMap.put("wav", "audio/wav");
        extMap.put("amr", "audio/amr");
        extMap.put("aac", "audio/aac");
        extMap.put("ogg", "audio/ogg");
        extMap.put("flac", "audio/flac");
        extMap.put("m4a", "audio/mp4");
        // 视频
        extMap.put("mp4", "video/mp4");
        extMap.put("avi", "video/x-msvideo");
        extMap.put("mov", "video/quicktime");
        extMap.put("wmv", "video/x-ms-wmv");
        extMap.put("flv", "video/x-flv");
        extMap.put("mkv", "video/x-matroska");
        extMap.put("3gp", "video/3gpp");
        // 文档
        extMap.put("pdf", "application/pdf");
        extMap.put("doc", "application/msword");
        extMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        extMap.put("xls", "application/vnd.ms-excel");
        extMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        extMap.put("ppt", "application/vnd.ms-powerpoint");
        extMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        extMap.put("txt", "text/plain");
        extMap.put("csv", "text/csv");
        extMap.put("html", "text/html");
        extMap.put("htm", "text/html");
        extMap.put("xml", "application/xml");
        extMap.put("json", "application/json");
        // 压缩包
        extMap.put("zip", "application/zip");
        extMap.put("rar", "application/vnd.rar");
        extMap.put("7z", "application/x-7z-compressed");
        extMap.put("gz", "application/gzip");
        extMap.put("tar", "application/x-tar");
        EXT_MIME_MAP = Collections.unmodifiableMap(extMap);

        // 反向映射：MIME -> 代表性扩展名
        Map<String, String> mimeExtMap = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : extMap.entrySet()) {
            mimeExtMap.putIfAbsent(entry.getValue(), entry.getKey());
        }
        MIME_EXT_MAP = Collections.unmodifiableMap(mimeExtMap);

        // Magic Number 规则
        // 注意：相同前缀（如 RIFF）的规则，更具体的必须放在前面
        MAGIC_RULES = new MagicRule[] {
            // 图片
            new MagicRule(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, "image/jpeg"),
            new MagicRule(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, "image/png"),
            new MagicRule(new byte[] {'G', 'I', 'F', '8'}, "image/gif"),
            new MagicRule(new byte[] {'B', 'M'}, "image/bmp"),
            new MagicRule(new byte[] {0x00, 0x00, 0x01, 0x00}, "image/x-icon"),
            // 视频
            new MagicRule(new byte[] {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x70, 0x34}, "video/mp4"),
            new MagicRule(new byte[] {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}, "video/mp4"),
            // 压缩包
            new MagicRule(new byte[] {'P', 'K', 0x03, 0x04}, "application/zip"),
            new MagicRule(new byte[] {'R', 'a', 'r', '!'}, "application/vnd.rar"),
            new MagicRule(new byte[] {'7', 'z', (byte) 0xBC, (byte) 0xAF, 0x27, 0x1C}, "application/x-7z-compressed"),
            new MagicRule(new byte[] {0x1F, (byte) 0x8B}, "application/gzip"),
            // 文档
            new MagicRule(new byte[] {'%', 'P', 'D', 'F'}, "application/pdf"),
            new MagicRule(new byte[] {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1},
                "application/msword"),
            // 音频
            new MagicRule(new byte[] {'I', 'D', '3'}, "audio/mpeg"),
            new MagicRule(new byte[] {0x23, 0x21, 0x41, 0x4D, 0x52}, "audio/amr"),
            // RIFF 容器：只能识别到格式族，具体类型需扩展名辅助
            new MagicRule(new byte[] {'R', 'I', 'F', 'F'}, "application/octet-stream")
        };
    }

    private MimeTypeUtils() {
    }

    /**
     * 基于文件内容检测 MIME 类型
     * <p>
     * 通过读取文件头 magic number 判断，适用于无文件名或文件名不可信的场景。
     *
     * @param data 文件内容字节数组
     * @return MIME 类型，检测失败返回 {@link #APPLICATION_OCTET_STREAM}
     */
    public static String getMimeType(byte[] data) {
        if (data == null || data.length == 0) {
            return APPLICATION_OCTET_STREAM;
        }
        String mimeType = detectByMagic(data);
        return StringUtils.isBlank(mimeType) ? APPLICATION_OCTET_STREAM : mimeType;
    }

    /**
     * 基于文件名检测 MIME 类型
     * <p>
     * 仅根据文件扩展名判断，不读取文件内容。
     *
     * @param fileName 文件名
     * @return MIME 类型，检测失败返回 {@link #APPLICATION_OCTET_STREAM}
     */
    public static String getMimeType(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return APPLICATION_OCTET_STREAM;
        }
        String ext = extractExtension(fileName);
        if (StringUtils.isBlank(ext)) {
            return APPLICATION_OCTET_STREAM;
        }
        String mimeType = EXT_MIME_MAP.get(ext);
        return StringUtils.isBlank(mimeType) ? APPLICATION_OCTET_STREAM : mimeType;
    }

    /**
     * 综合文件内容和文件名检测 MIME 类型（推荐）
     * <p>
     * 优先用扩展名映射（覆盖面广），未命中再走 magic number 兜底。
     *
     * @param data     文件内容字节数组
     * @param fileName 文件名
     * @return MIME 类型，检测失败返回 {@link #APPLICATION_OCTET_STREAM}
     */
    public static String getMimeType(byte[] data, String fileName) {
        // 1. 优先扩展名映射
        String byExt = getMimeType(fileName);
        if (!APPLICATION_OCTET_STREAM.equals(byExt)) {
            return byExt;
        }
        // 2. 扩展名未命中，走 magic number
        if (data == null || data.length == 0) {
            return APPLICATION_OCTET_STREAM;
        }
        String byMagic = detectByMagic(data);
        return StringUtils.isBlank(byMagic) ? APPLICATION_OCTET_STREAM : byMagic;
    }

    /**
     * 综合文件内容和文件名检测 MIME 类型，并在无法识别时使用声明的 MIME 类型兜底
     * <p>
     * 检测策略：
     * <ol>
     *   <li>优先使用扩展名 + 文件内容综合检测，防止扩展名/Content-Type 伪造；</li>
     *   <li>当仅能识别为 {@link #APPLICATION_OCTET_STREAM}（未知二进制流）时，
     *       若调用方提供了可信度更高的声明值（非空且非 octet-stream），则采用声明值兜底。</li>
     * </ol>
     * 适用于文件上传等场景：{@code fallbackMimeType} 通常取自客户端声明的 Content-Type。
     *
     * @param data             文件内容字节数组
     * @param fileName         文件名
     * @param fallbackMimeType 无法识别时的兜底 MIME 类型，可为空
     * @return MIME 类型，最终兜底返回 {@link #APPLICATION_OCTET_STREAM}
     */
    public static String getMimeType(byte[] data, String fileName, String fallbackMimeType) {
        String mimeType = getMimeType(data, fileName);
        if (APPLICATION_OCTET_STREAM.equals(mimeType)
                && StringUtils.isNotBlank(fallbackMimeType)
                && !APPLICATION_OCTET_STREAM.equalsIgnoreCase(fallbackMimeType)) {
            return fallbackMimeType;
        }
        return mimeType;
    }

    /**
     * 根据 MIME 类型获取对应的文件扩展名
     *
     * @param mimeType MIME 类型
     * @return 扩展名（包含点号，如 ".pdf"），获取失败返回 null
     */
    public static String getExtension(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            return null;
        }
        String ext = MIME_EXT_MAP.get(mimeType);
        return ext == null ? null : "." + ext;
    }

    /**
     * 从文件名提取扩展名（小写、无点号）
     *
     * @param fileName 文件名
     * @return 扩展名，无扩展名返回 null
     */
    private static String extractExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        int slashIdx = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        String name = slashIdx >= 0 ? fileName.substring(slashIdx + 1) : fileName;
        int dotIdx = name.lastIndexOf('.');
        if (dotIdx < 0 || dotIdx == name.length() - 1) {
            return null;
        }
        return name.substring(dotIdx + 1).toLowerCase();
    }

    /**
     * 基于文件头 magic number 检测 MIME 类型
     *
     * @param data 文件内容
     * @return MIME 类型，未命中返回 null
     */
    private static String detectByMagic(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        for (MagicRule rule : MAGIC_RULES) {
            if (rule.matches(data)) {
                return rule.mimeType;
            }
        }
        return null;
    }

    /**
     * Magic Number 规则
     */
    private static final class MagicRule {
        final byte[] magic;
        final String mimeType;

        MagicRule(byte[] magic, String mimeType) {
            this.magic = magic;
            this.mimeType = mimeType;
        }

        boolean matches(byte[] data) {
            if (data.length < magic.length) {
                return false;
            }
            for (int i = 0; i < magic.length; i++) {
                if (data[i] != magic[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}