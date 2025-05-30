package io.cloud.platform.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "actuator")
public class AuthorizedConfig {
    /**
     * 健康检查授权码
     * 此处没有对接授权系统而采用授权码是为了
     * 防止授权系统崩溃,导致此时无法查看健康检查
     */
    private String authorized;
}

