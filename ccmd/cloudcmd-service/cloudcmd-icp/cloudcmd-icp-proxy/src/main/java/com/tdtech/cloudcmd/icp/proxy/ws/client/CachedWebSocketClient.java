package com.tdtech.cloudcmd.icp.proxy.ws.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;

import javax.annotation.PreDestroy;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class CachedWebSocketClient extends ReconnectableWebSocketClient {

    private final BlockingQueue<WebSocketFrame> cache;

    public CachedWebSocketClient(ApplicationEventPublisher eventPublisher,
        Supplier<WebSocketMessageHandler> messageHandler) {
        super(eventPublisher, messageHandler);
        cache = new ArrayBlockingQueue<>(1000);
    }

    @Override
    public CachedWebSocketClient connect(URI uri) {
        super.connect(uri);
        Channel channel = super.channel();
        cache.forEach(msg -> channel.writeAndFlush(msg).syncUninterruptibly());
        return this;
    }

    /**
     * channel not ready时 将消息缓存在queue中， channel ready后 直接发消息
     */
    @Override
    public ChannelFuture write(WebSocketFrame frame) {
        Objects.requireNonNull(frame);
        if (!super.isReady()) {
            addCache(frame);
            return null;
        }
        return super.write(frame);
    }

    @Override
    public ChannelFuture writeSync(WebSocketFrame frame) {
        Objects.requireNonNull(frame);
        if (!super.isReady()) {
            addCache(frame);
            return null;
        }
        return super.writeSync(frame);
    }

    private void addCache(WebSocketFrame frame) {
        try {
            if (!cache.offer(frame, 3, TimeUnit.SECONDS)) {
                throw new WebSocketCacheException();
            }
        } catch (InterruptedException e) {
            log.error("offer msg interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    @PreDestroy
    public void preDestroy() {
        this.shutdown();
    }

    public static class WebSocketCacheException extends RuntimeException {
    }
}
