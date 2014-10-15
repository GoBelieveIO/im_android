package com.gameservice.sdk.im;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;

public class IMMessage {
    public long sender;
    public long receiver;
    public int msgLocalID;
    public String content;
}


class Command{
    public static final int MSG_HEARTBEAT = 1;
    public static final int MSG_AUTH = 2;
    public static final int MSG_AUTH_STATUS = 3;
    public static final int MSG_IM = 4;
    public static final int MSG_ACK = 5;
    public static final int MSG_RST = 6;
    public static final int MSG_GROUP_NOTIFICATION = 7;
    public static final int MSG_GROUP_IM = 8;
    public static final int MSG_PEER_ACK = 9;
    public static final int MSG_INPUTTING = 10;
    public static final int MSG_SUBSCRIBE_ONLINE_STATE = 11;
    public static final int MSG_ONLINE_STATE = 12;
}


class MessagePeerACK {
    public long sender;
    public long receiver;
    public int msgLocalID;
}

class MessageInputing {
    public long sender;
    public long receiver;
}

class MessageOnlineState {
    public long sender;
    public int online;
}

class MessageSubscribe {
    public ArrayList<Long> uids;
}

class Message {

    public static final int HEAD_SIZE = 8;
    public int cmd;
    public int seq;
    public Object body;

    public byte[] pack() {
        int pos = 0;
        byte[] buf = new byte[64*1024];
        BytePacket.writeInt32(seq, buf, pos);
        pos += 4;
        buf[pos] = (byte)cmd;
        pos += 4;

        if (cmd == Command.MSG_HEARTBEAT) {
            return Arrays.copyOf(buf, HEAD_SIZE);
        } else if (cmd == Command.MSG_AUTH) {
            BytePacket.writeInt64((Long) body, buf, pos);
            return Arrays.copyOf(buf, HEAD_SIZE+8);
        } else if (cmd == Command.MSG_IM) {
            IMMessage im = (IMMessage) body;
            BytePacket.writeInt64(im.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(im.receiver, buf, pos);
            pos += 8;
            BytePacket.writeInt32(im.msgLocalID, buf, pos);
            pos += 4;
            try {
                byte[] c = im.content.getBytes("UTF-8");
                if (c.length + 28 > 64 * 1024) {
                    Log.e("imservice", "packet buffer overflow");
                    return null;
                }
                System.arraycopy(c, 0, buf, pos, c.length);
                return Arrays.copyOf(buf, HEAD_SIZE + 20 + c.length);
            } catch (Exception e) {
                Log.e("imservice", "encode utf8 error");
                return null;
            }
        } else if (cmd == Command.MSG_ACK) {
            BytePacket.writeInt32((Integer)body, buf, pos);
            return Arrays.copyOf(buf, HEAD_SIZE+4);
        } else if (cmd == Command.MSG_SUBSCRIBE_ONLINE_STATE) {
            MessageSubscribe sub = (MessageSubscribe)body;
            BytePacket.writeInt32(sub.uids.size(), buf, pos);
            pos += 4;
            for (int i = 0; i < sub.uids.size(); i++) {
                Long uid = sub.uids.get(i);
                BytePacket.writeInt64(uid, buf, pos);
                pos += 8;
            }
            return Arrays.copyOf(buf, HEAD_SIZE + 4 + sub.uids.size()*8);
        } else if (cmd == Command.MSG_INPUTTING) {
            MessageInputing in = (MessageInputing)body;
            BytePacket.writeInt64(in.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(in.receiver, buf, pos);
            return Arrays.copyOf(buf, HEAD_SIZE + 16);
        }
        return null;
    }

    public boolean unpack(byte[] data) {
        int pos = 0;
        this.seq = BytePacket.readInt32(data, pos);
        pos += 4;
        cmd = data[pos];
        pos += 4;
        if (cmd == Command.MSG_RST) {
            return true;
        } else if (cmd == Command.MSG_AUTH_STATUS) {
            int status = BytePacket.readInt32(data, pos);
            this.body = new Integer(status);
            return true;
        } else if (cmd == Command.MSG_IM) {
            IMMessage im = new IMMessage();
            im.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            im.receiver = BytePacket.readInt64(data, pos);
            pos += 8;
            im.msgLocalID = BytePacket.readInt32(data, pos);
            pos += 4;
            try {
                im.content = new String(data, pos, data.length - 28, "UTF-8");
                this.body = im;
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (cmd == Command.MSG_ACK) {
            int s = BytePacket.readInt32(data, pos);
            this.body = new Integer(s);
            return true;
        } else if (cmd == Command.MSG_PEER_ACK) {
            MessagePeerACK ack = new MessagePeerACK();
            ack.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            ack.receiver = BytePacket.readInt64(data, pos);
            pos += 8;
            ack.msgLocalID = BytePacket.readInt32(data, pos);
            this.body = ack;
            return true;
        } else if (cmd == Command.MSG_INPUTTING) {
            MessageInputing inputing = new MessageInputing();
            inputing.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            inputing.receiver = BytePacket.readInt64(data, pos);
            this.body = inputing;
            return true;
        } else if (cmd == Command.MSG_ONLINE_STATE) {
            MessageOnlineState state = new MessageOnlineState();
            state.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            state.online = BytePacket.readInt32(data, pos);
            this.body = state;
            return true;
        } else if (cmd == Command.MSG_RST) {
            return true;
        } else {
            return false;
        }
    }
}
