package com.gone.load_balancer.config;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpHost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class HttpClientConnectionConfig {

    @Bean
    public HttpClientConnectionManager httpClientConnectionManager() {

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for localhost:80 to 50
//        HttpHost localhost = new HttpHost("locahost", 80);
//        cm.setMaxPerRoute(new HttpRoute(localhost), 50);
        return cm;
//        HttpClientContext context = HttpClientContext.create();
//        HttpClientConnectionManager connMrg = new BasicHttpClientConnectionManager();
//        HttpRoute route = new HttpRoute(new HttpHost("localhost", 80));
//        // Request new connection. This can be a long process
//        ConnectionRequest connRequest = connMrg.requestConnection(route, null);
//        // Wait for connection up to 10 sec
//        HttpClientConnection conn = connRequest.get(10, TimeUnit.SECONDS);
//        try {
//            // If not open
//            if (!conn.isOpen()) {
//                // establish connection based on its route info
//                connMrg.connect(conn, route, 1000, context);
//                // and mark it as route complete
//                connMrg.routeComplete(conn, route, context);
//            }
//            // Do useful things with the connection.
//        } finally {
//            connMrg.releaseConnection(conn, null, 1, TimeUnit.MINUTES);
//        }
    }

}
