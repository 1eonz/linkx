package com.tdtech.cloudcmd.auth.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.tdtech.cloudcmd.auth.exception.BaseAuthException;
import com.tdtech.cloudcmd.auth.exception.OAuthException;
import com.tdtech.cloudcmd.auth.exception.UnAuthException;
import com.tdtech.cloudcmd.bean.R;
import com.tdtech.cloudcmd.i18n.I18nUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author zhuangzl
 * @date 2020-06-01 14:48
 */
@Slf4j
@ControllerAdvice
@ResponseBody
public class AuthExceptionAdvice {

    /**
     * 401 - Unauthorized
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(BaseAuthException.class)
    public R handleBaseAuthException(BaseAuthException e) {
        return R.failure(e.getCode(), e.getMessage());
    }

    /**
     * 401 - Unauthorized
     */
    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(OAuthException.class)
    public R handleOAuthException(OAuthException e) {
        return R.failure(e.getCode(), e.getMessage(), e.getData());
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnAuthException.class)
    public R handleUnAuthException(UnAuthException e) {
        return R.failure(I18nUtil.get(e.getMessage()));
    }
}
