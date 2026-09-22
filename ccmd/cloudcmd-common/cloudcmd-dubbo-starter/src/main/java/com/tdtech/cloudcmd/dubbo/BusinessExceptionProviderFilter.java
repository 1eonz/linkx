package com.tdtech.cloudcmd.dubbo;

import com.tdtech.cloudcmd.exception.BusinessException;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.service.GenericService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Activate(group = CommonConstants.PROVIDER, order = Integer.MAX_VALUE)
public class BusinessExceptionProviderFilter implements Filter, Filter.Listener {
    private static final Logger logger = LoggerFactory.getLogger(BusinessExceptionProviderFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (appResponse.hasException() && GenericService.class != invoker.getInterface()) {
            try {
                Throwable exception = appResponse.getException();

                // directly throw if it's checked exception
                if (!(exception instanceof BusinessException)) {
                    return;
                }
                // 返回自定义异常
                var bizExp = (BusinessException)exception;
                appResponse.setException((new BusinessRPCException(bizExp.getMessage(), bizExp)));
            } catch (Throwable e) {
                logger.warn("Fail to ExceptionFilter when called by {}. service: {}, method: {}, exception: {}: {}",
                    RpcContext.getServiceContext().getRemoteAddressString(), invoker.getInterface().getName(),
                    invocation.getMethodName(), e.getClass().getName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        logger.error("Error occurred when calling service: {}, method: {}, exception: {}: {}",
            invoker.getInterface().getName(), invocation.getMethodName(), t.getClass().getName(), t.getMessage(), t);
    }
}