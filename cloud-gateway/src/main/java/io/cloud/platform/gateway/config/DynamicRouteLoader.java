package io.cloud.platform.gateway.config;


import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.consul.config.ConsulConfigProperties;
import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

/**
 * 动态路由加载器
 *
 * @author : zyf
 * date :2020-11-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicRouteLoader extends InMemoryRouteDefinitionRepository {

    private final ObjectMapper objectMapper;

    private final ConsulClient consulClient;

    private final ConsulConfigProperties properties;

    @Value("${whiteList:''}")
    private String whiteList;

    @Value("${spring.application.name}")
    private String applicationName;

    @PostConstruct
    public void init() {
        // 初始化白名单
        whiteListConfig();
    }


    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        String consulKeyPath = "config/" + applicationName + "/routes";
        Response<List<GetValue>> response = consulClient.getKVValues(consulKeyPath, properties.getAclToken());
        List<GetValue> value = response.getValue();

        if (value == null) {
            log.error("无法从配置中心读取路由配置,请检查 " + consulKeyPath + "/default 文件是否正确设置！文件格式应为json");
            return Flux.empty();
        }

        String routes = value.get(0).getDecodedValue();
        List<RouteDefinition> list;
        try {
            list = objectMapper.readValue(routes, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        log.info("读取配置成功,配置信息:{}", list);
        return Flux.fromIterable(list);
    }


    private void whiteListConfig() {
        if (whiteList.isEmpty()) {
            return;
        }
        String[] split = whiteList.split(",");
        WhiteList.DATA.addAll(Arrays.asList(split));
        log.info("==> 初始化白名单完成 当前白名单:{}", WhiteList.DATA);
    }
}
