package com.gone.load_balancer.common;

import lombok.Getter;

@Getter
public enum LoadBalanceEnums {

    POLL(Constants.LOAD_BALANCE_STRATEGY_POOL),
    IP_HASH(Constants.LOAD_BALANCE_STRATEGY_IP_HASH);

    String value;

    LoadBalanceEnums(String value) {
        this.value = value;
    }
}
