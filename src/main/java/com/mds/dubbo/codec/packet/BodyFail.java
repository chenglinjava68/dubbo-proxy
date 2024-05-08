package com.mds.dubbo.codec.packet;


import lombok.Getter;

@Getter
public class BodyFail extends Body {
    private String errorMessage;

    public BodyFail(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
