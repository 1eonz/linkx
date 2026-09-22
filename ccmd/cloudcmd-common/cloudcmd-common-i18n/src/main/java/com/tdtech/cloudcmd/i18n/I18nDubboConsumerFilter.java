package com.tdtech.cloudcmd.i18n;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.springframework.context.i18n.LocaleContextHolder;

@Activate(group = CommonConstants.CONSUMER)
public class I18nDubboConsumerFilter implements Filter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String lan = LocaleContextHolder.getLocale().toLanguageTag();
        RpcContext context = RpcContext.getContext();
        if (context != null) {
            context.setAttachment("cloudcmd-i18n-language", lan);
        }
        return invoker.invoke(invocation);
    }
}
