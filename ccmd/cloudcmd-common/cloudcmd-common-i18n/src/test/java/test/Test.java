package test;

import java.util.Locale;

import javax.annotation.Resource;

import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.tdtech.cloudcmd.i18n.CommonLocaleConfig;
import com.tdtech.cloudcmd.i18n.VersionedResourceBundleMessageSource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CommonLocaleConfig.class, properties = "spring.messages.basename=base_servcie")
public class Test {
    @Resource
    private VersionedResourceBundleMessageSource versionedResourceBundleMessageSource;

    @org.junit.Test
    public void test() {

        versionedResourceBundleMessageSource.getMessage("ENG_DICTIONARY_ITEM_140", null, Locale.SIMPLIFIED_CHINESE);
    }
}
