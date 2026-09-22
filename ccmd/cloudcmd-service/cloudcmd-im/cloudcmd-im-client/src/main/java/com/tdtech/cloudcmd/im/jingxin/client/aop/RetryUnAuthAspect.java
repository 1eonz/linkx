package com.tdtech.cloudcmd.im.jingxin.client.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.tdtech.cloudcmd.im.jingxin.client.ImHttpClient;
import com.tdtech.cloudcmd.web.utils.HttpClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class RetryUnAuthAspect {


  @Around("@annotation(com.tdtech.cloudcmd.im.jingxin.client.aop.RetryUnauth)")
  public Object handleRetryUnauth(ProceedingJoinPoint joinPoint) throws Throwable {
    // 增强逻辑：在方法执行前、后或异常时进行处理
    try {
      // 执行原始方法
      return joinPoint.proceed();
    } catch (HttpClient.HttpStatusException e) {
      if (e.getStatus() == HttpStatus.UNAUTHORIZED) {
        log.warn("retry unauth", e);
          var target = (ImHttpClient) joinPoint.getTarget();
          target.refreshToken();
        return joinPoint.proceed();
      } else {
        throw e;
      }
    }
  }
}
