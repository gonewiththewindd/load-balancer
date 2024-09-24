package com.gone.load_balancer.strategy.impl;

import com.gone.load_balancer.common.Constants;
import com.gone.load_balancer.strategy.LBParams;
import com.gone.load_balancer.strategy.LoadBalanceStrategy;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import org.springframework.stereotype.Component;

@Component(Constants.LOAD_BALANCE_STRATEGY_IP_HASH)
public class IPHashStrategy implements LoadBalanceStrategy {
    @Override
    public Service loadBalance(LBParams params, Upstream upstream) {
        String remoteAddr = params.getRemoteAddr();
        if(remoteAddr.contains(Constants.COLON_CHAR)){
            remoteAddr = remoteAddr.split(Constants.COLON_CHAR)[0];
        }
        int serviceIndex = remoteAddr.hashCode() % upstream.getServices().size();
        return upstream.getServices().get(serviceIndex);
    }
}
