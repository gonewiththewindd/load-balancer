package com.gone.load_balancer.common;

public interface Constants {

    String DEFAULT_LOAD_BALANCE_STRATEGY = LoadBalanceEnums.POLL.name();
    String LOAD_BALANCE_STRATEGY_POOL = "POLL";
    String LOAD_BALANCE_STRATEGY_IP_HASH = "IP_HASH";

}
