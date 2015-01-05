package com.gameservice.sdk.im;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;


import java.io.IOException;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class IMService {

    public enum ConnectState {
        STATE_UNCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_CONNECTFAIL,
    }


    private final String TAG = "imservice";
    private final int HEARTBEAT = 60 * 3;


    private volatile boolean stopped = true;
    private TCP tcp;
    private TCP.TCPConnectCallback onConnect;
    private TCP.TCPReadCallback onRead;
    private IoLoop.Timer connectTimer;
    private IoLoop.Timer heartbeatTimer;
    private IoLoop.Timer pongTimeoutTimer;

    private volatile boolean isRST = false;

    private volatile ConnectState connectState = ConnectState.STATE_UNCONNECTED;

    private long uid;

    private String accessToken;

    private int authStatus = -1;

    ArrayList<IMServiceObserver> observers = new ArrayList<IMServiceObserver>();


    private int connectFailCount = 0;
    private int seq = 0;
    HashMap<Integer, IMMessage> peerMessages = new HashMap<Integer, IMMessage>();
    private byte[] data;

    private Handler mainThreadhandler = new Handler();

    private static IMService instance = new IMService();

    public static IMService getInstance() {
        return instance;
    }

    private IMService() {
        this.connectTimer = new IoLoop.Timer() {
            @Override
            public void handleTimeout() {
                connect();
            }
        };

        this.heartbeatTimer = new IoLoop.Timer() {
            @Override
            public void handleTimeout() {
                if (IMService.this.authStatus != 0) {
                    return;
                }
                IMService.this.sendPing();
                IoLoop.getDefaultLoop().setTimeout(50 * 1000, pongTimeoutTimer);
            }
        };

        this.pongTimeoutTimer = new IoLoop.Timer() {
            @Override
            public void handleTimeout() {
                Log.i("PUSH", "pong timeout");
                closeTCP();
                reconnect();
            }
        };

        this.onConnect = new TCP.TCPConnectCallback() {
            @Override
            public void onConnect(TCP tcp, int status) {
                if (status != 0) {
                    Log.i(TAG, "connect err:" + status);
                    IMService.this.connectFailCount++;
                    IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
                    IMService.this.publishConnectState();
                    IMService.this.closeTCP();
                    IMService.this.reconnect();
                } else {
                    Log.i(TAG, "tcp connected");
                    IMService.this.connectFailCount = 0;
                    IMService.this.connectState = ConnectState.STATE_CONNECTED;
                    IMService.this.publishConnectState();
                    IMService.this.sendAuth();
                    IMService.this.tcp.startRead(IMService.this.onRead);

                    IoLoop loop = IoLoop.getDefaultLoop();
                    loop.setTimeout(HEARTBEAT * 1000, heartbeatTimer);
                }
            }
        };

        this.onRead = new TCP.TCPReadCallback() {
            @Override
            public void onRead(TCP tcp, byte[] data) {
                if (data == null || data.length == 0) {
                    IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                    IMService.this.publishConnectState();
                    IMService.this.handleClose();
                } else {
                    boolean b = IMService.this.handleData(data);
                    if (!b) {
                        IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                        IMService.this.publishConnectState();
                        IMService.this.handleClose();
                    }
                }
            }
        };
    }

    public ConnectState getConnectState() {
        return connectState;
    }



    public void setUID(long uid) {
        if (uid == 0) {
            throw new IllegalArgumentException("uid can not be 0");
        }
        this.uid = uid;
    }

    public void setAccessToken(String token) {
        if (TextUtils.isEmpty(token)) {
            throw new IllegalArgumentException("access can not be empty");
        }
        this.accessToken = token;
    }


    public void addObserver(IMServiceObserver ob) {
        if (observers.contains(ob)) {
            return;
        }
        observers.add(ob);
    }

    public void removeObserver(IMServiceObserver ob) {
        observers.remove(ob);
    }

    public void start() {
        if (!this.stopped) {
            Log.w(TAG, "im service repeat start");
            return;
        }
        try {
            IoLoop loop = IoLoop.getDefaultLoop();
            if (!loop.isAlive()) {
                loop.prepare();
                loop.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        this.stopped = false;
        this.isRST = false;

        final IoLoop loop = IoLoop.getDefaultLoop();
        loop.asyncSend(new IoLoop.IoRunnable() {
            @Override
            public void run() {
                loop.setTimeout(0, connectTimer);
            }
        });
    }

    public void stop() {
        if (this.stopped) {
            Log.w(TAG, "im service repeat stop");
            return;
        }
        stopped = true;
        final IoLoop loop = IoLoop.getDefaultLoop();
        loop.asyncSend(new IoLoop.IoRunnable() {
            @Override
            public void run() {
                loop.setTimeout(0, null);
                IMService.this.closeTCP();
                Iterator iter = peerMessages.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Integer, IMMessage> entry =
                        (Map.Entry<Integer, IMMessage>) iter.next();
                    IMMessage im = entry.getValue();
                    publishPeerMessageFailure(im.msgLocalID, im.receiver);
                }
                peerMessages.clear();

                if (IMService.this.connectState != ConnectState.STATE_UNCONNECTED) {
                    IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                    IMService.this.publishConnectState();
                }
            }
        });
    }

    public boolean sendPeerMessage(final IMMessage im) {
        IoLoop loop = IoLoop.getDefaultLoop();
        loop.asyncSend(new IoLoop.IoRunnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.cmd = Command.MSG_IM;
                msg.body = im;
                if (!sendMessage(msg)) {
                    return;
                }

                peerMessages.put(new Integer(msg.seq), im);
            }
        });
        return true;
    }

    private void closeTCP() {
        if (this.tcp != null) {
            this.tcp.close();
            this.tcp = null;
        }
    }

    private void reconnect() {
        if (this.stopped || this.isRST) {
            return;
        }
        Log.d(TAG, "start connect timer");

        long t;
        if (this.connectFailCount > 60) {
            t = 60;
        } else {
            t = this.connectFailCount;
        }
        IoLoop loop = IoLoop.getDefaultLoop();
        loop.setTimeout(t * 1000, connectTimer);
    }

    private void connect() {
        if (this.stopped || this.tcp != null) {
            return;
        }
        this.authStatus = -1;
        this.connectState = ConnectState.STATE_CONNECTING;
        publishConnectState();
        IoLoop loop = IoLoop.getDefaultLoop();
        Selector s = loop.getSelector();
        this.tcp = new TCP(s);
        Log.i(TAG, "new tcp...");
        //此处不用常量防止混淆后暴露ip地址
        this.tcp.connect("58.22.120.51", 23000, this.onConnect);

        //connect timeout 60sec
        loop.setTimeout(60 * 1000, new IoLoop.Timer() {
            @Override
            public void handleTimeout() {
                Log.i(TAG, "connect timeout");
                IMService.this.connectFailCount++;
                IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
                IMService.this.publishConnectState();
                closeTCP();
                reconnect();
            }
        });
    }

    private void handleAuthStatus(Message msg) {
        Integer status = (Integer) msg.body;
        this.authStatus = status;
        Log.d(TAG, "auth status:" + status);
    }

    private void handleIMMessage(Message msg) {
        IMMessage im = (IMMessage) msg.body;
        Log.d(TAG, "im message sender:" + im.sender + " receiver:" + im.receiver + " content:"
            + im.content);
        publishPeerMessage(im);
        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);
    }

    private void handleClose() {
        Iterator iter = peerMessages.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, IMMessage> entry = (Map.Entry<Integer, IMMessage>) iter.next();
            IMMessage im = entry.getValue();
            publishPeerMessageFailure(im.msgLocalID, im.receiver);
        }
        peerMessages.clear();
        closeTCP();
        reconnect();
    }

    private void handleACK(Message msg) {
        Integer seq = (Integer) msg.body;
        IMMessage im = peerMessages.get(seq);
        if (im == null) {
            return;
        }

        peerMessages.remove(seq);
        publishPeerMessageACK(im.msgLocalID, im.receiver);
    }

    private void handlePeerACK(Message msg) {
        MessagePeerACK ack = (MessagePeerACK) msg.body;
        publishPeerMessageRemoveACK(ack.msgLocalID, ack.sender);
    }

    private void handleRST(Message msg) {
        Log.i(TAG, "the user login at outside");
        isRST = true;
        publishRST();
    }

    private void handlePong(Message msg) {
        Log.i(TAG, "pong");
        IoLoop.getDefaultLoop().setTimeout(HEARTBEAT * 1000, heartbeatTimer);
    }

    private void handleMessage(Message msg) {
        if (msg.cmd == Command.MSG_AUTH_STATUS) {
            handleAuthStatus(msg);
        } else if (msg.cmd == Command.MSG_IM) {
            handleIMMessage(msg);
        } else if (msg.cmd == Command.MSG_ACK) {
            handleACK(msg);
        } else if (msg.cmd == Command.MSG_PEER_ACK) {
            handlePeerACK(msg);
        } else if (msg.cmd == Command.MSG_RST) {
            handleRST(msg);
        } else if (msg.cmd == Command.MSG_PONG) {
            handlePong(msg);
        } else {
            Log.i(TAG, "unknown message cmd:" + msg.cmd);
        }
    }

    private void appendData(byte[] data) {
        if (this.data != null) {
            int l = this.data.length + data.length;
            byte[] buf = new byte[l];
            System.arraycopy(this.data, 0, buf, 0, this.data.length);
            System.arraycopy(data, 0, buf, this.data.length, data.length);
            this.data = buf;
        } else {
            this.data = data;
        }
    }

    private boolean handleData(byte[] data) {
        appendData(data);

        int pos = 0;
        while (true) {
            if (this.data.length < pos + 4) {
                break;
            }
            int len = BytePacket.readInt32(this.data, pos);
            if (this.data.length - pos < 4 + Message.HEAD_SIZE + len) {
                break;
            }
            Message msg = new Message();
            byte[] buf = new byte[Message.HEAD_SIZE + len];
            System.arraycopy(this.data, pos + 4, buf, 0, Message.HEAD_SIZE + len);
            if (!msg.unpack(buf)) {
                Log.i(TAG, "unpack message error");
                return false;
            }
            handleMessage(msg);
            pos += 4 + Message.HEAD_SIZE + len;
        }

        byte[] left = new byte[this.data.length - pos];
        System.arraycopy(this.data, pos, left, 0, left.length);
        this.data = left;
        return true;
    }

    private void sendAuth() {
        Message msg = new Message();
        if (this.uid > 0) {
            msg.cmd = Command.MSG_AUTH;
            msg.body = new Long(this.uid);
        } else if (!TextUtils.isEmpty(this.accessToken)) {
            msg.cmd = Command.MSG_AUTH_TOKEN;
            msg.body = this.accessToken;
        } else {
            Log.e(TAG, "no auth info");
            return;
        }
        sendMessage(msg);
    }

    private void sendPing() {
        Message msg = new Message();
        msg.cmd = Command.MSG_PING;
        sendMessage(msg);
        Log.i(TAG, "ping...");
    }

    private boolean sendMessage(Message msg) {
        if (this.tcp == null || connectState != ConnectState.STATE_CONNECTED)
            return false;
        this.seq++;
        msg.seq = this.seq;
        byte[] p = msg.pack();
        int l = p.length - Message.HEAD_SIZE;
        byte[] buf = new byte[p.length + 4];
        BytePacket.writeInt32(l, buf, 0);
        System.arraycopy(p, 0, buf, 4, p.length);
        this.tcp.writeData(buf);
        return true;
    }

    private void publishPeerMessage(final IMMessage msg) {
        mainThreadhandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < observers.size(); i++) {
                    IMServiceObserver ob = observers.get(i);
                    ob.onPeerMessage(msg);
                }
            }
        });
    }

    private void publishPeerMessageACK(final int msgLocalID, final long uid) {
        mainThreadhandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < observers.size(); i++) {
                    IMServiceObserver ob = observers.get(i);
                    ob.onPeerMessageACK(msgLocalID, uid);
                }
            }
        });
    }

    private void publishPeerMessageRemoveACK(final int msgLocalID, final long uid) {
        mainThreadhandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < observers.size(); i++) {
                    IMServiceObserver ob = observers.get(i);
                    ob.onPeerMessageRemoteACK(msgLocalID, uid);
                }
            }
        });
    }

    private void publishPeerMessageFailure(final int msgLocalID, final long uid) {
        mainThreadhandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < observers.size(); i++) {
                    IMServiceObserver ob = observers.get(i);
                    ob.onPeerMessageFailure(msgLocalID, uid);
                }
            }
        });
    }

    private void publishConnectState() {
        mainThreadhandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < observers.size(); i++) {
                    IMServiceObserver ob = observers.get(i);
                    ob.onConnectState(connectState);
                }
            }
        });

    }

    private void publishRST() {
        mainThreadhandler.post(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < observers.size(); i++) {
                    IMServiceObserver ob = observers.get(i);
                    ob.onReset();
                }
            }
        });
    }
}
