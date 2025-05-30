package io.cloud.platform.gateway.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloud.platform.gateway.config.AuthorizedConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <pre>
 * 所有端点加密,仅暴露一些用于健康检查的端点,其他端点需要授权
 * 授权没有走平台登录接口,因在特殊情况下,授权系统 也处于不可用状态
 * 但我们又希望此时可以用来检查其他服务，故而没有走平台授权接口
 * 此处授权仅与spring-admin相关
 * </pre>
 *
 * @author a4182
 */
@Order(0)
@Component
public class ActuatorAuthorizedFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(ActuatorAuthorizedFilter.class);

    public static final Set<String> WhiteList = new CopyOnWriteArraySet<>();

    static {
        WhiteList.add("/actuator/info");
        WhiteList.add("/*/actuator/info");

        WhiteList.add("/actuator");
        WhiteList.add("/*/actuator");

        WhiteList.add("/actuator/health/**");
        WhiteList.add("/*/actuator/health/**");
    }

    private final ObjectMapper objectMapper;

    private final AuthorizedConfig authorizedConfig;

    public ActuatorAuthorizedFilter(ObjectMapper objectMapper, AuthorizedConfig authorizedConfig) {
        this.objectMapper = objectMapper;
        this.authorizedConfig = authorizedConfig;
    }


    private static boolean whiteList(String uri) {
        PathMatcher pathMatcher = new AntPathMatcher();
        boolean result = false;
        for (String regex : WhiteList) {
            if (pathMatcher.match(regex, uri)) {
                result = true;
                break;
            }
        }
        return result;
    }



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        String uri = request.getURI().getPath();
        String scheme = request.getURI().getScheme();
        String host = request.getURI().getHost();
        int port = request.getURI().getPort();
        String basePath = scheme + "://" + host + ":" + port;
        log.debug("请求路径 {} 请求地址 {} ", uri, basePath);


        if (!uri.contains("/actuator")) {
            log.debug("非端点请求直接放行 {}", uri);
            return chain.filter(exchange);
        }

        // 白名单直接放行
        if (whiteList(uri)) {
            log.debug("白名单放行 {}", uri);
            return chain.filter(exchange);
        }

        String header = headers.getFirst("x-internal-token");
        if (authorizedConfig.getAuthorized().equals(header)) {
            log.debug("携带token放行 {}", uri);
            return chain.filter(exchange);
        }

        log.debug("拦截 {}", uri);
        return FilterUtil.error(exchange,objectMapper);
    }
}

