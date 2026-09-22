package com.tdtech.cloudcmd.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.listener.InvokerListenerAdapter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Activate(group = {CommonConstants.CONSUMER})
public class InvokerListenerLogger extends InvokerListenerAdapter {
    @Override
    public void referred(Invoker<?> invoker) throws RpcException {
        super.referred(invoker);
        log.info("[DUBBO] referred invoker {} of URL: {}", invoker.getInterface(), invoker.getUrl());
    }

    @Override
    public void destroyed(Invoker<?> invoker) {
        super.destroyed(invoker);
        log.info("[DUBBO] destroyed invoker {} of URL: {}", invoker.getInterface(), invoker.getUrl());
    }
}
