package com.chinasoft.cloud.module.aiagent.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chinasoft.cloud.framework.common.exception.util.ServiceExceptionUtil;
import com.chinasoft.cloud.framework.common.util.json.JsonUtils;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.enums.ErrorCodeConstants;
import com.chinasoft.cloud.module.aiagent.util.MimeTypeUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI智能体文件上传服务实现
 */
@Slf4j
@Service
public class AiAgentFileServiceImpl implements AiAgentFileService {

    private static final String CACHE_KEY_PREFIX = "crs:ai-agent:file:session:";
    private static final Duration CACHE_EXPIRE = Duration.ofMinutes(30);
    private static final Pattern VAR_REF_PATTERN =
        Pattern.compile("\\$\\{([A-Za-z0-9_.-]+)}|\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*}}");

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
        .callTimeout(30L, TimeUnit.MINUTES)
        .readTimeout(30L, TimeUnit.MINUTES)
        .connectTimeout(Duration.ofSeconds(30L))
        .build();

    // 默认支持的文件格式
    private static final List<String> DEFAULT_AUDIO_TYPES = Arrays.asList(
        ".mp3", ".aac", ".pcm", ".wav", ".amr", ".m4a", ".webm"
    );
    private static final List<String> DEFAULT_VIDEO_TYPES = Arrays.asList(
        ".mp4", ".mov", ".webm", ".mpeg", ".mpga"
    );
    private static final List<String> DEFAULT_IMAGE_TYPES = Arrays.asList(
        ".jpg", ".jpeg", ".gif", ".png", ".bmp", ".webp", ".svg"
    );
    private static final List<String> DEFAULT_DOCUMENT_TYPES = Arrays.asList(
        ".md", ".doc", ".docx", ".pdf", ".xlsx", ".xls", ".ppt", ".pptx", ".txt", ".html", ".csv", ".eml", ".xml", ".epub", ".msg", ".markdown"
    );

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AiAgentConfigService agentConfigService;

    @Resource
    private AgentAttachmentConfigService attachmentConfigService;

    // 文件存储根路径
    private final String fileStoragePath = "/home/agent/upload/ai_attachment";

    @Override
    public AiFileInfo uploadFile(MultipartFile file, Long agentId, String sessionId, String userId) {
        if (file == null || file.isEmpty()) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.FILE_UPLOAD_FAILED);
        }

        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            fileName = UUID.randomUUID().toString();
        }

        if (!validateFileType(fileName, agentId)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.FILE_TYPE_NOT_SUPPORTED);
        }

        String filePath = saveToLocal(file, fileName);
        String fileUrl = "/ai_attachment/" + filePath;

        AgentConfig config = agentConfigService.getById(agentId);
        if (config == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.AGENT_CONFIG_NOT_FOUND);
        }

        String aiFileId = null;
        if (config.getFileInterfaceId() != null) {
            AgentAttachmentConfig attachmentConfig = attachmentConfigService.getById(config.getFileInterfaceId());
            if (attachmentConfig == null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.ATTACHMENT_CONFIG_NOT_FOUND);
            }

            aiFileId = uploadToAiServer(filePath, fileName, userId, config, attachmentConfig);
        }

        AiFileInfo fileInfo = new AiFileInfo();
        fileInfo.setSessionId(sessionId);
        fileInfo.setFileId(aiFileId);
        fileInfo.setFilePath(fileUrl);
        fileInfo.setFileName(fileName);
        fileInfo.setFileUrl(fileUrl);
        fileInfo.setFileCategory(determineFileCategory(fileName, agentId));
        fileInfo.setAgentId(agentId);
        fileInfo.setUserId(userId);
        fileInfo.setMimeType(file.getContentType());
        fileInfo.setFileSize(file.getSize());

        String cacheKey = CACHE_KEY_PREFIX + sessionId;
        stringRedisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonString(fileInfo), CACHE_EXPIRE);

        log.info("File uploaded successfully: sessionId={}, fileId={}, fileName={}", sessionId, aiFileId, fileName);
        return fileInfo;
    }

    @Override
    public AiFileInfo getFileInfo(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return null;
        }
        String cacheKey = CACHE_KEY_PREFIX + sessionId;
        String value = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return JsonUtils.parseObject(value, AiFileInfo.class);
    }

    @Override
    public void deleteFileInfo(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            return;
        }
        String cacheKey = CACHE_KEY_PREFIX + sessionId;
        stringRedisTemplate.delete(cacheKey);
    }

    @Override
    public String determineFileCategory(String fileName, Long agentId) {
        if (StringUtils.isBlank(fileName)) {
            return "document";
        }
        String ext = MimeTypeUtils.getFileExtension(fileName).toLowerCase();

        AgentConfig config = agentConfigService.getById(agentId);
        List<String> audioTypes = config != null && !CollectionUtils.isEmpty(config.getAudioTypeList())
            ? config.getAudioTypeList() : DEFAULT_AUDIO_TYPES;
        List<String> videoTypes = config != null && !CollectionUtils.isEmpty(config.getVideoTypeList())
            ? config.getVideoTypeList() : DEFAULT_VIDEO_TYPES;
        List<String> imageTypes = config != null && !CollectionUtils.isEmpty(config.getImageTypeList())
            ? config.getImageTypeList() : DEFAULT_IMAGE_TYPES;
        List<String> documentTypes = config != null && !CollectionUtils.isEmpty(config.getDocumentTypeList())
            ? config.getDocumentTypeList() : DEFAULT_DOCUMENT_TYPES;

        if (audioTypes.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
            return "audio";
        }
        if (videoTypes.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
            return "video";
        }
        if (imageTypes.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
            return "image";
        }
        if (documentTypes.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
            return "document";
        }
        return "document";
    }

    @Override
    public boolean validateFileType(String fileName, Long agentId) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        String ext = MimeTypeUtils.getFileExtension(fileName).toLowerCase();
        if (StringUtils.isBlank(ext)) {
            return false;
        }

        AgentConfig config = agentConfigService.getById(agentId);
        if (config == null) {
            return false;
        }

        // 检查各类型是否启用且格式匹配
        if (config.getAudio() != null && config.getAudio() == 1) {
            List<String> types = CollectionUtils.isEmpty(config.getAudioTypeList())
                ? DEFAULT_AUDIO_TYPES : config.getAudioTypeList();
            if (types.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
                return true;
            }
        }
        if (config.getVideo() != null && config.getVideo() == 1) {
            List<String> types = CollectionUtils.isEmpty(config.getVideoTypeList())
                ? DEFAULT_VIDEO_TYPES : config.getVideoTypeList();
            if (types.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
                return true;
            }
        }
        if (config.getImage() != null && config.getImage() == 1) {
            List<String> types = CollectionUtils.isEmpty(config.getImageTypeList())
                ? DEFAULT_IMAGE_TYPES : config.getImageTypeList();
            if (types.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
                return true;
            }
        }
        if (config.getDocument() != null && config.getDocument() == 1) {
            List<String> types = CollectionUtils.isEmpty(config.getDocumentTypeList())
                ? DEFAULT_DOCUMENT_TYPES : config.getDocumentTypeList();
            if (types.stream().anyMatch(t -> t.equalsIgnoreCase(ext))) {
                return true;
            }
        }

        return false;
    }

    private String saveToLocal(MultipartFile file, String fileName) {
        try {
            // 按文件类型分目录
            String ext = MimeTypeUtils.getFileExtension(fileName);
            String categoryDir = getFileCategoryByExtension(ext);
            String dirPath = fileStoragePath + "/" + categoryDir;

            Path dir = Paths.get(dirPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            String newFileName = generateUniqueFileName(dir, fileName, ext);

            Path filePath = dir.resolve(newFileName);
            file.transferTo(filePath.toFile());

            return categoryDir + "/" + newFileName;
        } catch (IOException e) {
            log.error("Failed to save file locally: {}", fileName, e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.FILE_UPLOAD_FAILED);
        }
    }

    private String generateUniqueFileName(Path dir, String originalFileName, String ext) {
        String baseName = getBaseName(originalFileName);

        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String newFileName = baseName + "_" + dateTime + (StringUtils.isNotBlank(ext) ? ext : "");
        Path filePath = dir.resolve(newFileName);

        // 如果同一秒上传，添加序号
        int counter = 1;
        while (Files.exists(filePath)) {
            newFileName = baseName + "_" + dateTime + "_" + counter + (StringUtils.isNotBlank(ext) ? ext : "");
            filePath = dir.resolve(newFileName);
            counter++;
        }

        return newFileName;
    }

    private String getFileCategoryByExtension(String ext) {
        if (StringUtils.isBlank(ext)) {
            return "document";
        }
        String extLower = ext.toLowerCase();
        if (DEFAULT_AUDIO_TYPES.stream().anyMatch(t -> t.equalsIgnoreCase(extLower))) {
            return "audio";
        }
        if (DEFAULT_VIDEO_TYPES.stream().anyMatch(t -> t.equalsIgnoreCase(extLower))) {
            return "video";
        }
        if (DEFAULT_IMAGE_TYPES.stream().anyMatch(t -> t.equalsIgnoreCase(extLower))) {
            return "image";
        }
        return "document";
    }

    private String uploadToAiServer(String localPath, String fileName, String userId, AgentConfig config,
                                    AgentAttachmentConfig attachmentConfig) {
        try {
            JSONObject context = buildFileUploadContext(config, fileName, userId, localPath);
            log.info("AI server upload context: {}", context);
            String url = buildFileUploadUrl(attachmentConfig, context);
            if (StringUtils.isBlank(url)) {
                log.error("Invalid attachment config: id={}, url is blank", attachmentConfig.getId());
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.ATTACHMENT_CONFIG_NOT_FOUND);
            }

            log.info("Uploading file to AI server: url={}, fileName={}", url, fileName);

            Path filePath = Paths.get(fileStoragePath, localPath);
            File localFile = filePath.toFile();
            Request request = buildFileUploadRequest(url, localFile, fileName, config, attachmentConfig, context);

            try (Response response = HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("AI server upload failed: code={}, body={}", response.code(), errorBody);
                    throw ServiceExceptionUtil.exception(ErrorCodeConstants.AI_FILE_UPLOAD_FAILED);
                }

                String responseBody = response.body() != null ? response.body().string() : "{}";
                log.info("AI server response: {}", responseBody);
                return extractFileIdFromResponse(responseBody, attachmentConfig.getResponseFileField());
            }
        } catch (IOException e) {
            log.error("Failed to upload file to AI server: {}", fileName, e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.AI_FILE_UPLOAD_FAILED);
        }
    }

    private JSONObject buildFileUploadContext(AgentConfig config, String fileName, String userId, String localPath) {
        JSONObject context = new JSONObject(true);

        JSONObject configJson = (JSONObject) JSON.toJSON(config);
        log.info("Agent config: {}", configJson);
        context.putAll(configJson);
        context.put("configId", config.getId());
        context.put("agentId", config.getId());
        context.put("agentName", config.getName());
        context.put("config", configJson);

        context.put("userId", userId);
        context.put("user", userId);

        String ext = MimeTypeUtils.getFileExtension(fileName).toLowerCase();
        String mimeType = MimeTypeUtils.getMimeType(fileName);
        log.info("File info: mimeType={}", mimeType);
        context.put("fileName", fileName);
        context.put("fileExt", ext);
        context.put("fileCategory", getFileCategoryByExtension(ext));
        context.put("mimeType", mimeType);
        context.put("filePath", localPath);

        Path filePath = Paths.get(fileStoragePath, localPath);
        File file = filePath.toFile();
        context.put("fileSize", file.length());

        // 文件对象（便于统一引用）
        context.put("file", new JSONObject(true) {{
            put("name", fileName);
            put("ext", ext);
            put("path", localPath);
            put("category", context.getString("fileCategory"));
            put("mimeType", mimeType);
            put("size", context.getLong("fileSize"));
        }});

        return context;
    }

    private String buildFileUploadUrl(AgentAttachmentConfig attachmentConfig, JSONObject context) {
        String baseUrl = attachmentConfig.buildFullUrl();

        if (StringUtils.isBlank(attachmentConfig.getQuery())) {
            return baseUrl;
        }

        try {
            Object queryObj = resolveConfigValue(attachmentConfig.getQuery(), context);
            Map<String, Object> queryMap = normalizeToMap(queryObj);

            if (queryMap == null || queryMap.isEmpty()) {
                return baseUrl;
            }

            HttpUrl httpUrl = HttpUrl.parse(baseUrl);
            if (httpUrl == null) {
                throw new IllegalArgumentException("invalid url: " + baseUrl);
            }

            HttpUrl.Builder builder = httpUrl.newBuilder();
            queryMap.forEach((key, value) -> {
                if (key != null && value != null) {
                    builder.addQueryParameter(key, String.valueOf(value));
                }
            });
            return builder.build().toString();
        } catch (Exception e) {
            log.warn("Failed to parse query config: {}", attachmentConfig.getQuery(), e);
            return baseUrl;
        }
    }

    private Request buildFileUploadRequest(String url, File localFile, String fileName,
                                           AgentConfig config, AgentAttachmentConfig attachmentConfig,
                                           JSONObject context) throws IOException {
        Request.Builder requestBuilder = new Request.Builder().url(url);

        addHeaders(requestBuilder, attachmentConfig, config, context);

        // 判断请求类型
        String bodyConfig = attachmentConfig.getBody();
        boolean useMultipart = true;

        if (StringUtils.isNotBlank(bodyConfig)) {
            // 如果包含 fileBase64 且不包含 fileContent，使用 JSON 方式
            useMultipart = !bodyConfig.contains("fileBase64") || bodyConfig.contains("fileContent");
        }

        String method = StringUtils.defaultIfBlank(attachmentConfig.getMethod(), "POST").toUpperCase();
        RequestBody requestBody;

        if (useMultipart) {
            requestBody = buildMultipartBody(localFile, fileName, bodyConfig, context);
        } else {
            requestBody = buildJsonBody(localFile, bodyConfig, context);
        }

        requestBuilder.method(method, requestBody);
        return requestBuilder.build();
    }

    private void addHeaders(Request.Builder requestBuilder, AgentAttachmentConfig attachmentConfig,
                            AgentConfig config, JSONObject context) {
        if (StringUtils.isNotBlank(config.getToken())) {
            requestBuilder.header("Authorization", "Bearer " + config.getToken());
        }

        if (StringUtils.isBlank(attachmentConfig.getHeader())) {
            return;
        }

        try {
            Object headerObj = resolveConfigValue(attachmentConfig.getHeader(), context);
            Map<String, Object> headerMap = normalizeToMap(headerObj);
            if (headerMap != null) {
                headerMap.forEach((key, value) -> {
                    if (key != null && value != null && !"Authorization".equalsIgnoreCase(key)) {
                        requestBuilder.header(key, String.valueOf(value));
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Failed to parse header config: {}", attachmentConfig.getHeader(), e);
        }
    }

    /**
     * 构建 Multipart 请求体
     */
    private RequestBody buildMultipartBody(File file, String fileName, String bodyConfig, JSONObject context) {
        String mimeType = MimeTypeUtils.getMimeType(fileName);
        MediaType mediaType = MediaType.parse(mimeType);
        RequestBody fileBody = RequestBody.create(file, mediaType);
        MultipartBody.Builder builder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM);

        if (StringUtils.isBlank(bodyConfig)) {
            // 默认配置：file + user
            builder.addFormDataPart("file", fileName, fileBody);
            if (StringUtils.isNotBlank(context.getString("userId"))) {
                builder.addFormDataPart("user", context.getString("userId"));
            }
        } else {
            // 解析 body 配置
            try {
                Object bodyObj = resolveConfigValue(bodyConfig, context);
                Map<String, Object> bodyMap = normalizeToMap(bodyObj);
                if (bodyMap != null) {
                    bodyMap.forEach((key, value) -> {
                        if (key == null) return;

                        if (isFileContentPlaceholder(value)) {
                            builder.addFormDataPart(key, fileName, fileBody);
                        } else if (value != null) {
                            builder.addFormDataPart(key, String.valueOf(value));
                        }
                    });
                }
            } catch (Exception e) {
                log.warn("Failed to parse body config for multipart: {}", bodyConfig, e);
                // 降级为默认配置
                builder.addFormDataPart("file", fileName, fileBody);
            }
        }

        return builder.build();
    }

    /**
     * 构建 JSON 请求体（Base64 方式）
     */
    private RequestBody buildJsonBody(File file, String bodyConfig, JSONObject context) throws IOException {
        // 将文件内容转为 Base64
        byte[] fileBytes = Files.readAllBytes(file.toPath());
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);
        context.put("fileBase64", base64Content);

        // 解析 body 配置
        Object bodyObj = resolveConfigValue(bodyConfig, context);
        String bodyStr = bodyObj instanceof String ? (String) bodyObj : JsonUtils.toJsonString(bodyObj);

        return RequestBody.create(bodyStr, MediaType.parse("application/json; charset=utf-8"));
    }

    private boolean isFileContentPlaceholder(Object value) {
        if (value == null) return false;
        String str = String.valueOf(value);
        return "${fileContent}".equals(str) || "{{fileContent}}".equals(str);
    }

    private Object resolveConfigValue(String configText, JSONObject context) {
        if (StringUtils.isBlank(configText)) {
            return null;
        }

        try {
            Object parsed = JSON.parse(configText);
            return resolveReferences(parsed, context);
        } catch (Exception notJson) {
            return resolveTextReference(configText, context);
        }
    }

    private Object resolveReferences(Object value, JSONObject context) {
        if (value instanceof JSONObject jsonObject) {
            JSONObject result = new JSONObject(true);
            jsonObject.forEach((key, item) -> result.put(key, resolveReferences(item, context)));
            return result;
        }
        if (value instanceof JSONArray array) {
            JSONArray result = new JSONArray();
            array.forEach(item -> result.add(resolveReferences(item, context)));
            return result;
        }
        if (value instanceof String text) {
            return resolveTextReference(text, context);
        }
        return value;
    }

    private Object resolveTextReference(String text, JSONObject context) {
        Matcher wholeMatcher = VAR_REF_PATTERN.matcher(text);
        if (wholeMatcher.matches()) {
            String path = firstNonBlank(wholeMatcher.group(1), wholeMatcher.group(2));
            Object value = getJsonPathValue(context, path);
            return value == null ? "" : value;
        }
        Matcher matcher = VAR_REF_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String path = firstNonBlank(matcher.group(1), matcher.group(2));
            Object value = getJsonPathValue(context, path);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Object getJsonPathValue(JSONObject params, String path) {
        if (StringUtils.isBlank(path)) return null;
        Object value = params;
        for (String part : path.split("\\.")) {
            if (!(value instanceof JSONObject jsonObject)) return null;
            value = jsonObject.get(part);
        }
        return value;
    }

    private Map<String, Object> normalizeToMap(Object value) {
        if (value == null) return null;
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> { if (k != null) result.put(String.valueOf(k), v); });
            return result;
        }
        if (value instanceof JSONObject json) {
            Map<String, Object> result = new LinkedHashMap<>();
            json.forEach(result::put);
            return result;
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (StringUtils.isNotBlank(v)) return v;
        }
        return null;
    }

    private String extractFileIdFromResponse(String response, String fieldName) {
        if (StringUtils.isBlank(response) || StringUtils.isBlank(fieldName)) {
            return null;
        }
        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            // 支持嵌套字段，如 "data.id"
            String[] parts = fieldName.split("\\.");
            Object value = jsonResponse;
            for (String part : parts) {
                if (value instanceof JSONObject jsonObject) {
                    value = jsonObject.get(part);
                } else {
                    return null;
                }
            }
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.error("Failed to extract file id from response: field={}, response={}", fieldName, response, e);
            return null;
        }
    }

    private String getBaseName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1) {
            return fileName;
        }
        return fileName.substring(0, lastDot);
    }

}
