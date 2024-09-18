package com.gone.load_balancer.strategy;

import com.gone.load_balancer.common.Constants;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component(Constants.LOAD_BALANCE_STRATEGY_IP_HASH)
public class IPHashStrategy implements LoadBalanceStrategy {
    @Override
    public Service loadBalance(HttpServletRequest request, Upstream upstream) {
        String remoteAddr = request.getRemoteAddr();
        int serviceIndex = remoteAddr.hashCode() % upstream.getServices().size();
        return upstream.getServices().get(serviceIndex);
    }
}
