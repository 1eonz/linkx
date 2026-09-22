package com.tdtech.cloudcmd.i18n;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import lombok.Getter;

@Getter
public class CustomizedResourceBundleMessageSource extends VersionedResourceBundleMessageSource {

    private final ObjectProvider<I18nMessageCustomizer> i18nMessageCustomizers;
    private final Set<String> basenameSet = new LinkedHashSet<>();

    public CustomizedResourceBundleMessageSource(String messageNames, String version,
        ObjectProvider<I18nMessageCustomizer> i18nMessageCustomizers) throws IOException {
        super(messageNames, version);
        this.i18nMessageCustomizers = i18nMessageCustomizers;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        Map<String, Properties> propertiesMap = new HashMap<>();
        i18nMessageCustomizers.stream().map(I18nMessageCustomizer::i18nProperties).forEach(propertiesMap::putAll);
        var any = i18nMessageCustomizers.stream().map(I18nMessageCustomizer::baseName).flatMap(Arrays::stream)
            .map(name -> calculateAllFilenames(name, locale)).flatMap(Collection::stream).map(propertiesMap::get)
            .filter(Objects::nonNull)
            .filter(a -> a.containsKey(code)).map(a -> a.get(code)).findAny();
        if (any.isPresent()) {
            return any.map(o -> super.createMessageFormat(o.toString(), locale)).orElse(null);
        } else {
            return super.resolveCode(code, locale);
        }
    }
}
