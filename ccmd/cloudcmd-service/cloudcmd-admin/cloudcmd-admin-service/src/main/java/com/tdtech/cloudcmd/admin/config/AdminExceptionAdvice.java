package com.tdtech.cloudcmd.admin.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.tdtech.cloudcmd.admin.exception.AdminException;
import com.tdtech.cloudcmd.auth.PwdUtil;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lsm
 * @date 2020-06-01 14:48
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
@ResponseBody
public class AdminExceptionAdvice {

    /**
     * 处理AdminException
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(AdminException.class)
    public R handleOAuthException(AdminException e) {
        log.error(e.getMessage(), e);
        return R.failure(e.getCode(), I18nUtil.get(e.getMessage()), e.getData());
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(PwdUtil.PwdException.class)
    public R handlePwdException(PwdUtil.PwdException e) {
        log.error("", e);
        return R.failure(e.getCode(), I18nUtil.get(e.getMessage()));
    }
}
