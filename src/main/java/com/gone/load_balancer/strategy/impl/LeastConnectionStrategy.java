package com.gone.load_balancer.strategy.impl;

import com.gone.load_balancer.common.Constants;
import com.gone.load_balancer.strategy.LBParams;
import com.gone.load_balancer.strategy.LoadBalanceStrategy;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Slf4j
@Component(Constants.LOAD_BALANCE_STRATEGY_LEAST_CONNECTION)
public class LeastConnectionStrategy implements LoadBalanceStrategy {
    @Override
    public Service loadBalance(LBParams params, Upstream upstream) {
        return upstream.getServices()
                .stream()
                .min(Comparator.comparing(s -> s.getActiveConnections().get()))
                .get();
    }
}
