package com.tdtech.cloudcmd.base.aspect;

import java.lang.reflect.Field;
import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.i18n.I18nUtil;

@Aspect
@Component
public class InternationalAspect {

    private final String REMARK = "remark";
    private final String FILED_NAME = "name";
    private final String PREFIX = "ENG_";
    private final Logger logger = LoggerFactory.getLogger(InternationalAspect.class);

    /**
     * 切入点，所有的controller
     */
    @Pointcut("execution ( * com.tdtech.cloudcmd.base.controller.*.*(..))")
    public void controllerCut() {}

    /**
     * global切入点
     */
    @Pointcut("execution ( * com.tdtech.cloudcmd.base.controller.GlobalsController.*(..))")
    public void globalCut() {}

    /**
     * 增强 1.获取方法入参 2.根据入参得到返回对象 3.编辑对象，并返回
     * 
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around(value = "controllerCut()")
    public Object around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] args = proceedingJoinPoint.getArgs();
        logger.debug("args: [{}]", JSONObject.toJSONString(args));
        Object result = proceedingJoinPoint.proceed(args);
        if (result instanceof R) {
            Object data = ((R)result).getData();
            if (data instanceof List) {
                List dataList = (List)data;
                for (Object dataObject : dataList) {
                    international(dataObject, FILED_NAME);
                }
                return result;
            }
            if (data instanceof String) {
                if (((String)data).startsWith(PREFIX)) {
                    data = I18nUtil.get((String)data);
                    ((R)result).setData(data);
                    return result;
                }
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
            for (Field field : fields) {
                if (filedName.equals(field.getName())) {
                    Field dataFiled = objectClass.getDeclaredField(filedName);
                    dataFiled.setAccessible(true);
                    Object name = dataFiled.get(object);
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

}
