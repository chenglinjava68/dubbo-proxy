package com.mds.dubbo.codec.packet;


import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class BodyRequest extends Body {
    private final String dubboVersion;
    private final String path;
    private final String version;
    private final String methodName;
    private final String parameterTypesDesc;
    private final List<Object> parameterValues;
    private final Map<String, Object> attachments;

    public BodyRequest(String dubboVersion, String path, String version,
                       String methodName, String parameterTypesDesc,
                       Map<String, Object> attachments,
                       List<Object> parameterValues) {
        this.dubboVersion = dubboVersion;
        this.path = path;
        this.version = version;
        this.methodName = methodName;
        this.parameterTypesDesc = parameterTypesDesc;
        this.attachments = attachments;
        this.parameterValues = parameterValues;
    }

}