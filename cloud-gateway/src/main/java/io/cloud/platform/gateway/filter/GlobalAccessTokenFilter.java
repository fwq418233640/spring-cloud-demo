package io.cloud.platform.gateway.filter;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloud.platform.gateway.config.WhiteList;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 *
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class GlobalAccessTokenFilter implements GlobalFilter {
    public static final String USER_INFO = "User-Info";

    private final ObjectMapper objectMapper;

    private final StringRedisTemplate redisTemplate;

    final String PREFIX_USER_TOKEN = "Authorization:user:token:";

    @Value("${token.timeout:24}")
    public long timeout;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();

        String url = request.getURI().getPath();
        String scheme = request.getURI().getScheme();
        String host = request.getURI().getHost();
        int port = request.getURI().getPort();
        String basePath = scheme + "://" + host + ":" + port;
        log.debug("请求路径 {} 请求地址 {} ", url, basePath);

        //将现在的request，添加当前身份
        ServerHttpRequest mutableReq = request.mutate()
                .header(USER_INFO, "{}")
                .build();
        ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();

        // 过白名单直接放行
        if (whiteList(url)) {
            return chain.filter(mutableExchange);
        }

        String token = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(token)) {
            log.error("请求中未携带" + HttpHeaders.AUTHORIZATION);
            return FilterUtil.error(exchange, objectMapper);
        } else {
            String data = this.authentication(token);
            if (data == null) {
                log.error("无法从redis中获取token对应的用户信息");
                return FilterUtil.error(exchange, objectMapper);
            }
            String encode = URLEncoder.encode(data, StandardCharsets.UTF_8);
            ServerHttpRequest userInfo = request.mutate()
                    .header(USER_INFO, encode)
                    .build();
            ServerWebExchange serverWebExchange = exchange.mutate().request(userInfo).build();
            return chain.filter(serverWebExchange);
        }
    }


    // 白名单
    private static boolean whiteList(String url) {
        PathMatcher pathMatcher = new AntPathMatcher();
        boolean result = false;
        for (String regex : WhiteList.DATA) {
            if (pathMatcher.match(regex, url)) {
                result = true;
                break;
            }
        }
        if (result) {
            log.info("==> 白名单放行 {}", url);
        }
        return result;
    }


    // 鉴权
    private String authentication(String token) {
        String key = PREFIX_USER_TOKEN + token;
        String json = redisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object data = map.get("userInfo");
            this.refreshToken(key);
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // 刷新用户token失效时间
    private void refreshToken(String key) {
        try {
            redisTemplate.expire(key, Duration.ofHours(timeout));
        } catch (Exception e) {
            log.error("==> 刷新token 失效时间失败 {}", e.getMessage());
        }
    }
}
