package com.mds.dubbo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author cheng
 */

@Data
@Configuration
@ConfigurationProperties(prefix = "server")
public class ServerConfig {
    private String type;
    private String serverIp;
    private int serverPort;
    private String backendIp;
    private int backendPort;
    private int receiveBuffer;
    private int sendBuffer;
    private AllocatorType allocatorType;
    private int maxContentLength;
    private int workThreadNums ;

    private List<AppInfo> backend;
}
