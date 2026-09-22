package com.tdtech.cloudcmd.admin.config;

import java.util.function.Function;

import lombok.Getter;

/**
 * @program: back-new-huawei
 * @description: PlaceholderResolver
 * @author: yj
 * @date: 2024-08-16
 **/
public class PlaceholderResolver {

    /**
     * 默认前缀占位符
     */
    public static final String DEFAULT_PLACEHOLDER_PREFIX = "{";

    /**
     * 默认后缀占位符
     */
    public static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";

    /**
     * 默认单例占位符解析器，即占位符前缀为"{", 后缀为"}"
     */
    @Getter
    private static final PlaceholderResolver defaultResolver = new PlaceholderResolver();

    /**
     * 占位符前缀
     */
    private final String placeholderPrefix = DEFAULT_PLACEHOLDER_PREFIX;

    /**
     * 占位符后缀
     */
    private final String placeholderSuffix = DEFAULT_PLACEHOLDER_SUFFIX;

    /**
     * 根据替换规则来替换指定模板中的占位符值
     *
     * @param content 要解析的字符串
     * @param rule    解析规则回调
     */
    public String resolveByRule(String content, Function<String, String> rule) {
        int start = content.indexOf(this.placeholderPrefix);
        if (start == -1) {
            return content;
        }
        StringBuilder result = new StringBuilder(content);
        while (start != -1) {
            int end = result.indexOf(this.placeholderSuffix, start);
            String placeholder = result.substring(start + this.placeholderPrefix.length(), end);
            String replaceContent = placeholder.trim().isEmpty() ? "" : rule.apply(placeholder);
            result.replace(start, end + this.placeholderSuffix.length(), replaceContent);
            start = result.indexOf(this.placeholderPrefix, start + replaceContent.length());
        }
        return result.toString();
    }

}
