package com.gone.load_balancer.endpoint;

import com.gone.load_balancer.common.LoadBalanceEnums;
import com.gone.load_balancer.pool.HttpClientPooledConnectionManager;
import com.gone.load_balancer.rule.Router;
import com.gone.load_balancer.strategy.LoadBalanceStrategy;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.InputStreamEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

@Controller
@RequestMapping("/")
public class ListenEndpoint {

    @Autowired
    HttpClientPooledConnectionManager httpClientConnectionManager;
    @Autowired
    private Map<String, LoadBalanceStrategy> strategyMap;
    @Autowired
    private Map<String, Upstream> upstreamMap;
    @Autowired
    private Router router;

    @RequestMapping
    public void incomingRequest(HttpServletRequest req, HttpServletResponse res) {
        String requestURI = req.getRequestURI();
        String upstreamId = router.route(requestURI);
        if ("404".equals(upstreamId)) {

        }
        Upstream upstream = upstreamMap.get(upstreamId);
        LoadBalanceEnums lbe = upstream.getLbe();
        LoadBalanceStrategy balanceStrategy = strategyMap.get(lbe.getValue());
        Service service = balanceStrategy.loadBalance(req, upstream);
        String upstreamRequestURI = service.getIp().concat(requestURI);
        dispatchRequest(upstreamRequestURI, req, res);
    }

    private void dispatchRequest(String upstreamRequestURI, HttpServletRequest req, HttpServletResponse res) {
        HttpRequest httpRequest = convertServletRequestToHttpClientRequest(upstreamRequestURI, req);
        HttpResponse httpResponse = httpClientConnectionManager.execute(httpRequest);
        transferResponse(httpResponse, res);
    }

    private void transferResponse(HttpResponse httpResponse, HttpServletResponse res) {
        StatusLine statusLine = httpResponse.getStatusLine();
        try (InputStream content = httpResponse.getEntity().getContent();
             ServletOutputStream outputStream = res.getOutputStream()) {
            byte[] buffer = new byte[1024 * 1024];
            int length;
            while ((length = content.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (statusLine.getStatusCode() != 200) {
            // failed
        }
    }

    private HttpRequest convertServletRequestToHttpClientRequest(String requestURI, HttpServletRequest req) {
        try {
            String httpMethod = req.getMethod().toUpperCase();
            HttpRequest httpRequest;
            switch (httpMethod) {
                case "GET":
                    httpRequest = new HttpGet(requestURI);
                    break;
                case "HEAD":
                    httpRequest = new HttpHead(requestURI);
                    break;
                case "POST":
                    httpRequest = new HttpPost(requestURI);
                    ((HttpPost) httpRequest).setEntity(new InputStreamEntity(req.getInputStream()));
                    break;
                case "PUT":
                    httpRequest = new HttpPut(requestURI);
                    ((HttpPut) httpRequest).setEntity(new InputStreamEntity(req.getInputStream()));
                    break;
                case "PATCH":
                    httpRequest = new HttpPatch(requestURI);
                    ((HttpPatch) httpRequest).setEntity(new InputStreamEntity(req.getInputStream()));
                    break;
                case "DELETE":
                    httpRequest = new HttpDelete(requestURI);
                    break;
                case "OPTIONS":
                    httpRequest = new HttpOptions(requestURI);
                    break;
                case "TRACE":
                    httpRequest = new HttpTrace(requestURI);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + httpMethod);
            }

            setupHeaders(httpRequest, req);

            return httpRequest;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupHeaders(HttpRequest httpRequest, HttpServletRequest req) {
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            httpRequest.setHeader(headerName, req.getHeader(headerName));
        }
    }
}
