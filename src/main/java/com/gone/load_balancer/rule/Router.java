package com.gone.load_balancer.rule;

import org.springframework.stereotype.Component;

@Component
public class Router {

    //TODO 路由规则注入(request uri -> upstream)
    private TrieTree trieTree;

    public String route(String requestURI) {
        return trieTree.search(requestURI);
    }

}
