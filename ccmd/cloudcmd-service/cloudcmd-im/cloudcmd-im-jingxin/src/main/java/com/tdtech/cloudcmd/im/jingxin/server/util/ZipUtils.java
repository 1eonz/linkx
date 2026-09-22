package com.tdtech.cloudcmd.im.jingxin.server.util;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author lsc
 * @date 2025/11/17
 **/
@Slf4j
public class ZipUtils {
    /**
     * 压缩文件夹为 ZIP 包
     */
    public static void compressFolder(String sourceDirPath, String zipFilePath) throws IOException {
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new FileNotFoundException("源文件夹不存在或不是目录：" + sourceDirPath);
        }

        // 只使用 ZipOutputStream，避免嵌套 BufferedOutputStream
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            log.info("zipFilePath:{}", zipFilePath);
            // 递归遍历文件夹并添加文件到 ZIP
            compressFile(sourceDir, sourceDir.getName(), zos);
        }
    }

    /**
     * 递归处理单个文件/子文件夹
     */
    private static void compressFile(File file, String parentEntryName, ZipOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            // 添加文件夹条目（末尾需加 "/" 标识是目录）
            String dirEntryName = parentEntryName + "/";
            log.info("dirEntryName:{}", dirEntryName);
            zos.putNextEntry(new ZipEntry(dirEntryName));
            zos.closeEntry();

            // 遍历子文件/子文件夹
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    compressFile(child, dirEntryName + child.getName(), zos);
                }
            }
        } else {
            log.info("file:{}", file);
            // 添加文件条目
            ZipEntry zipEntry = new ZipEntry(parentEntryName);
            zos.putNextEntry(zipEntry);

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
