package com.beetle.push.connect;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by houxh on 14-9-9.
 */
public class Protocol {
    public static final int COMMAND_HEADER_SIZE = 12;


    public static final int CMD_PING = 1;
    public static final int CMD_PONG = 2;
    public static final int CMD_REGISTER_CLIENT = 3;
    public static final int CMD_CLIENT_DEVICE_TOKEN = 4;
    public static final int CMD_AUTH = 5;
    public static final int CMD_AUTH_STATUS = 6;
    public static final int CMD_NOTIFICATION = 7;
    public static final int CMD_ACK_NOTIFICATION = 8;


    public static abstract class Command {
        public int seq;

        abstract public int getCmd();

        abstract public byte[] toData();

        abstract public boolean fromData(ByteBuffer buffer, int length);
    }


    public static class Ping extends Command {
        public int getCmd() {
            return CMD_PING;
        }

        public byte[] toData() {
            return new byte[0];
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            return true;
        }
    }


    public static class Pong extends Command {
        public int getCmd() {
            return CMD_PONG;
        }

        public byte[] toData() {
            return new byte[0];
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            return true;
        }
    }


    public static class RegisterClient extends Command {
        public long appid;
        public String appkey;

        public int getCmd() {
            return CMD_REGISTER_CLIENT;
        }

        public byte[] toData() {
            try {
                byte[] b = appkey.getBytes();
                ByteBuffer buf = ByteBuffer.allocate(b.length + 8);
                buf.order(ByteOrder.BIG_ENDIAN);
                buf.putLong(appid);
                buf.put(b);
                return buf.array();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            return false;
        }
    }


    public static class ClientDeviceToken extends Command {
        public byte[] token;

        public int getCmd() {
            return CMD_CLIENT_DEVICE_TOKEN;
        }


        public byte[] toData() {
            return null;
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            try {
                token = new byte[length];
                buffer.get(token);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }


    public static class Authentication extends Command {
        public byte[] token;

        public int getCmd() {
            return CMD_AUTH;
        }

        public byte[] toData() {
            return token;
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            return false;
        }
    }


    public static class AuthenticationStatus extends Command {
        public int status;

        public int getCmd() {
            return CMD_AUTH_STATUS;
        }

        public byte[] toData() {
            return null;
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            try {
                if (length != 4) {
                    return false;
                }
                status = buffer.getInt();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }


    public static class Notification extends Command {
        public long nid;
        public long appid;
        public byte[] content;

        public int getCmd() {
            return CMD_NOTIFICATION;
        }

        public byte[] toData() {
            return null;
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            try {
                nid = buffer.getLong();
                appid = buffer.getLong();
                content = new byte[length - 16];
                buffer.get(content);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

        }

        public byte[] getContent() {
            return content;
        }
    }


    public static class ACKNotification extends Command {
        public int ack_seq;

        public int getCmd() {
            return CMD_ACK_NOTIFICATION;
        }

        public byte[] toData() {
            ByteBuffer buffer = ByteBuffer.allocate(4);
            buffer.putInt(ack_seq);
            return buffer.array();
        }

        public boolean fromData(ByteBuffer buffer, int length) {
            return false;
        }
    }


    public static class Header {
        public int length;
        public int seq;
        public byte cmd;
    }

    public static Header ReadHeader(ByteBuffer buffer) {
        try {
            Header header = new Header();
            header.length = buffer.getInt();
            header.seq = buffer.getInt();
            header.cmd = buffer.get();

            byte[] p = new byte[3];
            buffer.get(p);
            return header;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] WriteHeader(Header header) {
        ByteBuffer buffer = ByteBuffer.allocate(COMMAND_HEADER_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(header.length);
        buffer.putInt(header.seq);
        buffer.put(header.cmd);

        byte[] p = new byte[3];
        buffer.put(p);

        return buffer.array();
    }
}

