package com.tdtech.cloudcmd.icp.proxy.util;

import com.tdtech.cloudcmd.util.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 文件附件工具类 - 处理文件上传和下载
 *
 * 文件存储路径: {basePath}/{timeId}_{originalFilename}
 * 文件命名格式: {yyyyMMddHHmmss+4random}_{originalFilename}
 */
@Slf4j
public class AttachmentUtil {

    /**
     * 默认缓冲区大小
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 时间格式化器：yyyyMMddHHmmss
     */
    private static final DateTimeFormatter TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 上传文件到本地
     *
     * @param file     上传的文件
     * @param basePath 存储路径
     * @return 上传结果信息
     */
    public static UploadResult upload(MultipartFile file, String basePath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        try {
            // 1. 获取文件信息
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            long fileSize = file.getSize();
            Long fileId = generateFileId();

            // 2. 生成存储文件名：雪花ID_原文件名
            String storageFileName = generateStorageFileName(fileId, originalFilename);

            // 3. 创建存储目录
            createDirectoryIfNotExists(basePath);

            // 4. 保存文件到本地
            Path filePath = Paths.get(basePath, storageFileName);
            file.transferTo(filePath.toFile());
            log.info("附件上传成功: {}", filePath);

            // 5. 返回上传结果
            UploadResult result = new UploadResult();
            result.setFileId(fileId);
            result.setOriginalFilename(originalFilename);
            result.setStorageFileName(storageFileName);
            result.setFilePath(filePath.toString());
            result.setContentType(contentType);
            result.setFileSize(fileSize);
            return result;

        } catch (IOException e) {
            log.error("附件上传失败: fileName={}", file.getOriginalFilename(), e);
            throw new RuntimeException("附件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传字节数组到本地
     *
     * @param content     文件内容
     * @param originalName 原始文件名
     * @param contentType 文件类型
     * @param basePath    存储路径
     * @return 上传结果信息
     */
    public static UploadResult upload(byte[] content, String originalName, String contentType, String basePath) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("文件内容不能为空");
        }

        try {
            // 1. 生成文件信息
            Long fileId = generateFileId();
            String storageFileName = generateStorageFileName(fileId, originalName);

            // 2. 创建存储目录
            createDirectoryIfNotExists(basePath);

            // 3. 保存文件到本地
            Path filePath = Paths.get(basePath, storageFileName);
            Files.write(filePath, content);
            log.info("附件上传成功: {}", filePath);

            // 4. 返回上传结果
            UploadResult result = new UploadResult();
            result.setFileId(fileId);
            result.setOriginalFilename(originalName);
            result.setStorageFileName(storageFileName);
            result.setFilePath(filePath.toString());
            result.setContentType(contentType);
            result.setFileSize(content.length);
            return result;

        } catch (IOException e) {
            log.error("附件上传失败: fileName={}", originalName, e);
            throw new RuntimeException("附件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 上传输入流到本地
     *
     * @param inputStream  输入流
     * @param originalName 原始文件名
     * @param contentType  文件类型
     * @param basePath     存储路径
     * @return 上传结果信息
     */
    public static UploadResult upload(InputStream inputStream, String originalName, String contentType, String basePath) {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }

        try {
            // 1. 生成文件信息
            Long fileId = generateFileId();
            String storageFileName = generateStorageFileName(fileId, originalName);

            // 2. 创建存储目录
            createDirectoryIfNotExists(basePath);

            // 3. 保存文件到本地
            Path filePath = Paths.get(basePath, storageFileName);
            long fileSize = copyStreamToFile(inputStream, filePath);
            log.info("附件上传成功: {}", filePath);

            // 4. 返回上传结果
            UploadResult result = new UploadResult();
            result.setFileId(fileId);
            result.setOriginalFilename(originalName);
            result.setStorageFileName(storageFileName);
            result.setFilePath(filePath.toString());
            result.setContentType(contentType);
            result.setFileSize(fileSize);
            return result;

        } catch (IOException e) {
            log.error("附件上传失败: fileName={}", originalName, e);
            throw new RuntimeException("附件上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件（写入响应流）
     *
     * @param filePath 文件路径
     * @param response HTTP响应
     */
    public static void download(String filePath, HttpServletResponse response) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        try {
            // 1. 解析原始文件名（移除雪花ID前缀）
            String originalFilename = parseOriginalFilename(path.getFileName().toString());

            // 2. 设置响应头
            setDownloadResponseHeaders(response, originalFilename, Files.size(path));

            // 3. 写入响应流
            try (InputStream is = Files.newInputStream(path);
                 OutputStream os = response.getOutputStream()) {
                copyStream(is, os);
            }

            log.info("附件下载成功: {}", filePath);

        } catch (IOException e) {
            log.error("附件下载失败: {}", filePath, e);
            throw new RuntimeException("附件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件（写入响应流，指定文件名）
     *
     * @param filePath     文件路径
     * @param filename     下载文件名
     * @param response     HTTP响应
     */
    public static void download(String filePath, String filename, HttpServletResponse response) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        try {
            // 1. 设置响应头
            setDownloadResponseHeaders(response, filename, Files.size(path));

            // 2. 写入响应流
            try (InputStream is = Files.newInputStream(path);
                 OutputStream os = response.getOutputStream()) {
                copyStream(is, os);
            }

            log.info("附件下载成功: {}", filePath);

        } catch (IOException e) {
            log.error("附件下载失败: {}", filePath, e);
            throw new RuntimeException("附件下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件（返回字节数组）
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static byte[] downloadAsBytes(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载文件（返回输入流）
     *
     * @param filePath 文件路径
     * @return 文件输入流
     */
    public static InputStream downloadAsStream(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            log.error("读取文件失败: {}", filePath, e);
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 在线预览文件（写入响应流，不下载）
     *
     * @param filePath    文件路径
     * @param contentType 文件类型
     * @param response    HTTP响应
     */
    public static void preview(String filePath, String contentType, HttpServletResponse response) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new RuntimeException("文件不存在: " + filePath);
        }

        try {
            // 1. 设置响应头（inline 表示在线预览）
            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            response.setContentLengthLong(Files.size(path));
            response.setHeader("Content-Disposition", "inline");

            // 2. 写入响应流
            try (InputStream is = Files.newInputStream(path);
                 OutputStream os = response.getOutputStream()) {
                copyStream(is, os);
            }

            log.info("附件预览成功: {}", filePath);

        } catch (IOException e) {
            log.error("附件预览失败: {}", filePath, e);
            throw new RuntimeException("附件预览失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean delete(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }

        try {
            Path path = Paths.get(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                log.info("附件删除成功: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.warn("附件删除失败: {}", filePath, e);
            return false;
        }
    }

    /**
     * 删除目录及其下所有文件
     *
     * @param dirPath 目录路径
     * @return 是否删除成功
     */
    public static boolean deleteDirectory(String dirPath) {
        if (StringUtils.isBlank(dirPath)) {
            return false;
        }

        try {
            Path path = Paths.get(dirPath);
            if (!Files.exists(path)) {
                return true;
            }

            // 递归删除目录下所有文件
            try (java.util.stream.Stream<Path> walk = Files.walk(path)) {
                walk.sorted((a, b) -> -a.compareTo(b)) // 先删除文件，再删除目录
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                log.warn("删除失败: {}", p, e);
                            }
                        });
            }

            log.info("目录删除成功: {}", dirPath);
            return true;
        } catch (IOException e) {
            log.warn("目录删除失败: {}", dirPath, e);
            return false;
        }
    }

    /**
     * 检查文件是否存在
     *
     * @param filePath 文件路径
     * @return 是否存在
     */
    public static boolean exists(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }
        return Files.exists(Paths.get(filePath));
    }

    /**
     * 获取文件大小
     *
     * @param filePath 文件路径
     * @return 文件大小（字节）
     */
    public static long getFileSize(String filePath) {
        if (!exists(filePath)) {
            return 0;
        }
        try {
            return Files.size(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("获取文件大小失败: {}", filePath, e);
            return 0;
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（包含点号）
     */
    public static String getExtension(String filename) {
        if (StringUtils.isBlank(filename)) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot);
        }
        return "";
    }

    /**
     * 获取文件 MIME 类型
     *
     * @param filePath 文件路径
     * @return MIME 类型
     */
    public static String getMimeType(String filePath) {
        if (!exists(filePath)) {
            return "application/octet-stream";
        }
        try {
            return Files.probeContentType(Paths.get(filePath));
        } catch (IOException e) {
            log.warn("获取MIME类型失败: {}", filePath, e);
            return "application/octet-stream";
        }
    }

    /**
     * 生成文件ID（时间格式 + 随机数）
     * 格式：yyyyMMddHHmmss + 4位随机数
     * 示例：2026052011201234
     *
     * @return 文件ID
     */
    private static Long generateFileId() {
        String timeStr = LocalDateTime.now().format(TIME_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(10000);
        return Long.parseLong(timeStr + String.format("%04d", random));
    }

    /**
     * 生成存储文件名
     *
     * @param fileId           文件ID
     * @param originalFilename 原始文件名
     * @return 存储文件名
     */
    private static String generateStorageFileName(Long fileId, String originalFilename) {
        return fileId + "_" + (originalFilename != null ? originalFilename : "file");
    }

    /**
     * 解析原始文件名（移除雪花ID前缀）
     *
     * @param storageFileName 存储文件名
     * @return 原始文件名
     */
    public static String parseOriginalFilename(String storageFileName) {
        if (StringUtils.isBlank(storageFileName)) {
            return storageFileName;
        }
        int underscoreIndex = storageFileName.indexOf('_');
        if (underscoreIndex > 0 && underscoreIndex < storageFileName.length() - 1) {
            return storageFileName.substring(underscoreIndex + 1);
        }
        return storageFileName;
    }

    /**
     * 创建目录（如果不存在）
     *
     * @param dirPath 目录路径
     */
    private static void createDirectoryIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log.info("创建目录: {}", dirPath);
        }
    }

    /**
     * 设置下载响应头
     *
     * @param response HTTP响应
     * @param filename 文件名
     * @param fileSize 文件大小
     */
    private static void setDownloadResponseHeaders(HttpServletResponse response, String filename, long fileSize)
            throws UnsupportedEncodingException {
        response.setContentType("application/octet-stream");
        response.setContentLengthLong(fileSize);
        // 中文文件名需要 URL 编码
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"");
    }

    /**
     * 复制流
     *
     * @param is 输入流
     * @param os 输出流
     */
    private static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
    }

    /**
     * 复制流到文件
     *
     * @param is       输入流
     * @param filePath 文件路径
     * @return 文件大小
     */
    private static long copyStreamToFile(InputStream is, Path filePath) throws IOException {
        try (OutputStream os = Files.newOutputStream(filePath)) {
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            os.flush();
            return totalBytes;
        }
    }

    /**
     * 上传结果
     */
    @Data
    public static class UploadResult {
        private Long fileId;
        private String originalFilename;
        private String storageFileName;
        private String filePath;
        private String fileUrl;
        private String contentType;
        private long fileSize;
    }
}
