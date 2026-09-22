package com.tdtech.cloudcmd.msip.aop;

import com.tdtech.cloudcmd.bean.UserInfo;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.msip.constant.MSIPConstant;
import com.tdtech.cloudcmd.msip.entity.OperationLog;
import com.tdtech.cloudcmd.msip.enums.OperationTypeEnum;
import com.tdtech.cloudcmd.msip.util.ReportUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Aspect
@Component
public class LogReportAspect {

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final String PREFIX = "ENG_";

    @Resource
    private ReportUtil reportUtil;

    @Around("@annotation(logReport)")
    public Object handleLogReport(ProceedingJoinPoint joinPoint, LogReport logReport) throws Throwable {
        Object result = null;
        OperationLog operationLog = buildParam(joinPoint, logReport);
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.error("handleLogReport error: {}", e.getMessage());
            operationLog.setStatus(MSIPConstant.OPERATION_FAILURE);
            throw e;
        } finally {
            reportUtil.saveOperationLog(operationLog);
        }
        return result;
    }

    private OperationLog buildParam(ProceedingJoinPoint joinPoint, LogReport logReport) {
        OperationTypeEnum type = logReport.type();
        OperationLog log = new OperationLog(type);

        String value = parseValueFromParams(joinPoint);
        boolean hasValue = StringUtils.isNotBlank(value);
        boolean hasFormatFlag = log.getOperation().contains("%s");
        boolean isNeedFormat = hasValue && hasFormatFlag;
        if (isNeedFormat) {
            log.setOperation(String.format(log.getOperation(), value));
        } else if (!hasValue && hasFormatFlag) {
            log.setOperation(log.getOperation().replaceAll("%s", ""));
        }

        UserInfo user = SecurityUtils.getUser();
        if (Objects.nonNull(user)) {
            log.setOperator(user.getUserName());
        }
        return log;
    }

    private String parseValueFromParams(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Annotation[][] anns = sig.getMethod().getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < anns.length; i++) {
            LogReportParam logReportParam = findLogReportParam(anns[i]);
            if (logReportParam == null) continue;

            String value = parseValue(args[i], logReportParam);
            boolean international = logReportParam.international();
            if (!international) {
                // 只取第一个
                return value;
            }
            if (StringUtils.isNotEmpty(value) && value.startsWith(PREFIX)) {
                return I18nUtil.get(value);
            } else {
                return value;
            }

        }
        return "";
    }

    private String parseValue(Object arg, LogReportParam lt) {
        List<?> list = arg instanceof List<?> ? (List<?>)arg : Collections.singletonList(arg);
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        List<String> values = new ArrayList<>(list.size());
        for (Object o : list) {
            ctx.setVariable("it", o);
            try {
                Object val = PARSER.parseExpression("#it." + lt.field()).getValue(ctx);
                values.add(val == null ? "" : val.toString());
            } catch (Exception e) {
                log.error("parseValue error: {}", e.getMessage());
            }
        }
        return String.join(lt.delimiter(), values);
    }

    private LogReportParam findLogReportParam(Annotation[] arr) {
        return Arrays.stream(arr)
                .filter(a -> a instanceof LogReportParam)
                .map(a -> (LogReportParam) a)
                .findFirst()
                .orElse(null);
    }
}
