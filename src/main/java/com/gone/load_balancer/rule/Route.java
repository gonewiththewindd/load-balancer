package com.gone.load_balancer.rule;

public interface Route {
    void insert(String path, String upstream);

    String search(String path);
}
