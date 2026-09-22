package com.tdtech.cloudcmd.util;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import lombok.extern.slf4j.Slf4j;

/**
 * Class Name: XmlConvertUtil Description: xml格式转化工具类
 *
 * @author cWX576353
 */
@Slf4j
@SuppressWarnings("unchecked")
public final class XmlConvertUtil {

    /**
     * 通过JAXB生成XML字符串
     *
     * @param obj
     * @param withoutFragment 是否带XML头信息
     * @return
     */
    public static String obj2XML(Object obj, Boolean withoutFragment) {
        StringWriter sw = new StringWriter();
        String xml = null;
        try {
            // 利用jdk中自带的转换类实现
            JAXBContext context = JAXBContext.newInstance(obj.getClass());

            Marshaller marshaller = context.createMarshaller();
            // 格式化xml输出的格式
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, withoutFragment);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            // 将对象转换成输出流形式的xml
            marshaller.marshal(obj, sw);
            xml = sw.toString();
        } catch (JAXBException e) {
            log.error("", e);
            e.printStackTrace();
        }
        return xml;
    }

    /**
     * 通过JAXB将XML转化为Object
     *
     * @param clazz
     * @param xml
     * @return
     */
    public static <T> T xml2Obj(Class<T> clazz, String xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            Unmarshaller um = jaxbContext.createUnmarshaller();
            return (T)um.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            log.error("JAXBException:", e);
        } catch (Exception e) {
            log.error("Exception:", e);
        }
        return null;
    }

}