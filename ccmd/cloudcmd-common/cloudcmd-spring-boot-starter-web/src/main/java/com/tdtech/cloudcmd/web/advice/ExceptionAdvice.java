package com.tdtech.cloudcmd.web.advice;

import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.enums.ResponseCodeEnum;
import com.tdtech.cloudcmd.exception.BusinessException;
import com.tdtech.cloudcmd.i18n.I18nUtil;
import com.tdtech.cloudcmd.web.utils.SecurityUtils;
import com.tdtech.cloudcmd.web.utils.ServletRequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import java.util.Set;

/**
 * 异常处理(自定义异常在BizException在controller里)
 *
 * @author zhangweibo
 * @since 1.0.0
 */
@ControllerAdvice
@ResponseBody
@Slf4j
public class ExceptionAdvice {

    /**
     * 200 - BizException
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BusinessException.class)
    public R handleBizException(BusinessException e) {
        log.warn("业务异常！{}", e.getMessage());
        return R.failure(e.getCode(), I18nUtil.get(e.getMessage()));
    }

    /**
     * 400 - Bad RequestEntity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.error("缺少请求参数", e);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_104.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_104.getMsg()));
    }

    /**
     * 400 - Bad RequestEntity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("参数解析失败", e);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_105.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_105.getMsg()));
    }

    /**
     * 400 - Bad RequestEntity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public R handleBindException(BindException e) {
        log.error("参数绑定失败:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String field = error.getField();
        String code = error.getDefaultMessage();
        String message = String.format("%s:%s", field, code);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_105.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_105.getMsg()) + "参数：" + message);
    }

    /**
     * 400 - Bad RequestEntity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("参数验证失败:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        BindingResult result = e.getBindingResult();
        FieldError error = result.getFieldError();
        String field = error.getField();
        String code = error.getDefaultMessage();
        String message = String.format("%s:%s", field, code);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_106.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_106.getMsg()) + "参数：" + message);
    }

    /**
     * 400 - Bad RequestEntity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public R handleConstraintViolationException(ConstraintViolationException e) {
        log.error("参数验证失败:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        ConstraintViolation<?> violation = violations.iterator().next();
        String message = violation.getMessage();
        return R.failure(ResponseCodeEnum.COMMON_ERROR_106.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_106.getMsg()) + "参数：" + message);
    }

    /**
     * 400 - Bad RequestEntity
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public R handleValidationException(ValidationException e) {
        log.error("参数验证失败:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_106.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_106.getMsg()));
    }

    /**
     * 401 - Unauthorized
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(SecurityException.class)
    public R handleClientException(SecurityException e) {
        log.error("传输参数验证失败:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_108.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_108.getMsg()));
    }

    /**
     * 401 - Unauthorized
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(SecurityUtils.UnAuthException.class)
    public R handleClientException(SecurityUtils.UnAuthException e) {
        try {
            var request = ServletRequestContext.getRequest();
            log.warn("invalid token:{}", request.getRequestURI());
        } catch (Exception ex) {
            //Ignore
            log.warn("invalid token", e);
        }
        return R.failure(ResponseCodeEnum.COMMON_ERROR_107.getCode(), I18nUtil.get(e.getMessage()));
    }

    /**
     * 405 - Method Not Allowed
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("不支持当前请求方法:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_102.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_102.getMsg()));
    }

    /**
     * 415 - Unsupported Media Type
     */
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public R handleHttpMediaTypeNotSupportedException(Exception e) {
        log.error("不支持当前媒体类型:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_103.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_103.getMsg()));
    }

    /**
     * 500 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(SystemException.class)
    public R handleServiceException(SystemException e) {
        log.error("系统内部异常:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_100.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_100.getMsg()));
    }

    /**
     * 500 - Internal Server Error
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public R handleException(Exception e) {
        log.error("未知错误!:{}", ServletRequestContext.getRequest().getRequestURI(), e);
        return R.failure(ResponseCodeEnum.FAILURE.getCode(), I18nUtil.get(ResponseCodeEnum.FAILURE.getMsg()));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R handleMaxSizeException(MaxUploadSizeExceededException exc) {
        log.error("文件上传大小超出限制:{}", ServletRequestContext.getRequest().getRequestURI(), exc);
        return R.failure(ResponseCodeEnum.COMMON_ERROR_181.getCode(),
            I18nUtil.get(ResponseCodeEnum.COMMON_ERROR_181.getMsg()));
    }
}
