package io.cloud.platform.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
public class FilterUtil {

    public static Mono<Void> error(ServerWebExchange exchange, ObjectMapper objectMapper) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("拦截 请求 {}", request.getURI());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        DataBufferFactory factory = response.bufferFactory();
        try {
            DataBuffer wrap = factory.wrap(objectMapper.writeValueAsBytes(new ResponseEntity<>(HttpStatus.UNAUTHORIZED)));
            return response.writeWith(Mono.fromSupplier(() -> wrap));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return response.setComplete();
        }
    }
}
