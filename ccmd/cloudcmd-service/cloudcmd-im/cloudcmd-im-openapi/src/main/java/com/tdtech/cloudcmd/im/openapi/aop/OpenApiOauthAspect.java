package com.tdtech.cloudcmd.im.openapi.aop;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.im.openapi.controller.entity.Token;
import com.tdtech.cloudcmd.im.openapi.service.OpenApiOAuthService;
import com.tdtech.cloudcmd.web.utils.ServletRequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Aspect
@Configuration
@RequiredArgsConstructor
public class OpenApiOauthAspect {

    private final OpenApiOAuthService openApiOAuthService;

    @Around("@annotation(com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth)||@within(com.tdtech.cloudcmd.im.openapi.aop.OpenApiOauth)")
    public Object joinPoint(ProceedingJoinPoint pjp) throws Throwable {
        // try class
        var openApiOauth = getAnnotationFromClass(pjp);
        // try method
        if (openApiOauth == null) {
            openApiOauth = getAnnotationFromMethod(pjp);
        }
        if (openApiOauth == null) {
            log.warn("OpenApiOauth join point not found");
            return pjp.proceed();
        }
        var request = ServletRequestContext.getRequest();
        String authHeader;
        Token token;
        if (request == null || (authHeader = request.getHeader("Authorization")) == null || authHeader.isBlank()
                || (token = openApiOAuthService.getToken(authHeader)) == null) {
            return R.failure("token验证失败");
        }
//        if (token.getScope() == null || !token.getScope().contains(openApiOauth.value().getCode())) {
//            return R.failure("不具有"+openApiOauth.value().getMsg()+"权限");
//        }
        try {
            OAuthContext.setToken(token);
            return pjp.proceed();
        } finally {
            OAuthContext.clear();
        }
    }

    private OpenApiOauth getAnnotationFromMethod(ProceedingJoinPoint pjp) {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        // 获取目标方法
        var method = signature.getMethod();
        // 获取方法上的OpenApiOauth注解
        return method.getAnnotation(OpenApiOauth.class);
    }

    private OpenApiOauth getAnnotationFromClass(ProceedingJoinPoint pjp) {
        // 获取目标对象
        Object target = pjp.getTarget();
        // 使用Spring的AopProxyUtils获取最终目标类（解包代理类）
        Class<?> userClass = AopProxyUtils.ultimateTargetClass(target);
        // 获取类上的OpenApiOauth注解
        return userClass.getAnnotation(OpenApiOauth.class);
    }
}
