package com.gone.load_balancer.pool;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HttpClientPooledConnectionManager {

    @Autowired
    private HttpClientConnectionManager connectionManager;

    public HttpResponse execute(HttpRequest httpRequest) {
        try {
            CloseableHttpClient client = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .build();
            String host = resolveHost(httpRequest.getRequestLine().getUri());
            HttpHost httpHost = HttpHost.create(host);
            // client execute的时候已经使用了connection manager的requestConnection方法，也就是如果连接管理器本身支持池化，通过这种方式调用也会应用到池化
            return client.execute(httpHost, httpRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveHost(String uri) {
        int schemaIndex = uri.indexOf("://");
        if (schemaIndex == -1) {
            schemaIndex = 0;
        }
        String hostWithoutSchema = uri.substring(schemaIndex == 0 ? 0 : schemaIndex + 3);
        int i = hostWithoutSchema.indexOf("/");
        if (i > 0) {
            return hostWithoutSchema.substring(0, i);
        } else {
            return hostWithoutSchema;
        }
    }

}
