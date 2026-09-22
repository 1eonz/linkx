package com.tdtech.cloudcmd.i18n;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.i18n.LocaleContextHolder;

public class I18nUtil {

    private static final Logger logger = LoggerFactory.getLogger(I18nUtil.class);
    private static VersionedResourceBundleMessageSource messageSource;

    public I18nUtil(VersionedResourceBundleMessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    public static String getVersion() {
        return messageSource == null ? null : messageSource.getVersion();
    }

    /**
     * 获取单个国际化翻译值
     */
    public static String get(String msgKey) {
        var locale = LocaleContextHolder.getLocale();
        return get(msgKey, locale);
    }

    /**
     * 获取单个国际化翻译值
     */
    public static String get(String msgKey, Locale locale) {
        try {
            return messageSource.getMessage(msgKey, null, locale);
        } catch (Exception e) {
            logger.debug("转换异常:", msgKey);
            return msgKey;
        }
    }

    public static String getSentence(String sentence, String msgKey) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            if (locale == Locale.ENGLISH) {
                return messageSource.getMessage(msgKey, null, LocaleContextHolder.getLocale()) + sentence;
            }
            return sentence + messageSource.getMessage(msgKey, null, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return sentence + msgKey;
        }
    }
}
