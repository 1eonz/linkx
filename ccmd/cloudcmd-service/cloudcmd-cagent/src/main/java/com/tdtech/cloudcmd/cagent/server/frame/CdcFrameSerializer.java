package com.tdtech.cloudcmd.cagent.server.frame;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.netty.buffer.ByteBuf;

/**
 * transfer serialize protocol implementation
 */
public class CdcFrameSerializer {

    private CdcFrameSerializer() {
        throw new UnsupportedOperationException("not supported");
    }

    public static CdcFrame read(int length, ByteBuf in) {
        short type = in.readShort();
        CdcFrameHeader header = new CdcFrameHeader(type);
        header.setSequence(in.readInt());
        header.setPriority(in.readByte());
        header.setCompress(in.readByte());
        short tokenLen = in.readShort();
        short subSystemLen = in.readShort();
        header.setToken(in.readCharSequence(tokenLen, StandardCharsets.UTF_8).toString());
        header.setSubsystem(in.readCharSequence(subSystemLen, StandardCharsets.UTF_8).toString());
        String body = in
            .readCharSequence(length - CdcFrameHeader.FIX_HEADER_SIZE - tokenLen - subSystemLen, StandardCharsets.UTF_8)
            .toString();
        return new CdcFrame(header, body);
    }

    public static void write(CdcFrame frame, ByteBuf out) {
        Objects.requireNonNull(frame);
        Objects.requireNonNull(out);
        String body = frame.getBody();
        CdcFrameHeader header = frame.getHeader();
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        byte[] tokenBytes = header.getToken().getBytes(StandardCharsets.UTF_8);
        byte[] subsystemBytes = header.getSubsystem().getBytes(StandardCharsets.UTF_8);
        // write
        out.writeInt(CdcFrameHeader.FIX_HEADER_SIZE + tokenBytes.length + subsystemBytes.length + bodyBytes.length);// length
        out.writeShort(header.getType());// type
        out.writeInt(header.getSequence());// seq
        out.writeByte(header.getPriority());// pri
        out.writeByte(header.getCompress());// compress
        out.writeShort((short)tokenBytes.length);// tk length
        out.writeShort((short)subsystemBytes.length);// sub sys length
        out.writeBytes(tokenBytes);// token
        out.writeBytes(subsystemBytes);// sub sys
        out.writeBytes(bodyBytes);// payload
    }

}
