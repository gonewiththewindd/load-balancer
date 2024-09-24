package com.gone.load_balancer.upstream;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    private String serviceName;
    private String ip;
    private String host;
    private int port;
    private int weight = 1;
    private String status;

    public transient AtomicInteger activeConnections = new AtomicInteger();
}
