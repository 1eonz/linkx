package com.chinasoft.cloud.module.aiagent.excel.handler;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chinasoft.cloud.framework.common.util.object.BeanUtils;
import com.chinasoft.cloud.framework.utils.IdWorker;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentAttachmentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.AgentConfig;
import com.chinasoft.cloud.module.aiagent.dal.dataobj.Category;
import com.chinasoft.cloud.module.aiagent.dal.mysql.AgentAttachmentConfigMapper;
import com.chinasoft.cloud.module.aiagent.dal.mysql.CategoryMapper;
import com.chinasoft.cloud.module.aiagent.enums.BodyTypeEnum;
import com.chinasoft.cloud.module.aiagent.enums.ScopeEnum;
import com.chinasoft.cloud.module.aiagent.enums.YesNoEnum;
import com.chinasoft.cloud.module.aiagent.excel.bo.AgentConfigBO;
import com.chinasoft.cloud.module.aiagent.excel.bo.ResultBO;
import com.chinasoft.cloud.module.aiagent.excel.verification.AgentCommonVerification;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.chinasoft.cloud.module.aiagent.excel.constant.AgentErrorMessage.ERROR_MESSAGE_FILE_INTERFACE;

@Component
public class AgentConfigHandler {
    public static final Map<String, Integer> PRIORITY_VALUES = Map.of("高", 0, "中", 1, "低", 2);

    public static final Set<String> REQUEST_METHODS = Set.of("GET", "POST", "PUT", "DELETE");

    public static final Set<String> AUDIO_TYPES = Set.of(".mp3", ".webm", ".aac", ".pcm", ".wav", ".amr", ".m4a");

    public static final Set<String> VIDEO_TYPES = Set.of(".mp4", ".mov", ".webm", ".mpeg", ".mpga");

    public static final Set<String> IMAGE_TYPES = Set.of(".jpg", ".svg", ".jpeg", ".gif", ".png", ".bmp", ".webp");

    public static final Set<String> DOCUMENT_TYPES = Set.of(".xml", ".ppt", ".md", ".doc", ".pptx", ".epub", ".msg",
            ".txt", ".docx", ".pdf", ".html", ".markdown", ".csv", ".xlsx", ".xls", ".eml");

    public static final String MULTIMODAL_DELIMITER = "/";

    public static final String AGENT_SHEET_NAME = "AI智能体";

    public static final String AGENT_ATTACHMENT_SHEET_NAME = "AI前置文件接口";

    public static final Integer HEAD_ROW_NUMBER = 3;

    private static final String DEFAULT_AVATAR_PATH = "/images/default-avatar.png";

    private static final String DELIMITER = ",";

    private static final Integer NOT_SUPPORT = 0;

    private static final Integer SUPPORT = 1;

    @Resource
    private IdWorker idWorker;

    @Resource
    private CategoryMapper categoryMapper;

    @Resource
    private AgentAttachmentConfigMapper attachmentConfigMapper;

    public List<AgentConfig> handler(List<AgentConfigBO> configs, ResultBO<AgentConfig> resultBO) {
        Set<String> fileInterfaceSet = configs.stream()
                .map(AgentConfigBO::getFileInterface)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, Long> fileInterface2IdMap = getFileInterfaceName2IdMap(fileInterfaceSet);
        List<AgentConfigBO> filteredAgentConfigs = configs.stream().filter(
                        config -> {
                            boolean result = StringUtils.isBlank(config.getFileInterface())
                                    || fileInterface2IdMap.containsKey(config.getFileInterface());
                            AgentCommonVerification.assemblyErrorMsg(resultBO, config.getRowIndex(),
                                    result, ERROR_MESSAGE_FILE_INTERFACE);
                            return result;
                        })
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(filteredAgentConfigs)) {
            return Collections.emptyList();
        }

        Set<String> categorySet = filteredAgentConfigs.stream().map(config ->
                        config.getCategories().split(DELIMITER))
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());
        Map<String, Long> categoryName2IdMap = getCategoryName2IdMap(categorySet);
        return filteredAgentConfigs.stream().map(config -> {
            AgentConfig agentConfig = BeanUtils.toBean(config, AgentConfig.class);
            handler(config, agentConfig, categoryName2IdMap, fileInterface2IdMap);
            return agentConfig;
        }).collect(Collectors.toList());
    }

    private void handler(AgentConfigBO source, AgentConfig target, Map<String, Long> categoryName2IdMap,
                         Map<String, Long> fileInterface2IdMap) {
        target.setId(idWorker.nextId());
        target.setHeader(StringUtils.isBlank(source.getHeader()) ? null : source.getHeader());
        target.setQuery(StringUtils.isBlank(source.getQuery()) ? null : source.getQuery());
        target.setBody(StringUtils.isBlank(source.getBody()) ? null : source.getBody());
        target.setBodyType(BodyTypeEnum.nameOf(source.getBodyTypeStr()).getType());
        target.setEndFlag(YesNoEnum.nameOf(source.getEndFlagStr()).getType());
        target.setPriority(PRIORITY_VALUES.get(source.getPriorityStr()));
        target.setAvatar(DEFAULT_AVATAR_PATH);
        target.setCategoryIds(getCategoryIds(source, categoryName2IdMap));
        target.setIsRestricted(0);
        target.setReceiveIm(YesNoEnum.nameOf(source.getReceiveImStr(), YesNoEnum.NO).getType());
        target.setScope(ScopeEnum.nameOf(source.getScopeStr()).getType());
        boolean audioTypeIsBlank = StringUtils.isBlank(source.getAudioTypeStr());
        target.setAudio(audioTypeIsBlank ? NOT_SUPPORT : SUPPORT);
        target.setAudioType(getMultimodalType(audioTypeIsBlank, source.getAudioTypeStr()));

        boolean videoTypeIsBlank = StringUtils.isBlank(source.getVideoTypeStr());
        target.setVideo(videoTypeIsBlank ? NOT_SUPPORT : SUPPORT);
        target.setVideoType(getMultimodalType(videoTypeIsBlank, source.getVideoTypeStr()));

        boolean imageTypeIsBlank = StringUtils.isBlank(source.getImageTypeStr());
        target.setImage(imageTypeIsBlank ? NOT_SUPPORT : SUPPORT);
        target.setImageType(getMultimodalType(imageTypeIsBlank, source.getImageTypeStr()));

        boolean documentTypeIsBlank = StringUtils.isBlank(source.getDocumentTypeStr());
        target.setDocument(documentTypeIsBlank ? NOT_SUPPORT : SUPPORT);
        target.setDocumentType(getMultimodalType(documentTypeIsBlank, source.getDocumentTypeStr()));

        target.setFileInterfaceId(StringUtils.isBlank(source.getFileInterface()) ?
                null : fileInterface2IdMap.get(source.getFileInterface()));
    }

    private String getMultimodalType(boolean isBlank, String source) {
        if (isBlank) {
            return null;
        }

        List<String> audioTypes = Arrays.stream(source
                        .split(MULTIMODAL_DELIMITER))
                .distinct()
                .collect(Collectors.toList());
        return JSON.toJSONString(audioTypes);
    }

    private String getCategoryIds(AgentConfigBO source, Map<String, Long> categoryName2IdMap) {
        String[] categories = source.getCategories().split(DELIMITER);
        return Arrays.stream(categories).map(categoryName ->
                        String.valueOf(categoryName2IdMap.get(categoryName)))
                .collect(Collectors.joining(DELIMITER));
    }

    private Map<String, Long> getFileInterfaceName2IdMap(Set<String> fileInterfaceSet) {
        if (CollectionUtils.isEmpty(fileInterfaceSet)) {
            return Collections.emptyMap();
        }

        List<AgentAttachmentConfig> existingFileInterfaces = attachmentConfigMapper
                .selectList(Wrappers.lambdaQuery(AgentAttachmentConfig.class)
                        .in(AgentAttachmentConfig::getName, fileInterfaceSet));
        return existingFileInterfaces.stream().collect(Collectors
                .toMap(AgentAttachmentConfig::getName, AgentAttachmentConfig::getId));
    }

    private Map<String, Long> getCategoryName2IdMap(Set<String> categorySet) {
        Map<String, Long> categoryName2IdMap = new HashMap<>();
        List<Category> existingCategories = categoryMapper.selectList(Wrappers.lambdaQuery(Category.class)
                .in(Category::getName, categorySet));
        if (!CollectionUtils.isEmpty(existingCategories)) {
            categoryName2IdMap.putAll(existingCategories.stream()
                    .collect(Collectors.toMap(Category::getName, Category::getId)));
            categorySet.removeAll(categoryName2IdMap.keySet());
        }

        List<Category> insertCategoryList = categorySet.stream().map(category -> {
            Category newCategory = new Category();
            newCategory.setName(category);
            newCategory.setId(idWorker.nextId());
            categoryName2IdMap.put(newCategory.getName(), newCategory.getId());

            return newCategory;
        }).collect(Collectors.toList());
        categoryMapper.insert(insertCategoryList);

        return categoryName2IdMap;
    }
}