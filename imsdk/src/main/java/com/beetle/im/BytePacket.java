package com.beetle.im;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by houxh on 14-7-21.
 */
public class BytePacket {
    static public void writeInt64(long v, byte[] dst, int pos) {
        ByteBuffer b = ByteBuffer.allocate(8);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putLong(v);
        byte[] t = b.array();
        System.arraycopy(t, 0, dst, pos, t.length);
    }

    static public void writeInt32(int v, byte[] dst, int pos) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(v);
        byte[] t = b.array();
        System.arraycopy(t, 0, dst, pos, t.length);
    }

    static public void writeInt16(short v, byte[] dst, int pos) {
        ByteBuffer b = ByteBuffer.allocate(2);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putShort(v);
        byte[] t = b.array();
        System.arraycopy(t, 0, dst, pos, t.length);
    }

    static public long readInt64(byte[] bytes, int pos) {
        ByteBuffer b = ByteBuffer.wrap(bytes, pos, 8);
        b.order(ByteOrder.BIG_ENDIAN);
        return b.getLong();
    }

    static public int readInt32(byte[] bytes, int pos) {
        ByteBuffer b = ByteBuffer.wrap(bytes, pos, 4);
        b.order(ByteOrder.BIG_ENDIAN);
        return b.getInt();
    }

    static public short readInt16(byte[] bytes, int pos) {
        ByteBuffer b = ByteBuffer.wrap(bytes, pos, 2);
        b.order(ByteOrder.BIG_ENDIAN);
        return b.getShort();
    }

    static public int packInetAddress(byte[] bytes) {
        ByteBuffer b2 = ByteBuffer.wrap(bytes, 0, 4);
        b2.order(ByteOrder.BIG_ENDIAN);
        return b2.getInt();
    }

    static public byte[] unpackInetAddress(int iaddr) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(iaddr);
        byte[] t = b.array();
        return t;
    }

    //little->net
    static public int ltonl(int v) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(v);
        byte[] t = b.array();
        ByteBuffer b2 = ByteBuffer.wrap(t, 0, 4);
        b2.order(ByteOrder.BIG_ENDIAN);
        return b2.getInt();
    }
    //net->little
    static public int ntoll(int v) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(v);
        byte[] t = b.array();
        ByteBuffer b2 = ByteBuffer.wrap(t, 0, 4);
        b2.order(ByteOrder.LITTLE_ENDIAN);
        return b2.getInt();
    }

    //little -> native
    static public int ltohl(int v) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putInt(v);
        byte[] t = b.array();
        ByteBuffer b2 = ByteBuffer.wrap(t, 0, 4);
        b2.order(ByteOrder.nativeOrder());
        return b2.getInt();
    }

    //native -> little
    static public int htoll(int v) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.nativeOrder());
        b.putInt(v);
        byte[] t = b.array();
        ByteBuffer b2 = ByteBuffer.wrap(t, 0, 4);
        b2.order(ByteOrder.LITTLE_ENDIAN);
        return b2.getInt();
    }

    static public int ntohl(int v) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.nativeOrder());
        b.putInt(v);
        byte[] t = b.array();
        ByteBuffer b2 = ByteBuffer.wrap(t, 0, 4);
        b2.order(ByteOrder.BIG_ENDIAN);
        return b2.getInt();
    }

    static public int htonl(int v) {
        ByteBuffer b = ByteBuffer.allocate(4);
        b.order(ByteOrder.BIG_ENDIAN);
        b.putInt(v);
        byte[] t = b.array();
        ByteBuffer b2 = ByteBuffer.wrap(t, 0, 4);
        b2.order(ByteOrder.nativeOrder());
        return b2.getInt();
    }

}
