/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.im;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import com.beetle.AsyncSSLTCP;
import com.beetle.AsyncTCP;
import com.beetle.AsyncTCPInterface;
import com.beetle.TCPConnectCallback;
import com.beetle.TCPReadCallback;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static android.os.SystemClock.uptimeMillis;

/**
 * Created by houxh on 14-7-21.
 */
public class IMService {
    private static final boolean ENABLE_SSL = false;
    private static final String HOST = "imnode2.gobelieve.io";
    private static int PORT;

    {
        if (ENABLE_SSL) {
            PORT = 24430;
        } else {
            PORT = 23000;
        }
    }


    private static final String TAG = "imservice";
    private static final int HEARTBEAT = 60*3;
    private static final String HEATBEAT_ACTION = "io.gobelieve.HEARTBEAT";
    private static final int CONNECT_TIMEOUT = 60;

    public enum ConnectState {
        STATE_UNCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_CONNECTFAIL,
        STATE_AUTHENTICATION_FAIL,
    }

    private AsyncTCPInterface tcp;
    private boolean stopped = true;
    private boolean suspended = true;
    private boolean reachable = true;
    private boolean isBackground = false;

    private Timer connectTimer;
    private Timer heartbeatTimer;
    private int pingTimestamp;
    private int connectTimestamp;//发起socket连接的时间戳
    private int connectFailCount = 0;
    private int seq = 0;
    private ConnectState connectState = ConnectState.STATE_UNCONNECTED;

    private String hostIP;
    private int timestamp;

    //set before call start
    private String host;
    private int port;
    private String token;
    private String deviceID;
    private int connectTimeout;
    private Looper looper;
    private Handler handler;
    private Handler mainThreadHandler;//调用observer

    private boolean keepAlive;//应用在后台，保持socket连接
    private PendingIntent alarmIntent;
    private PowerManager.WakeLock wakeLock;

    private long roomID;

    //确保一个时刻只有一个同步过程在运行，以免收到重复的消息
    private long syncKey;
    //在同步过程中收到新的syncnotify消息
    private long pendingSyncKey;
    private boolean isSyncing;
    private int syncTimestamp;

    private static class GroupSync {
        public long groupID;
        public long syncKey;
        //在同步过程中收到新的syncnotify消息
        private long pendingSyncKey;
        private boolean isSyncing;
        private int syncTimestamp;
    }

    private HashMap<Long, GroupSync> groupSyncKeys = new HashMap<Long, GroupSync>();

    SyncKeyHandler syncKeyHandler;
    PeerMessageHandler peerMessageHandler;
    GroupMessageHandler groupMessageHandler;
    CustomerMessageHandler customerMessageHandler;
    ArrayList<IMServiceObserver> observers = new ArrayList<IMServiceObserver>();
    ArrayList<GroupMessageObserver> groupObservers = new ArrayList<GroupMessageObserver>();
    ArrayList<PeerMessageObserver> peerObservers = new ArrayList<PeerMessageObserver>();
    ArrayList<SystemMessageObserver> systemMessageObservers = new ArrayList<SystemMessageObserver>();
    ArrayList<CustomerMessageObserver> customerServiceMessageObservers = new ArrayList<CustomerMessageObserver>();
    ArrayList<RTMessageObserver> rtMessageObservers = new ArrayList<RTMessageObserver>();
    ArrayList<RoomMessageObserver> roomMessageObservers = new ArrayList<RoomMessageObserver>();

    ArrayList<Message> messages = new ArrayList<Message>();//已发出，等待ack的消息

    ArrayList<IMMessage> receivedGroupMessages = new ArrayList<IMMessage>();

    Message metaMessage;

    private byte[] data;

    private static IMService im = new IMService();

    public static IMService getInstance() {
        return im;
    }

    public IMService() {
        this.host = HOST;
        this.port = PORT;
        this.connectTimeout = CONNECT_TIMEOUT;
        this.setLooper(Looper.myLooper());
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public ConnectState getConnectState() {
        return connectState;
    }

    public void setLooper(Looper looper) {
        this.looper = looper;
        this.handler = new Handler(looper);
    }

    public Looper getLooper() {
        return looper;
    }

    private void createTimer() {
        if (connectTimer != null && heartbeatTimer != null) {
            return;
        }

        connectTimer = new Timer(looper) {
            @Override
            protected void fire() {
                IMService.this.connect();
            }
        };

        heartbeatTimer = new Timer(looper) {
            @Override
            protected void fire() {
                IMService.this.sendHeartbeat();
            }
        };
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
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

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public void setWakeLock(PowerManager.WakeLock wl) {
        this.wakeLock = wl;
    }

    public void setSyncKey(long syncKey) {
        this.syncKey = syncKey;
    }

    public void addSuperGroupSyncKey(long groupID, long syncKey) {
        GroupSync s = new GroupSync();
        s.groupID = groupID;
        s.syncKey = syncKey;
        this.groupSyncKeys.put(groupID, s);
        this.sendGroupSync(groupID, syncKey);
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

    //call on main thread
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

    public void enterBackground() {
        runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                IMService.this._enterBackground();
            }
        });
    }

    public void _enterBackground() {
        Log.i(TAG, "im service enter background");
        this.isBackground = true;
        if (!this.stopped) {
            suspend();
            if (!keepAlive) {
                IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                IMService.this.publishConnectState();
                this.close();
            }
        }
    }

    public void enterForeground() {
        runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                IMService.this._enterForeground();
            }
        });
    }

    public void _enterForeground() {
        Log.i(TAG, "im service enter foreground");
        this.isBackground = false;
        if (!this.stopped) {
            resume();
        }
    }

    private static boolean isOnNet(Context context) {
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

    static class NetworkReceiver extends BroadcastReceiver {
        private final String TAG = "imservice";

        @Override
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "broadcast receive action:" + action);
            if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
                if (isOnNet(context)) {
                    Log.i(TAG, "connectivity status:on");
                    IMService.im.reachable = true;
                    if (!IMService.im.stopped && !IMService.im.isBackground) {
                        //todo 优化 可以判断当前连接的socket的localip和当前网络的ip是一样的情况下
                        //就没有必要重连socket
                        Log.i(TAG, "reconnect im service");
                        IMService.im.suspend();
                        IMService.im.connectState = ConnectState.STATE_UNCONNECTED;
                        IMService.im.publishConnectState();
                        IMService.im.close();
                        IMService.im.resume();
                    }
                } else {
                    Log.i(TAG, "connectivity status:off");
                    IMService.im.reachable = false;
                    if (!IMService.im.stopped) {
                        IMService.im.suspend();
                        IMService.im.connectState = ConnectState.STATE_UNCONNECTED;
                        IMService.im.publishConnectState();
                        IMService.im.close();
                    }
                }
            } else if (action.equals(IMService.HEATBEAT_ACTION)) {
                if (!IMService.im.keepAlive) {
                    Log.w(TAG, "not keepalive, dummy alarm heatbeat action");
                    return;
                }
                if (!IMService.im.isBackground) {
                    Log.w(TAG, "not in background, dummy alarm heatbeat action");
                    return;
                }

                if (IMService.im.wakeLock != null) {
                    IMService.im.wakeLock.acquire(1000);
                }

                IMService.ConnectState state = IMService.im.getConnectState();
                Log.i(TAG, "im state:" + state);
                if (state == IMService.ConnectState.STATE_CONNECTFAIL || state == IMService.ConnectState.STATE_UNCONNECTED) {
                    Log.i(TAG, "connect im service");
                    im.connect();
                } else if (state == IMService.ConnectState.STATE_CONNECTED) {
                    Log.i(TAG, "send heartbeat");
                    im.sendHeartbeat();
                } else if (state == IMService.ConnectState.STATE_CONNECTING) {
                    int t = IMService.im.connectTimestamp;
                    int n = now();
                    //90s timeout
                    if (n - t > 90) {
                        Log.i(TAG, "im service connect timeout, reconnect");
                        im.close();
                        im.connect();
                    }
                }
            }
        }
    };

    public void registerConnectivityChangeReceiver(Context context) {
        NetworkReceiver  receiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction(HEATBEAT_ACTION);
        context.registerReceiver(receiver, filter, null, handler);
        this.reachable = isOnNet(context);
    }

    //设置了keepalive之后需要创建系统级的定时器
    public void startAlarm(Context context) {
        if (!keepAlive) {
            Log.w(TAG, "keepalive false, can't start alarm");
            return;
        }

        Log.i(TAG, "start alarm");
        Context appContext = context;
        final int ALARM_INTERVAL = HEARTBEAT*1000;//3 * 60 * 1000;
        AlarmManager alarmMgr;
        PendingIntent alarmIntent;
        alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent();
        intent.setAction(HEATBEAT_ACTION);
        intent.setPackage(context.getPackageName());
        alarmIntent = PendingIntent.getBroadcast(appContext, 999, intent, 0);
        Calendar calendar = Calendar.getInstance();

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                ALARM_INTERVAL, alarmIntent);

        this.alarmIntent = alarmIntent;
    }

    public void stopAlarm(Context context) {
        if (alarmIntent != null) {
            Log.i(TAG, "stop alarm");
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.cancel(alarmIntent);
            alarmIntent = null;
        }
    }

    public void start() {
        runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                IMService.this._start();
            }
        });
    }

    private void _start() {
        if (this.token.length() == 0) {
            throw new RuntimeException("NO TOKEN PROVIDED");
        }

        if (!this.stopped) {
            Log.i(TAG, "already started");
            return;
        }
        Log.i(TAG, "start im service");
        this.stopped = false;
        createTimer();
        this.resume();

        //应用在后台的情况下基本不太可能调用start
        if (this.isBackground) {
            Log.w(TAG, "start im service when app is background");
        }
    }

    public void stop() {
        runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                IMService.this._stop();
            }
        });
    }

    private void _stop() {
        if (this.stopped) {
            Log.i(TAG, "already stopped");
            return;
        }
        Log.i(TAG, "stop im service");
        stopped = true;
        suspend();
        IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
        IMService.this.publishConnectState();
        this.close();
        if (this.isBackground) {
            Log.w(TAG, "stop im service when app is background");
        }
    }

    private void suspend() {
        if (this.suspended) {
            Log.i(TAG, "suspended");
            return;
        }

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

    public void sendPeerMessageAsync(final IMMessage im) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                boolean r = IMService.this.sendPeerMessage(im);
                if (!r) {
                    if (peerMessageHandler != null) {
                        peerMessageHandler.handleMessageFailure(im);
                    }
                    publishPeerMessageFailure(im);
                }
            }
        });
    }

    public boolean sendPeerMessage(IMMessage im) {
        assertLooper();
        Message msg = new Message();
        msg.cmd = Command.MSG_IM;
        msg.body = im;
        if (im.isText) {
            msg.flag = Flag.MESSAGE_FLAG_TEXT;
        }
        if (sendMessage(msg)) {
            messages.add(msg);
            //在发送需要回执的消息时尽快发现socket已经断开的情况
            sendHeartbeat();
            return true;
        } else if (!suspended) {
            msg.failCount = 1;
            messages.add(msg);
            return true;
        } else {
            return  false;
        }

    }

    public void sendGroupMessageAsync(final IMMessage im) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                boolean r = IMService.this.sendGroupMessage(im);
                if (!r) {
                    if (groupMessageHandler != null) {
                        groupMessageHandler.handleMessageFailure(im);
                    }
                    publishGroupMessageFailure(im);
                }
            }
        });
    }

    public boolean sendGroupMessage(IMMessage im) {
        assertLooper();
        Message msg = new Message();
        msg.cmd = Command.MSG_GROUP_IM;
        msg.body = im;
        if (im.isText) {
            msg.flag = Flag.MESSAGE_FLAG_TEXT;
        }
        if (sendMessage(msg)) {
            messages.add(msg);
            //在发送需要回执的消息时尽快发现socket已经断开的情况
            sendHeartbeat();

            return true;
        } else if (!suspended) {
            msg.failCount = 1;
            messages.add(msg);
            return true;
        } else {
            return false;
        }
    }

    public void sendCustomerMessageAsync(final CustomerMessage im) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                boolean r = IMService.this.sendCustomerMessage(im);
                if (!r) {
                    if (customerMessageHandler != null) {
                        customerMessageHandler.handleMessageFailure(im);
                    }
                    publishCustomerServiceMessageACK(im);
                }
            }
        });
    }


    public boolean sendCustomerMessage(CustomerMessage im) {
        assertLooper();
        Message msg = new Message();
        msg.cmd = Command.MSG_CUSTOMER;
        msg.body = im;
        if (sendMessage(msg)) {
            messages.add(msg);
            //在发送需要回执的消息时尽快发现socket已经断开的情况
            sendHeartbeat();

            return true;
        } else if (!suspended) {
            msg.failCount = 1;
            messages.add(msg);
            return true;
        } else {
            return false;
        }
    }

    public void sendCustomerSupportMessageAsync(final CustomerMessage im) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                boolean r = IMService.this.sendCustomerSupportMessage(im);
                if (!r) {
                    if (customerMessageHandler != null) {
                        customerMessageHandler.handleMessageFailure(im);
                    }
                    publishCustomerServiceMessageFailure(im);
                }
            }
        });
    }

    public boolean sendCustomerSupportMessage(CustomerMessage im) {
        assertLooper();
        Message msg = new Message();
        msg.cmd = Command.MSG_CUSTOMER_SUPPORT;
        msg.body = im;
        if (sendMessage(msg)) {
            messages.add(msg);
            //在发送需要回执的消息时尽快发现socket已经断开的情况
            sendHeartbeat();

            return true;
        } else if (!suspended) {
            msg.failCount = 1;
            messages.add(msg);
            return true;
        } else {
            return false;
        }

    }

    public void sendRTMessageAsync(final RTMessage rt) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                IMService.this.sendRTMessage(rt);

            }
        });
    }

    public boolean sendRTMessage(RTMessage rt) {
        assertLooper();
        Message msg = new Message();
        msg.cmd = Command.MSG_RT;
        msg.body = rt;
        if (!sendMessage(msg)) {
            return false;
        }
        return true;
    }

    public void sendRoomMessageAsync(final RoomMessage rm) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                IMService.this.sendRoomMessage(rm);
            }
        });
    }

    public boolean sendRoomMessage(RoomMessage rm) {
        assertLooper();
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


    public void enterRoom(final long roomID) {
        if (roomID == 0) {
            return;
        }

        runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                IMService.this.roomID = roomID;
                sendEnterRoom(roomID);
            }
        });
    }

    public void leaveRoom(final long roomID) {
        if (roomID == 0) {
            return;
        }
        runOnWorkThread(new Runnable() {
            @Override
            public void run() {
                if (IMService.this.roomID != roomID) {
                    return;
                }
                sendLeaveRoom(roomID);
                IMService.this.roomID = 0;
            }
        });
    }

    private void close() {
        if (receivedGroupMessages.size() > 0) {
            Log.i(TAG, "socket closed, received group messages:" + receivedGroupMessages);
            receivedGroupMessages.clear();
        }

        if (metaMessage != null) {
            Log.i(TAG, "socket closed, meta message:" + metaMessage);
            metaMessage = null;
        }

        ArrayList<IMMessage> peerMessages = new ArrayList<IMMessage>();
        ArrayList<IMMessage> groupMessages = new ArrayList<IMMessage>();
        ArrayList<CustomerMessage> customerMessages = new ArrayList<CustomerMessage>();
        ArrayList<Message> resendMessages = new ArrayList<Message>();
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            //消息只会被重发一次
            if (m.failCount > 0 || suspended) {
                if (m.cmd == Command.MSG_IM) {
                    peerMessages.add((IMMessage)m.body);
                } else if (m.cmd == Command.MSG_GROUP_IM) {
                    groupMessages.add((IMMessage)m.body);
                } else if (m.cmd == Command.MSG_CUSTOMER || m.cmd == Command.MSG_CUSTOMER_SUPPORT) {
                    customerMessages.add((CustomerMessage)m.body);
                }
            } else {
                m.failCount += 1;
                resendMessages.add(m);
            }
        }

        for (int i = 0; i < peerMessages.size(); i++) {
            IMMessage im = peerMessages.get(i);
            if (peerMessageHandler != null) {
                peerMessageHandler.handleMessageFailure(im);
            }
            publishPeerMessageFailure(im);
        }
        for (int i = 0; i < groupMessages.size(); i++) {
            IMMessage im = groupMessages.get(i);
            if (groupMessageHandler != null) {
                groupMessageHandler.handleMessageFailure(im);
            }
            publishGroupMessageFailure(im);
        }

        for (int i= 0; i < customerMessages.size(); i++) {
            CustomerMessage im = customerMessages.get(i);
            if (customerMessageHandler != null) {
                customerMessageHandler.handleMessageFailure(im);
            }
            publishCustomerServiceMessageFailure(im);
        }

        messages = resendMessages;


        if (this.tcp != null) {
            Log.i(TAG, "close tcp");
            this.tcp.close();
            this.tcp = null;
        }
    }

    private static int now() {
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
                    InetAddress[] inetAddresses = InetAddress.getAllByName(host);
                    for (int i = 0; i < inetAddresses.length; i++) {
                        InetAddress inetAddress = inetAddresses[i];
                        Log.i(TAG, "host name:" + inetAddress.getHostName() + " " + inetAddress.getHostAddress());
                        if (inetAddress instanceof Inet4Address) {
                            return inetAddress.getHostAddress();
                        }
                    }
                    return "";
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

        int now = now();
        this.data = null;
        this.connectFailCount = 0;
        this.connectState = ConnectState.STATE_CONNECTED;
        this.publishConnectState();
        this.sendAuth();
        if (this.roomID > 0) {
            this.sendEnterRoom(this.roomID);
        }
        this.sendSync(this.syncKey);
        this.isSyncing = true;
        this.syncTimestamp = now;
        this.pendingSyncKey = 0;
        for (Map.Entry<Long, GroupSync> e : this.groupSyncKeys.entrySet()) {
            GroupSync s = e.getValue();
            this.sendGroupSync(e.getKey(), s.syncKey);
            s.isSyncing = true;
            s.syncTimestamp = now;
            s.pendingSyncKey = 0;
        }
        //重发失败的消息
        for (Message m : messages) {
            this.sendMessage(m);
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
        this.connectTimestamp = now();
        this.connectState = ConnectState.STATE_CONNECTING;
        IMService.this.publishConnectState();
        if (ENABLE_SSL) {
            this.tcp = new AsyncSSLTCP();
        } else {
            this.tcp = new AsyncTCP();
        }
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

        Log.i(TAG, "tcp connect host ip:" + this.hostIP + " port:" + port);
        boolean r = this.tcp.connect(this.hostIP, this.port);
        if (!r) {
            Log.i(TAG, "connect failure");
            this.tcp = null;
            IMService.this.connectFailCount++;
            IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
            publishConnectState();
            startConnectTimer();
        } else if (connectTimeout > 0){
            Timer t = new Timer() {
                @Override
                protected void fire() {
                    int now = now();
                    if (IMService.this.connectState == ConnectState.STATE_CONNECTING &&
                            now - IMService.this.connectTimestamp >= connectTimeout) {
                        //connect timeout
                        Log.i(TAG, "connect timeout");
                        IMService.this.connectFailCount++;
                        IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
                        IMService.this.publishConnectState();
                        IMService.this.close();
                        IMService.this.startConnectTimer();
                    }
                }
            };

            t.setTimer(uptimeMillis()+1000*connectTimeout+100);
            t.resume();
        }
    }

    private void handleAuthStatus(Message msg) {
        Integer status = (Integer)msg.body;
        Log.d(TAG, "auth status:" + status);
        if (status != 0) {
            //失效的accesstoken,2s后重新连接
            this.connectFailCount = 2;
            this.connectState = ConnectState.STATE_AUTHENTICATION_FAIL;
            this.publishConnectState();
            this.close();
            this.startConnectTimer();
        }
    }

    private void handleIMMessage(Message msg) {
        IMMessage im = (IMMessage)msg.body;
        Log.d(TAG, "im message sender:" + im.sender + " receiver:" + im.receiver + " content:" + im.content);

        im.isSelf = (msg.flag & Flag.MESSAGE_FLAG_SELF) != 0;
        if (peerMessageHandler != null && !peerMessageHandler.handleMessage(im)) {
            Log.i(TAG, "handle im message fail");
            return;
        }
        if (im.secret) {
            publishPeerSecretMessage(im);
        } else {
            publishPeerMessage(im);
        }
        sendACK(msg.seq);
    }

    private void handleGroupIMMessage(Message msg) {
        IMMessage im = (IMMessage)msg.body;
        Log.d(TAG, "group im message sender:" + im.sender + " receiver:" + im.receiver + " content:" + im.content);

        im.isSelf = (msg.flag & Flag.MESSAGE_FLAG_SELF) != 0;

        if ((msg.flag & Flag.MESSAGE_FLAG_PUSH) != 0) {
            ArrayList<IMMessage> array = new ArrayList<IMMessage>();
            array.add(im);
            if (groupMessageHandler != null && !groupMessageHandler.handleMessages(array)) {
                Log.i(TAG, "handle group messages fail");
                return;
            }
            publishGroupMessages(array);
        } else {
            receivedGroupMessages.add(im);
        }
        sendACK(msg.seq);
    }

    private void handleGroupNotification(Message msg) {
        String notification = (String)msg.body;
        Log.d(TAG, "group notification:" + notification);

        if ((msg.flag & Flag.MESSAGE_FLAG_PUSH) != 0) {
            ArrayList<IMMessage> array = new ArrayList<IMMessage>();
            IMMessage im = new IMMessage();
            im.content = notification;
            im.isGroupNotification = true;
            array.add(im);
            if (groupMessageHandler != null && !groupMessageHandler.handleMessages(array)) {
                Log.i(TAG, "handle group messages fail");
                return;
            }
            publishGroupMessages(array);
        } else {
            IMMessage im = new IMMessage();
            im.content = notification;
            im.isGroupNotification = true;
            receivedGroupMessages.add(im);
        }

        sendACK(msg.seq);
    }

    private void handleClose() {
        close();
        startConnectTimer();
    }

    private void handleACK(Message msg) {
        MessageACK ack = (MessageACK)msg.body;
        Integer seq = ack.seq;

        int index = -1;
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);
            if (m.seq == seq) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return;
        }

        Message m = messages.get(index);
        messages.remove(index);
        IMMessage im = null;
        IMMessage groupMsg = null;
        CustomerMessage cm = null;
        if (m.cmd == Command.MSG_IM) {
            im = (IMMessage)m.body;
        } else if (m.cmd == Command.MSG_GROUP_IM) {
            groupMsg = (IMMessage)m.body;
        } else if (m.cmd == Command.MSG_CUSTOMER || m.cmd == Command.MSG_CUSTOMER_SUPPORT) {
            cm = (CustomerMessage)m.body;
        }

        if (im != null) {
            if (peerMessageHandler != null && !peerMessageHandler.handleMessageACK(im, ack.status)) {
                Log.w(TAG, "handle message ack fail");
                return;
            }
            publishPeerMessageACK(im, ack.status);
        }

        if (groupMsg != null) {
            if (groupMessageHandler != null && !groupMessageHandler.handleMessageACK(groupMsg, ack.status)) {
                Log.i(TAG, "handle group message ack fail");
                return;
            }
            publishGroupMessageACK(groupMsg, ack.status);
        }

        if (cm != null) {
            if (customerMessageHandler != null && !customerMessageHandler.handleMessageACK(cm)) {
                Log.i(TAG, "handle customer service message ack fail");
                return;
            }
            publishCustomerServiceMessageACK(cm);
        }

        Message metaMessage = this.metaMessage;
        this.metaMessage = null;
        if (metaMessage != null && metaMessage.seq + 1 == msg.seq) {
            Metadata metadata = (Metadata)metaMessage.body;
            if (metadata.prevSyncKey == 0 || metadata.syncKey == 0) {
                return;
            }

            long newSyncKey = metadata.syncKey;
            if ((msg.flag & Flag.MESSAGE_FLAG_SUPER_GROUP) != 0) {
                if (groupMsg == null) {
                    return;
                }

                long groupID = groupMsg.receiver;
                GroupSync s = null;
                if (this.groupSyncKeys.containsKey(groupID)) {
                    s = this.groupSyncKeys.get(groupID);
                } else {
                    return;
                }

                if (s.syncKey == metadata.prevSyncKey && newSyncKey != s.syncKey) {
                    s.syncKey = newSyncKey;
                    if (this.syncKeyHandler != null) {
                        this.syncKeyHandler.saveGroupSyncKey(groupID, s.syncKey);
                        this.sendGroupSyncKey(groupID, s.syncKey);
                    }
                }
            } else {
                if (this.syncKey == metadata.prevSyncKey && newSyncKey != this.syncKey) {
                    this.syncKey = newSyncKey;
                    if (this.syncKeyHandler != null) {
                        this.syncKeyHandler.saveSyncKey(this.syncKey);
                        this.sendSyncKey(this.syncKey);
                    }
                }
            }
        }
    }


    private void handleSystemMessage(Message msg) {
        String sys = (String)msg.body;
        for (int i = 0; i < systemMessageObservers.size(); i++ ) {
            SystemMessageObserver ob = systemMessageObservers.get(i);
            ob.onSystemMessage(sys);
        }

        sendACK(msg.seq);
    }

    private void handleCustomerMessage(Message msg) {
        CustomerMessage cs = (CustomerMessage)msg.body;

        cs.isSelf = (msg.flag & Flag.MESSAGE_FLAG_SELF) != 0;
        if (customerMessageHandler != null && !customerMessageHandler.handleMessage(cs)) {
            Log.i(TAG, "handle customer service message fail");
            return;
        }

        publishCustomerMessage(cs);

        sendACK(msg.seq);
    }

    private void handleCustomerSupportMessage(Message msg) {
        CustomerMessage cs = (CustomerMessage)msg.body;
        if (customerMessageHandler != null && !customerMessageHandler.handleCustomerSupportMessage(cs)) {
            Log.i(TAG, "handle customer service message fail");
            return;
        }

        publishCustomerSupportMessage(cs);

        sendACK(msg.seq);
    }

    private void handleRTMessage(Message msg) {
        final RTMessage rt = (RTMessage)msg.body;
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < rtMessageObservers.size(); i++ ) {
                    RTMessageObserver ob = rtMessageObservers.get(i);
                    ob.onRTMessage(rt);
                }
            }
        });
    }

    private void handleRoomMessage(Message msg) {
        final RoomMessage rm = (RoomMessage)msg.body;
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i= 0; i < roomMessageObservers.size(); i++) {
                    RoomMessageObserver ob = roomMessageObservers.get(i);
                    ob.onRoomMessage(rm);
                }
            }
        });
    }

    private void handleSyncNotify(Message msg) {
        SyncNotify notify = (SyncNotify)msg.body;
        Long newSyncKey = notify.syncKey;
        Log.i(TAG, "sync notify:" + newSyncKey);

        int now = now();

        //4s同步超时
        boolean isSyncing = this.isSyncing && (now - this.syncTimestamp < 4);

        if (!isSyncing && newSyncKey > this.syncKey) {
            sendSync(this.syncKey);
            this.isSyncing = true;
            this.syncTimestamp = now;
        } else if (newSyncKey > this.pendingSyncKey) {
            //等待此次同步结束后，再同步
            this.pendingSyncKey = newSyncKey;
        }
    }

    private void handleSyncBegin(Message msg) {
        Log.i(TAG, "sync begin...:" + msg.body);
    }

    private void handleSyncEnd(Message msg) {
        Log.i(TAG, "sync end...:" + msg.body);
        if (receivedGroupMessages.size() > 0) {
            if (groupMessageHandler != null && !groupMessageHandler.handleMessages(receivedGroupMessages)) {
                Log.i(TAG, "handle group messages fail");
                return;
            }
            publishGroupMessages(receivedGroupMessages);
            receivedGroupMessages = new ArrayList<IMMessage>();
        }

        Long newSyncKey = (Long)msg.body;
        if (newSyncKey != this.syncKey) {
            this.syncKey = newSyncKey;
            if (this.syncKeyHandler != null) {
                this.syncKeyHandler.saveSyncKey(this.syncKey);
                this.sendSyncKey(this.syncKey);
            }
        }

        int now = now();
        this.isSyncing = false;
        if (this.pendingSyncKey > this.syncKey) {
            //上次同步过程中，再次收到了新的SyncGroupNotify消息
            this.sendSync(this.syncKey);
            this.isSyncing = true;
            this.syncTimestamp = now;
            this.pendingSyncKey = 0;
        }
    }

    private void handleSyncGroupNotify(Message msg) {
        GroupSyncNotify key = (GroupSyncNotify)msg.body;
        Log.i(TAG, "group sync notify:" + key.groupID + " " + key.syncKey);

        GroupSync s = null;
        if (this.groupSyncKeys.containsKey(key.groupID)) {
            s = this.groupSyncKeys.get(key.groupID);
        } else {
            //接受到新加入的超级群消息
            s = new GroupSync();
            s.groupID = key.groupID;
            s.syncKey = 0;
            this.groupSyncKeys.put(new Long(key.groupID), s);
        }

        int now = now();
        //4s同步超时
        boolean isSyncing = s.isSyncing && (now - s.syncTimestamp < 4);
        if (!isSyncing && key.syncKey > s.syncKey) {
            this.sendGroupSync(key.groupID, s.syncKey);
            s.isSyncing = true;
            s.syncTimestamp = now;
        } else if (key.syncKey > s.pendingSyncKey) {
            s.pendingSyncKey = key.syncKey;
        }
    }

    private void handleSyncGroupBegin(Message msg) {
        GroupSyncKey key = (GroupSyncKey)msg.body;
        Log.i(TAG, "sync group begin...:" + key.groupID + " " + key.syncKey);
    }

    private void handleSyncGroupEnd(Message msg) {
        GroupSyncKey key = (GroupSyncKey)msg.body;
        Log.i(TAG, "sync group end...:" + key.groupID + " " + key.syncKey);

        if (receivedGroupMessages.size() > 0) {
            if (groupMessageHandler != null && !groupMessageHandler.handleMessages(receivedGroupMessages)) {
                Log.i(TAG, "handle group messages fail");
                return;
            }
            publishGroupMessages(receivedGroupMessages);
            receivedGroupMessages = new ArrayList<IMMessage>();
        }

        GroupSync s = null;
        if (this.groupSyncKeys.containsKey(key.groupID)) {
            s = this.groupSyncKeys.get(key.groupID);
        } else {
            Log.e(TAG, "no group:" + key.groupID + " sync key");
            return;
        }

        if (key.syncKey != s.syncKey) {
            s.syncKey = key.syncKey;
            if (this.syncKeyHandler != null) {
                this.syncKeyHandler.saveGroupSyncKey(key.groupID, key.syncKey);
                this.sendGroupSyncKey(key.groupID, key.syncKey);
            }
        }

        s.isSyncing = false;

        int now = now();
        if (s.pendingSyncKey > s.syncKey) {
            //上次同步过程中，再次收到了新的SyncGroupNotify消息
            this.sendGroupSync(s.groupID, s.syncKey);
            s.isSyncing = true;
            s.syncTimestamp = now;
            s.pendingSyncKey = 0;
        }
    }

    private void handleMetadata(Message msg) {
        this.metaMessage = msg;
    }

    private void handlePong(Message msg) {
        this.pingTimestamp = 0;
    }

    private void handleMessage(Message msg) {
        Log.i(TAG, "message cmd:" + msg.cmd);

        //处理服务器推到客户端的消息,
        Metadata metadata = null;
        if ((msg.flag & Flag.MESSAGE_FLAG_PUSH) != 0) {
            Message metaMessage = this.metaMessage;
            this.metaMessage = null;
            if (metaMessage != null && metaMessage.seq + 1 == msg.seq) {
                metadata = (Metadata)metaMessage.body;
            } else {
                return;
            }

            if (metadata.prevSyncKey == 0 || metadata.syncKey == 0) {
                return;
            }

            if ((msg.flag & Flag.MESSAGE_FLAG_SUPER_GROUP) != 0) {
                if (msg.cmd != Command.MSG_GROUP_IM) {
                    return;
                }

                IMMessage m = (IMMessage)msg.body;
                long groupID = m.receiver;
                GroupSync s = null;
                if (this.groupSyncKeys.containsKey(groupID)) {
                    s = this.groupSyncKeys.get(groupID);
                } else {
                    return;
                }

                if (metadata.prevSyncKey != s.syncKey) {
                    Log.i(TAG, "super group sync key is not sequence:" + metadata.prevSyncKey + "-----" + s.syncKey + ", ignore push message");
                    return;
                }

            } else {
                if (metadata.prevSyncKey != this.syncKey) {
                    Log.i(TAG, "sync key is not sequence:" + metadata.prevSyncKey + "-----" + this.syncKey + ", ignore push message");
                    return;
                }
            }
        }

        if (msg.cmd == Command.MSG_AUTH_STATUS) {
            handleAuthStatus(msg);
        } else if (msg.cmd == Command.MSG_IM) {
            handleIMMessage(msg);
        } else if (msg.cmd == Command.MSG_ACK) {
            handleACK(msg);
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
        } else if (msg.cmd == Command.MSG_METADATA) {
            handleMetadata(msg);
        } else {
            Log.i(TAG, "unknown message cmd:"+msg.cmd);
        }

        //保存synckey
        if ((msg.flag & Flag.MESSAGE_FLAG_PUSH) != 0) {
            long newSyncKey = metadata.syncKey;
            if ((msg.flag & Flag.MESSAGE_FLAG_SUPER_GROUP) != 0) {
                if (msg.cmd != Command.MSG_GROUP_IM) {
                    return;
                }

                IMMessage m = (IMMessage)msg.body;
                long groupID = m.receiver;
                GroupSync s = null;
                if (this.groupSyncKeys.containsKey(groupID)) {
                    s = this.groupSyncKeys.get(groupID);
                } else {
                    return;
                }

                if (newSyncKey != s.syncKey) {
                    s.syncKey = newSyncKey;
                    if (this.syncKeyHandler != null) {
                        this.syncKeyHandler.saveGroupSyncKey(groupID, s.syncKey);
                        this.sendGroupSyncKey(groupID, s.syncKey);
                    }
                }
            } else {
                if (newSyncKey != this.syncKey) {
                    this.syncKey = newSyncKey;
                    if (this.syncKeyHandler != null) {
                        this.syncKeyHandler.saveSyncKey(this.syncKey);
                        this.sendSyncKey(this.syncKey);
                    }
                }
            }
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

    private void sendSyncKey(long syncKey) {
        Message msg = new Message();
        msg.cmd = Command.MSG_SYNC_KEY;
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

    private void sendGroupSyncKey(long groupID, long syncKey) {
        Message msg = new Message();
        msg.cmd = Command.MSG_GROUP_SYNC_KEY;
        GroupSyncKey key = new GroupSyncKey();
        key.groupID = groupID;
        key.syncKey = syncKey;
        msg.body = key;
        sendMessage(msg);
    }

    private void sendACK(int seq) {
        MessageACK a = new MessageACK();
        a.seq = seq;
        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = a;
        sendMessage(ack);
    }

    private void sendHeartbeat() {
        if (connectState == ConnectState.STATE_CONNECTED && this.pingTimestamp == 0) {
            Log.i(TAG, "send ping");
            Message msg = new Message();
            msg.cmd = Command.MSG_PING;
            sendMessage(msg);

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

    private void publishGroupMessages(final List<IMMessage> msgs) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < groupObservers.size(); i++ ) {
                    GroupMessageObserver ob = groupObservers.get(i);
                    ob.onGroupMessages(msgs);
                }
            }
        });
    }

    private void publishGroupMessageACK(final IMMessage im, final int error) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < groupObservers.size(); i++) {
                    GroupMessageObserver ob = groupObservers.get(i);
                    ob.onGroupMessageACK(im, error);
                }
            }
        });
    }

    private void publishGroupMessageFailure(final IMMessage im) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < groupObservers.size(); i++) {
                    GroupMessageObserver ob = groupObservers.get(i);
                    ob.onGroupMessageFailure(im);
                }
            }
        });
    }

    private void publishPeerMessage(final IMMessage msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < peerObservers.size(); i++) {
                    PeerMessageObserver ob = peerObservers.get(i);
                    ob.onPeerMessage(msg);
                }
            }
        });
    }

    private void publishPeerSecretMessage(final IMMessage msg) {

        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < peerObservers.size(); i++) {
                    PeerMessageObserver ob = peerObservers.get(i);
                    ob.onPeerSecretMessage(msg);
                }
            }
        });

    }

    private void publishPeerMessageACK(final IMMessage msg, final int error) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < peerObservers.size(); i++ ) {
                    PeerMessageObserver ob = peerObservers.get(i);
                    ob.onPeerMessageACK(msg, error);
                }
            }
        });

    }

    private void publishPeerMessageFailure(final IMMessage msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < peerObservers.size(); i++ ) {
                    PeerMessageObserver ob = peerObservers.get(i);
                    ob.onPeerMessageFailure(msg);
                }
            }
        });


    }

    private void publishConnectState() {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < observers.size(); i++ ) {
                    IMServiceObserver ob = observers.get(i);
                    ob.onConnectState(connectState);
                }
            }
        });

    }

    private void publishCustomerMessage(final CustomerMessage cs) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
                    CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
                    ob.onCustomerMessage(cs);
                }
            }
        });

    }

    private void publishCustomerSupportMessage(final CustomerMessage cs) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
                    CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
                    ob.onCustomerSupportMessage(cs);
                }
            }
        });

    }

    private void publishCustomerServiceMessageACK(final CustomerMessage msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
                    CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
                    ob.onCustomerMessageACK(msg);
                }
            }
        });

    }


    private void publishCustomerServiceMessageFailure(final CustomerMessage msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < customerServiceMessageObservers.size(); i++) {
                    CustomerMessageObserver ob = customerServiceMessageObservers.get(i);
                    ob.onCustomerMessageFailure(msg);
                }
            }
        });
    }

    private void runOnMainThread(Runnable r) {
        runOnThread(mainThreadHandler, r);
    }

    private void runOnWorkThread(Runnable r) {
        runOnThread(handler, r);
    }

    private void runOnThread(Handler handler, Runnable r) {
        if (Looper.myLooper() == handler.getLooper()) {
            r.run();
        } else {
            handler.post(r);
        }
    }

    private void assertLooper() {
        if (Looper.myLooper() != looper) {
            throw new AssertionError("looper assert");
        }
    }

}
