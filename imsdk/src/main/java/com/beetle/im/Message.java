/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.im;

import android.util.Log;
import java.util.Arrays;

/**
 * Created by houxh on 14-7-23.
 */

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
    public static final int MSG_PING = 13;
    public static final int MSG_PONG = 14;
    public static final int MSG_AUTH_TOKEN = 15;
    public static final int MSG_LOGIN_POINT = 16;
    public static final int MSG_RT = 17;
    public static final int MSG_ENTER_ROOM = 18;
    public static final int MSG_LEAVE_ROOM = 19;
    public static final int MSG_ROOM_IM = 20;
    public static final int MSG_SYSTEM = 21;
    public static final int MSG_CUSTOMER_SERVICE = 23;

    public static final int MSG_SYNC = 26;
    public static final int MSG_SYNC_BEGIN = 27;
    public static final int MSG_SYNC_END = 28;
    public static final int MSG_SYNC_NOTIFY = 29;

    public static final int MSG_SYNC_GROUP = 30;
    public static final int MSG_SYNC_GROUP_BEGIN = 31;
    public static final int MSG_SYNC_GROUP_END = 32;
    public static final int MSG_SYNC_GROUP_NOTIFY = 33;

    public static final int MSG_SYNC_KEY = 34;
    public static final int MSG_GROUP_SYNC_KEY = 35;

    public static final int MSG_METADATA = 37;

    public static final int MSG_CUSTOMER = 64;
}

class Flag {
    public static final int MESSAGE_FLAG_TEXT = 1;
    public static final int MESSAGE_FLAG_UNPERSISTENT = 2;
    public static final int MESSAGE_FLAG_GROUP = 4;
    public static final int MESSAGE_FLAG_SELF = 8;

    //服务器主动下发的消息
    public static final int MESSAGE_FLAG_PUSH = 0x10;

    public static final int MESSAGE_FLAG_SUPER_GROUP = 0x20;
}


class AuthenticationToken {
    public String token;
    public int platformID;
    public String deviceID;
}


//个人消息：typedef long SyncKey

//超级群
class GroupSyncKey {
    public long groupID;
    public long syncKey;
}

class SyncNotify {
    public long syncKey;
}

class GroupSyncNotify {
    public long groupID;
    public long syncKey;
}

class Metadata {
    public long syncKey;
    public long prevSyncKey;
}

public class Message {

    private static final int VERSION = 2;

    public static final int HEAD_SIZE = 8;
    public int cmd;
    public int seq;
    public int flag;
    public Object body;

    public int failCount;//发送失败的次数

    public byte[] pack() {
        int pos = 0;
        byte[] buf = new byte[64*1024];
        BytePacket.writeInt32(seq, buf, pos);
        pos += 4;
        buf[pos++] = (byte)cmd;
        buf[pos++] = (byte)VERSION;
        buf[pos++] = (byte)flag;
        pos += 1;

        if (cmd == Command.MSG_HEARTBEAT || cmd == Command.MSG_PING) {
            return Arrays.copyOf(buf, HEAD_SIZE);
        } else if (cmd == Command.MSG_AUTH) {
            BytePacket.writeInt64((Long) body, buf, pos);
            return Arrays.copyOf(buf, HEAD_SIZE + 8);
        } else if (cmd == Command.MSG_AUTH_TOKEN) {
            AuthenticationToken auth = (AuthenticationToken)body;
            buf[pos] = (byte)auth.platformID;
            pos++;
            byte[] token = auth.token.getBytes();
            buf[pos] = (byte)token.length;
            pos++;
            System.arraycopy(token, 0, buf, pos, token.length);
            pos += token.length;

            byte[] deviceID = auth.deviceID.getBytes();
            buf[pos] = (byte)deviceID.length;
            pos++;
            System.arraycopy(deviceID, 0, buf, pos, deviceID.length);
            pos += deviceID.length;

            return Arrays.copyOf(buf, pos);
        } else if (cmd == Command.MSG_IM || cmd == Command.MSG_GROUP_IM) {
            IMMessage im = (IMMessage) body;
            BytePacket.writeInt64(im.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(im.receiver, buf, pos);
            pos += 8;
            BytePacket.writeInt32(im.timestamp, buf, pos);
            pos += 4;
            BytePacket.writeInt32(0, buf, pos);
            pos += 4;
            try {
                byte[] c = im.content.getBytes("UTF-8");
                if (c.length + 24 >= 32 * 1024) {
                    Log.e("imservice", "packet buffer overflow");
                    return null;
                }
                System.arraycopy(c, 0, buf, pos, c.length);
                return Arrays.copyOf(buf, HEAD_SIZE + 24 + c.length);
            } catch (Exception e) {
                Log.e("imservice", "encode utf8 error");
                return null;
            }
        } else if (cmd == Command.MSG_ACK) {
            MessageACK ack = (MessageACK)body;
            BytePacket.writeInt32(ack.seq, buf, pos);
            pos += 4;
            buf[pos++] = (byte)ack.status;
            return Arrays.copyOf(buf, HEAD_SIZE+5);
        } else if (cmd == Command.MSG_CUSTOMER) {
            CustomerMessage cs = (CustomerMessage) body;
            BytePacket.writeInt64(cs.senderAppID, buf, pos);
            pos += 8;
            BytePacket.writeInt64(cs.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(cs.receiverAppID, buf, pos);
            pos += 8;
            BytePacket.writeInt64(cs.receiver, buf, pos);
            pos += 8;
            BytePacket.writeInt32(cs.timestamp, buf, pos);
            pos += 4;
            try {
                byte[] c = cs.content.getBytes("UTF-8");
                if (c.length + 36 >= 32 * 1024) {
                    Log.e("imservice", "packet buffer overflow");
                    return null;
                }
                System.arraycopy(c, 0, buf, pos, c.length);
                return Arrays.copyOf(buf, HEAD_SIZE + 36 + c.length);
            } catch (Exception e) {
                Log.e("imservice", "encode utf8 error");
                return null;
            }
        } else if (cmd == Command.MSG_RT) {
            RTMessage rt = (RTMessage) body;
            BytePacket.writeInt64(rt.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(rt.receiver, buf, pos);
            pos += 8;
            try {
                byte[] c = rt.content.getBytes("UTF-8");
                if (c.length + 24 >= 32 * 1024) {
                    Log.e("imservice", "packet buffer overflow");
                    return null;
                }
                System.arraycopy(c, 0, buf, pos, c.length);
                return Arrays.copyOf(buf, HEAD_SIZE + 16 + c.length);
            } catch (Exception e) {
                Log.e("imservice", "encode utf8 error");
                return null;
            }
        } else if (cmd == Command.MSG_ROOM_IM) {
            RoomMessage rm = (RoomMessage)body;

            BytePacket.writeInt64(rm.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(rm.receiver, buf, pos);
            pos += 8;
            try {
                byte[] c = rm.content.getBytes("UTF-8");
                if (c.length + 24 >= 32 * 1024) {
                    Log.e("imservice", "packet buffer overflow");
                    return null;
                }
                System.arraycopy(c, 0, buf, pos, c.length);
                return Arrays.copyOf(buf, HEAD_SIZE + 16 + c.length);
            } catch (Exception e) {
                Log.e("imservice", "encode utf8 error");
                return null;
            }
        } else if (cmd == Command.MSG_ENTER_ROOM || cmd == Command.MSG_LEAVE_ROOM) {
            Long roomID = (Long) body;
            BytePacket.writeInt64(roomID, buf, pos);
            return Arrays.copyOf(buf, HEAD_SIZE + 8);
        } else if (cmd == Command.MSG_SYNC ||
                cmd == Command.MSG_SYNC_KEY) {
            Long syncKey = (Long) body;
            BytePacket.writeInt64(syncKey, buf, pos);
            return Arrays.copyOf(buf, HEAD_SIZE + 8);
        } else if (cmd == Command.MSG_SYNC_GROUP ||
                cmd == Command.MSG_GROUP_SYNC_KEY) {
            GroupSyncKey syncKey = (GroupSyncKey)body;
            BytePacket.writeInt64(syncKey.groupID, buf, pos);
            pos += 8;
            BytePacket.writeInt64(syncKey.syncKey, buf, pos);
            pos += 8;
            return Arrays.copyOf(buf, HEAD_SIZE + 16);
        } else {
            return null;
        }
    }

    public boolean unpack(byte[] data) {
        int pos = 0;
        this.seq = BytePacket.readInt32(data, pos);
        pos += 4;
        cmd = data[pos];
        flag = data[pos + 2];
        pos += 4;
        if (cmd == Command.MSG_PONG) {
            return true;
        } else if (cmd == Command.MSG_AUTH_STATUS) {
            int status = BytePacket.readInt32(data, pos);
            this.body = new Integer(status);
            return true;
        } else if (cmd == Command.MSG_IM || cmd == Command.MSG_GROUP_IM) {
            IMMessage im = new IMMessage();
            im.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            im.receiver = BytePacket.readInt64(data, pos);
            pos += 8;
            im.timestamp = BytePacket.readInt32(data, pos);
            pos += 4;
            im.msgLocalID = BytePacket.readInt32(data, pos);
            pos += 4;
            try {
                im.content = new String(data, pos, data.length - 32, "UTF-8");
                this.body = im;
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (cmd == Command.MSG_ACK) {
            MessageACK ack = new MessageACK();
            ack.seq = BytePacket.readInt32(data, pos);
            pos += 4;
            ack.status = data[pos];
            this.body = ack;
            return true;
        } else if (cmd == Command.MSG_GROUP_NOTIFICATION) {
            try {
                this.body = new String(data, pos, data.length - HEAD_SIZE, "UTF-8");
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (cmd == Command.MSG_SYSTEM) {
            try {
                this.body = new String(data, pos, data.length - HEAD_SIZE, "UTF-8");
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (cmd == Command.MSG_CUSTOMER) {
            CustomerMessage cs = new CustomerMessage();
            cs.senderAppID = BytePacket.readInt64(data, pos);
            pos += 8;
            cs.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            cs.receiverAppID = BytePacket.readInt64(data, pos);
            pos += 8;
            cs.receiver = BytePacket.readInt64(data, pos);
            pos += 8;
            cs.timestamp = BytePacket.readInt32(data, pos);
            pos += 4;
            try {
                cs.content = new String(data, pos, data.length - 36 - HEAD_SIZE, "UTF-8");
                this.body = cs;
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (cmd == Command.MSG_RT) {
            RTMessage rt = new RTMessage();
            rt.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            rt.receiver = BytePacket.readInt64(data, pos);
            pos += 8;
            try {
                rt.content = new String(data, pos, data.length - pos, "UTF-8");
                this.body = rt;
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (cmd == Command.MSG_ROOM_IM) {
            RoomMessage rt = new RoomMessage();
            rt.sender = BytePacket.readInt64(data, pos);
            pos += 8;
            rt.receiver = BytePacket.readInt64(data, pos);
            pos += 8;
            try {
                rt.content = new String(data, pos, data.length - pos, "UTF-8");
                this.body = rt;
                return true;
            } catch (Exception e) {
                return false;
            }
        } else if (cmd == Command.MSG_ENTER_ROOM || cmd == Command.MSG_LEAVE_ROOM) {
            long roomID = BytePacket.readInt64(data, pos);
            this.body = new Long(roomID);
            return true;
        } else if (cmd == Command.MSG_SYNC_BEGIN ||
                cmd == Command.MSG_SYNC_END) {
            long key = BytePacket.readInt64(data, pos);
            this.body = new Long(key);
            return true;
        } else if (cmd == Command.MSG_SYNC_GROUP_BEGIN ||
                cmd == Command.MSG_SYNC_GROUP_END) {
            GroupSyncKey key = new GroupSyncKey();
            key.groupID = BytePacket.readInt64(data, pos);
            pos += 8;
            key.syncKey = BytePacket.readInt64(data, pos);
            pos += 8;
            this.body = key;
            return true;
        } else if (cmd == Command.MSG_SYNC_NOTIFY) {
            SyncNotify notify = new SyncNotify();
            notify.syncKey = BytePacket.readInt64(data, pos);
            pos += 8;
            this.body = notify;
            return true;
        } else if (cmd == Command.MSG_SYNC_GROUP_NOTIFY) {
            GroupSyncNotify key = new GroupSyncNotify();
            key.groupID = BytePacket.readInt64(data, pos);
            pos += 8;
            key.syncKey = BytePacket.readInt64(data, pos);
            pos += 8;
            this.body = key;
            return true;
        } else if (cmd == Command.MSG_METADATA) {
            Metadata key = new Metadata();
            key.syncKey = BytePacket.readInt64(data, pos);
            pos += 8;
            key.prevSyncKey = BytePacket.readInt64(data, pos);
            this.body = key;
            return true;
        } else {
            return true;
        }
    }
}
