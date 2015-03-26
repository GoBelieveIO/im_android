package com.beetle.push.connect;

import android.os.PowerManager;

import com.beetle.push.core.log.PushLog;
import com.beetle.push.core.util.io.IoUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class PushClient {
    private final static String TAG = "PushClient";
    private final int AUTH_STATUS_SUCCESS = 0;
    private final int AUTH_STATUS_INVALID_TOKEN = 1;
    private final int AUTH_STATUS_INTERNAL_ERROR = 2;

    private PushClientObserver observer;
    private String host;
    private int port;

    private byte[] mDeviceToken;

    private TCP tcp;
    private volatile boolean stopped = true;

    private TCP.TCPConnectCallback onConnect;
    private TCP.TCPReadCallback onRead;
    private TCP.TCPWriteExceptionCallback onWriteException;

    private IoLoop.Timer connectTimer;
    private IoLoop.Timer pongTimeoutTimer;


    private int pF0 = 0;
    private int pF1 = 1;

    private long mAppID;
    private String mAppKey;

    private final int RECV_BUF_SIZE = 1024 * 64;

    private byte[] recvBuf;
    private int position;

    private int seq;


    public static enum ClientState {
        UNCONNECTED, CONNECTING, CONNECTED
    }


    private ClientState mClientState = ClientState.UNCONNECTED;
    private PowerManager.WakeLock mWakeLock;

    public PushClient(PushClientObserver ob, PowerManager.WakeLock wakeLock) {
        System.out.println("init PushClient");
        this.mWakeLock = wakeLock;
        this.observer = ob;
        this.recvBuf = new byte[RECV_BUF_SIZE];

        this.onRead = new TCP.TCPReadCallback() {
            @Override
            public void onRead(TCP t, byte[] data) {
                if (data == null || data.length == 0) {
                    PushLog.d(TAG, "tcp closed");
                    closeTCP();
                    reconnect();
                    return;
                }

                PushLog.d(TAG, "readed data:" + data.length);

                if (data.length + position > RECV_BUF_SIZE) {
                    PushLog.d(TAG, "recv buffer overflow");
                    closeTCP();
                    reconnect();
                    return;
                }

                System.arraycopy(data, 0, recvBuf, position, data.length);
                position += data.length;

                ByteBuffer buf = ByteBuffer.wrap(recvBuf, 0, position);
                buf.order(ByteOrder.BIG_ENDIAN);

                boolean error = false;
                int offset = 0;
                while (true) {
                    offset = buf.position();
                    if (buf.remaining() < Protocol.COMMAND_HEADER_SIZE) {
                        break;
                    }

                    Protocol.Header header = Protocol.ReadHeader(buf);
                    if (buf.remaining() < header.length) {
                        break;
                    }

                    Protocol.Command command = null;
                    switch (header.cmd) {
                        case Protocol.CMD_CLIENT_DEVICE_TOKEN:
                            command = new Protocol.ClientDeviceToken();
                            break;
                        case Protocol.CMD_AUTH_STATUS:
                            command = new Protocol.AuthenticationStatus();
                            break;
                        case Protocol.CMD_NOTIFICATION:
                            command = new Protocol.Notification();
                            break;
                        case Protocol.CMD_PONG:
                            command = new Protocol.Pong();
                            break;
                        default:
                            break;
                    }
                    if (command == null) {
                        error = true;
                        break;
                    }
                    if (!command.fromData(buf, header.length)) {
                        error = true;
                        break;
                    }
                    command.seq = header.seq;
                    handleCommand(command);
                }
                if (error) {
                    PushLog.d(TAG, "protocol error");
                    closeTCP();
                    reconnect();
                    return;
                }

                if (offset > 0 && position > offset) {
                    System.arraycopy(recvBuf, offset, recvBuf, 0, position - offset);
                    position -= offset;
                    PushLog.d(TAG, "recv buffer left size:" + position);
                } else {
                    position = 0;
                }
            }
        };

        this.onConnect = new TCP.TCPConnectCallback() {
            @Override
            public void onConnect(TCP t, int status) {
                //取消连接的超时timer
                IoLoop.getDefaultLoop().setTimeout(0, null);

                if (status != 0) {
                    PushLog.d(TAG, "connect error:" + status);
                    closeTCP();
                    reconnect();
                    return;
                }
                mClientState = ClientState.CONNECTED;
                position = 0;
                seq = 0;
                pF0 = 0;
                pF1 = 1;

                tcp.startRead(onRead);

                tcp.setWriteExceptionCallback(onWriteException);

                if (mDeviceToken == null || mDeviceToken.length == 0) {
                    sendRegisterClient();
                } else {
                    assert (observer != null);
                    sendAuth();
                }
            }
        };

        this.onWriteException = new TCP.TCPWriteExceptionCallback() {
            @Override
            public void onWriteException(TCP tcp) {
                PushLog.d(TAG, "tcp write exception");
                closeTCP();
                reconnect();
            }
        };

        this.connectTimer = new IoLoop.Timer() {
            @Override
            public void handleTimeout() {
                connect();
            }
        };


        this.pongTimeoutTimer = new IoLoop.Timer() {
            @Override
            public void handleTimeout() {
                PushLog.d(TAG, "pong timeout");
                closeTCP();
                reconnect();
            }
        };
    }

    private void connect() {
        if (tcp != null || stopped) {
            return;
        }

        this.mWakeLock.acquire(60 * 1000);
        IoLoop loop = IoLoop.getDefaultLoop();
        tcp = new TCP(loop.getSelector());

        PushLog.d(TAG, "push server:" + host + " port:" + port);
        boolean r = tcp.connect(host, port, this.onConnect);
        if (!r) {
            PushLog.d(TAG, "connect host:" + host + " port:" + port + " fail");
            closeTCP();
            reconnect();
            return;
        }
        mClientState = ClientState.CONNECTING;

        //connect timeout 60sec
        loop.setTimeout(60 * 1000, new IoLoop.Timer() {
            @Override
            public void handleTimeout() {
                PushLog.d(TAG, "connect timeout");
                closeTCP();
                reconnect();
            }
        });
    }

    private void reconnect() {
        int nextConTime = pF0 + pF1;

        if (nextConTime > 900) {
            pF0 = 1;
            pF1 = 2;
            nextConTime = 901;
        } else {
            nextConTime = pF0 + pF1;
            pF0 = pF1;
            pF1 = nextConTime;
        }
        PushLog.d(TAG, "connect timer:" + nextConTime);
        IoLoop loop = IoLoop.getDefaultLoop();
        loop.setTimeout(nextConTime * 1000, connectTimer);
    }

    public void start() {
        if (!stopped) {
            return;
        }
        stopped = false;

        PushLog.d(TAG, "registerService push client");
        final IoLoop loop = IoLoop.getDefaultLoop();
        loop.asyncSend(new IoLoop.IoRunnable() {
            @Override
            public void run() {
                if (tcp != null) {
                    return;
                }
                pF0 = 0;
                pF1 = 1;
                loop.setTimeout(0, connectTimer);
            }
        });
    }

    public void stop() {
        if (stopped) {
            return;
        }
        stopped = true;

        PushLog.d(TAG, "stop push client");
        final IoLoop loop = IoLoop.getDefaultLoop();
        loop.asyncSend(new IoLoop.IoRunnable() {
            @Override
            public void run() {
                closeTCP();
                loop.setTimeout(0, null);
            }
        });
    }

    public void setAppID(long appid) {
        mAppID = appid;
    }

    public void setAppKey(String appKey) {
        mAppKey = appKey;
    }

    public void setDeviceToken(byte[] token) {
        mDeviceToken = token;
    }

    public byte[] getDeviceToken() {
        return mDeviceToken;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }


    private void closeTCP() {
        if (tcp != null) {
            tcp.close();
            tcp = null;
        }
        mClientState = ClientState.UNCONNECTED;
    }

    private void sendCommand(Protocol.Command command) {
        Protocol.Header header = new Protocol.Header();
        byte[] data = command.toData();

        header.length = data.length;
        header.cmd = (byte) command.getCmd();
        header.seq = this.seq++;
        byte[] headerData = Protocol.WriteHeader(header);

        byte[] tmp = new byte[headerData.length + data.length];

        System.arraycopy(headerData, 0, tmp, 0, headerData.length);
        System.arraycopy(data, 0, tmp, headerData.length, data.length);
        tcp.writeData(tmp);
    }

    private void sendRegisterClient() {
        Protocol.RegisterClient reg = new Protocol.RegisterClient();
        reg.appid = this.mAppID;
        reg.appkey = this.mAppKey;
        sendCommand(reg);
    }

    private void sendAuth() {
        Protocol.Authentication auth = new Protocol.Authentication();
        PushLog.d(TAG, "token:" + IoUtil.bin2HexForTest(mDeviceToken));
        auth.token = mDeviceToken;
        sendCommand(auth);
    }

    public void sendPing() {
        if (mClientState != ClientState.CONNECTED) {
            return;
        }
        PushLog.d(TAG, "ping");
        Protocol.Ping p = new Protocol.Ping();
        sendCommand(p);

        mWakeLock.acquire(60 * 1000);
        IoLoop.getDefaultLoop().setTimeout(50 * 1000, pongTimeoutTimer);
    }


    private void handleCommand(Protocol.Command command) {
        switch (command.getCmd()) {
            case Protocol.CMD_CLIENT_DEVICE_TOKEN:
                handleDeviceToken((Protocol.ClientDeviceToken) command);
                break;
            case Protocol.CMD_AUTH_STATUS:
                handleAuthStatus((Protocol.AuthenticationStatus) command);
                break;
            case Protocol.CMD_NOTIFICATION:
                handleNotification((Protocol.Notification) command);
                break;
            case Protocol.CMD_PONG:
                handlePong();
                break;
            default:
                PushLog.d(TAG, "unknown command:" + command.getCmd());
                break;
        }
    }

    private void handleDeviceToken(Protocol.ClientDeviceToken deviceToken) {
        mDeviceToken = deviceToken.token;
        PushLog.d(TAG, "device token:" + IoUtil.bin2HexForTest(mDeviceToken));
        observer.onDeviceToken(mDeviceToken);
        sendAuth();
    }

    private void handleAuthStatus(Protocol.AuthenticationStatus authStatus) {
        PushLog.d(TAG, "auth status:" + authStatus.status);
        if (authStatus.status == AUTH_STATUS_INVALID_TOKEN) {
            PushLog.d(TAG, "auth fail:invalid device token");
            this.mDeviceToken = null;
            closeTCP();
            reconnect();
        } else if (authStatus.status == AUTH_STATUS_INTERNAL_ERROR) {
            PushLog.d(TAG, "auth fail:internal error");
            closeTCP();
            reconnect();
        } else {
            mWakeLock.release();
        }
    }

    private void handlePong() {
        PushLog.d(TAG, "pong");
        mWakeLock.release();
        final IoLoop loop = IoLoop.getDefaultLoop();
        //cancel pong timeout
        loop.setTimeout(0, null);
    }

    private void handleNotification(Protocol.Notification notification) {
        PushLog.d(TAG, "receive notification nid:" + notification.nid);
        observer.onPushMessage(notification);

        Protocol.ACKNotification ack = new Protocol.ACKNotification();
        ack.ack_seq = notification.seq;
        sendCommand(ack);
    }
}


