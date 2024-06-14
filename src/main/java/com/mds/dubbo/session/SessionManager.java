package com.mds.dubbo.session;

import com.mds.dubbo.config.AppInfo;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * session管理器
 *
 * @author baoyoujia
 * @date 2024/6/13
 */
@Slf4j
public class SessionManager {

    private SessionManager() {
    }

    /**
     * 保存proxy和provide之间的连接，应用名和channel
     */
    private final Map<String,Channel> readyRegistry = new ConcurrentHashMap<>(16);

    private static class Singleton {
        static SessionManager instance = new SessionManager();
    }

    public static SessionManager getInstance() {
        return Singleton.instance;
    }

    public void addConnection(AppInfo appInfo, Channel channel, Channel inboundChannel) {
        readyRegistry.put(appInfo.getName(), channel);
    }

    public Channel getConnection(String appName) {
        return readyRegistry.get(appName);
    }

    public boolean existConnection(String appName) {
        return readyRegistry.containsKey(appName);
    }

}
