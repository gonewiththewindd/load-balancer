package com.gone.load_balancer.strategy.impl;

import com.gone.load_balancer.strategy.LBParams;
import com.gone.load_balancer.strategy.LoadBalanceStrategy;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class ComposeLoadBalanceStrategy implements LoadBalanceStrategy {

    @Autowired
    private Map<String, LoadBalanceStrategy> strategyMap;

    @Override
    public Service loadBalance(LBParams params, Upstream upstream) {
        LoadBalanceStrategy balanceStrategy = strategyMap.get(upstream.getLbe().getValue());
        Service service = balanceStrategy.loadBalance(params, upstream);
        log.info("request '{}' \ndistribute to '{}', using load balance strategy '{}'", params.getRequestURI(), service, upstream.getLbe());
        return service;
    }
}
