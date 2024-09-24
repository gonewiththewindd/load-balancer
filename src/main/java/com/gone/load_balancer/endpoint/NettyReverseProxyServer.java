package com.gone.load_balancer.endpoint;

import com.gone.load_balancer.common.ProtocolTypeEnums;
import com.gone.load_balancer.handler.LoadBalanceHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class NettyReverseProxyServer {

    private int port;
    private ProtocolTypeEnums protocolType;
    private LoadBalanceHandler loadBalanceHandler;
    private Channel serverChannel;
    private NioEventLoopGroup bossGroup = new NioEventLoopGroup();
    private NioEventLoopGroup workerGroup = new NioEventLoopGroup();

    public NettyReverseProxyServer(int port, ProtocolTypeEnums protocolType, LoadBalanceHandler loadBalanceHandler) {
        this.port = port;
        this.protocolType = protocolType;
        this.loadBalanceHandler = loadBalanceHandler;
    }

    public void run() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) {
                            switch (protocolType) {
                                case HTTP:
                                    ch.pipeline().addLast(new HttpRequestDecoder());
                                    ch.pipeline().addLast(new HttpRequestDecoder());
                                    ch.pipeline().addLast(new HttpResponseEncoder());
                                    ch.pipeline().addLast(new HttpObjectAggregator(1024 * 1024 * 1024));
                                    ch.pipeline().addLast(new ChunkedWriteHandler());
                                case TCP:
                                    break;
                                case UDP:
                                    break;
                            }
                            ch.pipeline().addLast(loadBalanceHandler);
                        }
                    });

            ChannelFuture f = serverBootstrap.bind(new InetSocketAddress(port)).sync();
            serverChannel = f.channel();
            log.info("Netty reverse proxy server started on port {}, protocol type {}", port, protocolType);
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {
        serverChannel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        log.info("Netty reverse proxy server stop...");
    }
}
