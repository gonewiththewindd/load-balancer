package com.gone.load_balancer.rule;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Router {

    private TrieTree trieTree;

    public String route(String requestURI) {
        String upstreamId = trieTree.search(requestURI);
        log.info("requestURI '{}' match to :{}", requestURI, upstreamId);
        return upstreamId;
    }

    @PostConstruct
    public void init() {
        trieTree = new TrieTreeImpl();
        // *, select*Users, *getUser, getUser*
        trieTree.insert("/api/user-service/users/*", "user-service");
        trieTree.insert("/api/user-service/users/select*Users", "user-service");
        trieTree.insert("/api/user-service/users/*getUser", "user-service");
        trieTree.insert("/api/user-service/users/getUser*", "user-service");
    }

}
