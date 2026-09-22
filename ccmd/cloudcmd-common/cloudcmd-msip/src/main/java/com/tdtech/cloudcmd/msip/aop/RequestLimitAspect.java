package com.tdtech.cloudcmd.msip.aop;

import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.enums.AlarmTemplateZhEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.util.CollectionUtils;
import com.tdtech.cloudcmd.util.DateFormatUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestLimitAspect {

    @Resource
    private ReportUtil reportUtil;

    @Autowired
    private KeyedSecondCounter counter;


    @Around("@annotation(com.tdtech.cloudcmd.msip.aop.RequestLimit)||@within(com.tdtech.cloudcmd.msip.aop.RequestLimit)")
    public Object joinPoint(ProceedingJoinPoint pjp) throws Throwable {
        RequestLimit methodAnno = ((MethodSignature) pjp.getSignature())
                .getMethod()
                .getAnnotation(RequestLimit.class);
        RequestLimit classAnno = pjp.getTarget().getClass()
                .getAnnotation(RequestLimit.class);
        RequestLimit anno = methodAnno != null ? methodAnno : classAnno;
//        String className  = pjp.getTarget().getClass().getName();
        counter.incr(anno.business());

        return pjp.proceed();
    }

    @Scheduled(initialDelay = 10L * 1000L, fixedDelay = 1L * 1000L)
    public void check() {
        String now = DateFormatUtil.format(new Date());
        Map<String, Integer> snapshot = counter.rotate();
        if (CollectionUtils.isEmpty(snapshot)) {
            clearAlarm();
        }
        snapshot.forEach((key, cnt) -> {
            if (cnt > MSIPConstant.MAX_LIMIT_COUNT) {
                String detail = String.format("%s业务接口超限：限制数 %d, 当前数：%d", key, MSIPConstant.MAX_LIMIT_COUNT, cnt);
                log.warn(detail);
                reportAlarm(key, MSIPConstant.MAX_LIMIT_COUNT);
            } else {
                clearAlarm();
            }
        });
    }


    private void reportAlarm(Object... params) {
        // 发送告警
        reportUtil.saveAlarm2MSIP(AlarmTemplateZhEnum.TOO_MANY_REQUESTS, params);
    }

    private void clearAlarm() {
        // 擦除告警
        reportUtil.clearAlarm2MSIP(AlarmTemplateZhEnum.TOO_MANY_REQUESTS);
    }

}
