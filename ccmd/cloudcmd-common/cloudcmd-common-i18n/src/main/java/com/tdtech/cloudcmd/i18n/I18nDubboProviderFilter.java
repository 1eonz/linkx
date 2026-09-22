package com.tdtech.cloudcmd.i18n;

import java.util.Locale;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.springframework.context.i18n.LocaleContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Activate(group = CommonConstants.PROVIDER)
public class I18nDubboProviderFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            RpcContext context = RpcContext.getContext();
            if (context != null) {
                String lan = context.getAttachment("cloudcmd-i18n-language");
                if (lan != null && lan.length() > 0) {
                    LocaleContextHolder.setLocale(Locale.forLanguageTag(lan));
                    log.debug("I18nDubboProviderFilter lan:{}", LocaleContextHolder.getLocale());
                }
            }
        } catch (Throwable e) {
            log.error("", e);
        }
        return invoker.invoke(invocation);
    }

}
