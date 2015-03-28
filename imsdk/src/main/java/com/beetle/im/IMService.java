package com.beetle.im;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import com.beetle.AsyncTCP;
import com.beetle.TCPConnectCallback;
import com.beetle.TCPReadCallback;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static android.os.SystemClock.uptimeMillis;

/**
 * Created by houxh on 14-7-21.
 */
public class IMService {

    private final String HOST = "imnode.gobelieve.io";
    private final int PORT = 23000;

    public enum ConnectState {
        STATE_UNCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_CONNECTFAIL,
    }

    private final String TAG = "imservice";
    private final int HEARTBEAT = 60*3;
    private AsyncTCP tcp;
    private boolean stopped = true;
    private boolean suspended = true;
    private boolean reachable = true;
    private boolean isBackground = false;

    private Timer connectTimer;
    private Timer heartbeatTimer;
    private int pingTimestamp;
    private int connectFailCount = 0;
    private int seq = 0;
    private ConnectState connectState = ConnectState.STATE_UNCONNECTED;

    private String hostIP;
    private int timestamp;

    private String host;
    private int port;
    private String token;
    private String deviceID;

    PeerMessageHandler peerMessageHandler;
    GroupMessageHandler groupMessageHandler;
    ArrayList<IMServiceObserver> observers = new ArrayList<IMServiceObserver>();
    HashMap<Integer, IMMessage> peerMessages = new HashMap<Integer, IMMessage>();
    HashMap<Integer, IMMessage> groupMessages = new HashMap<Integer, IMMessage>();

    private byte[] data;

    private static IMService im = new IMService();

    public static IMService getInstance() {
        return im;
    }

    public IMService() {
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

        this.host = HOST;
        this.port = PORT;
    }

    public void registerConnectivityChangeReceiver(Context context) {
        class NetworkReceiver extends BroadcastReceiver {
            @Override
            public void onReceive (Context context, Intent intent) {
                if (isOnNet(context)) {
                    Log.i(TAG, "connectivity status:on");
                    if (!IMService.this.stopped && !IMService.this.isBackground) {
                        Log.i(TAG, "reconnect");
                        IMService.this.resume();
                    }
                } else {
                    Log.i(TAG, "connectivity status:on");
                    if (!IMService.this.stopped) {
                        IMService.this.suspend();
                    }
                }
            }
            boolean isOnNet(Context context) {
                if (null == context) {
                    Log.e("", "context is null");
                    return false;
                }
                boolean isOnNet = false;
                ConnectivityManager connectivityManager = (ConnectivityManager) context
                        .getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
                if (null != activeNetInfo) {
                    isOnNet = activeNetInfo.isConnected();
                }
                return isOnNet;
            }

        };

        NetworkReceiver  receiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(receiver, filter);
    }

    public ConnectState getConnectState() {
        return connectState;
    }

    public void setHost(String host) {
        this.host = host;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public void setPeerMessageHandler(PeerMessageHandler handler) {
        this.peerMessageHandler = handler;
    }
    public void setGroupMessageHandler(GroupMessageHandler handler) {
        this.groupMessageHandler = handler;
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

    public void enterBackground() {
        Log.i(TAG, "im service enter background");
        this.isBackground = true;
        if (!this.stopped) {
            suspend();
        }
    }

    public void enterForeground() {
        Log.i(TAG, "im service enter foreground");
        this.isBackground = false;
        if (!this.stopped && this.reachable) {
            resume();
        }
    }

    public void start() {
        if (this.token.length() == 0) {
            throw new RuntimeException("NO TOKEN PROVIDED");
        }

        if (!this.stopped) {
            Log.i(TAG, "already started");
            return;
        }
        Log.i(TAG, "start im service");
        this.stopped = false;
        if (this.reachable) {
            this.resume();
        }
        //应用在后台的情况下基本不太可能调用start
        if (this.isBackground) {
            Log.w(TAG, "start im service when app is background");
        }
    }

    public void stop() {
        if (this.stopped) {
            Log.i(TAG, "already stopped");
            return;
        }
        Log.i(TAG, "stop im service");
        stopped = true;
        suspend();
    }

    private void suspend() {
        if (this.suspended) {
            Log.i(TAG, "suspended");
            return;
        }
        Log.i(TAG, "suspend im service");
        this.suspended = true;

        heartbeatTimer.suspend();
        connectTimer.suspend();
        this.close();
    }

    private void resume() {
        if (!this.suspended) {
            return;
        }
        Log.i(TAG, "resume im service");
        this.suspended = false;

        connectTimer.setTimer(uptimeMillis());
        connectTimer.resume();

        heartbeatTimer.setTimer(uptimeMillis(), HEARTBEAT*1000);
        heartbeatTimer.resume();
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

    public boolean sendGroupMessage(IMMessage im) {
        Message msg = new Message();
        msg.cmd = Command.MSG_GROUP_IM;
        msg.body = im;
        if (!sendMessage(msg)) {
            return false;
        }

        groupMessages.put(new Integer(msg.seq), im);
        return true;
    }

    private void close() {
        if (this.tcp != null) {
            Log.i(TAG, "close tcp");
            this.tcp.close();
            this.tcp = null;
        }
        if (this.stopped) {
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

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    private void refreshHost() {
        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... urls) {
                return lookupHost(IMService.this.host);
            }

            private String lookupHost(String host) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(host);
                    Log.i(TAG, "host name:" + inetAddress.getHostName() + " " + inetAddress.getHostAddress());
                    return inetAddress.getHostAddress();
                } catch (UnknownHostException exception) {
                    exception.printStackTrace();
                    return "";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.length() > 0) {
                    IMService.this.hostIP = result;
                    IMService.this.timestamp = now();
                }
            }
        }.execute();
    }

    private void connect() {
        if (this.tcp != null) {
            return;
        }
        if (this.stopped) {
            Log.e(TAG, "opps....");
            return;
        }

        if (hostIP == null || hostIP.length() == 0) {
            refreshHost();
            IMService.this.connectFailCount++;
            Log.i(TAG, "host ip is't resolved");

            long t;
            if (this.connectFailCount > 60) {
                t = uptimeMillis() + 60*1000;
            } else {
                t = uptimeMillis() + this.connectFailCount*1000;
            }
            connectTimer.setTimer(t);
            return;
        }

        if (now() - timestamp > 5*60) {
            refreshHost();
        }

        this.connectState = ConnectState.STATE_CONNECTING;
        IMService.this.publishConnectState();
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

        boolean r = this.tcp.connect(this.hostIP, this.port);
        Log.i(TAG, "tcp connect:" + r);
        if (!r) {
            this.tcp = null;
            IMService.this.connectFailCount++;
            IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
            publishConnectState();
            Log.d(TAG, "start connect timer");

            long t;
            if (this.connectFailCount > 60) {
                t = uptimeMillis() + 60*1000;
            } else {
                t = uptimeMillis() + this.connectFailCount*1000;
            }
            connectTimer.setTimer(t);
        }
    }

    private void handleAuthStatus(Message msg) {
        Integer status = (Integer)msg.body;
        Log.d(TAG, "auth status:" + status);
        if (status != 0) {
            //失效的accesstoken,2s后重新连接
            this.connectFailCount = 2;
            this.connectState = ConnectState.STATE_UNCONNECTED;
            this.publishConnectState();
            this.close();
        }
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

    private void handleGroupIMMessage(Message msg) {
        IMMessage im = (IMMessage)msg.body;
        Log.d(TAG, "group im message sender:" + im.sender + " receiver:" + im.receiver + " content:" + im.content);
        if (groupMessageHandler != null && !groupMessageHandler.handleMessage(im)) {
            Log.i(TAG, "handle im message fail");
            return;
        }
        publishGroupMessage(im);
        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);
    }

    private void handleGroupNotification(Message msg) {
        String notification = (String)msg.body;
        Log.d(TAG, "group notification:" + notification);
        if (groupMessageHandler != null && !groupMessageHandler.handleGroupNotification(notification)) {
            Log.i(TAG, "handle group notification fail");
            return;
        }
        publishGroupNotification(notification);

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
            if (peerMessageHandler != null) {
                peerMessageHandler.handleMessageFailure(im.msgLocalID, im.receiver);
            }
            publishPeerMessageFailure(im.msgLocalID, im.receiver);
        }


        peerMessages.clear();

        iter = groupMessages.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, IMMessage> entry = (Map.Entry<Integer, IMMessage>)iter.next();
            IMMessage im = entry.getValue();
            if (groupMessageHandler != null) {
                groupMessageHandler.handleMessageFailure(im.msgLocalID, im.receiver);
            }
            publishGroupMessageFailure(im.msgLocalID, im.receiver);
        }
        groupMessages.clear();

        close();
    }

    private void handleACK(Message msg) {
        Integer seq = (Integer)msg.body;
        IMMessage im = peerMessages.get(seq);
        if (im != null) {
            if (peerMessageHandler != null && !peerMessageHandler.handleMessageACK(im.msgLocalID, im.receiver)) {
                Log.w(TAG, "handle message ack fail");
                return;
            }
            peerMessages.remove(seq);
            publishPeerMessageACK(im.msgLocalID, im.receiver);
            return;
        }
        im = groupMessages.get(seq);
        if (im != null) {

            if (groupMessageHandler != null && !groupMessageHandler.handleMessageACK(im.msgLocalID, im.receiver)) {
                Log.i(TAG, "handle group message ack fail");
                return;
            }
            groupMessages.remove(seq);
            publishGroupMessageACK(im.msgLocalID, im.receiver);
        }
    }

    private void handlePeerACK(Message msg) {
        MessagePeerACK ack = (MessagePeerACK)msg.body;
        this.peerMessageHandler.handleMessageRemoteACK(ack.msgLocalID, ack.sender);

        publishPeerMessageRemoteACK(ack.msgLocalID, ack.sender);

    }

    private void handleInputting(Message msg) {
        MessageInputing inputting = (MessageInputing)msg.body;
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerInputting(inputting.sender);
        }
    }

    private void handlePong(Message msg) {
        this.pingTimestamp = 0;
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
        } else if (msg.cmd == Command.MSG_INPUTTING) {
            handleInputting(msg);
        } else if (msg.cmd == Command.MSG_PONG) {
            handlePong(msg);
        } else if (msg.cmd == Command.MSG_GROUP_IM) {
            handleGroupIMMessage(msg);
        } else if (msg.cmd == Command.MSG_GROUP_NOTIFICATION) {
            handleGroupNotification(msg);
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
            if (this.data.length < pos + 4 + Message.HEAD_SIZE + len) {
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
        final int PLATFORM_ANDROID = 2;

        Message msg = new Message();
        msg.cmd = Command.MSG_AUTH_TOKEN;
        AuthenticationToken auth = new AuthenticationToken();
        auth.platformID = PLATFORM_ANDROID;
        auth.token = this.token;
        auth.deviceID = this.deviceID;
        msg.body = auth;

        sendMessage(msg);
    }

    private void sendHeartbeat() {
        if (this.pingTimestamp > 0 && now() - this.pingTimestamp > 60) {
            Log.i(TAG, "ping timeout");
            handleClose();
            return;
        }
        Log.i(TAG, "send ping");
        Message msg = new Message();
        msg.cmd = Command.MSG_PING;
        boolean r = sendMessage(msg);
        if (r && this.pingTimestamp == 0) {
            this.pingTimestamp = now();
        }
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

    private void publishGroupNotification(String notification) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onGroupNotification(notification);
        }
    }

    private void publishGroupMessage(IMMessage msg) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onGroupMessage(msg);
        }
    }

    private void publishGroupMessageACK(int msgLocalID, long gid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onGroupMessageACK(msgLocalID, gid);
        }
    }


    private void publishGroupMessageFailure(int msgLocalID, long gid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onGroupMessageFailure(msgLocalID, gid);
        }
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

    private void publishPeerMessageRemoteACK(int msgLocalID, long uid) {
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
}
