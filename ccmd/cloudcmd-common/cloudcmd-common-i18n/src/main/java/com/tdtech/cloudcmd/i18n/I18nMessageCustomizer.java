package com.tdtech.cloudcmd.i18n;

import java.util.Map;
import java.util.Properties;

public interface I18nMessageCustomizer {

    String[] baseName();

    Map<String, Properties> i18nProperties();

}
