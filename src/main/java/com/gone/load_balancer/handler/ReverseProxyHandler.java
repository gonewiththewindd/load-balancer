package com.gone.load_balancer.handler;

import com.gone.load_balancer.upstream.Service;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ReverseProxyHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private ChannelHandlerContext context;
    private Service service;

    public ReverseProxyHandler(ChannelHandlerContext context, Service service) {
        this.context = context;
        this.service = service;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
        response.retain();
        context.channel().writeAndFlush(response);
        service.activeConnections.decrementAndGet();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        context.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage(), cause);
    }
}
