package com.gone.load_balancer.handler;

import io.netty.channel.Channel;
import io.netty.channel.pool.AbstractChannelPoolHandler;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.stereotype.Component;

@Component
public class SimpleChannelPoolHandler extends AbstractChannelPoolHandler {
    @Override
    public void channelCreated(Channel ch) {
        ch.pipeline().addLast("HttpRequestEncoder", new HttpRequestEncoder());
        ch.pipeline().addLast("HttpResponseDecoder", new HttpResponseDecoder());
        ch.pipeline().addLast("HttpObjectAggregator", new HttpObjectAggregator(1024 * 1024 * 1024));
        ch.pipeline().addLast("ChunkedWriteHandler", new ChunkedWriteHandler());
    }
}
