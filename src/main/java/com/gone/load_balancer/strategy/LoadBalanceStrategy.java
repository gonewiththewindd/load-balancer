package com.gone.load_balancer.strategy;

import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;

public interface LoadBalanceStrategy {

    Service loadBalance(LBParams params, Upstream upstream);

}
