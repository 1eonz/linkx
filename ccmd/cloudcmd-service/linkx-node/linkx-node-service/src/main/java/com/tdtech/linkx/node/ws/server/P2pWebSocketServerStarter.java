package com.tdtech.linkx.node.ws.server;

import com.tdtech.linkx.node.util.ChallengeManager;
import com.tdtech.linkx.node.util.P2pJwtUtil;
import com.tdtech.linkx.node.config.NodeProperties;
import com.tdtech.linkx.node.service.ICoopShareMessageHandler;
import com.tdtech.linkx.node.service.IPeerNodeClientService;
import com.tdtech.linkx.node.service.IPeerNodeStatusService;
import com.tdtech.linkx.node.service.IWsAuthGrantMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Slf4j
@Component
public class P2pWebSocketServerStarter implements SmartLifecycle {

    @Resource
    private NodeProperties nodeProperties;

    @Resource
    private ChallengeManager challengeManager;

    @Resource
    private P2pJwtUtil p2pJwtUtil;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private IPeerNodeClientService clientService;

    @Resource
    private IPeerNodeStatusService peerNodeStatusService;

    @Resource
    private IWsAuthGrantMessageHandler authGrantMessageHandler;

    @Resource
    private ICoopShareMessageHandler coopShareMessageHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private volatile boolean running = false;

    @Override
    public void start() {
        if (!nodeProperties.getServer().isEnabled()) {
            log.info("P2P WebSocket Server is disabled");
            return;
        }

        int port = nodeProperties.getServer().getPort();
        log.info("Starting P2P WebSocket Server on port: {}", port);

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new P2pWebSocketServerInitializer(
                            challengeManager,
                            p2pJwtUtil,
                            sessionManager,
                            clientService,
                            peerNodeStatusService,
                            coopShareMessageHandler,
                            authGrantMessageHandler
                    ));

            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            running = true;
            log.info("P2P WebSocket Server started successfully on port: {}", port);

        } catch (InterruptedException e) {
            log.error("P2P WebSocket Server start interrupted", e);
            Thread.currentThread().interrupt();
            shutdown();
        }
    }

    @Override
    public void stop() {
        log.info("Stopping P2P WebSocket Server...");
        shutdown();
        running = false;
        log.info("P2P WebSocket Server stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 10;
    }

    @PreDestroy
    public void preDestroy() {
        stop();
    }

    private void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }

}
