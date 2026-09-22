package com.tdtech.cloudcmd.cagent.server;

import java.net.InetSocketAddress;

import javax.annotation.Resource;

import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Configuration;

import com.tdtech.cloudcmd.cagent.conf.CagentProperties;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class NettyServerStarter implements SmartLifecycle {
    @Resource
    private CagentProperties cagentProperties;
    @Resource
    private WebSocketChannelInitializer webSocketChannelInitializer;

    private EventLoopGroup workGroup;
    private EventLoopGroup websocketBossGroup;
    private NettyServer webSocketServer;

    private boolean isRunning = false;

    @Override
    public void start() {
        websocketBossGroup = new NioEventLoopGroup(cagentProperties.getBossGroupThread());
        workGroup = new NioEventLoopGroup(cagentProperties.getWorkGroupThread());
        webSocketServer = new NettyServer();
        webSocketServer.start(websocketBossGroup, workGroup, webSocketChannelInitializer, cagentProperties.getWebPort(),
            "ws server");
        isRunning = true;
    }

    @Override
    public void stop() {
        if (webSocketServer != null) {
            webSocketServer.destroy(() -> {
                if (websocketBossGroup != null) {
                    websocketBossGroup.shutdownGracefully();
                }
                if (workGroup != null) {
                    workGroup.shutdownGracefully();
                }
            });
        }
        log.info("netty server been stopped");
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public static class NettyServer {

        private Channel channel;

        public void start(EventLoopGroup bossGroup, EventLoopGroup workGroup,
            ChannelInitializer<Channel> channelInitializer, int port, String name) {
            ServerBootstrap serverBootstrap =
                new ServerBootstrap().group(bossGroup, workGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_RCVBUF, 32 * 1024).childHandler(channelInitializer)
                    .childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.TCP_NODELAY, true);
            if (log.isDebugEnabled()) {
                serverBootstrap.handler(new LoggingHandler(LogLevel.DEBUG));
            }
            ChannelFuture channelFuture =
                serverBootstrap.bind(new InetSocketAddress("0.0.0.0", port)).addListener(f -> {
                    if (f.isDone() && f.isSuccess()) {
                        log.info("{} started on port:{}", name, port);
                    } else {
                        log.error("name:{} isdone:{} issucceed:{} iscancelled:{}", name, f.isDone(), f.isSuccess(),
                            f.isCancelled());
                    }
                });
            channel = channelFuture.syncUninterruptibly().channel();
        }

        public void destroy(Runnable closeRun) {
            if (channel != null) {
                channel.close().addListener(f -> closeRun.run());
            } else {
                closeRun.run();
            }
        }
    }
}
