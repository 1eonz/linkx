package com.tdtech.cloudcmd.icp.proxy.ws.client;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.util.concurrent.GenericFutureListener;

import java.net.URI;

public interface WebSocketClient {

    boolean isActive();

    /**
     * 幂等 可以重复调用 重复调用时并不会增加新的链接
     */
    NettyWebSocketClient connect(URI uri);

    /**
     * 异步写数据，会导致写失败日志、异常等丢失</br>
     * 需要手动添加listener或者调用sync方法
     *
     * @see ChannelFuture#addListener(GenericFutureListener)
     */
    ChannelFuture write(WebSocketFrame frame);

    /**
     * 同步写数据，不同于异步方法，添加listener将会失效
     *
     * @see this#write(WebSocketFrame)
     */
    ChannelFuture writeSync(WebSocketFrame frame);

    void closeChannel();
}
