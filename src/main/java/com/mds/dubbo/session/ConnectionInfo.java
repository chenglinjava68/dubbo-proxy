package com.mds.dubbo.session;

import com.mds.dubbo.config.AppInfo;
import io.netty.channel.Channel;
import lombok.Data;

/**
 * 链接信息
 *
 * @author baoyoujia
 * @date 2024/6/13
 */
@Data
public class ConnectionInfo {
    private AppInfo appInfo;

    private Channel channel;
}
