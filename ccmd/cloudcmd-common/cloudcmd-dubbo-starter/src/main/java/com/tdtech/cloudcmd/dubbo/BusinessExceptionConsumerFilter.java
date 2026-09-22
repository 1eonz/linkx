package com.tdtech.cloudcmd.dubbo;

import com.tdtech.cloudcmd.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

@Slf4j
@Activate(group = CommonConstants.CONSUMER, order = Integer.MAX_VALUE)
public class BusinessExceptionConsumerFilter implements Filter, Filter.Listener {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        if (appResponse.hasException()) {
            Throwable exception = appResponse.getException();
            // directly throw if it's RpcException
            if ((exception instanceof BusinessRPCException)) {
                appResponse.setException(new BusinessException(exception.getMessage()));
            }
        }
    }

    @Override
    public void onError(Throwable e, Invoker<?> invoker, Invocation invocation) {
    }
}
