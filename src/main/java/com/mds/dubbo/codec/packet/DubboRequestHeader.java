package com.mds.dubbo.codec.packet;

import io.netty.buffer.ByteBuf;

public class DubboRequestHeader {
    private  ByteBuf headerBytes;
    // request and serialization flag.
    private  byte flag;
    private  byte status;

    private  long requestId;
    // 8 - 1-request/0-response
    private  byte type;
    private  int bodyLength;

    public DubboRequestHeader(ByteBuf headerBytes, byte flag, byte status, long requestId, byte type, int bodyLength) {
        this.headerBytes = headerBytes;
        this.flag = flag;
        this.status = status;
        this.requestId = requestId;
        this.type = type;
        this.bodyLength = bodyLength;
    }


    public ByteBuf getHeaderBytes() {
        return headerBytes;
    }

    public void setHeaderBytes(ByteBuf headerBytes) {
        this.headerBytes = headerBytes;
    }

    public byte getFlag() {
        return flag;
    }

    public void setFlag(byte flag) {
        this.flag = flag;
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public int getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(int bodyLength) {
        this.bodyLength = bodyLength;
    }
    public boolean release() {
        if (headerBytes != null && headerBytes.refCnt() > 0) {
            return headerBytes.release();
        } else {
            return false;
        }
    }
}