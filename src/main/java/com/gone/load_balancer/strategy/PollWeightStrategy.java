package com.gone.load_balancer.strategy;

import com.gone.load_balancer.common.Constants;
import com.gone.load_balancer.common.RuntimeCounter;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component(Constants.LOAD_BALANCE_STRATEGY_POOL)
public class PollWeightStrategy implements LoadBalanceStrategy {
    @Override
    public Service loadBalance(HttpServletRequest request, Upstream upstream) {
        long rc = RuntimeCounter.getUpstreamRequestCounter(upstream.getId()).incrementAndGet();
        long bitmapIndex = rc % upstream.getWeightsBitmap().length;
        int serviceIndex = upstream.getWeightsBitmap()[(int) bitmapIndex];
        return upstream.getServices().get(serviceIndex);
    }
}
