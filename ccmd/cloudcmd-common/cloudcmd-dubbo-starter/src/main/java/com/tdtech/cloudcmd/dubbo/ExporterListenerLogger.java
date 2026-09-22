package com.tdtech.cloudcmd.dubbo;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.listener.ExporterListenerAdapter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Activate(group = CommonConstants.PROVIDER)
public class ExporterListenerLogger extends ExporterListenerAdapter {
    @Override
    public void exported(Exporter<?> exporter) throws RpcException {
        super.exported(exporter);
        Invoker<?> invoker = exporter.getInvoker();
        log.info("[DUBBO] registry exported {} of URL: {}", invoker.getInterface(), invoker.getUrl());
    }

    @Override
    public void unexported(Exporter<?> exporter) throws RpcException {
        super.unexported(exporter);
        Invoker<?> invoker = exporter.getInvoker();
        log.info("[DUBBO] registry unexported {} of URL: {}", invoker.getInterface(), invoker.getUrl());
    }
}
