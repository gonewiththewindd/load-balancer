package com.gone.load_balancer.strategy.impl;

import com.gone.load_balancer.common.Constants;
import com.gone.load_balancer.common.RuntimeCounter;
import com.gone.load_balancer.strategy.LBParams;
import com.gone.load_balancer.strategy.LoadBalanceStrategy;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component(Constants.LOAD_BALANCE_STRATEGY_POOL)
public class PollWeightStrategy implements LoadBalanceStrategy {
    @Override
    public Service loadBalance(LBParams params, Upstream upstream) {
        long rc = RuntimeCounter.getUpstreamRequestCounter(upstream.getId()).getAndIncrement();
        long bitmapIndex = rc % upstream.getWeightsBitmap().length;
        int serviceIndex = upstream.getWeightsBitmap()[(int) bitmapIndex];
        return upstream.getServices().get(serviceIndex);
    }
}
