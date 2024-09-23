package com.gone.load_balancer.endpoint;

import com.gone.load_balancer.LoadBalancerApplication;
import com.gone.load_balancer.pool.HttpClientPooledConnectionManager;
import com.gone.load_balancer.rule.Router;
import com.gone.load_balancer.strategy.LoadBalanceStrategy;
import com.gone.load_balancer.upstream.Service;
import com.gone.load_balancer.upstream.Upstream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.InputStreamEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.gone.load_balancer.common.Constants.SCHEMA;

@Slf4j
@Controller
@RequestMapping
public class ListenEndpoint {

    @Autowired
    HttpClientPooledConnectionManager httpClientConnectionManager;
    @Autowired
    private Map<String, LoadBalanceStrategy> strategyMap;

    private Map<String, Upstream> upstreamMap = new HashMap<>() {{
        Service service1 = new Service();
        service1.setServiceName("user-service-instance-1");
        service1.setIp("127.0.0.1");
        service1.setHost("127.0.0.1");
        service1.setPort(8088);
        Upstream upstream = new Upstream("user-service", null, Arrays.asList(service1));
        put("user-service", upstream);
    }};

    @Autowired
    private Router router;

//    @RequestMapping("/**")
//    public void incomingRequest(HttpServletRequest req, HttpServletResponse res) throws IOException {
//        String requestURI = req.getRequestURI();
//        String upstreamId = router.route(requestURI);
//        if (Objects.nonNull(upstreamId)) {
//            Upstream upstream = upstreamMap.get(upstreamId);
//            LoadBalanceStrategy balanceStrategy = strategyMap.get(upstream.getLbe().getValue());
//            Service service = balanceStrategy.loadBalance(new LBParams(req.getRemoteAddr(), requestURI), upstream);
//            String upstreamRequestURI = convertToUpstreamRequestURI(req, requestURI, service);
//            dispatchRequest(upstreamRequestURI, req, res);
//        } else {
//            try (ServletOutputStream outputStream = res.getOutputStream()) {
//                outputStream.write(new String("resource not found").getBytes(StandardCharsets.UTF_8));
//            }
//        }
//    }

    private String convertToUpstreamRequestURI(HttpServletRequest req, String requestURI, Service service) {
        String prefix = req.getContextPath();
        int i = requestURI.indexOf(prefix);
        if (i >= 0) {
            requestURI = requestURI.substring(i + prefix.length());
        }
        String upstreamRequestURI = SCHEMA.concat(service.getHost())
                .concat(":")
                .concat(String.valueOf(service.getPort()))
                .concat(requestURI);
        if (StringUtils.isNoneBlank(req.getQueryString())) {
            upstreamRequestURI = upstreamRequestURI.concat("?").concat(req.getQueryString());
        }
        return upstreamRequestURI;
    }

    private void dispatchRequest(String upstreamRequestURI, HttpServletRequest req, HttpServletResponse res) {
        log.info("dispatch request to '{}'", upstreamRequestURI);
        HttpRequest httpRequest = convertServletRequestToHttpClientRequest(upstreamRequestURI, req);
        HttpResponse httpResponse = httpClientConnectionManager.execute(httpRequest);
        transferResponse(httpResponse, res);
    }

    private void transferResponse(HttpResponse httpResponse, HttpServletResponse res) {
        log.info("transfer response from upstream...");
//        transferHeaders(httpResponse, res);
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

    private void transferHeaders(HttpResponse httpResponse, HttpServletResponse res) {
        for (Header header : httpResponse.getAllHeaders()) {
            res.addHeader(header.getName(), header.getValue());
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
            if ("Content-Length".equalsIgnoreCase(headerName)) {
                continue;
            }
            httpRequest.setHeader(headerName, req.getHeader(headerName));
        }
    }

    @GetMapping("/stopServer")
    public void stopServer() {
        LoadBalancerApplication.server.stop();
    }

}
