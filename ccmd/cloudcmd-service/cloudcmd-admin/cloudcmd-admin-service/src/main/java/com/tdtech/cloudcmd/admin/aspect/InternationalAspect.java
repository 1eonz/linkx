package com.tdtech.cloudcmd.admin.aspect;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.bean.PageResult;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.i18n.I18nUtil;

@Aspect
@Component
public class InternationalAspect {

    private final String REMARK = "remark";
    private final String FILED_NAME = "name";
    private final String TYPE_NAME = "typeName";
    private final String APP_NAME = "applicationName";
    private final String LABEL = "label";
    private final String PREFIX = "ENG_";
    private final String VALUE = "value";

    private final Logger logger = LoggerFactory.getLogger(InternationalAspect.class);

    /**
     * 简单类型集合，不需要进行国际化处理
     */
    private static final Set<Class<?>> SIMPLE_TYPES = Set.of(
        String.class,
        Integer.class,
        Long.class,
        Double.class,
        Float.class,
        Boolean.class,
        Byte.class,
        Short.class,
        Character.class
    );

    private final String controllerCut = "execution (* com.tdtech.cloudcmd.admin.resource.controller.*.*(..))";

    /**
     * 切入点，所有的controller
     */
    @Pointcut("execution (* com.tdtech.cloudcmd.admin.resource.controller.*.*(..))")
    public void controllerCut() {
        logger.info("所有controller切入--------------");
    }

    /**
     * global切入点
     */
    @Pointcut("execution (* com.tdtech.cloudcmd.admin.resource.controller.GlobalsController.*(..))")
    public void globalCut() {
        logger.info("所有gloabal切入--------------");
    }

    @Around(value = "globalCut()")
    public Object globalAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] args = proceedingJoinPoint.getArgs();
        logger.debug("args: [{}]", JSONObject.toJSONString(args));
        Object result = proceedingJoinPoint.proceed(args);
        if (result instanceof R) {
            Object data = ((R)result).getData();
            if (data instanceof List) {
                List dataList = (List)data;
                for (Object dataObject : dataList) {
                    // 优化：使用辅助方法判断是否为简单类型
                    if (!isSimpleType(dataObject)) {
                        international(dataObject, REMARK);
                    }
                }
                return result;
            }
        }
        return result;
    }

    /**
     * 增强 1.获取方法入参 2.根据入参得到返回对象 3.编辑对象，并返回
     * 
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around(value = controllerCut)
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        logger.debug("请求结果 {}", result);

        if (result instanceof R) {
            Object data = ((R)result).getData();
            logger.debug("The data is {}", data);
            if (data instanceof List) {
                List dataList = (List)data;
                for (Object dataObject : dataList) {
                    logger.debug("The dataObject is {}", dataObject);
                    if (!isSimpleType(dataObject)) {
                        international(dataObject, FILED_NAME);
                        international(dataObject, TYPE_NAME);
                        international(dataObject, APP_NAME);
                        international(dataObject, LABEL);
                    }
                }
                return result;
            }
            /**
             * 分页类型国际化处理
             */
            if (data instanceof PageResult) {
                List records = ((PageResult)data).getRecords();
                if (records == null) {
                    return null;
                }
                for (Object record : records) {
                    international(record, FILED_NAME);
                    international(records, TYPE_NAME);
                    international(records, APP_NAME);
                    international(records, LABEL);
                }
                return result;
            }
        }
        return result;
    }

    /**
     * 反射将对象的name字段国际化
     * 
     * @param object
     */
    private void international(Object object, String filedName) {
        try {
            if (object == null) {
                return;
            }
            Class<?> objectClass = object.getClass();
            Field[] fields = objectClass.getDeclaredFields();
            logger.debug("fileds is [{}]", fields);
            for (Field field : fields) {
                logger.debug("filed name is", field.getName());
                if (filedName.equals(field.getName())) {
                    Field dataFiled = objectClass.getDeclaredField(filedName);
                    dataFiled.setAccessible(true);
                    Object name = dataFiled.get(object);
                    logger.debug("name is {}", name);
                    if (name instanceof String) {
                        if (((String)name).startsWith(PREFIX)) {
                            logger.debug("The name is", name);
                            name = I18nUtil.get((String)name);
                            dataFiled.set(object, name);
                        }
                    }
                }
                // 单独解决stationName
                if (VALUE.equals(field.getName())) {
                    Field dataFiled = objectClass.getDeclaredField(VALUE);
                    dataFiled.setAccessible(true);
                    Object name = dataFiled.get(object);
                    logger.debug("name is {}", name);
                    if (name instanceof String) {
                        if (((String)name).startsWith(PREFIX)) {
                            name = I18nUtil.get((String)name);
                            dataFiled.set(object, name);
                        }
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 判断是否为简单类型（不需要国际化处理）
     * 
     * @param obj 待判断对象
     * @return true-简单类型，false-复杂类型
     */
    private boolean isSimpleType(Object obj) {
        if (obj == null) {
            return true;
        }
        return SIMPLE_TYPES.contains(obj.getClass()) 
            || obj.getClass().isPrimitive()
            || Number.class.isAssignableFrom(obj.getClass());
    }

}
