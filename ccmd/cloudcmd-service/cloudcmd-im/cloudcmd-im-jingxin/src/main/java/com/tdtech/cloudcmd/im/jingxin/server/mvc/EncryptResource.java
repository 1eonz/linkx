package com.tdtech.cloudcmd.im.jingxin.server.mvc;

import com.tdtech.cloudcmd.encryptor.service.EncryptionService;
import com.tdtech.cloudcmd.im.jingxin.server.util.EncryptImUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.AbstractResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.tdtech.cloudcmd.im.jingxin.server.util.EncryptImUtil.READ_TMP_DIR;

@Slf4j
public class EncryptResource extends AbstractResource {
    private final String encryptDir;
    private final String fileName;
    private final boolean isChunked;
    private final Path encryptedFile;
    private final Path chunkMetaFile;
    private final EncryptionService encryptionService;

    private final EncryptImUtil encryptImUtil;

    public EncryptResource(String encryptDir, String fileName, boolean isChunked,
                           Path encryptedFile, Path chunkMetaFile,
                           EncryptionService encryptionService, EncryptImUtil encryptImUtil) {
        log.info("EncryptResource created: encryptDir={}, fileName={}, isChunked={}", encryptDir, fileName, isChunked);
        this.encryptDir = encryptDir;
        this.fileName = fileName;
        this.isChunked = isChunked;
        this.encryptedFile = encryptedFile;
        this.chunkMetaFile = chunkMetaFile;
        this.encryptionService = encryptionService;
        this.encryptImUtil = encryptImUtil;
    }

    @Override
    public String getDescription() {
        return "DecryptedResource[" + encryptDir + "/" + fileName + "]";
    }

    @Override
    public InputStream getInputStream() throws IOException {
        log.info("EncryptResource.getInputStream() called for: {}", fileName);
        try {
            // 优先使用已存在的临时文件，避免重复解密
            String realFileName = fileName.endsWith(".enc") ? fileName.substring(0, fileName.length() - 4) : fileName;
            Path tempFile = Paths.get(encryptDir, READ_TMP_DIR, realFileName);
            if (Files.exists(tempFile)) {
                log.info("临时文件已存在，直接读取: {}", tempFile);
                // 更新文件最后修改时间，防止被定时清理
                touchFile(tempFile);
                return new BufferedInputStream(Files.newInputStream(tempFile));
            }
            
            if (isChunked) {
                return new ChunkedDecryptInputStream(chunkMetaFile, Paths.get(encryptDir), fileName, encryptionService);
            } else {
                return new SingleDecryptInputStream(encryptedFile, encryptionService);
            }
        } catch (Exception e) {
            log.error("getInputStream error for: {}", fileName, e);
            throw e;
        }
    }

    @Override
    public String getFilename() {
        String realFileName = fileName;
        // 去掉 .enc 后缀，让 Spring 正确判断 Content-Type
        if (realFileName != null && realFileName.endsWith(".enc")) {
            realFileName = realFileName.substring(0, realFileName.length() - 4);
        }
        log.info("EncryptResource.getFilename() called: {} -> {}", fileName, realFileName);
        return realFileName;
    }

    @Override
    public long contentLength() throws IOException {
        log.debug("EncryptResource.contentLength() called for: {}", fileName);
        
        try {
            if (isChunked) {
                // 分块文件：从 .meta 文件中读取原始文件大小
                return getChunkedFileSize();
            } else {
                // 单文件：检查临时文件是否存在，存在则返回大小
                // 否则返回加密文件大小（SM4 对称加密，大小基本一致）
                return getSingleFileSize();
            }
        } catch (Exception e) {
            log.warn("无法获取文件大小: {}", fileName, e);
            return -1;
        }
    }
    
    /**
     * 获取单文件解密后的大小
     * 如果临时文件不存在，先解密再返回大小
     */
    private long getSingleFileSize() throws IOException {
        // 预解密, 保证视频类文件的range读取
        String fileToRead = encryptImUtil.getFileToRead(encryptDir, fileName, null);
        Path tempFile = Paths.get(fileToRead);

        // 如果临时文件存在，返回其大小
        if (Files.exists(tempFile)) {
            long size = Files.size(tempFile);
            log.debug("临时文件已存在，大小: {} bytes", size);
            // 更新文件最后修改时间，防止被定时清理
            touchFile(tempFile);
            return size;
        }
        return -1;
    }
    
    /**
     * 获取分块文件解密后的大小
     * 从 .meta 文件中读取原始文件大小
     */
    private long getChunkedFileSize() throws IOException {
        if (!Files.exists(chunkMetaFile)) {
            log.warn("Meta 文件不存在: {}", chunkMetaFile);
            return -1;
        }
        
        // 读取 meta 文件
        // 格式：第一行是分块数量，第二行是原始文件大小（如果存在）
        java.util.List<String> lines = Files.readAllLines(chunkMetaFile);
        
        if (lines.size() >= 2) {
            // 第二行是原始文件大小
            try {
                long originalSize = Long.parseLong(lines.get(1));
                log.debug("从 meta 文件读取原始文件大小: {} bytes", originalSize);
                return originalSize;
            } catch (NumberFormatException e) {
                log.warn("Meta 文件格式错误，无法解析文件大小: {}", lines.get(1));
            }
        }
        return -1;
    }

    @Override
    public boolean exists() {
        log.info("EncryptResource.exists() called");
        return true;
    }

    @Override
    public long lastModified() throws IOException {
        log.info("EncryptResource.lastModified() called");
        return System.currentTimeMillis();
    }

    /**
     * 单文件流式解密
     * 优化：使用 FileInputStream 流式读取，避免全量加载到内存
     */
    private static class SingleDecryptInputStream extends InputStream {
        private final FileInputStream fin;

        public SingleDecryptInputStream(Path encryptedFile, EncryptionService encryptionService) throws IOException {
            log.debug("SingleDecryptInputStream for: {}", encryptedFile);
            String tempPath = encryptedFile.getParent().resolve(READ_TMP_DIR).toString();
            String decryptFile = encryptionService.decryptFile(encryptedFile.toFile().getCanonicalPath(), tempPath);
            // 使用 FileInputStream 流式读取，不加载到内存
            this.fin = new FileInputStream(decryptFile);
        }

        @Override
        public int read() throws IOException {
            return fin.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return fin.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            fin.close();
        }
    }

    /**
     * 更新文件最后修改时间，防止被定时清理
     * 使用 File.setLastModified() 方法，在 Windows 上更可靠
     */
    private void touchFile(Path file) {
        try {
            File f = file.toFile();
            long newTime = System.currentTimeMillis();
            boolean success = f.setLastModified(newTime);
            if (!success) {
                log.warn("更新文件最后修改时间失败: {}", file);
            }
        } catch (Exception e) {
            log.warn("更新文件最后修改时间异常: {}", file, e);
        }
    }
    
    /**
     * 分块流式解密
     * 优化：使用 FileInputStream 流式读取，避免全量加载到内存
     */
    private static class ChunkedDecryptInputStream extends InputStream {
        private final FileInputStream fin;

        public ChunkedDecryptInputStream(Path metaFile, Path encryptDir, String fileName,
                                        EncryptionService encryptionService) throws IOException {
            log.debug("ChunkedDecryptInputStream for: {}", fileName);
            // 使用 encryptionService.decryptFile 解密整个分块文件
            String tempPath = encryptDir.resolve(READ_TMP_DIR).toString();
            String decryptedFilePath = encryptionService.decryptFile(metaFile.toFile().getCanonicalPath(), tempPath);
            // 使用 FileInputStream 流式读取，不加载到内存
            this.fin = new FileInputStream(decryptedFilePath);
        }

        @Override
        public int read() throws IOException {
            return fin.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return fin.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            fin.close();
        }
    }
}
