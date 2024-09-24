package com.gone.load_balancer.common;

public interface Constants {

    String DEFAULT_LOAD_BALANCE_STRATEGY = LoadBalanceEnums.POLL.name();
    String LOAD_BALANCE_STRATEGY_POOL = "POLL";
    String LOAD_BALANCE_STRATEGY_IP_HASH = "IP_HASH";
    String LOAD_BALANCE_STRATEGY_LEAST_CONNECTION = "LEAST_CONNECTION";


    String PATH_SEPARATOR = "/";
    String SINGLE_WILDCARDS = "*";
    String SINGLE_WILDCARDS_REPLACEMENT = "<-!->";
    String DOUBLE_WILDCARDS = "**";
    String DOUBLE_WILDCARDS_REPLACEMENT = "<-!--!->";


    String SCHEMA = "http://";
    String COLON_CHAR = ":";

}
