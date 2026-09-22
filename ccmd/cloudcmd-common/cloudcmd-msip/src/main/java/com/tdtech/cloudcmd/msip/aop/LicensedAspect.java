package com.tdtech.cloudcmd.msip.aop;

import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.enums.LicenseEnum;
import com.tdtech.cloudcmd.msip.util.LicenseUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LicensedAspect {

    @Resource
    private LicenseUtil licenseUtil;

    @Around("@annotation(com.tdtech.cloudcmd.msip.aop.Licensed)||@within(com.tdtech.cloudcmd.msip.aop.Licensed)")
    public Object joinPoint(ProceedingJoinPoint pjp) throws Throwable {
        /* 优先级：方法注解 > 类注解 */
        Licensed methodAnno = ((MethodSignature) pjp.getSignature())
                .getMethod()
                .getAnnotation(Licensed.class);
        Licensed classAnno = pjp.getTarget().getClass()
                .getAnnotation(Licensed.class);
        Licensed anno = methodAnno != null ? methodAnno : classAnno;

        String code = anno.module().getCode();
        boolean available = isAvailable(code);
        if (!available) {
            throw new BusinessException(anno.message());
        }
        return pjp.proceed();
    }


    private boolean isAvailable(String code) {
        Map<String, String> map = licenseUtil.getMSIPLicenseItemMap();
        if (CollectionUtils.isEmpty(map)) {
            return false;
        }
        // LINKXBS 值为0则表示系统不可用，所有的功能都不可用，这个是前置条件
        String baseValue = map.get(LicenseEnum.ALL.getCode());
        if (StringUtils.isBlank(baseValue) || MSIPConstant.NOT_AVAILABLE.equals(baseValue)) {
            return false;
        }
        // 比较功能项的值
        String matchedValue = map.get(code);
        if (StringUtils.isNotBlank(matchedValue) && MSIPConstant.AVAILABLE.equals(matchedValue)) {
            return true;
        }

        return false;
    }

}
