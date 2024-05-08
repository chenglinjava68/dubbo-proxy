package com.mds.dubbo.codec.packet;


import lombok.Getter;

@Getter
public class DubboPacket {
    private DubboRequestHeader dubboRequestHeader;
    private  Body body;

    public void setDubboRequestHeader(DubboRequestHeader dubboRequestHeader) {
        this.dubboRequestHeader = dubboRequestHeader;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public DubboPacket(DubboRequestHeader header) {
        this.dubboRequestHeader = header;
    }


    public void release() {
        dubboRequestHeader.release();
        body.release();
    }


}