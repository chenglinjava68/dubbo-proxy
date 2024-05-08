package com.mds.dubbo.codec.packet;


import lombok.Getter;

@Getter
public class BodyHeartBeat extends Body {

    private  Object event;

    public BodyHeartBeat(Object event) {
        this.event = event;
    }

}
