package io.cloud.platform.gateway.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
@RefreshScope
public class ConfigChangeListener {

    private final Environment environment;

    public ConfigChangeListener(Environment environment) {
        this.environment = environment;
    }

    @EventListener
    public void handleEnvironmentChange(EnvironmentChangeEvent event) {
        if (event.getKeys().contains("whiteList")) {
            String updatedWhiteList = environment.getProperty("whiteList", "");
            if (updatedWhiteList.isEmpty()) {
                return;
            }

            // whiteList 发生了变化
            String[] split = updatedWhiteList.split(",");
            WhiteList.DATA.addAll(Arrays.asList(split));
            log.info("==> 添加白名单项 {}", Arrays.toString(split));
        }
    }
}

