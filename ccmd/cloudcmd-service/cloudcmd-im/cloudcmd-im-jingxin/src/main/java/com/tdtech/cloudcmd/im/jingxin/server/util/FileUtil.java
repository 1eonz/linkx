package com.tdtech.cloudcmd.im.jingxin.server.util;

import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.util.IdWorker;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 文件工具类 - 处理头像/图标文件的下载和本地存储
 * <p>
 * 核心功能：
 * 1. 从 IM 服务器下载头像文件到本地
 * 2. 检查本地是否已存在头像文件
 * 3. 将本地文件路径转换为 URL 访问路径
 * <p>
 * 文件存储路径: /home/linkx/im/
 * URL访问路径: /collaboration/static/
 * 文件命名格式: {id}_{fileId}
 *   - 用户头像: {userId}_{fileId}
 *   - 群头像: {groupId}_{fileId}
 *   - 其他文件: {snowflakeId}_{fileId}
 */
@Component
@Slf4j
public class FileUtil {
    /** 本地文件存储根路径 */
    public static final String FILE_PATH = "/home/linkx/im/";
    /** URL访问路径前缀 */
    public static final String FILE_PATH_URL = "/collaboration/static/";
    /** 默认图标文件名 */
    public static final String DEFAULT_ICON = "default.png";
    /** 默认头像文件名 */
    public static final String DEFAULT_HEAD_ICON = "default-head.png";
    /** 默认头像标识（用户未设置头像时使用此值） */
    public static final String DEFAULT_AVATAR_FLAG = "@1";

    @Resource
    private IdWorker idWorker;

    /**
     * 下载并保存图标文件（无关联ID）
     * <p>
     * 适用场景：下载非用户/群头像的普通图标文件
     *
     * @param imHttpClient IM客户端，用于调用远程下载接口
     * @param fileId 文件ID
     * @return 文件的URL访问路径
     */
    public String downloadSaveIcon(ImHttpClient imHttpClient, String fileId) {
        // 委托给带关联ID的方法，relatedId=null 时使用雪花算法生成唯一ID
        return downloadSaveIcon(imHttpClient, null, fileId);
    }

    /**
     * 下载并保存图标文件（带关联ID）
     * <p>
     * 文件命名规则：
     * - 有 relatedId 时：{relatedId}_{fileId}（如用户头像：27457759805983_abc123）
     * - 无 relatedId 时：{snowflakeId}_{fileId}（雪花算法生成唯一ID）
     * <p>
     * 适用场景：
     * - 用户头像：relatedId = userId
     * - 群头像：relatedId = groupId
     * - 其他文件：relatedId = null（使用雪花ID）
     *
     * @param imHttpClient IM客户端，用于调用远程下载接口
     * @param relatedId 关联ID（用户ID或群ID），用于生成唯一文件名
     * @param fileId 文件ID
     * @return 文件的URL访问路径（格式：/collaboration/static/{relatedId}_{fileId}）
     */
    public String downloadSaveIcon(ImHttpClient imHttpClient, Long relatedId, String fileId) {
        log.debug("[downloadSaveIcon] 开始下载文件, relatedId={}, fileId={}", relatedId, fileId);

        // 【文件命名】生成唯一文件名
        // - 有 relatedId：使用 {relatedId}_{fileId} 格式，便于后续查找和清理
        // - 无 relatedId：使用雪花算法生成唯一ID，避免文件名冲突
        String name;
        if (relatedId != null) {
            name = relatedId + "_" + fileId;
        } else {
            name = idWorker.nextId() + "_" + fileId;
        }
        log.debug("[downloadSaveIcon] 生成本地文件名: {}", name);

        // 【文件下载】调用 IM 客户端下载文件
        // - 目标路径：/home/linkx/im/{name}
        // - 下载失败时使用默认图标：/home/linkx/im/default.png
        var path = imHttpClient.downloadIconOrDefault(fileId, Path.of(FILE_PATH, name), Path.of(FILE_PATH, DEFAULT_ICON));
        log.info("[downloadSaveIcon] 文件下载完成, relatedId={}, fileId={}, 本地路径={}", relatedId, fileId, path);

        // 【路径转换】将本地绝对路径转换为 URL 访问路径
        // 例如：/home/linkx/im/27457759805983_abc123 -> /collaboration/static/27457759805983_abc123
        String urlPath = path.toString().replace(FILE_PATH, FILE_PATH_URL);
        log.debug("[downloadSaveIcon] URL路径: {}", urlPath);
        return urlPath;
    }

    /**
     * 获取默认头像的URL路径
     *
     * @return 默认头像URL路径（/collaboration/static/default-head.png）
     */
    public String getDefaultPath(){
        var path = Path.of(FILE_PATH, DEFAULT_HEAD_ICON);
        String urlPath = path.toString().replace(FILE_PATH, FILE_PATH_URL);
        log.debug("[getDefaultPath] 默认头像路径: {}", urlPath);
        return urlPath;
    }

    /**
     * 获取头像路径，如果本地不存在则下载
     * <p>
     * 核心方法，供业务层调用，实现头像的"按需下载"逻辑
     * <p>
     * 处理逻辑:
     * 1. fileId为空或等于"@1"时，返回默认头像路径
     * 2. 检查本地是否已存在该fileId对应的文件（模糊匹配，查找任意以 _{fileId} 结尾的文件）
     * 3. 存在则直接返回本地文件路径，不存在则下载
     *
     * @param imHttpClient IM客户端
     * @param fileId 头像文件ID（可能为 @1 表示默认头像）
     * @return 头像URL路径
     */
    public String getAvatarPathOrDownload(ImHttpClient imHttpClient, String fileId) {
        log.debug("[getAvatarPathOrDownload] 开始处理, fileId={}", fileId);

        // 【步骤1】空值或默认头像标识，直接返回默认路径
        // IM 系统中 "@1" 表示用户未设置头像
        if (StringUtils.isBlank(fileId) || DEFAULT_AVATAR_FLAG.equals(fileId)) {
            log.debug("[getAvatarPathOrDownload] fileId为空或为默认标识, 返回默认头像");
            return getDefaultPath();
        }

        // 【步骤2】检查本地是否存在该 fileId 对应的文件
        // 使用模糊匹配：查找任意以 _{fileId} 结尾的文件
        // 例如：查找 _abc123 结尾的文件，可能匹配 27457759805983_abc123
        log.debug("[getAvatarPathOrDownload] 检查本地文件是否存在, fileId={}", fileId);
        Path existingFile = findAvatarFile(fileId);
        if (existingFile != null) {
            // 本地已存在，直接返回 URL 路径，避免重复下载
            String urlPath = existingFile.toString().replace(FILE_PATH, FILE_PATH_URL);
            log.debug("[getAvatarPathOrDownload] 本地文件已存在, fileId={}, 路径={}, 跳过下载", fileId, existingFile);
            return urlPath;
        }

        // 【步骤3】本地不存在，执行下载
        // 注意：这里没有传 relatedId，下载时使用雪花ID命名
        // 如果需要使用 {userId}_{fileId} 格式，应该在调用前使用 downloadSaveIcon(imHttpClient, userId, fileId)
        log.debug("[getAvatarPathOrDownload] 本地文件不存在, fileId={}, 开始下载", fileId);
        return downloadSaveIcon(imHttpClient, fileId);
    }

    /**
     * 查找本地是否存在该 fileId 对应的头像文件（模糊匹配）
     * <p>
     * 文件命名格式：{id}_{fileId}
     * <p>
     * 模糊匹配：查找任意以 _{fileId} 结尾的文件
     *
     * @param fileId 文件ID
     * @return 找到的文件路径，未找到返回null
     */
    private Path findAvatarFile(String fileId) {
        return findAvatarFile(null, fileId);
    }

    /**
     * 查找本地是否存在该 fileId 对应的头像文件
     * <p>
     * 匹配模式：
     * - 有 relatedId：精确匹配 {relatedId}_{fileId}
     * - 无 relatedId：模糊匹配任意以 _{fileId} 结尾的文件
     * <p>
     * 文件命名格式：{relatedId}_{fileId}
     *
     * @param relatedId 关联ID（用户ID或群ID），null 表示模糊匹配
     * @param fileId 文件ID
     * @return 找到的文件路径，未找到返回null
     */
    private Path findAvatarFile(Long relatedId, String fileId) {
        File dir = new File(FILE_PATH);

        // 【目录检查】确保目录存在且是目录
        log.debug("检查目录是否存在: {}", FILE_PATH);
        if (!dir.exists()) {
            log.debug("[findAvatarFile] 目录不存在: {}", FILE_PATH);
            return null;
        }
        if (!dir.isDirectory()) {
            log.debug("[findAvatarFile] 路径不是目录: {}", FILE_PATH);
            return null;
        }

        // 【文件查找】根据是否有 relatedId 决定匹配模式
        File[] files;
        if (relatedId != null) {
            // 精确匹配：查找文件名完全等于 {relatedId}_{fileId} 的文件
            // 例如：查找 27457759805983_abc123
            String targetName = relatedId + "_" + fileId;
            files = dir.listFiles((dir1, name) -> name.equals(targetName));
        } else {
            // 模糊匹配：查找以 _{fileId} 结尾的文件
            // 例如：查找 _abc123 结尾的文件，可能匹配 27457759805983_abc123 或 1234567890_abc123
            files = dir.listFiles((dir1, name) -> name.endsWith("_" + fileId));
        }
        log.debug("[files]:", files);

        // 【结果处理】返回找到的第一个文件
        if (files != null && files.length > 0) {
            Path foundPath;
            try {
                foundPath = files[0].toPath().toRealPath();
            } catch (IOException e) {
                log.warn("[findAvatarFile] 获取规范化路径失败: {}", e.getMessage());
                foundPath = files[0].toPath();
            }
            log.debug("[findAvatarFile] 找到匹配文件: relatedId={}, fileId={}, 文件名={}, 路径={}",
                relatedId, fileId, files[0].getName(), foundPath);
            return foundPath;
        }

        log.debug("[findAvatarFile] 未找到匹配文件: relatedId={}, fileId={}", relatedId, fileId);
        return null;
    }

    /**
     * 检查本地是否存在该 fileId 对应的头像文件（模糊匹配）
     *
     * @param fileId 文件ID
     * @return true-存在, false-不存在
     */
    public boolean existsAvatarFile(String fileId) {
        boolean exists = findAvatarFile(fileId) != null;
        log.debug("[existsAvatarFile] fileId={}, exists={}", fileId, exists);
        return exists;
    }

    /**
     * 检查本地是否存在该 fileId 对应的头像文件（精确匹配）
     * <p>
     * 精确匹配：查找文件名完全等于 {relatedId}_{fileId} 的文件
     *
     * @param relatedId 关联ID（用户ID或群ID）
     * @param fileId 文件ID
     * @return true-存在, false-不存在
     */
    public boolean existsAvatarFile(Long relatedId, String fileId) {
        boolean exists = findAvatarFile(relatedId, fileId) != null;
        log.debug("[existsAvatarFile] relatedId={}, fileId={}, exists={}", relatedId, fileId, exists);
        return exists;
    }
}