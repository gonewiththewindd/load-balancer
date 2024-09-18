package com.gone.load_balancer.pool;

import org.apache.http.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class HttpClientPooledConnectionManager {

    @Autowired
    private HttpClientConnectionManager connectionManager;

    public HttpResponse execute(HttpRequest httpRequest) {

        HttpClientContext context = HttpClientContext.create();// TODO 这是个什么东西？
        HttpRoute httpRoute = new HttpRoute(HttpHost.create(httpRequest.getRequestLine().getUri()));
        ConnectionRequest connectionRequest = connectionManager.requestConnection(httpRoute, null);
        HttpClientConnection connection = null;
        try {
            connection = connectionRequest.get(10, TimeUnit.SECONDS);
            // If not open
            if (!connection.isOpen()) {
                // establish connection based on its route info
                connectionManager.connect(connection, httpRoute, 1000, context);
                // and mark it as route complete
                connectionManager.routeComplete(connection, httpRoute, context);
            }
            // Do useful things with the connection.
            connection.sendRequestHeader(httpRequest);
            if (httpRequest instanceof HttpEntityEnclosingRequest) {
                HttpEntityEnclosingRequest enclosingRequest = (HttpEntityEnclosingRequest) httpRequest;
                if (Objects.nonNull(enclosingRequest.getEntity())) {
                    connection.sendRequestEntity(enclosingRequest);
                }
            }
            connection.flush();

            if (connection.isResponseAvailable(30000)) {
                HttpResponse httpResponse = connection.receiveResponseHeader();
                connection.receiveResponseEntity(httpResponse);
                return httpResponse;
            }
            throw new RuntimeException("连接超时未返回应答");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            connectionManager.releaseConnection(connection, null, 1, TimeUnit.MINUTES);
        }
    }

}
