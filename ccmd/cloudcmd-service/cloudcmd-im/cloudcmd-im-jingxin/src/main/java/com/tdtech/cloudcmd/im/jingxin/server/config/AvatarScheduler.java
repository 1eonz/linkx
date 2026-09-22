package com.tdtech.cloudcmd.im.jingxin.server.config;

import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImPage;
import com.tdtech.cloudcmd.im.jingxin.client.entity.ImUser;
import com.tdtech.cloudcmd.im.jingxin.server.util.FileUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Resource;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * 头像定时任务调度器
 * <p>
 * 功能：
 * 1. 增量下载用户头像（每天凌晨2点执行）
 * 2. 清理孤立头像文件（删除不再被任何用户引用的头像）
 * <p>
 * 文件命名格式：{userId}_{fileId}
 * 清理策略：只清理用户头像，群头像不会被删除（通过判断文件名前缀是否为用户ID）
 */
@Slf4j
@Configuration
public class AvatarScheduler {

    @Resource
    private ImHttpClient imHttpClient;

    @Resource
    private FileUtil fileUtil;

    /**
     * 每天凌晨 2 点执行头像增量下载并清理孤立文件
     * <p>
     * 执行流程：
     * 1. 分页查询所有用户
     * 2. 检查每个用户的头像是否已下载到本地
     * 3. 未下载则从 IM 服务器下载，文件命名格式：{userId}_{fileId}
     * 4. 清理孤立头像文件（不再被任何用户引用的头像）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void downloadAvatars() {
        log.info("开始执行头像增量下载任务");

        // 分页参数
        int pageNo = 1;
        int pageSize = 100;
        // 统计计数
        int totalDownloaded = 0;
        int totalSkipped = 0;
        // 【用户ID集合】用于清理时判断文件是否为用户头像
        Set<Long> userIds = new HashSet<>();
        // 【有效fileId集合】用于清理时判断头像是否仍被用户引用
        Set<String> validFileIds = new HashSet<>();

        try {
            // 【步骤1】分页查询所有用户，增量下载头像
            while (true) {
                // 调用 IM 接口分页查询用户列表
                ImPage<ImUser> userPage = imHttpClient.userPageByDepartment(
                    pageNo, pageSize, null, 1, null, null, null, null
                );

                // 检查响应有效性
                if (userPage == null || userPage.getRecords() == null) {
                    log.warn("第 {} 页返回无效响应，结束分页查询", pageNo);
                    break;
                }

                // 检查 total 是否有效
                if (userPage.getTotal() == null || userPage.getTotal() <= 0) {
                    log.warn("第 {} 页 total 无效: {}，结束分页查询", pageNo, userPage.getTotal());
                    break;
                }

                // 遍历当前页用户，处理头像下载
                for (ImUser user : userPage.getRecords()) {
                    String avatar = user.getAvatar();

                    // 【跳过】用户无头像或使用默认头像（@1）
                    if (StringUtils.isBlank(avatar) || FileUtil.DEFAULT_AVATAR_FLAG.equals(avatar)) {
                        totalSkipped++;
                        continue;
                    }

                    // 【收集】记录用户ID和fileId，用于后续清理判断
                    userIds.add(user.getId());
                    validFileIds.add(avatar);

                    // 【检查】本地是否已存在该用户的头像文件
                    // 使用精确匹配：查找文件名完全等于 {userId}_{fileId} 的文件
                    if (fileUtil.existsAvatarFile(user.getId(), avatar)) {
                        totalSkipped++;
                        continue;
                    }

                    // 【下载】本地不存在，从 IM 服务器下载
                    // 文件命名格式：{userId}_{fileId}
                    // 例如：27457759805983_abc123
                    try {
                        fileUtil.downloadSaveIcon(imHttpClient, user.getId(), avatar);
                        totalDownloaded++;
                    } catch (Exception e) {
                        log.warn("下载用户头像失败, userId: {}, avatar: {}", user.getId(), avatar, e);
                    }
                }

                log.info("第 {} 页处理完成，获取 {} 条记录，total={}", pageNo, userPage.getRecords().size(), userPage.getTotal());

                // 【分页】检查是否已到达最后一页
                if (userPage.getTotal() <= pageNo * pageSize) {
                    log.info("已处理完所有数据，total={}, 处理页数={}, 结束分页查询", userPage.getTotal(), pageNo);
                    break;
                }
                pageNo++;
            }

            log.info("头像增量下载任务完成, 下载: {}, 跳过: {}", totalDownloaded, totalSkipped);

            // 【步骤2】清理孤立头像文件
            cleanOrphanAvatars(userIds, validFileIds);

        } catch (Exception e) {
            log.error("头像增量下载任务执行失败", e);
        }
    }

    /**
     * 清理孤立头像文件
     * <p>
     * 孤立文件定义：文件存在但不再被任何用户引用
     * <p>
     * 判断逻辑：
     * 1. userIds 集合包含所有用户ID（从 IM 接口查询得到）
     * 2. validFileIds 集合包含所有用户当前的 avatar fileId
     * 3. 扫描本地头像目录，文件名格式为 {id}_{fileId}
     * 4. 从文件名提取 id，判断是否为用户ID
     * 5. 如果是用户ID且fileId不在validFileIds中，则为孤立文件，删除
     * 6. 如果id不是用户ID（如群ID），跳过不处理（保护群头像）
     *
     * @param userIds 所有用户ID集合
     * @param validFileIds 所有有效的头像fileId集合
     */
    private void cleanOrphanAvatars(Set<Long> userIds, Set<String> validFileIds) {
        log.info("开始执行头像文件清理, 用户数量: {}, 有效头像数量: {}", userIds.size(), validFileIds.size());

        // 【目录检查】确保头像目录存在
        File dir = new File(FileUtil.FILE_PATH);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("头像目录不存在: {}", FileUtil.FILE_PATH);
            return;
        }

        // 【获取文件列表】列出目录下所有文件
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            log.info("头像目录为空，无需清理");
            return;
        }

        // 统计计数
        int deletedCount = 0;
        long freedBytes = 0;
        int skippedCount = 0;

        // 【遍历文件】逐个检查是否为孤立文件
        for (File file : files) {
            // 跳过子目录
            if (file.isDirectory()) {
                continue;
            }

            String fileName = file.getName();

            // 【解析文件名】提取 id 和 fileId
            // 文件名格式：{id}_{fileId}
            // 例如：27457759805983_abc123 -> id=27457759805983, fileId=abc123
            Long id = extractId(fileName);
            String fileId = extractFileId(fileName);
            if (id == null || fileId == null) {
                // 文件名格式不正确，跳过
                continue;
            }

            // 【判断文件类型】检查是否为用户头像文件
            // 通过判断 id 是否在用户ID集合中来区分用户头像和群头像
            if (!userIds.contains(id)) {
                // id 不是用户ID，说明是群头像或其他文件，跳过不处理
                // 这样可以保护群头像不被误删
                skippedCount++;
                continue;
            }

            // 【判断是否孤立】检查 fileId 是否仍被用户引用
            if (!validFileIds.contains(fileId)) {
                // fileId 不在有效集合中，说明是孤立文件，删除
                long fileSize = file.length();
                if (file.delete()) {
                    deletedCount++;
                    freedBytes += fileSize;
                    log.debug("删除孤立头像文件: {}, 大小: {} bytes", fileName, fileSize);
                } else {
                    log.warn("删除头像文件失败: {}", fileName);
                }
            }
        }

        log.info("头像文件清理完成, 删除: {}, 跳过非用户头像: {}, 释放空间: {} MB",
            deletedCount, skippedCount, freedBytes / 1024.0 / 1024.0);
    }

    /**
     * 从文件名提取 id（文件名前缀）
     * <p>
     * 文件名格式：{id}_{fileId}
     * 例如：27457759805983_abc123 -> 27457759805983
     *
     * @param fileName 文件名
     * @return id，解析失败返回null
     */
    private Long extractId(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        // 查找第一个下划线的位置
        int firstUnderscore = fileName.indexOf('_');
        if (firstUnderscore == -1 || firstUnderscore == 0) {
            // 文件名不包含下划线，或下划线在开头，格式不正确
            return null;
        }
        try {
            // 提取下划线前的部分作为 id
            return Long.parseLong(fileName.substring(0, firstUnderscore));
        } catch (NumberFormatException e) {
            // id 不是有效的 Long 类型，格式不正确
            return null;
        }
    }

    /**
     * 从文件名提取 fileId（文件名后缀）
     * <p>
     * 文件名格式：{id}_{fileId}
     * 例如：27457759805983_abc123 -> abc123
     *
     * @param fileName 文件名
     * @return fileId，解析失败返回null
     */
    private String extractFileId(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return null;
        }
        // 查找第一个下划线的位置
        int firstUnderscore = fileName.indexOf('_');
        if (firstUnderscore == -1 || firstUnderscore == fileName.length() - 1) {
            // 文件名不包含下划线，或下划线在末尾，格式不正确
            return null;
        }
        // 提取下划线后的部分作为 fileId
        return fileName.substring(firstUnderscore + 1);
    }
}
