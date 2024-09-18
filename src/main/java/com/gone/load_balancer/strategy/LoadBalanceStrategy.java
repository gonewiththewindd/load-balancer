package com.gone.load_balancer.strategy;

import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import jakarta.servlet.http.HttpServletRequest;

public interface LoadBalanceStrategy {

    Service loadBalance(HttpServletRequest request, Upstream upstream);

}
