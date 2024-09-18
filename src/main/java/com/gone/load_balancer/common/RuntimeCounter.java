package com.gone.load_balancer.common;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RuntimeCounter {

    private static final ConcurrentHashMap<String, AtomicLong> REQUEST_COUNTER = new ConcurrentHashMap<>();

    public static AtomicLong getUpstreamRequestCounter(String upstreamId) {
        return REQUEST_COUNTER.computeIfAbsent(upstreamId, (k) -> new AtomicLong(0));
    }
}
