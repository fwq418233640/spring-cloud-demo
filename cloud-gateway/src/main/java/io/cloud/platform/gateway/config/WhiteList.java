package io.cloud.platform.gateway.config;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 白名单
 *
 * @author a4182
 */
public class WhiteList {

    public static final Set<String> DATA = new CopyOnWriteArraySet<>();

    static {
        DATA.add("/v2/api-docs");
    }
}
