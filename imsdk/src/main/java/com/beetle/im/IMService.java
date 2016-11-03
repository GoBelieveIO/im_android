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

    private final String HOST = "imnode2.gobelieve.io";
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
    private long uid;
    private long appID;

    private long roomID;

    private long syncKey;
    private HashMap<Long, Long> groupSyncKeys = new HashMap<Long, Long>();

    SyncKeyHandler syncKeyHandler;
    PeerMessageHandler peerMessageHandler;
    GroupMessageHandler groupMessageHandler;
    CustomerMessageHandler customerMessageHandler;
    ArrayList<IMServiceObserver> observers = new ArrayList<IMServiceObserver>();
    ArrayList<GroupMessageObserver> groupObservers = new ArrayList<GroupMessageObserver>();
    ArrayList<PeerMessageObserver> peerObservers = new ArrayList<PeerMessageObserver>();
    ArrayList<SystemMessageObserver> systemMessageObservers = new ArrayList<SystemMessageObserver>();
    ArrayList<CustomerMessageObserver> customerServiceMessageObservers = new ArrayList<CustomerMessageObserver>();
    ArrayList<VOIPObserver> voipObservers = new ArrayList<VOIPObserver>();
    ArrayList<RTMessageObserver> rtMessageObservers = new ArrayList<RTMessageObserver>();
    ArrayList<RoomMessageObserver> roomMessageObservers = new ArrayList<RoomMessageObserver>();

    HashMap<Integer, IMMessage> peerMessages = new HashMap<Integer, IMMessage>();
    HashMap<Integer, IMMessage> groupMessages = new HashMap<Integer, IMMessage>();
    HashMap<Integer, CustomerMessage> customerMessages = new HashMap<Integer, CustomerMessage>();

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

    private boolean isOnNet(Context context) {
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
            Log.i(TAG, "active net info:" + activeNetInfo);
        }
        return isOnNet;
    }

    class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive (Context context, Intent intent) {
            if (isOnNet(context)) {
                Log.i(TAG, "connectivity status:on");
                IMService.this.reachable = true;
                if (!IMService.this.stopped && !IMService.this.isBackground) {
                    //todo 优化 可以判断当前连接的socket的localip和当前网络的ip是一样的情况下
                    //就没有必要重连socket
                    Log.i(TAG, "reconnect im service");
                    IMService.this.suspend();
                    IMService.this.resume();
                }
            } else {
                Log.i(TAG, "connectivity status:off");
                IMService.this.reachable = false;
                if (!IMService.this.stopped) {
                    IMService.this.suspend();
                }
            }
        }
    };

    public void registerConnectivityChangeReceiver(Context context) {
        NetworkReceiver  receiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(receiver, filter);
        this.reachable = isOnNet(context);
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
    public void setUID(long uid) { this.uid = uid; }
    //普通app不需要设置
    public void setAppID(long appID) { this.appID = appID; }
    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    public void addSuperGroupSyncKey(long groupID, long syncKey) {
        this.groupSyncKeys.put(groupID, syncKey);
    }

    public void removeSuperGroupSyncKey(long groupID) {
        this.groupSyncKeys.remove(groupID);
    }

    public void clearSuperGroupSyncKeys() {
        this.groupSyncKeys.clear();
    }

    public void setSyncKeyHandler(SyncKeyHandler handler) {
        this.syncKeyHandler = handler;
    }

    public void setPeerMessageHandler(PeerMessageHandler handler) {
        this.peerMessageHandler = handler;
    }
    public void setGroupMessageHandler(GroupMessageHandler handler) {
        this.groupMessageHandler = handler;
    }
    public void setCustomerMessageHandler(CustomerMessageHandler handler) {
        this.customerMessageHandler = handler;
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


    public void addPeerObserver(PeerMessageObserver ob) {
        if (peerObservers.contains(ob)) {
            return;
        }
        peerObservers.add(ob);
    }

    public void removePeerObserver(PeerMessageObserver ob) {
        peerObservers.remove(ob);
    }

    public void addGroupObserver(GroupMessageObserver ob) {
        if (groupObservers.contains(ob)) {
            return;
        }
        groupObservers.add(ob);
    }

    public void removeGroupObserver(GroupMessageObserver ob) {
        groupObservers.remove(ob);
    }

    public void addSystemObserver(SystemMessageObserver ob) {
        if (systemMessageObservers.contains(ob)) {
            return;
        }
        systemMessageObservers.add(ob);
    }

    public void removeSystemObserver(SystemMessageObserver ob) {
        systemMessageObservers.remove(ob);
    }

    public void addCustomerServiceObserver(CustomerMessageObserver ob) {
        if (customerServiceMessageObservers.contains(ob)) {
            return;
        }
        customerServiceMessageObservers.add(ob);
    }

    public void removeCustomerServiceObserver(CustomerMessageObserver ob) {
        customerServiceMessageObservers.remove(ob);
    }

    public void addRTObserver(RTMessageObserver ob) {
        if (rtMessageObservers.contains(ob)) {
            return;
        }
        rtMessageObservers.add(ob);
    }

    public void removeRTObserver(RTMessageObserver ob){
        rtMessageObservers.remove(ob);
    }

    public void addRoomObserver(RoomMessageObserver ob) {
        if (roomMessageObservers.contains(ob)) {
            return;
        }
        roomMessageObservers.add(ob);
    }

    public void removeRoomObserver(RoomMessageObserver ob) {
        roomMessageObservers.remove(ob);
    }

    public void pushVOIPObserver(VOIPObserver ob) {
        if (voipObservers.contains(ob)) {
            return;
        }
        voipObservers.add(ob);
    }

    public void popVOIPObserver(VOIPObserver ob) {
        voipObservers.remove(ob);
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
        if (!this.stopped) {
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
        this.resume();

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
        this.close();
        heartbeatTimer.suspend();
        connectTimer.suspend();
        this.suspended = true;

        Log.i(TAG, "suspend im service");
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

    public boolean isPeerMessageSending(long peer, int msgLocalID) {
        for(Map.Entry<Integer, IMMessage> entry : peerMessages.entrySet()) {
            IMMessage m = entry.getValue();
            if (m.receiver == peer && m.msgLocalID == msgLocalID) {
                return true;
            }
        }
        return false;
    }

    public boolean isGroupMessageSending(long groupID, int msgLocalID) {
        for(Map.Entry<Integer, IMMessage> entry : groupMessages.entrySet()) {
            IMMessage m = entry.getValue();
            if (m.receiver == groupID && m.msgLocalID == msgLocalID) {
                return true;
            }
        }
        return false;
    }
    public boolean isCustomerMessageSending(long storeID, int msgLocalID) {
        for(Map.Entry<Integer, CustomerMessage> entry : customerMessages.entrySet()) {
            CustomerMessage m = entry.getValue();
            if (m.storeID == storeID && m.msgLocalID == msgLocalID) {
                return true;
            }
        }
        return false;
    }
    public boolean isCustomerSupportMessageSending(long customerID, long customerAppID, int msgLocalID) {
        for(Map.Entry<Integer, CustomerMessage> entry : customerMessages.entrySet()) {
            CustomerMessage m = entry.getValue();
            if (m.customerID == customerID &&
                    m.customerAppID == customerAppID &&
                    m.msgLocalID == msgLocalID) {
                return true;
            }
        }
        return false;
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

    public boolean sendCustomerMessage(CustomerMessage im) {
        Message msg = new Message();
        msg.cmd = Command.MSG_CUSTOMER;
        msg.body = im;
        if (!sendMessage(msg)) {
            return false;
        }

        customerMessages.put(new Integer(msg.seq), im);
        return true;
    }

    public boolean sendCustomerSupportMessage(CustomerMessage im) {
        Message msg = new Message();
        msg.cmd = Command.MSG_CUSTOMER_SUPPORT;
        msg.body = im;
        if (!sendMessage(msg)) {
            return false;
        }

        customerMessages.put(new Integer(msg.seq), im);
        return true;
    }

    public boolean sendRTMessage(RTMessage rt) {
        Message msg = new Message();
        msg.cmd = Command.MSG_RT;
        msg.body = rt;
        if (!sendMessage(msg)) {
            return false;
        }
        return true;
    }

    public boolean sendVOIPControl(VOIPControl ctl) {
        Message msg = new Message();
        msg.cmd = Command.MSG_VOIP_CONTROL;
        msg.body = ctl;
        return sendMessage(msg);
    }

    public boolean sendRoomMessage(RoomMessage rm) {
        Message msg = new Message();
        msg.cmd = Command.MSG_ROOM_IM;
        msg.body = rm;
        return sendMessage(msg);
    }

    private void sendEnterRoom(long roomID) {
        Message msg = new Message();
        msg.cmd = Command.MSG_ENTER_ROOM;
        msg.body = new Long(roomID);
        sendMessage(msg);
    }

    private void sendLeaveRoom(long roomID) {
        Message msg = new Message();
        msg.cmd = Command.MSG_LEAVE_ROOM;
        msg.body = new Long(roomID);
        sendMessage(msg);
    }

    public void enterRoom(long roomID) {
        if (roomID == 0) {
            return;
        }
        this.roomID = roomID;
        sendEnterRoom(roomID);
    }

    public void leaveRoom(long roomID) {
        if (this.roomID != roomID || roomID == 0) {
            return;
        }
        sendLeaveRoom(roomID);
        this.roomID = 0;
    }

    private void close() {
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

        iter = customerMessages.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, CustomerMessage> entry = (Map.Entry<Integer, CustomerMessage>)iter.next();
            CustomerMessage im = entry.getValue();
            if (customerMessageHandler != null) {
                customerMessageHandler.handleMessageFailure(im);
            }
            publishCustomerServiceMessageFailure(im);
        }
        customerMessages.clear();

        if (this.tcp != null) {
            Log.i(TAG, "close tcp");
            this.tcp.close();
            this.tcp = null;
        }
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

    private void startConnectTimer() {
        if (this.stopped || this.suspended || this.isBackground) {
            return;
        }
        long t;
        if (this.connectFailCount > 60) {
            t = uptimeMillis() + 60*1000;
        } else {
            t = uptimeMillis() + this.connectFailCount*1000;
        }
        connectTimer.setTimer(t);
        Log.d(TAG, "start connect timer:" + this.connectFailCount);
    }

    private void onConnected() {
        Log.i(TAG, "tcp connected");
        this.connectFailCount = 0;
        this.connectState = ConnectState.STATE_CONNECTED;
        this.publishConnectState();
        this.sendAuth();
        if (this.roomID > 0) {
            this.sendEnterRoom(IMService.this.roomID);
        }
        this.sendSync(IMService.this.syncKey);
        for (Map.Entry<Long, Long> e : this.groupSyncKeys.entrySet()) {
            this.sendGroupSync(e.getKey(), e.getValue());
        }
        this.tcp.startRead();
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

        this.pingTimestamp = 0;
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
                    IMService.this.startConnectTimer();
                } else {
                    IMService.this.onConnected();
                }
            }
        });

        this.tcp.setReadCallback(new TCPReadCallback() {
            @Override
            public void onRead(Object tcp, byte[] data) {
                if (data.length == 0) {
                    Log.i(TAG, "tcp read eof");
                    IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                    IMService.this.publishConnectState();
                    IMService.this.handleClose();
                } else {
                    IMService.this.pingTimestamp = 0;
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
            startConnectTimer();
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
            this.startConnectTimer();
        }
    }

    private void handleIMMessage(Message msg) {
        IMMessage im = (IMMessage)msg.body;
        Log.d(TAG, "im message sender:" + im.sender + " receiver:" + im.receiver + " content:" + im.content);

        if (im.sender == this.uid) {
            if (peerMessageHandler != null && !peerMessageHandler.handleMessage(im, im.receiver)) {
                Log.i(TAG, "handle im message fail");
                return;
            }
        } else {
            if (peerMessageHandler != null && !peerMessageHandler.handleMessage(im, im.sender)) {
                Log.i(TAG, "handle im message fail");
                return;
            }
        }
        publishPeerMessage(im);
        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);

        if (im.sender == this.uid) {
            if (peerMessageHandler != null && !peerMessageHandler.handleMessageACK(im.msgLocalID, im.receiver)) {
                Log.w(TAG, "handle message ack fail");
                return;
            }
            publishPeerMessageACK(im.msgLocalID, im.receiver);
        }
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

        if (im.sender == this.uid) {
            if (groupMessageHandler != null && !groupMessageHandler.handleMessageACK(im.msgLocalID, im.receiver)) {
                Log.i(TAG, "handle group message ack fail");
                return;
            }
            publishGroupMessageACK(im.msgLocalID, im.receiver);
        }
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
        close();
        startConnectTimer();
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

        CustomerMessage cm = customerMessages.get(seq);
        if (cm != null) {
            if (customerMessageHandler != null && !customerMessageHandler.handleMessageACK(cm)) {
                Log.i(TAG, "handle customer service message ack fail");
                return;
            }
            customerMessages.remove(seq);
            publishCustomerServiceMessageACK(cm);
        }
    }

    private void handleInputting(Message msg) {
        MessageInputing inputting = (MessageInputing)msg.body;
        for (int i = 0; i < peerObservers.size(); i++ ) {
            PeerMessageObserver ob = peerObservers.get(i);
            ob.onPeerInputting(inputting.sender);
        }
    }

    private void handleSystemMessage(Message msg) {
        String sys = (String)msg.body;
        for (int i = 0; i < systemMessageObservers.size(); i++ ) {
            SystemMessageObserver ob = systemMessageObservers.get(i);
            ob.onSystemMessage(sys);
        }

        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);
    }

    private void handleCustomerMessage(Message msg) {
        CustomerMessage cs = (CustomerMessage)msg.body;
        if (customerMessageHandler != null && !customerMessageHandler.handleMessage(cs)) {
            Log.i(TAG, "handle customer service message fail");
            return;
        }

        publishCustomerMessage(cs);

        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);

        if ((this.appID == 0 || this.appID == cs.customerAppID) && this.uid == cs.customerID) {
            if (customerMessageHandler != null && !customerMessageHandler.handleMessageACK(cs)) {
                Log.w(TAG, "handle customer service message ack fail");
                return;
            }
            publishCustomerServiceMessageACK(cs);
        }
    }

    private void handleCustomerSupportMessage(Message msg) {
        CustomerMessage cs = (CustomerMessage)msg.body;
        if (customerMessageHandler != null && !customerMessageHandler.handleCustomerSupportMessage(cs)) {
            Log.i(TAG, "handle customer service message fail");
            return;
        }

        publishCustomerSupportMessage(cs);

        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);

        if (this.appID > 0 && this.appID != cs.customerAppID && this.uid == cs.sellerID) {
            if (customerMessageHandler != null && !customerMessageHandler.handleMessageACK(cs)) {
                Log.w(TAG, "handle customer service message ack fail");
                return;
            }
            publishCustomerServiceMessageACK(cs);
        }
    }

    private void handleRTMessage(Message msg) {
        RTMessage rt = (RTMessage)msg.body;
        for (int i = 0; i < rtMessageObservers.size(); i++ ) {
            RTMessageObserver ob = rtMessageObservers.get(i);
            ob.onRTMessage(rt);
        }
    }

    private void handleVOIPControl(Message msg) {
        VOIPControl ctl = (VOIPControl)msg.body;

        int count = voipObservers.size();
        if (count == 0) {
            return;
        }
        VOIPObserver ob = voipObservers.get(count-1);
        ob.onVOIPControl(ctl);
    }

    private void handleRoomMessage(Message msg) {
        RoomMessage rm = (RoomMessage)msg.body;
        for (int i= 0; i < roomMessageObservers.size(); i++) {
            RoomMessageObserver ob = roomMessageObservers.get(i);
            ob.onRoomMessage(rm);
        }
    }

    private void handleSyncNotify(Message msg) {
        Log.i(TAG, "sync notify:" + msg.body);
        Long newSyncKey = (Long)msg.body;
        if (newSyncKey > this.syncKey) {
            sendSync(this.syncKey);
        }
    }

    private void handleSyncBegin(Message msg) {
        Log.i(TAG, "sync begin...:" + msg.body);
    }

    private void handleSyncEnd(Message msg) {
        Log.i(TAG, "sync end...:" + msg.body);
        Long newSyncKey = (Long)msg.body;
        if (newSyncKey > this.syncKey) {
            this.syncKey = newSyncKey;
            if (this.syncKeyHandler != null) {
                this.syncKeyHandler.saveSyncKey(this.syncKey);
            }
        }
    }

    private void handleSyncGroupNotify(Message msg) {
        GroupSyncKey key = (GroupSyncKey)msg.body;
        Log.i(TAG, "group sync notify:" + key.groupID + " " + key.syncKey);

        Long origin = new Long(0);
        if (this.groupSyncKeys.containsKey(key.groupID)) {
            origin = this.groupSyncKeys.get(key.groupID);
        }
        if (key.syncKey > origin) {
            this.sendGroupSync(key.groupID, origin);
        }
    }

    private void handleSyncGroupBegin(Message msg) {
        GroupSyncKey key = (GroupSyncKey)msg.body;
        Log.i(TAG, "sync group begin...:" + key.groupID + " " + key.syncKey);
    }

    private void handleSyncGroupEnd(Message msg) {
        GroupSyncKey key = (GroupSyncKey)msg.body;
        Log.i(TAG, "sync group end...:" + key.groupID + " " + key.syncKey);

        Long origin = new Long(0);
        if (this.groupSyncKeys.containsKey(key.groupID)) {
            origin = this.groupSyncKeys.get(key.groupID);
        }
        if (key.syncKey > origin) {
            this.groupSyncKeys.put(key.groupID, key.syncKey);
            if (this.syncKeyHandler != null) {
                this.syncKeyHandler.saveGroupSyncKey(key.groupID, key.syncKey);
            }
        }
    }

    private void handlePong(Message msg) {
        this.pingTimestamp = 0;
    }

    private void handleMessage(Message msg) {
        Log.i(TAG, "message cmd:" + msg.cmd);
        if (msg.cmd == Command.MSG_AUTH_STATUS) {
            handleAuthStatus(msg);
        } else if (msg.cmd == Command.MSG_IM) {
            handleIMMessage(msg);
        } else if (msg.cmd == Command.MSG_ACK) {
            handleACK(msg);
        } else if (msg.cmd == Command.MSG_INPUTTING) {
            handleInputting(msg);
        } else if (msg.cmd == Command.MSG_PONG) {
            handlePong(msg);
        } else if (msg.cmd == Command.MSG_GROUP_IM) {
            handleGroupIMMessage(msg);
        } else if (msg.cmd == Command.MSG_GROUP_NOTIFICATION) {
            handleGroupNotification(msg);
        } else if (msg.cmd == Command.MSG_SYSTEM) {
            handleSystemMessage(msg);
        } else if (msg.cmd == Command.MSG_RT) {
            handleRTMessage(msg);
        } else if (msg.cmd == Command.MSG_VOIP_CONTROL) {
            handleVOIPControl(msg);
        } else if (msg.cmd == Command.MSG_CUSTOMER) {
            handleCustomerMessage(msg);
        } else if (msg.cmd == Command.MSG_CUSTOMER_SUPPORT) {
            handleCustomerSupportMessage(msg);
        } else if (msg.cmd == Command.MSG_ROOM_IM) {
            handleRoomMessage(msg);
        } else if (msg.cmd == Command.MSG_SYNC_NOTIFY) {
            handleSyncNotify(msg);
        } else if (msg.cmd == Command.MSG_SYNC_BEGIN) {
            handleSyncBegin(msg);
        } else if (msg.cmd == Command.MSG_SYNC_END) {
            handleSyncEnd(msg);
        } else if (msg.cmd == Command.MSG_SYNC_GROUP_NOTIFY) {
            handleSyncGroupNotify(msg);
        } else if (msg.cmd == Command.MSG_SYNC_GROUP_BEGIN) {
            handleSyncGroupBegin(msg);
        } else if (msg.cmd == Command.MSG_SYNC_GROUP_END) {
            handleSyncGroupEnd(msg);
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

    private void sendSync(long syncKey) {
        Message msg = new Message();
        msg.cmd = Command.MSG_SYNC;
        msg.body = new Long(syncKey);
        sendMessage(msg);
    }

    private void sendGroupSync(long groupID, long syncKey) {
        Message msg = new Message();
        msg.cmd = Command.MSG_SYNC_GROUP;
        GroupSyncKey key = new GroupSyncKey();
        key.groupID = groupID;
        key.syncKey = syncKey;
        msg.body = key;
        sendMessage(msg);
    }

    private void sendHeartbeat() {
        Log.i(TAG, "send ping");
        Message msg = new Message();
        msg.cmd = Command.MSG_PING;
        boolean r = sendMessage(msg);
        if (r && this.pingTimestamp == 0) {
            this.pingTimestamp = now();

            Timer t = new Timer() {
                @Override
                protected void fire() {
                    int now = now();
                    //3s未收到pong
                    if (pingTimestamp > 0 && now - pingTimestamp >= 3) {
                        Log.i(TAG, "ping timeout");
                        handleClose();
                        return;
                    }
                }
            };

            t.setTimer(uptimeMillis()+1000*3+100);
            t.resume();
        }
    }

    private boolean sendMessage(Message msg) {
        if (this.tcp == null || connectState != ConnectState.STATE_CONNECTED) return false;
        this.seq++;
        msg.seq = this.seq;
        byte[] p = msg.pack();
        if (p.length >= 32*1024) {
            Log.e(TAG, "message length overflow");
            return false;
        }
        int l = p.length - Message.HEAD_SIZE;
        byte[] buf = new byte[p.length + 4];
        BytePacket.writeInt32(l, buf, 0);
        System.arraycopy(p, 0, buf, 4, p.length);
        this.tcp.writeData(buf);
        return true;
    }

    private void publishGroupNotification(String notification) {
        for (int i = 0; i < groupObservers.size(); i++ ) {
            GroupMessageObserver ob = groupObservers.get(i);
            ob.onGroupNotification(notification);
        }
    }

    private void publishGroupMessage(IMMessage msg) {
        for (int i = 0; i < groupObservers.size(); i++ ) {
            GroupMessageObserver ob = groupObservers.get(i);
            ob.onGroupMessage(msg);
        }
    }

    private void publishGroupMessageACK(int msgLocalID, long gid) {
        for (int i = 0; i < groupObservers.size(); i++ ) {
            GroupMessageObserver ob = groupObservers.get(i);
            ob.onGroupMessageACK(msgLocalID, gid);
        }
    }


    private void publishGroupMessageFailure(int msgLocalID, long gid) {
        for (int i = 0; i < groupObservers.size(); i++ ) {
            GroupMessageObserver ob = groupObservers.get(i);
            ob.onGroupMessageFailure(msgLocalID, gid);
        }
    }

    private void publishPeerMessage(IMMessage msg) {
        for (int i = 0; i < peerObservers.size(); i++ ) {
            PeerMessageObserver ob = peerObservers.get(i);
            ob.onPeerMessage(msg);
        }
    }

    private void publishPeerMessageACK(int msgLocalID, long uid) {
        for (int i = 0; i < peerObservers.size(); i++ ) {
            PeerMessageObserver ob = peerObservers.get(i);
            ob.onPeerMessageACK(msgLocalID, uid);
        }
    }

    private void publishPeerMessageFailure(int msgLocalID, long uid) {
        for (int i = 0; i < peerObservers.size(); i++ ) {
            PeerMessageObserver ob = peerObservers.get(i);
            ob.onPeerMessageFailure(msgLocalID, uid);
        }
    }

    private void publishConnectState() {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onConnectState(connectState);
        }
    }

    private void publishCustomerMessage(CustomerMessage cs) {
        for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
            CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
            ob.onCustomerMessage(cs);
        }
    }

    private void publishCustomerSupportMessage(CustomerMessage cs) {
        for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
            CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
            ob.onCustomerSupportMessage(cs);
        }
    }

    private void publishCustomerServiceMessageACK(CustomerMessage msg) {
        for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
            CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
            ob.onCustomerMessageACK(msg);
        }
    }


    private void publishCustomerServiceMessageFailure(CustomerMessage msg) {
        for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
            CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
            ob.onCustomerMessageFailure(msg);
        }
    }
}
