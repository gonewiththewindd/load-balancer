package com.gone.load_balancer;

import com.gone.load_balancer.common.ProtocolTypeEnums;
import com.gone.load_balancer.endpoint.NettyReverseProxyServer;
import com.gone.load_balancer.handler.LoadBalanceHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class LoadBalancerApplication {

    public static NettyReverseProxyServer server;

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext context = SpringApplication.run(LoadBalancerApplication.class, args);
        LoadBalanceHandler loadBalanceHandler = context.getBean(LoadBalanceHandler.class);

        server = new NettyReverseProxyServer(80, ProtocolTypeEnums.HTTP, loadBalanceHandler);
        server.run();
    }

}
