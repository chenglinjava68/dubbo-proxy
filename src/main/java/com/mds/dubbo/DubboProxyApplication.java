package com.mds.dubbo;

import com.mds.dubbo.config.ServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class DubboProxyApplication  implements CommandLineRunner {

    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    ServerConfig serverConfig;
    public static void main(String[] args) {
        SpringApplication.run(DubboProxyApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        applicationContext.getBean(serverConfig.getType(), ProxyServer.class).doStart();
    }
}
