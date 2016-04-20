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
