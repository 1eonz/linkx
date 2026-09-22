package com.tdtech.cloudcmd.im.jingxin.server.mvc;

import com.tdtech.cloudcmd.im.jingxin.server.service.impl.GroupExtendsServiceImpl;
import com.tdtech.cloudcmd.im.jingxin.server.util.EncryptImUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 静态文件加密处理器
 */
@Slf4j
@Component
public class EncryptStaticResourceResolver implements ResourceResolver {
    @Autowired
    private EncryptImUtil encryptImUtil;

    @Override
    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        try {
            if (CollectionUtils.isEmpty(locations)) {
                return chain.resolveResource(request, requestPath, locations);
            }

            // URL 解码请求路径（处理中文和特殊字符）
            String decodedPath = decodeUrlPath(requestPath);
            log.debug("请求路径解码: {} -> {}", requestPath, decodedPath);

            if (!matchEncryptDirectFile(decodedPath)) {
                return chain.resolveResource(request, requestPath, locations);
            }

            // 只处理加密的资源
            String baseLocation  = locations.get(0).getURL().getPath();
            String fullPath = baseLocation + decodedPath;
            String encryptorDir = extractRelativePath(fullPath);

            // 返回流式资源（不加载文件）
            return encryptImUtil.getDecryptedResource(encryptorDir, getFileName(fullPath));
        } catch (Exception e) {
            log.error("加载静态资源失败: {}", requestPath, e);
            return chain.resolveResource(request, requestPath, locations);
        }
    }

    /**
     * URL 解码路径（处理中文和特殊字符）
     * 支持 %E4%B8%8B%E8%BD%BD (中文) 和 %20 (空格) 等编码
     */
    private String decodeUrlPath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        try {
            // 使用 Spring 的 UriUtils 进行解码
            return UriUtils.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("URL 解码失败，尝试使用 Java URLDecoder: {}", path);
            try {
                // 备选方案：使用 Java 的 URLDecoder
                return URLDecoder.decode(path, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                log.error("URL 解码失败，返回原始路径: {}", path, ex);
                return path;
            }
        }
    }

    private String getFileName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    @Override
    public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
        return chain.resolveUrlPath(resourcePath, locations);
    }
    /**
     * 从请求路径中健壮提取 /collaboration/static 之后的内容
     * 示例：
     * /collaboration/static/archive/data/linkx/a.1234 → /archive/data/linkx/a.1234
     * 返回 null 表示不符合规则
     */
    private String extractRelativePath(String requestPath) throws IOException {
        // 固定匹配前缀
        Path path = Paths.get(requestPath);
        return path.getParent().toFile().getCanonicalPath();
    }

    /**
     * 健壮判断：仅文件直接处于encrypt一级子目录才返回true，纯目录/多级子目录一律false
     * @param path 不带上下文的请求路径 例：collaboration/static/archive/data/encrypt/a.1234
     * @return 符合规则返回true
     */
    private boolean matchEncryptDirectFile(String path) {
        // 1. 空值直接放行
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        // 统一标准化：去除首尾斜杠，统一为正斜杠
        String standard = path.trim().replace("\\", "/");
        standard = standard.startsWith("/") ? standard.substring(1) : standard;

        // 2. 分割路径片段
        String[] parts = standard.split("/");
        // 至少需要两级：encrypt + 文件
        if (parts.length < 2) {
            return false;
        }
        // 3. 倒数第二个片段必须是 encrypt
        String parentDir = parts[parts.length - 2];
        if (!"encrypt".equals(parentDir)) {
            return false;
        }
        // 4. 最后一段必须是文件名（非空，排除纯目录）
        String fileName = parts[parts.length - 1];
        if (fileName.isEmpty()) {
            return false;
        }
        // 5. 确保没有更深层级：总层级只能刚好到 encrypt/文件
        return true;
    }
}