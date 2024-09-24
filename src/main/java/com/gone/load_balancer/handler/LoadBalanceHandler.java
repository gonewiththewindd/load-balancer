package com.gone.load_balancer.handler;

import com.gone.load_balancer.common.LoadBalanceEnums;
import com.gone.load_balancer.rule.Router;
import com.gone.load_balancer.strategy.impl.ComposeLoadBalanceStrategy;
import com.gone.load_balancer.strategy.LBParams;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ChannelHandler.Sharable
public class LoadBalanceHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Autowired
    private Router router;
    @Autowired
    private ComposeLoadBalanceStrategy composeLoadBalanceStrategy;

    @Autowired
    private ChannelPoolHandler channelPoolHandler;

    private Map<String, ChannelPool> channelPoolMap = new ConcurrentHashMap<>();

    private Map<String, Upstream> upstreamMap = new HashMap<>() {{
        Service service1 = new Service();
        service1.setServiceName("user-service-instance-1");
        service1.setIp("127.0.0.1");
        service1.setHost("127.0.0.1");
        service1.setWeight(1);
        service1.setPort(8087);

        Service service2 = new Service();
        service2.setServiceName("user-service-instance-2");
        service2.setIp("127.0.0.1");
        service2.setHost("127.0.0.1");
        service2.setWeight(2);
        service2.setPort(8088);

        Service service3 = new Service();
        service3.setServiceName("user-service-instance-3");
        service3.setIp("127.0.0.1");
        service3.setHost("127.0.0.1");
        service3.setPort(8089);
        service3.setWeight(3);
        Upstream upstream = new Upstream("user-service", LoadBalanceEnums.LEAST_CONNECTION, Arrays.asList(service1, service2, service3));
        put("user-service", upstream);
    }};

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
//        log.info("msg:{}", request.toString());
        String requestURI = request.uri();
        String upstreamId = router.route(requestURI);
        if (Objects.nonNull(upstreamId)) {
            Upstream upstream = upstreamMap.get(upstreamId);
            String remoteAddr = ctx.channel().remoteAddress().toString();
            LBParams lbParams = new LBParams().setRequestURI(requestURI).setRemoteAddr(remoteAddr);
            Service service = composeLoadBalanceStrategy.loadBalance(lbParams, upstream);
            String key = service.getIp().concat(":").concat(String.valueOf(service.getPort()));
            ChannelPool channelPool = channelPoolMap.computeIfAbsent(key, k -> new SimpleChannelPool(getBootstrap(service), channelPoolHandler));
            Future<Channel> proxiedChannelFuture = channelPool.acquire();

            request.retain();
            proxiedChannelFuture.addListener((FutureListener<Channel>) future -> {
                if (future.isSuccess()) {
                    service.activeConnections.incrementAndGet(); // 连接数统计
                    Channel ch = future.getNow();
                    if (Objects.nonNull(ch.pipeline().get("ReverseProxyHandler"))) {
                        // 覆盖反向代理处理器 源客户端和当前路由service
                        ReverseProxyHandler reverseProxyHandler = ((ReverseProxyHandler) ch.pipeline().get("ReverseProxyHandler"));
                        reverseProxyHandler.setContext(ctx);
                        reverseProxyHandler.setService(service);
                    } else {
                        // 响应透传
                        ch.pipeline().addLast("ReverseProxyHandler", new ReverseProxyHandler(ctx, service));
                    }
                    String[] split = requestURI.split("/");
                    String truncateRequestURI = "/" + String.join("/", Arrays.copyOfRange(split, 2, split.length));
                    request.setUri(truncateRequestURI);
                    ch.writeAndFlush(request);
                    // 连接释放回连接池
                    channelPool.release(ch);
                } else {
                    ctx.channel().writeAndFlush(new String("failed to connect to remote service").getBytes(StandardCharsets.UTF_8));
                }
            });
        } else {
            ctx.channel().writeAndFlush(new String("resource not found").getBytes(StandardCharsets.UTF_8));
        }
    }

    private static Bootstrap getBootstrap(Service service) {
        NioEventLoopGroup eventExecutors = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(eventExecutors)
                .channel(NioSocketChannel.class)
                .remoteAddress(service.getIp(), service.getPort());
        return bootstrap;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught", cause);
    }
}
