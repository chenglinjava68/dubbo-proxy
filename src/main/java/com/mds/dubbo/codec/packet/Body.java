package com.mds.dubbo.codec.packet;

import io.netty.buffer.ByteBuf;

public abstract class Body {
   private ByteBuf bodyBytes;

    public ByteBuf bytes() {
        return bodyBytes;
    }

    public boolean release() {
        if (bodyBytes != null && bodyBytes.refCnt() > 0) {
            return bodyBytes.release();
        } else {
            return false;
        }
    }
}
