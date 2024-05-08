package com.mds.dubbo.codec.packet;

import lombok.Getter;

import java.util.Map;

@Getter
public class BodyResponse extends Body {
    private Object value;
    private Object throwable;
    private Map<String, Object> attachments;

    public BodyResponse(Object value, Object throwable, Map<String, Object> attachments) {
        this.value = value;
        this.throwable = throwable;
        this.attachments = attachments;
    }

}
