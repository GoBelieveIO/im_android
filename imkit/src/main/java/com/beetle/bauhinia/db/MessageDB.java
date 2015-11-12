package com.beetle.bauhinia.db;

import android.util.Log;
import com.beetle.im.BytePacket;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 14-7-22.
 */

public class MessageDB {
    protected static final int HEADER_SIZE = 32;
    protected static final int IMMAGIC = 0x494d494d;
    protected static final int IMVERSION = (1<<16);

    public static boolean writeHeader(RandomAccessFile f) {
        try {
            byte[] buf = new byte[HEADER_SIZE];
            BytePacket.writeInt32(IMMAGIC, buf, 0);
            BytePacket.writeInt32(IMMAGIC, buf, 0);
            BytePacket.writeInt32(IMVERSION, buf, 4);
            f.write(buf);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public  static boolean checkHeader(RandomAccessFile f) {
        try {
            byte[] header = new byte[HEADER_SIZE];
            f.seek(0);
            int n = f.read(header);
            if (n != header.length) {
                return false;
            }
            int magic = BytePacket.readInt32(header, 0);
            int version = BytePacket.readInt32(header, 4);
            if (magic != IMMAGIC || version != IMVERSION) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean writeMessage(RandomAccessFile f, IMessage msg) {
        try {
            byte[] buf = new byte[64 * 1024];
            int pos = 0;

            byte[] content = msg.content.raw.getBytes("UTF-8");
            int len = content.length + 8 + 8 + 4 + 4;
            if (4 + 4 + len + 4 + 4 > 64 * 1024) return false;

            BytePacket.writeInt32(IMMAGIC, buf, pos);
            pos += 4;
            BytePacket.writeInt32(len, buf, pos);
            pos += 4;
            BytePacket.writeInt32(msg.flags, buf, pos);
            pos += 4;
            BytePacket.writeInt32(msg.timestamp, buf, pos);
            pos += 4;
            BytePacket.writeInt64(msg.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(msg.receiver, buf, pos);
            pos += 8;
            System.arraycopy(content, 0, buf, pos, content.length);
            pos += content.length;
            BytePacket.writeInt32(len, buf, pos);
            pos += 4;
            BytePacket.writeInt32(IMMAGIC, buf, pos);

            f.write(buf, 0, 4 + 4 + len + 4 + 4);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static IMessage readMessage(ReverseFile file) {
        try {
            byte[] buf = new byte[8];
            int n = file.read(buf);
            if (n != 8) {
                return null;
            }
            int len = BytePacket.readInt32(buf, 0);
            int magic = BytePacket.readInt32(buf, 4);
            if (magic != MessageDB.IMMAGIC) {
                return null;
            }

            buf = new byte[len + 8];
            n = file.read(buf);
            if (n != buf.length) {
                return null;
            }

            IMessage msg = new IMessage();
            msg.msgLocalID = (int)file.getFilePointer();
            int pos = 8;
            msg.flags = BytePacket.readInt32(buf, pos);
            pos += 4;
            msg.timestamp = BytePacket.readInt32(buf, pos);
            pos += 4;
            msg.sender = BytePacket.readInt64(buf, pos);
            pos += 8;
            msg.receiver = BytePacket.readInt64(buf, pos);
            pos += 8;
            msg.setContent(new String(buf, pos, len - 24, "UTF-8"));
            return msg;
        } catch (Exception e) {
            Log.e("imservice", "read file exception:" + e);
            return null;
        }
    }

    public static boolean insertMessage(RandomAccessFile f, IMessage msg) throws IOException{
        long size = f.length();
        if (size < HEADER_SIZE && size > 0) {
            f.setLength(0);
            size = 0;
            Log.e("imservice", "truncate file");
        }
        if (size == 0) {
            writeHeader(f);
            size = HEADER_SIZE;
        }
        msg.msgLocalID = (int)size;
        f.seek(size);
        writeMessage(f, msg);
        return true;
    }

    public static boolean addFlag(RandomAccessFile f, int msgLocalID, int flag) {
        try {
            f.seek(msgLocalID);
            byte[] buf = new byte[12];
            int n = f.read(buf);
            if (n != 12) {
                return false;
            }
            int magic = BytePacket.readInt32(buf, 0);
            if (magic != IMMAGIC) {
                return false;
            }
            int flags = BytePacket.readInt32(buf, 8);
            flags |= flag;
            f.seek(msgLocalID + 8);
            f.writeInt(flags);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public static boolean eraseFlag(RandomAccessFile f, int msgLocalID, int flag) {
        try {
            f.seek(msgLocalID);
            byte[] buf = new byte[12];
            int n = f.read(buf);
            if (n != 12) {
                return false;
            }
            int magic = BytePacket.readInt32(buf, 0);
            if (magic != IMMAGIC) {
                return false;
            }
            int flags = BytePacket.readInt32(buf, 8);
            flags &= ~flag;
            f.seek(msgLocalID + 8);
            f.writeInt(flags);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}
