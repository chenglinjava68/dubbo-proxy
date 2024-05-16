package com.mds.dubbo.codec;

import com.mds.dubbo.codec.packet.*;
import com.mds.dubbo.core.Serialization;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mds.dubbo.codec.Constant.*;

/**
 * Title: <br>
 * Description: <br>
 * Copyright: Copyright (c) 2024/5/5<br>
 * Company: 磨刀石<br>
 *
 * @author chenglin
 * @codeGenerator idea
 */
public class DubboRequestDecoder extends ByteToMessageDecoder {

    private static final int HEADER_LENGTH = 16;
    private State state = State.READ_HEADER;

    private DubboPacket dubboPacket;
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        boolean hasNext = false;
        do {
            switch (this.state) {
                case READ_HEADER: {
                    if (buffer.readableBytes() >= HEADER_LENGTH) {
                        try {
                            this.dubboPacket = new DubboPacket(parseHeader(buffer));
                        } catch (Exception e) {
                            exception(ctx, buffer, e);
                            throw e;
                        }
                        this.state = State.READ_BODY;
                        hasNext = buffer.isReadable();
                    } else {
                        hasNext = false;
                    }
                    break;
                }
                case READ_BODY: {
                    if (buffer.readableBytes() >= this.dubboPacket.getDubboRequestHeader().getBodyLength()) {
                        int markReaderIndex = buffer.readerIndex();
                        try {
                            Body body = parseBody(buffer);
                            buffer.readerIndex(16);
                            body.setBodyBytes(buffer.readRetainedSlice(dubboPacket.getDubboRequestHeader().getBodyLength()));
                            this.dubboPacket.setBody(body);
                        } catch (Exception e) {
                            exception(ctx, buffer, e);
                            this.dubboPacket.release();
                            throw e;
                        }
                        this.state = State.READ_HEADER;
                        out.add(this.dubboPacket);
                        this.dubboPacket = null;
                        hasNext = buffer.isReadable();
                    } else {
                        hasNext = false;
                    }
                    break;
                }

            }
        } while (hasNext);
    }

    private Body parseBody(ByteBuf buffer) throws IOException, ClassNotFoundException {
        byte flag = dubboPacket.getDubboRequestHeader().getFlag();
        byte status = dubboPacket.getDubboRequestHeader().getStatus();
        int bodyLength = dubboPacket.getDubboRequestHeader().getBodyLength();
        boolean flagResponse = (flag & FLAG_REQUEST) == 0;
        byte serializationProtoId = (byte) (flag & SERIALIZATION_MASK);
        if (flagResponse) {
            if (status == OK) {
                if ((flag & FLAG_EVENT) != 0) {
                    return readHeartBeat(buffer, bodyLength, serializationProtoId);
                } else {
                    try (Serialization.ObjectInput in = Serialization.codeOfDeserialize(serializationProtoId, buffer, bodyLength)) {
                        byte responseWith = buffer.readByte();
                        BodyResponse packetResponse;
                        switch (responseWith) {
                            case RESPONSE_NULL_VALUE:
                                packetResponse = new BodyResponse(null, null, null);
                                break;
                            case RESPONSE_VALUE:
                                packetResponse = new BodyResponse(in.readObject(), null, null);
                                break;
                            case RESPONSE_WITH_EXCEPTION:
                                packetResponse = new BodyResponse(null, in.readThrowable(), null);
                                break;
                            case RESPONSE_NULL_VALUE_WITH_ATTACHMENTS:
                                packetResponse = new BodyResponse(null, null, in.readAttachments());
                                break;
                            case RESPONSE_VALUE_WITH_ATTACHMENTS:
                                packetResponse = new BodyResponse(in.readObject(), null, in.readAttachments());
                                break;
                            case RESPONSE_WITH_EXCEPTION_WITH_ATTACHMENTS:
                                packetResponse = new BodyResponse(null, in.readThrowable(), in.readAttachments());
                                break;
                            default:
                                throw new IOException("Unknown result flag, expect '0' '1' '2' '3' '4' '5', but received: " + responseWith);
                        }
                        return packetResponse;
                    }
                }
            } else {
                try (Serialization.ObjectInput in = Serialization.codeOfDeserialize(serializationProtoId, buffer, bodyLength)) {
                    return new BodyFail(in.readUTF());
                }
            }
        } else {
            if ((flag & FLAG_EVENT) != 0) {
                return readHeartBeat(buffer, bodyLength, serializationProtoId);
            } else {
                try (Serialization.ObjectInput in = Serialization.codeOfDeserialize(serializationProtoId, buffer, bodyLength)) {
                    String dubboVersion = in.readUTF();
                    String path = in.readUTF();
                    String version = in.readUTF();
                    String methodName = in.readUTF();
                    String parameterTypesDesc = in.readUTF();
                    int countArgs = countArgs(parameterTypesDesc);
                    ArrayList<Object> args = new ArrayList<>(countArgs);
                    for (int i = 0; i < countArgs; i++) {
                        args.add(in.readObject());
                    }
                    Map<String, Object> attachments = in.readAttachments();
                    return new BodyRequest(dubboVersion, path, version, methodName, parameterTypesDesc, attachments, args);
                }
            }
        }
    }


    private BodyHeartBeat readHeartBeat(ByteBuf buffer, int bodyLength, byte serializationProtoId) throws IOException, ClassNotFoundException {
        Object data;
        byte[] payload = Serialization.getPayload(buffer, bodyLength);
        if (Serialization.isHeartBeat(payload, serializationProtoId)) {
            data = null;
        } else {
            try (Serialization.ObjectInput input = Serialization.codeOfDeserialize(serializationProtoId, new ByteArrayInputStream(payload))) {
                data = input.readEvent();
            }
        }
        return new BodyHeartBeat(data);
    }
    private DubboRequestHeader parseHeader(ByteBuf buffer) {
        // request and serialization flag.
        byte flag = buffer.getByte(2);
        byte status = buffer.getByte(3);
        long requestId = buffer.getLong(4);
        // 8 - 1-request/0-response
        byte type = buffer.getByte(8);
        int bodyLength = buffer.getInt(12);

        ByteBuf headerBytes = buffer.readRetainedSlice(HEADER_LENGTH);
        return new DubboRequestHeader(headerBytes, flag, status, requestId, type, bodyLength);
    }
    private <E extends Exception> void exception(ChannelHandlerContext ctx, ByteBuf buffer, E cause) throws Exception {
        buffer.release();
        ctx.close();
    }
    enum State {
        READ_HEADER, READ_BODY
    }
}
