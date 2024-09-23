package com.gone.load_balancer.reverse;

import com.gone.load_balancer.rule.Router;
import com.gone.load_balancer.strategy.LBParams;
import com.gone.load_balancer.strategy.LoadBalanceStrategy;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@ChannelHandler.Sharable
public class LoadBalanceHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Autowired
    private Router router;

    @Autowired
    private Map<String, LoadBalanceStrategy> strategyMap;

    private Map<String, Upstream> upstreamMap = new HashMap<>() {{
        Service service1 = new Service();
        service1.setServiceName("user-service-instance-1");
        service1.setIp("127.0.0.1");
        service1.setHost("127.0.0.1");
        service1.setPort(8088);
        Upstream upstream = new Upstream("user-service", null, Arrays.asList(service1));
        put("user-service", upstream);
    }};

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws InterruptedException {
//        log.info("msg:{}", request.toString());
        String requestURI = request.uri();
        String upstreamId = router.route(requestURI);
        if (Objects.nonNull(upstreamId)) {
            Upstream upstream = upstreamMap.get(upstreamId);
            LoadBalanceStrategy balanceStrategy = strategyMap.get(upstream.getLbe().getValue());
            String remoteAddr = ctx.channel().remoteAddress().toString();
            LBParams lbParams = new LBParams().setRequestURI(requestURI).setRemoteAddr(remoteAddr);
            Service service = balanceStrategy.loadBalance(lbParams, upstream);

            ChannelFuture channelFuture = new Bootstrap()
                    .group(ctx.channel()
                            .eventLoop())
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 响应透传
                            ch.pipeline().addLast(new HttpRequestEncoder());
                            ch.pipeline().addLast(new HttpResponseDecoder());
                            ch.pipeline().addLast(new HttpObjectAggregator(65536));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new ReverseProxyHandler(ctx));
                        }
                    }).remoteAddress(service.getIp(), service.getPort())
                    .connect();
            request.retain();
            channelFuture.addListener((ChannelFutureListener) future -> {
                String[] split = requestURI.split("/");
                String truncateRequestURI = "/" + String.join("/", Arrays.copyOfRange(split, 2, split.length));
                request.setUri(truncateRequestURI);
                future.channel().writeAndFlush(request);
            });
        } else {
            ctx.channel().writeAndFlush(new String("resource not found").getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught", cause);
    }
}
