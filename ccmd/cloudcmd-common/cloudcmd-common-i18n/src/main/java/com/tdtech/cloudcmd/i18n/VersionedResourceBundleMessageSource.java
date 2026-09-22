package com.tdtech.cloudcmd.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.support.AbstractResourceBasedMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePropertiesPersister;
import org.springframework.util.PropertiesPersister;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VersionedResourceBundleMessageSource extends AbstractResourceBasedMessageSource {

    @Getter
    private final String messageNames;
    @Getter
    private final String version;

    // Cache to hold filename lists per Locale
    private final ConcurrentMap<String, Map<Locale, List<String>>> cachedFilenames = new ConcurrentHashMap<>();

    private final Map<String, Properties> propertiesMap = new HashMap<>();

    private final PathMatchingResourcePatternResolver resourceLoader = new PathMatchingResourcePatternResolver();
    private final PropertiesPersister propertiesPersister = ResourcePropertiesPersister.INSTANCE;

    public VersionedResourceBundleMessageSource(String messageNames, String version) throws IOException {
        this.messageNames = messageNames;
        this.version = version;
        if (messageNames == null || messageNames.isBlank()) {
            return;
        }
        var split = messageNames.split(",");
        super.setBasenames(split);
        super.setDefaultEncoding("utf8");
        for (String name : split) {
            if (name.isBlank()) {
                continue;
            }
            var resources = resourceLoader.getResources("classpath*:/" + version + "/" + name + "*.properties");
            for (Resource resource : resources) {
                var properties = loadProperties(resource);
                propertiesMap.put(resource.getFilename(), properties);
            }
        }
    }

    protected Properties loadProperties(Resource resource) throws IOException {
        Properties props = new Properties();
        try (InputStream is = resource.getInputStream()) {
            String encoding = super.getDefaultEncoding();
            if (encoding != null) {
                log.info("Loading properties [{}] with encoding '{}'", resource.getFilename(), encoding);
                this.propertiesPersister.load(props, new InputStreamReader(is, encoding));
            } else {
                log.info("Loading properties [{}]", resource.getFilename());
                this.propertiesPersister.load(props, is);
            }
            return props;
        }
    }

    protected List<String> calculateFilenamesForLocale(String basename, Locale locale) {
        List<String> result = new ArrayList<>(3);
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String variant = locale.getVariant();
        StringBuilder temp = new StringBuilder(basename);

        temp.append('_');
        if (!language.isEmpty()) {
            temp.append(language);
            result.add(0, temp.toString());
        }

        temp.append('_');
        if (!country.isEmpty()) {
            temp.append(country);
            result.add(0, temp.toString());
        }

        if (!variant.isEmpty() && (!language.isEmpty() || !country.isEmpty())) {
            temp.append('_').append(variant);
            result.add(0, temp.toString());
        }

        return result;
    }

    protected List<String> calculateAllFilenames(String basename, Locale locale) {
        Map<Locale, List<String>> localeMap = this.cachedFilenames.get(basename);
        if (localeMap != null) {
            List<String> filenames = localeMap.get(locale);
            if (filenames != null) {
                return filenames;
            }
        }

        // Filenames for given Locale
        List<String> filenames = new ArrayList<>(7);
        filenames.addAll(calculateFilenamesForLocale(basename, locale));

        // Filenames for default Locale, if any
        Locale defaultLocale = getDefaultLocale();
        if (defaultLocale != null && !defaultLocale.equals(locale)) {
            List<String> fallbackFilenames = calculateFilenamesForLocale(basename, defaultLocale);
            for (String fallbackFilename : fallbackFilenames) {
                if (!filenames.contains(fallbackFilename)) {
                    // Entry for fallback locale that isn't already in filenames list.
                    filenames.add(fallbackFilename);
                }
            }
        }

        // Filename for default bundle file
        filenames.add(basename);

        if (localeMap == null) {
            localeMap = new ConcurrentHashMap<>();
            Map<Locale, List<String>> existing = this.cachedFilenames.putIfAbsent(basename, localeMap);
            if (existing != null) {
                localeMap = existing;
            }
        }
        localeMap.put(locale, filenames);
        return filenames;
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        var any = super.getBasenameSet().stream().map(name -> calculateAllFilenames(name, locale))
            .flatMap(Collection::stream).map(name -> name + ".properties").map(propertiesMap::get)
            .filter(Objects::nonNull).filter(a -> a.containsKey(code)).map(a -> a.get(code)).findAny();

        return any.map(o -> super.createMessageFormat(o.toString(), locale)).orElse(null);
    }
}
