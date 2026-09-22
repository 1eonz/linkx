package com.tdtech.cloudcmd.im.jingxin.server.util;

import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.server.mvc.EncryptResource;
import com.tdtech.cloudcmd.redis.RedisLockFactory;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.tdtech.cloudcmd.encryptor.service.impl.EncryptionServiceImpl.LOCK_PREFIX;
import static com.tdtech.cloudcmd.im.jingxin.server.enums.Constant.ENCRYPT_DIR;
import static com.tdtech.cloudcmd.im.jingxin.server.service.impl.GroupExtendsServiceImpl.FILE_PATH_URL;

/**
 * im消息加解密工具类
 */
@Slf4j
@Component
public class EncryptImUtil {

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ImHttpClient imHttpClient;

    public static final String READ_TMP_DIR = "readtmp";
    public static final String WRITE_TMP_DIR = "writetmp";
    
    /**
     * 分块加密文件的正则模式
     * 匹配格式: xxx.chunk数字.enc
     */
    private static final Pattern CHUNK_FILE_PATTERN = Pattern.compile(".*chunk\\d+\\.enc$");

    @Autowired
    private RedisLockFactory redisLockFactory;

    public boolean encryptEnabled() {
        return encryptionService.encryptEnabled();
    }

    /**
     * 处理读模式
     * 在encrypt目录下查找加密文件，解密到readtmp目录
     */
    public String decryptFile(String filePath, String fileName,
                              String tempDir, String groupId) throws IOException {
        // 构建加密文件路径
        Path encryptDir = Paths.get(filePath);
        String encryptedFileName = fileName + ".enc";
        Path encryptedFile = encryptDir.resolve(encryptedFileName);

        // 检查是否为分块加密文件
        Path chunkMetaFile = encryptDir.resolve(fileName + ".meta");
        boolean isChunked = Files.exists(chunkMetaFile);

        // 如果加密文件不存在且不是分块文件，返回原路径
        if (!Files.exists(encryptedFile) && !isChunked) {
            log.debug("加密文件不存在，返回原路径: {}", encryptedFile);
            return encryptDir.resolve(fileName).toString();
        }

        // 构建临时目录
        Path readTmpDir = Paths.get(tempDir);
        Path tmpFile = readTmpDir.resolve(fileName);

        // 检查临时文件是否已存在
        if (Files.exists(tmpFile)) {
            log.debug("临时文件已存在且未过期，直接返回: {}", tmpFile);
            return tmpFile.toString();
        }

        // 获取读锁
        if (isChunked) {
            // 分块解密
            return encryptionService.decryptFile(chunkMetaFile.toFile().getCanonicalPath(), tempDir);
        } else {
            // 单文件解密
            return encryptionService.decryptFile(encryptedFile.toFile().getCanonicalPath(), tempDir);
        }
    }

    /**
     * 提交写入的数据，加密后存储到encrypt目录
     * 超过50MB的文件采用分块加密
     * <p>
     * 注意：
     * 1. 调用方必须保证在调用此方法前，临时文件已完整写入
     * 2. 本方法会读取临时文件，进行加密，然后存储到encrypt目录
     * 3. 加密完成后会删除临时文件
     * 4. 本方法不负责原文件的写入逻辑，只负责加密处理
     *
     * @param tmpPath 临时文件路径（由 getPath 返回的写模式路径）
     * @param groupId 群组ID
     */
    public void commitWrite(String tmpPath, String groupId) {
        if (!encryptionService.encryptEnabled()) {
            return;
        }

        try {
            Path tmpFile = Paths.get(tmpPath);
            if (!Files.exists(tmpFile)) {
                log.warn("临时文件不存在: {}", tmpPath);
                return;
            }
            // 上一级目录即为加密后数据应该存储的位置
            Path encryptDir = tmpFile.getParent().getParent();
            encryptionService.encryptFile(tmpFile.toFile().getCanonicalPath(),
                    encryptDir.toFile().getCanonicalPath());
            // 标记读临时的db文件过期，避免读取到旧数据
            String fileName = tmpFile.toFile().getName();
            if (fileName.endsWith(".db")) {
                String readTempDir = getTempPath(encryptDir.toFile().getCanonicalPath(), true);
                Path readTempFile = Paths.get(readTempDir).resolve(fileName);
                if (Files.notExists(readTempFile)) {
                    return;
                }
                // 标记读临时文件已过期
                markFileExpired(readTempFile);
            }
        } catch (Exception e) {
            log.error("提交写入数据失败: tmpPath={}, groupId={}", tmpPath, groupId, e);
        }
    }

    /**
     * 标记文件过期
     *
     * @param file 文件
     */
    private void markFileExpired(Path file) {
        try {
            File f = file.toFile();
            long newTime = System.currentTimeMillis() - 10 * 60 * 1000;
            boolean success = f.setLastModified(newTime);
            if (!success) {
                log.warn("标记文件过期失败: {}", file);
            }
        } catch (Exception e) {
            log.warn("标记文件过期异常: {}", file, e);
        }
    }

    /**
     * 获取临时目录路径，不存在自动创建
     *
     * @param groupPath
     * @param readMode
     * @return
     */
    public String getTempPath(String groupPath, boolean readMode) {
        String tempPath = readMode ? groupPath + File.separator + READ_TMP_DIR : groupPath + File.separator + WRITE_TMP_DIR;
        try {
            Path path = Paths.get(tempPath);
            if (Files.notExists(path)) {
                Files.createDirectories(path);
            }
        } catch (Exception e) {
            log.error("创建临时目录失败{}", tempPath);
            throw new RuntimeException("创建临时目录失败", e);
        }
        return tempPath;
    }

    public String getFileToRead(String groupPathStr, String fileName, String groupId) {
        String tempPathStr = getTempPath(groupPathStr, true);
        // 解密需要保证原始的文件名
        fileName = fileName.endsWith(".enc") ? fileName.substring(0, fileName.length() - 4) : fileName;
        return getDecryptFile(groupPathStr, fileName, groupId, tempPathStr);
    }

    public boolean isEncryptFileExists(String groupPath, String fileName) {
        Path path = Paths.get(groupPath);
        // 判断加密文件是否存在
        return Files.exists(path.resolve(fileName + ".enc"))
                || Files.exists(path.resolve(fileName + ".meta"));
    }

    public String getFileToWrite(String groupPathStr, String fileName, String groupId) {
        String tempPathStr = getTempPath(groupPathStr, false);
        try {
            // 尝试删除临时文件，避免无法更新
            Path tempFile = Paths.get(tempPathStr).resolve(fileName);
            Files.deleteIfExists(tempFile);
            // 若加密文件不存在，不做任何处理,直接返回写临时文件的路径
            if (!isEncryptFileExists(groupPathStr, fileName)) {
                return tempFile.toFile().getCanonicalPath();
            }
        } catch (Exception e) {
            log.error("删除临时文件失败: {}", tempPathStr + File.separator + fileName);
        }
        return getDecryptFile(groupPathStr, fileName, groupId, tempPathStr);
    }

    private String getDecryptFile(String groupPathStr, String fileName, String groupId, String tempPathStr) {
        Path tempPath = Paths.get(tempPathStr);
        // 解密后的文件存在，直接返回
        Path decFile = tempPath.resolve(fileName);
        try {
            if (Files.exists(decFile) && !isFileExpired(decFile)) {
                return decFile.toFile().getCanonicalPath();
            }
            // 过期则删除重新解密
            if (Files.exists(decFile) && isFileExpired(decFile)) {
                clearFile(decFile.toFile().getCanonicalPath());
            }

            // 解密加密文件
            return decryptFile(groupPathStr, fileName, tempPath.toFile().getCanonicalPath(), groupId);
        } catch (Exception e) {
            log.error("获取群组{}的im信息异常", groupId);
            throw new RuntimeException("读取群组im信息异常", e);
        }
    }

    /**
     * 获取群组存储数据的基础路径-仅添加encrypt层级
     * 注: 自动创建目录
     *
     * @param basePath
     * @param groupId
     * @return 目录地址
     */
    public String getGroupPath(String basePath, String groupId) {
        String groupPathStr = basePath + File.separator + groupId;
        // 未开启加密
        if (!encryptionService.encryptEnabled()) {
            return groupPathStr;
        }
        // 开启加密
        return groupPathStr + File.separator + ENCRYPT_DIR;

    }

    /**
     * 获取群组存储数据的基础路径-仅添加encrypt层级
     * 注: 自动创建目录
     *
     * @param basePath
     * @param groupId
     * @return 目录地址
     */
    public String getGroupPathAndCreate(String basePath, String groupId) {
        String groupPath = getGroupPath(basePath, groupId);
        try {
            Files.createDirectories(Paths.get(groupPath));
            return groupPath;
        } catch (Exception e) {
            log.error("创建群组加密目录失败{}", groupId);
            throw new RuntimeException("创建群组加密目录失败", e);
        }
    }

    public String downloadSaveImageById(String id, String fileId, String fileName, Long groupId, String dbpathGlobal) {
        // 定义本地文件路径 以唯一id作为文件名，防止每次归档数据越来越大
        String groupIdStr = groupId.toString();
        String groupPath = getGroupPath(dbpathGlobal, groupIdStr);
        String tempPath = getTempPath(groupPath, false);
        fileName = id + "_" + fileName;
        Path filePath = Path.of(tempPath, fileName);
        try {
            // 加密文件存在则直接返回
            if (isEncryptFileExists(groupPath, fileName)) {
                return FILE_PATH_URL + encryptionService.getEncryptFilePath(fileName, groupPath);
            }
            // 先清理临时文件
            Files.deleteIfExists(filePath);

            // 不存在，从im下载后存入目录
            byte[] downloaded = imHttpClient.downloadIcon(fileId);
            if (downloaded == null || downloaded.length == 0) {
                return StringUtils.EMPTY;
            } else {
                Files.write(filePath, downloaded);
                String encryptFilePath = encryptionService.encryptFile(filePath.toFile().getCanonicalPath(), groupPath);
                return FILE_PATH_URL + encryptFilePath;
            }
        } catch (Exception e) {
            log.error("下载附件{}失败", id, e);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 【流式解密】直接返回解密流，不写临时文件，支持大文件，不OOM
     *
     * @param encryptDir 加密文件所在目录
     * @param fileName   原始文件名
     * @return Resource  可直接在 Spring 中返回，自动流式响应
     */
    public Resource getDecryptedResource(String encryptDir, String fileName) {
        if (!encryptionService.encryptEnabled()) {
            // 未加密，直接返回原文件
            Path originalFile = Paths.get(encryptDir, fileName);
            return new org.springframework.core.io.FileSystemResource(originalFile);
        }

        Path encryptedFile = Paths.get(encryptDir, fileName);
        Path chunkMetaFile = Paths.get(encryptDir, fileName + ".meta");
        boolean isChunked = Files.exists(chunkMetaFile);

        return new EncryptResource(encryptDir, fileName, isChunked, encryptedFile, chunkMetaFile, encryptionService, this);
    }

    /**
     * 压缩群组文件为 ZIP 包，不支持多层目录，与现有存储相对应，自动解密
     * 重要: 仅支持群组归档文件压缩，仅支持加密文件压缩
     */
    public void compressGroupFile(String groupPath, String singleZipPath, String groupId) throws IOException {
        File sourceDir = new File(groupPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new FileNotFoundException("源文件夹不存在或不是目录：" + groupPath);
        }

        // 只使用 ZipOutputStream，避免嵌套 BufferedOutputStream
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(singleZipPath))) {
            log.info("zipFilePath:{}", singleZipPath);
            // 添加文件到 ZIP
            compressGroupFile(groupPath, groupId, groupId, zos);
        }
    }

    /**
     * @param groupPath    群组归档的加密目录
     * @param zipEntryName 群组zip名称
     * @param groupId      群组id
     * @param zos          压缩包流
     * @throws IOException 异常
     */
    private void compressGroupFile(String groupPath, String zipEntryName,
                                   String groupId, ZipOutputStream zos) throws IOException {
        // 添加文件夹条目（末尾需加 "/" 标识是目录）
        String dirEntryName = zipEntryName + "/";
        log.info("dirEntryName:{}", dirEntryName);

        // 遍历文件，跳过文件夹
        File[] children = Paths.get(groupPath).toFile().listFiles();
        if (children != null) {
            for (File child : children) {
                log.info("file:{}", child);
                if (child.isDirectory()) {
                    log.info("file:{}isDirectory, skip zip", child);
                    continue;
                }
                // 添加文件条目
                if (CHUNK_FILE_PATTERN.matcher(child.getName()).matches()) {
                    // 分块文件不压缩
                    log.info("file:{}isChunkFile, skip zip", child);
                    continue;
                }
                String fileName = child.getName();
                if (child.getName().endsWith(".meta")) {
                    // 分块文件,找原文件执行压缩
                    fileName = fileName.substring(0, fileName.length() - 5);
                }

                String fileToRead = getFileToRead(groupPath, fileName, groupId);
                File file = Paths.get(fileToRead).toFile();
                String fileEntryName = file.getName();
                zos.putNextEntry(new ZipEntry(fileEntryName)); // 为每个文件创建条目
                // 读取文件内容写入 ZIP
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[1024 * 8];
                    int len;
                    while ((len = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, len);
                    }
                }
                zos.closeEntry(); // 关闭当前文件条目
            }
        }
    }

    /**
     * 获取分布式锁的key
     */
    private String getRedisLockKey(String fileName) {
        return LOCK_PREFIX + ":" + DigestUtils.md5Hex(fileName);
    }

    /**
     * 删除文件
     *
     * @param filePath 文件路径
     */
    public void clearFile(String filePath) {
        String lockKey = getRedisLockKey(filePath);
        RedisLockFactory.RedisLock lock = redisLockFactory.newRedisLock(lockKey, Duration.ofMinutes(5));
        if (lock.tryLock(5L, TimeUnit.SECONDS)) {
            try {
                Path file = Paths.get(filePath);
                Files.deleteIfExists(file);
            }catch (Exception e){
                log.error("删除文件失败: {}", filePath);
            } finally {
                lock.unlock();
            }
        } else {
            log.warn("获取分布式锁失败, 跳过文件清理: {}", filePath);
        }
    }

    /**
     * 检查文件是否过期
     */
    private boolean isFileExpired(Path file) throws IOException {
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        long expireTime = Instant.now().minus(Duration.ofMinutes(10)).toEpochMilli();
        return lastModified < expireTime;
    }
}

