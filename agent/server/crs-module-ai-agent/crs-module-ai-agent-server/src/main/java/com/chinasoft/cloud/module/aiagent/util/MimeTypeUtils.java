package com.chinasoft.cloud.module.aiagent.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * MIME 类型工具类
 * 根据文件扩展名获取对应的 MIME 类型
 */
public final class MimeTypeUtils {

    private MimeTypeUtils() {
    }

    // 图片类型
    public static final String IMAGE_JPEG = "image/jpeg";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_GIF = "image/gif";
    public static final String IMAGE_BMP = "image/bmp";
    public static final String IMAGE_WEBP = "image/webp";
    public static final String IMAGE_SVG = "image/svg+xml";

    // 视频类型
    public static final String VIDEO_MP4 = "video/mp4";
    public static final String VIDEO_MOV = "video/quicktime";
    public static final String VIDEO_WEBM = "video/webm";
    public static final String VIDEO_MPEG = "video/mpeg";

    // 音频类型
    public static final String AUDIO_MPEG = "audio/mpeg";
    public static final String AUDIO_AAC = "audio/aac";
    public static final String AUDIO_WAV = "audio/wav";
    public static final String AUDIO_PCM = "audio/pcm";
    public static final String AUDIO_AMR = "audio/amr";
    public static final String AUDIO_M4A = "audio/mp4";

    // 文档类型
    public static final String APPLICATION_PDF = "application/pdf";
    public static final String APPLICATION_DOC = "application/msword";
    public static final String APPLICATION_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    public static final String APPLICATION_XLS = "application/vnd.ms-excel";
    public static final String APPLICATION_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String APPLICATION_PPT = "application/vnd.ms-powerpoint";
    public static final String APPLICATION_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    public static final String APPLICATION_EPUB = "application/epub+zip";
    public static final String APPLICATION_EML = "message/rfc822";
    public static final String APPLICATION_MSG = "application/vnd.ms-outlook";

    // 文本类型
    public static final String TEXT_PLAIN = "text/plain";
    public static final String TEXT_HTML = "text/html";
    public static final String TEXT_MARKDOWN = "text/markdown";
    public static final String TEXT_CSV = "text/csv";

    // 数据类型
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_YAML = "application/x-yaml";

    // 通用类型
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    // 扩展名到MIME类型映射表
    private static final Map<String, String> MIME_TYPE_MAP = Map.ofEntries(
        // 图片
        Map.entry(".jpg", IMAGE_JPEG),
        Map.entry(".jpeg", IMAGE_JPEG),
        Map.entry(".png", IMAGE_PNG),
        Map.entry(".gif", IMAGE_GIF),
        Map.entry(".bmp", IMAGE_BMP),
        Map.entry(".webp", IMAGE_WEBP),
        Map.entry(".svg", IMAGE_SVG),

        // 视频
        Map.entry(".mp4", VIDEO_MP4),
        Map.entry(".mov", VIDEO_MOV),
        Map.entry(".webm", VIDEO_WEBM),
        Map.entry(".mpeg", VIDEO_MPEG),
        Map.entry(".mpga", AUDIO_MPEG),

        // 音频
        Map.entry(".mp3", AUDIO_MPEG),
        Map.entry(".aac", AUDIO_AAC),
        Map.entry(".wav", AUDIO_WAV),
        Map.entry(".pcm", AUDIO_PCM),
        Map.entry(".amr", AUDIO_AMR),
        Map.entry(".m4a", AUDIO_M4A),

        // 文档
        Map.entry(".pdf", APPLICATION_PDF),
        Map.entry(".doc", APPLICATION_DOC),
        Map.entry(".docx", APPLICATION_DOCX),
        Map.entry(".xls", APPLICATION_XLS),
        Map.entry(".xlsx", APPLICATION_XLSX),
        Map.entry(".ppt", APPLICATION_PPT),
        Map.entry(".pptx", APPLICATION_PPTX),
        Map.entry(".epub", APPLICATION_EPUB),
        Map.entry(".eml", APPLICATION_EML),
        Map.entry(".msg", APPLICATION_MSG),

        // 文本
        Map.entry(".txt", TEXT_PLAIN),
        Map.entry(".html", TEXT_HTML),
        Map.entry(".htm", TEXT_HTML),
        Map.entry(".md", TEXT_MARKDOWN),
        Map.entry(".markdown", TEXT_MARKDOWN),
        Map.entry(".csv", TEXT_CSV),

        // 数据
        Map.entry(".json", APPLICATION_JSON),
        Map.entry(".xml", APPLICATION_XML),
        Map.entry(".yaml", APPLICATION_YAML),
        Map.entry(".yml", APPLICATION_YAML)
    );

    /**
     * 根据文件名获取 MIME 类型
     *
     * @param fileName 文件名
     * @return MIME 类型
     */
    public static String getMimeType(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return APPLICATION_OCTET_STREAM;
        }

        String ext = getFileExtension(fileName).toLowerCase();
        return MIME_TYPE_MAP.getOrDefault(ext, APPLICATION_OCTET_STREAM);
    }

    /**
     * 获取文件扩展名（包含点号）
     *
     * @param fileName 文件名
     * @return 扩展名，如 ".jpg"、".png"
     */
    public static String getFileExtension(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot);
    }
}
