package com.tdtech.cloudcmd.encryptor.service.impl;

import cloudcmd.dto.SystemConfigDto;
import cloudcmd.service.rpc.SystemConfigRpcService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.tdtech.cloudcmd.encryptor.service.CachedConfig;
import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.web.utils.HttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.tdtech.cloudcmd.encryptor.constants.Constants.*;

/**
 * 国密加密服务封装类
 * 提供字符串加解密接口
 * <p>
 * SDK返回字节数组，使用Base64编码转换为字符串存储
 * <p>
 * 配置获取方式：
 * 1. 加密开关：从 CachedConfig 获取（带缓存）
 * 2. 服务地址：从 CachedConfig 获取
 */
@Slf4j
@Service
public class EncryptionServiceImpl implements EncryptionService {

    public static final String LOCK_PREFIX = "im:encrypt:lock:";

    @DubboReference
    private SystemConfigRpcService configRpcService;

    @Autowired
    private CachedConfig cachedConfig;

    @Autowired
    private HttpClient httpClient;

    @Autowired
    private RedisLockFactory redisLockFactory;

    /**
     * 获取加密开关状态
     * 从 CachedConfig 获取，自动缓存
     */
    @Override
    public boolean encryptEnabled() {
        // 若获取开关报错, 则错误外抛，避免部署后开个同时存在两种状态
        String config = cachedConfig.getSystemConfig(ENABLE_ENCRYPT);
        return StringUtils.isNotBlank(config) && Boolean.parseBoolean(config);
    }

    /**
     * 获取linkx-encryptor服务地址
     * 从 CachedConfig 获取
     */
    private String getLinkxEncryptorUrl() {
        String url = cachedConfig.getGlobalConfig(LINKX_ENCRYPTOR_URL);
        if (org.apache.commons.lang.StringUtils.isEmpty(url)) {
            throw new RuntimeException("未配置 linkx-encryptor 服务地址: " + LINKX_ENCRYPTOR_URL);
        }
        return url;
    }

    /**
     * 加密字符串
     *
     * @param plaintext 明文
     * @return 密文
     */
    public String encrypt(String plaintext) {
        if (!encryptEnabled() || org.apache.commons.lang.StringUtils.isEmpty(plaintext)) {
            return plaintext;
        }

        try {
            // 构造请求 URL
            String url = getLinkxEncryptorUrl() + SM4_ENCRYPT_URI;
            
            // 构造请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("data", plaintext);
            
            // 发送请求
            Map<String, Object> response = httpClient.postJson(
                URI.create(url), 
                null, 
                requestBody, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            if (response != null) {
                Integer code = (Integer) response.get("code");
                if (code != null && code == 0) {
                    return (String) response.get("data");
                } else {
                    String msg = (String) response.get("msg");
                    log.error("SM2 加密失败: {}", msg);
                    throw new RuntimeException("SM2 加密失败: " + msg);
                }
            } else {
                throw new RuntimeException("SM2 加密服务调用失败，响应为空");
            }
        } catch (Exception e) {
            log.error("加密失败: {}", plaintext, e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密字符串
     *
     * @param ciphertext 密文
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (!encryptEnabled() || org.apache.commons.lang.StringUtils.isEmpty(ciphertext)) {
            return ciphertext;
        }

        try {
            // 构造请求 URL
            String url = getLinkxEncryptorUrl() + SM4_DECRYPT_URI;
            
            // 构造请求体
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("data", ciphertext);
            
            // 发送请求
            Map<String, Object> response = httpClient.postJson(
                URI.create(url), 
                null, 
                requestBody, 
                new TypeReference<Map<String, Object>>() {}
            );
            
            if (response != null) {
                Integer code = (Integer) response.get("code");
                if (code != null && code == 0) {
                    return (String) response.get("data");
                } else {
                    String msg = (String) response.get("msg");
                    log.error("SM2 解密失败: {}", msg);
                    throw new RuntimeException("SM2 解密失败: " + msg);
                }
            } else {
                throw new RuntimeException("SM2 解密服务调用失败，响应为空");
            }
        } catch (Exception e) {
            log.error("解密失败: {}", ciphertext, e);
            throw new RuntimeException("解密失败", e);
        }
    }

    @Override
    public void enable() {
        configRpcService.saveOrUpdate(new SystemConfigDto(ENABLE_ENCRYPT, Boolean.TRUE.toString()));
        cachedConfig.clearSystemConfig();
    }

    @Override
    public void disable() {
        configRpcService.saveOrUpdate(new SystemConfigDto(ENABLE_ENCRYPT, Boolean.FALSE.toString()));
        cachedConfig.clearSystemConfig();
    }

    @Override
    public String encryptFile(String filePath, String encFileDir) {
        if (!encryptEnabled()) {
            return filePath;
        }

        try {
            // 临时文件不存在，则跳过
            Path inputFile = Paths.get(filePath);
            if (!Files.exists(inputFile)) {
                log.warn("文件不存在: {}", filePath);
                return filePath;
            }

            Path encryptDir = Paths.get(encFileDir);
            Files.createDirectories(encryptDir);

            String fileName = inputFile.getFileName().toString();
            long fileSize = Files.size(inputFile);

            String encryptedFilePath;
            if (fileSize > FILE_SIZE_THRESHOLD) {
                // 大文件：分块加密
                encryptedFilePath = encryptChunkedFile(inputFile, encryptDir, fileName);
            } else {
                // 小文件：整体加密
                encryptedFilePath = encryptSingleFile(inputFile, encryptDir, fileName);
            }
            log.info("成功加密文件: fileName={}, size={}, isChunked={}", 
                    fileName, fileSize, fileSize > FILE_SIZE_THRESHOLD);
            return encryptedFilePath;
        } catch (Exception e) {
            log.error("加密文件失败: filePath={}", filePath, e);
            throw new RuntimeException("加密文件失败", e);
        }
    }

    @Override
    public String decryptFile(String filePath, String decFileDir) {
        if (!encryptEnabled()) {
            return filePath;
        }

        try {
            Path inputFile = Paths.get(filePath);
            if (!Files.exists(inputFile)) {
                log.warn("文件不存在: {}", filePath);
                return filePath;
            }

            Path decryptDir = Paths.get(decFileDir);
            Files.createDirectories(decryptDir);

            // 文件名格式：fileName.enc 或 fileName.chunk0.enc
            String fileName = inputFile.getFileName().toString();
            
            // 判断是否为分块文件
            String decryptedFileName;
            if (fileName.endsWith(".meta")) {
                // 分块文件解密
                String originalFileName = fileName.substring(0, fileName.length() - 5);
                decryptedFileName = decryptChunkedFile(inputFile.getParent(), decryptDir, originalFileName);
            } else if (fileName.endsWith(".enc")) {
                // 单文件解密
                String originalFileName = fileName.substring(0, fileName.length() - 4);
                decryptedFileName = decryptSingleFile(inputFile, decryptDir.resolve(originalFileName));
            } else {
                log.warn("不支持的加密文件格式: {}", fileName);
                decryptedFileName = fileName;
            }

            log.info("成功解密文件: {}", fileName);
            return decryptedFileName;
        } catch (Exception e) {
            log.error("解密文件失败: filePath={}", filePath, e);
            throw new RuntimeException("解密文件失败", e);
        }
    }

    @Override
    public String getEncryptFilePath(String fileName, String encFileDir) {
        try {
            Path encDir = Paths.get(encFileDir);
            String rawFilePath = encDir.resolve(fileName).toFile().getCanonicalPath();
            if (!encryptEnabled()) {
                // 未开启加密，加密文件路径则为 fileName
                return rawFilePath;
            }

            // 小文件，加密文件路径则为 fileName.enc
            Path encFile = encDir.resolve(fileName + ".enc");
            if (Files.exists(encFile)) {
                return encFile.toFile().getCanonicalPath();
            }
            // 大文件，加密文件路径则为 fileName
            return rawFilePath;
        } catch (Exception e) {
            log.error("获取加密文件路径失败: fileName={}", fileName, e);
            throw new RuntimeException("获取加密文件路径失败", e);
        }
    }

    /**
     * 加密单个文件（小于50MB）
     * 使用 HTTP 流式加密接口
     */
    private String encryptSingleFile(Path inputFile, Path encryptDir, String fileName) throws IOException {
        Path encryptedFile = encryptDir.resolve(fileName + ".enc");
        
        // 使用 HTTP 流式加密接口
        String encryptedFilePath = encryptFileViaHttp(inputFile, encryptedFile);
        
        // 清理可能存在的分块文件和元数据文件
        cleanupChunkFiles(encryptDir, fileName);
        return encryptedFilePath;
    }

    /**
     * 分块加密文件（大于50MB）
     * 使用 HTTP 流式加密接口对每个分块进行加密
     */
    private String encryptChunkedFile(Path inputFile, Path encryptDir, String fileName) throws IOException {
        // 文件读写全局锁
        String lockKey = getRedisLockKey(inputFile);
        var lock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(5));
        if (lock.tryLock(5L, TimeUnit.MINUTES)) {
            try {
                long fileSize = Files.size(inputFile);
                int chunkCount = (int) Math.ceil((double) fileSize / CHUNK_SIZE);

                log.info("开始分块加密: fileName={}, fileSize={}, chunkCount={}", fileName, fileSize, chunkCount);

                List<String> metaLines = new ArrayList<>();
                metaLines.add(String.valueOf(chunkCount));
                metaLines.add(String.valueOf(fileSize)); // 记录原始文件大小，用于 Range 请求

                try (InputStream is = Files.newInputStream(inputFile)) {
                    byte[] buffer = new byte[CHUNK_SIZE];
                    int bytesRead;
                    int chunkIndex = 0;

                    while ((bytesRead = is.read(buffer)) > 0) {
                        // 读取实际数据
                        byte[] chunkData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

                        // 写入临时分块文件
                        Path tempChunkFile = encryptDir.resolve(fileName + ".chunk" + chunkIndex + ".tmp");
                        Files.write(tempChunkFile, chunkData);

                        // 使用 HTTP 流式加密接口加密分块
                        Path encryptedChunkFile = encryptDir.resolve(fileName + ".chunk" + chunkIndex + ".enc");
                        encryptFileViaHttp(tempChunkFile, encryptedChunkFile);

                        // 删除临时文件
                        Files.deleteIfExists(tempChunkFile);

                        chunkIndex++;
                    }
                }

                // 写入元数据文件
                Path metaFile = encryptDir.resolve(fileName + ".meta");
                Files.write(metaFile, metaLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // 删除可能存在的单文件加密文件
                Path encryptedFile = encryptDir.resolve(fileName + ".enc");
                Files.deleteIfExists(encryptedFile);

                log.info("分块加密完成: fileName={}, chunkCount={}", fileName, chunkCount);
                return encryptDir.resolve(fileName).toFile().getCanonicalPath();
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("获取写锁失败: lockKey={}", lockKey);
        }
        throw new RuntimeException("分块加密文件失败: " + fileName);
    }

    /**
     * 解密单个文件
     * 使用 HTTP 流式解密接口
     */
    private String decryptSingleFile(Path encryptedFile, Path outputFile) throws IOException {
        // 使用 HTTP 流式解密接口
        return decryptFileViaHttp(encryptedFile, outputFile);
    }

    /**
     * 解密分块文件
     * 使用 HTTP 流式解密接口对每个分块进行解密
     */
    private String decryptChunkedFile(Path encryptDir, Path decryptDir, String fileName) throws IOException {
        // 文件读写全局锁
        Path rawFile = encryptDir.resolve(fileName);
        String lockKey = getRedisLockKey(rawFile);
        var lock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(5));
        if (lock.tryLock(5L, TimeUnit.MINUTES)) {
            try {
                // 读取元数据文件
                Path metaFile = encryptDir.resolve(fileName + ".meta");
                if (!Files.exists(metaFile)) {
                    log.warn("元数据文件不存在: {}", metaFile);
                    return StringUtils.EMPTY;
                }

                List<String> metaLines = Files.readAllLines(metaFile);
                int chunkCount = Integer.parseInt(metaLines.get(0));

                Path outputFile = decryptDir.resolve(fileName);
                try (OutputStream os = Files.newOutputStream(outputFile)) {
                    for (int i = 0; i < chunkCount; i++) {
                        Path chunkFile = encryptDir.resolve(fileName + ".chunk" + i + ".enc");
                        if (Files.exists(chunkFile)) {
                            // 使用 HTTP 流式解密接口解密分块
                            Path tempDecryptedFile = decryptDir.resolve(fileName + ".chunk" + i + ".tmp");
                            decryptFileViaHttp(chunkFile, tempDecryptedFile);

                            // 读取解密后的数据并写入输出流
                            byte[] chunkData = Files.readAllBytes(tempDecryptedFile);
                            os.write(chunkData);

                            // 删除临时文件
                            Files.deleteIfExists(tempDecryptedFile);
                        }
                    }
                }
                log.debug("分块解密完成: {} 个分块", chunkCount);
                return outputFile.toFile().getCanonicalPath();
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("获取写锁失败: lockKey={}", lockKey);
        }
        throw new RuntimeException("分块解密文件失败: " + fileName);
    }

    /**
     * 通过 HTTP 流式接口加密文件
     */
    private String encryptFileViaHttp(Path inputFile, Path outputFile) throws IOException {
        String url = getLinkxEncryptorUrl() + SM4_STREAM_ENCRYPT_URI;

        try {
            // 构建 multipart/form-data 请求
            byte[] fileData = Files.readAllBytes(inputFile);
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            
            // 构建 multipart body
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + inputFile.getFileName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write("Content-Type: application/octet-stream\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write(fileData);
            baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            
            byte[] multipartBody = baos.toByteArray();
            
            // 发送请求
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(multipartBody);
            HttpResponse<byte[]> response = httpClient.sendRequest(
                URI.create(url),
                HttpClient.HttpMethodEnum.POST,
                headers,
                bodyPublisher,
                "multipart/form-data; boundary=" + boundary,
                Duration.ofMinutes(30)
            );
            
            if (response.statusCode() == 200 && response.body() != null) {
                // 文件读写全局锁
                String lockKey = getRedisLockKey(outputFile);
                var lock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(5));
                if (lock.tryLock(5L, TimeUnit.MINUTES)) {
                    try {
                        Files.write(outputFile, response.body());
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.warn("获取写锁失败: lockKey={}", lockKey);
                }
            } else {
                throw new RuntimeException("流式加密失败，状态码: " + response.statusCode());
            }
            return outputFile.toFile().getCanonicalPath();
        } catch (Exception e) {
            log.error("通过HTTP流式加密文件失败: {}", inputFile, e);
            throw new IOException("流式加密失败", e);
        }
    }

    /**
     * 获取分布式锁的key
     */
    private String getRedisLockKey(Path file) throws IOException {
        return LOCK_PREFIX + ":" + DigestUtils.md5Hex(file.toFile().getCanonicalPath());
    }

    /**
     * 通过 HTTP 流式接口解密文件
     */
    private String decryptFileViaHttp(Path inputFile, Path outputFile) throws IOException {
        String url = getLinkxEncryptorUrl() + SM4_STREAM_DECRYPT_URI;
        
        try {
            // 构建 multipart/form-data 请求
            byte[] fileData = Files.readAllBytes(inputFile);
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            
            // 构建 multipart body
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + inputFile.getFileName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
            baos.write("Content-Type: application/octet-stream\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
            baos.write(fileData);
            baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            
            byte[] multipartBody = baos.toByteArray();
            
            // 发送请求
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
            
            HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(multipartBody);
            HttpResponse<byte[]> response = httpClient.sendRequest(
                URI.create(url),
                HttpClient.HttpMethodEnum.POST,
                headers,
                bodyPublisher,
                "multipart/form-data; boundary=" + boundary,
                Duration.ofMinutes(30)
            );
            
            if (response.statusCode() == 200 && response.body() != null) {
                // 文件读写全局锁
                String lockKey = getRedisLockKey(outputFile);
                var lock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(5));
                if (lock.tryLock(5L, TimeUnit.MINUTES)) {
                    try {
                        Files.write(outputFile, response.body());
                    } finally {
                        lock.unlock();
                    }
                } else {
                    log.warn("获取写锁失败: lockKey={}", lockKey);
                }
            } else {
                throw new RuntimeException("流式解密失败，状态码: " + response.statusCode());
            }
            return outputFile.toFile().getCanonicalPath();
        } catch (Exception e) {
            log.error("通过HTTP流式解密文件失败: {}", inputFile, e);
            throw new IOException("流式解密失败", e);
        }
    }

    /**
     * 清理分块文件和元数据文件
     */
    private void cleanupChunkFiles(Path encryptDir, String fileName) throws IOException {
        // 文件读写全局锁
        Path rawFile = encryptDir.resolve(fileName);
        String lockKey = getRedisLockKey(rawFile);
        var lock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(5));
        if (lock.tryLock(5L, TimeUnit.MINUTES)) {
            try {
                // 删除元数据文件
                Path metaFile = encryptDir.resolve(fileName + ".meta");
                Files.deleteIfExists(metaFile);

                // 删除所有分块文件
                int index = 0;
                while (true) {
                    Path chunkFile = encryptDir.resolve(fileName + ".chunk" + index + ".enc");
                    if (!Files.exists(chunkFile)) {
                        break;
                    }
                    Files.delete(chunkFile);
                    index++;
                }
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("获取写锁失败: lockKey={}", lockKey);
        }
    }
}
