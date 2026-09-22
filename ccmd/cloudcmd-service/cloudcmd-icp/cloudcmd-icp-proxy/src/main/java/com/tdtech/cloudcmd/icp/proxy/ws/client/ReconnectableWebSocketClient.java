package com.tdtech.cloudcmd.icp.proxy.ws.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class ReconnectableWebSocketClient extends NettyWebSocketClient {

    private final Object connectLock = new Object();
    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor;
    @Getter
    private volatile boolean ready = false;

    public ReconnectableWebSocketClient(ApplicationEventPublisher eventPublisher,
        Supplier<WebSocketMessageHandler> messageHandler) {
        super(eventPublisher, messageHandler);
        scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
    }

    @Override
    public ReconnectableWebSocketClient connect(URI uri) {
        if (!ready) {
            synchronized (connectLock) {
                if (!ready) {
                    super.connect(uri);
                    // add lost connection listener
                    super.channel().closeFuture().addListener(new WebSocketReconnectHandler());
                    this.ready = true;
                }
            }
        }
        return this;
    }

    @Override
    public ChannelFuture write(WebSocketFrame frame) {
        if (!ready) {
            throw new WebSocketChannelNotReadyException();
        }
        return super.write(frame);
    }

    @Override
    public ChannelFuture writeSync(WebSocketFrame frame) {
        if (!ready) {
            throw new WebSocketChannelNotReadyException();
        }
        return super.writeSync(frame);
    }

    @Override
    public void shutdown() {
        super.shutdown();
        scheduledThreadPoolExecutor.shutdown();
    }

    @Override
    @PreDestroy
    public void preDestroy() {
        this.shutdown();
    }

    /**
     * 重连实现
     */
    public class WebSocketReconnectHandler implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) {
            ready = false;
            log.error("lost connection,waiting for reconnect in 10 seconds");
            if (!ReconnectableWebSocketClient.this.scheduledThreadPoolExecutor.isShutdown()) {
                ReconnectableWebSocketClient.this.scheduledThreadPoolExecutor.schedule(this::retry, 10,
                    TimeUnit.SECONDS);
            } else {
                log.warn("delay executor is shutting down,then cant retry");
            }
        }

        private void retry() {
            try {
                ReconnectableWebSocketClient.this.connect(ReconnectableWebSocketClient.this.getUri());
            } catch (Throwable e) {
                log.error("reconnect failed ,now retry reconnect in 10 seconds", e);
                if (!ReconnectableWebSocketClient.this.scheduledThreadPoolExecutor.isShutdown()) {
                    ReconnectableWebSocketClient.this.scheduledThreadPoolExecutor.schedule(this::retry, 10,
                        TimeUnit.SECONDS);
                } else {
                    log.warn("delay executor is shutting down,then cant retry");
                }
            }
        }
    }
}
