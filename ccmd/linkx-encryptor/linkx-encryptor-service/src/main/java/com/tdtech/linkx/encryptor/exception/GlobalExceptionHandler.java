package com.tdtech.linkx.encryptor.exception;

import com.tdtech.linkx.encryptor.resp.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public R<String> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return R.failure(e.getMessage());
    }

    /**
     * 处理所有异常
     */
    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("系统异常", e);
        return R.failure("系统异常: " + e.getMessage());
    }
}
