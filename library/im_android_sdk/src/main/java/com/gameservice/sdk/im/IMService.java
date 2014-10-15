package com.gameservice.sdk.im;
import android.util.Log;
import com.beetle.AsyncTCP;
import com.beetle.TCPConnectCallback;
import com.beetle.TCPReadCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.os.SystemClock.uptimeMillis;


public class IMService {

    public enum ConnectState {
        STATE_UNCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_CONNECTFAIL,
    }

    private final String TAG = "imservice";
    private final int HEARTBEAT = 10;
    private AsyncTCP tcp;
    private boolean stopped;
    private Timer connectTimer;
    private Timer heartbeatTimer;
    private int connectFailCount = 0;
    private boolean isRST = false;
    private int seq = 0;
    private ConnectState connectState = ConnectState.STATE_UNCONNECTED;

    private String host;
    private int port;
    private long uid;

    IMPeerMessageHandler peerMessageHandler;
    ArrayList<IMServiceObserver> observers = new ArrayList<IMServiceObserver>();
    HashMap<Integer, IMMessage> peerMessages = new HashMap<Integer, IMMessage>();

    private byte[] data;

    private static IMService instance = new IMService();

    public static IMService getInstance() {
        return instance;
    }

    private IMService() {
        this.stopped = true;

        connectTimer = new Timer() {
            @Override
            protected void fire() {
                IMService.this.connect();
            }
        };

        heartbeatTimer = new Timer() {
            @Override
            protected void fire() {
                IMService.this.sendHeartbeat();
            }
        };
    }

    public ConnectState getConnectState() {
        return connectState;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setUID(long uid) {
        this.uid = uid;
    }

    public void setPeerMessageHandler(IMPeerMessageHandler handler) {
        this.peerMessageHandler = handler;
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
        this.isRST = false;
        this.stopped = false;
        connectTimer.setTimer(uptimeMillis());
        connectTimer.resume();

        heartbeatTimer.setTimer(uptimeMillis(), HEARTBEAT*1000);
        heartbeatTimer.resume();
    }

    public void stop() {
        if (this.stopped) {
            Log.w(TAG, "im service repeat stop");
            return;
        }
        stopped = true;
        heartbeatTimer.suspend();
        connectTimer.suspend();
        this.close();
    }

    public boolean sendPeerMessage(IMMessage im) {
        Message msg = new Message();
        msg.cmd = Command.MSG_IM;
        msg.body = im;
        if (!sendMessage(msg)) {
            return false;
        }

        peerMessages.put(new Integer(msg.seq), im);
        return true;
    }

    private void close() {
        if (this.tcp != null) {
            this.tcp.close();
            this.tcp = null;
        }
        if (this.stopped || this.isRST) {
            return;
        }

        Log.d(TAG, "start connect timer");

        long t;
        if (this.connectFailCount > 60) {
            t = uptimeMillis() + 60*1000;
        } else {
            t = uptimeMillis() + this.connectFailCount*1000;
        }
        connectTimer.setTimer(t);
    }

    private void connect() {
        if (this.tcp != null) {
            return;
        }
        if (this.stopped) {
            Log.e(TAG, "opps....");
            return;
        }
        this.connectState = ConnectState.STATE_CONNECTING;
        publishConnectState();
        this.tcp = new AsyncTCP();
        Log.i(TAG, "new tcp...");

        this.tcp.setConnectCallback(new TCPConnectCallback() {
            @Override
            public void onConnect(Object tcp, int status) {
                if (status != 0) {
                    Log.i(TAG, "connect err:" + status);
                    IMService.this.connectFailCount++;
                    IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
                    IMService.this.publishConnectState();
                    IMService.this.close();
                } else {
                    Log.i(TAG, "tcp connected");
                    IMService.this.connectFailCount = 0;
                    IMService.this.connectState = ConnectState.STATE_CONNECTED;
                    IMService.this.publishConnectState();
                    IMService.this.sendAuth();
                    IMService.this.tcp.startRead();
                }
            }
        });

        this.tcp.setReadCallback(new TCPReadCallback() {
            @Override
            public void onRead(Object tcp, byte[] data) {
                if (data.length == 0) {
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
        });

        this.tcp.connect(this.host, this.port);
    }

    private void handleAuthStatus(Message msg) {
        Integer status = (Integer)msg.body;
        Log.d(TAG, "auth status:" + status);
    }

    private void handleIMMessage(Message msg) {
        IMMessage im = (IMMessage)msg.body;
        Log.d(TAG, "im message sender:" + im.sender + " receiver:" + im.receiver + " content:" + im.content);
        if (!peerMessageHandler.handleMessage(im)) {
            Log.i(TAG, "handle im message fail");
            return;
        }
        publishPeerMessage(im);
        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);
    }

    private void handleClose() {
        Iterator iter = peerMessages.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, IMMessage> entry = (Map.Entry<Integer, IMMessage>)iter.next();
            IMMessage im = entry.getValue();
            peerMessageHandler.handleMessageFailure(im.msgLocalID, im.receiver);
            publishPeerMessageFailure(im.msgLocalID, im.receiver);
        }
        peerMessages.clear();
        close();
    }

    private void handleACK(Message msg) {
        Integer seq = (Integer)msg.body;
        IMMessage im = peerMessages.get(seq);
        if (im == null) {
            return;
        }

        if (!peerMessageHandler.handleMessageACK(im.msgLocalID, im.receiver)) {
            Log.w(TAG, "handle message ack fail");
            return;
        }
        peerMessages.remove(seq);
        publishPeerMessageACK(im.msgLocalID, im.receiver);
    }

    private void handlePeerACK(Message msg) {
        MessagePeerACK ack = (MessagePeerACK)msg.body;
        this.peerMessageHandler.handleMessageRemoteACK(ack.msgLocalID, ack.sender);
        publishPeerMessageRemoveACK(ack.msgLocalID, ack.sender);
    }

    private void handleRST(Message msg) {
        Log.i(TAG, "the user login at outside");
        isRST = true;
        publishRST();
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
        } else {
            Log.i(TAG, "unknown message cmd:"+msg.cmd);
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
            if (this.data.length < 4 + Message.HEAD_SIZE + len) {
                break;
            }
            Message msg = new Message();
            byte[] buf = new byte[Message.HEAD_SIZE + len];
            System.arraycopy(this.data, pos+4, buf, 0, Message.HEAD_SIZE+len);
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
        msg.cmd = Command.MSG_AUTH;
        msg.body = new Long(this.uid);
        sendMessage(msg);
    }

    private void sendHeartbeat() {
        Message msg = new Message();
        msg.cmd = Command.MSG_HEARTBEAT;
        sendMessage(msg);
    }

    private boolean sendMessage(Message msg) {
        if (this.tcp == null || connectState != ConnectState.STATE_CONNECTED) return false;
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

    private void publishPeerMessage(IMMessage msg) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessage(msg);
        }
    }

    private void publishPeerMessageACK(int msgLocalID, long uid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessageACK(msgLocalID, uid);
        }
    }

    private void publishPeerMessageRemoveACK(int msgLocalID, long uid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessageRemoteACK(msgLocalID, uid);
        }
    }

    private void publishPeerMessageFailure(int msgLocalID, long uid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessageFailure(msgLocalID, uid);
        }
    }

    private void publishConnectState() {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onConnectState(connectState);
        }
    }

    private void publishRST() {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onReset();
        }
    }
}
