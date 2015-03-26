package com.beetle.push.connect;



import com.beetle.push.core.log.PushLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

/**
 * Created by houxh on 14-8-5.
 */
public class TCP implements IoLoop.Handler {
    private static final String TAG = "TCP";


    public static interface TCPReadCallback {
        public void onRead(TCP tcp, byte[] data);
    }


    public static interface TCPConnectCallback {
        public void onConnect(TCP tcp, int status);
    }

    public static interface  TCPWriteExceptionCallback {
        public void onWriteException(TCP tcp);
    }

    private TCPReadCallback readCallback;
    private TCPConnectCallback connectCallback;
    private TCPWriteExceptionCallback writeExceptionCallback;

    private Selector selector;
    private SelectionKey selectionKey;

    private SocketChannel socketChannel;

    private boolean connected = false;

    private int interestOps;
    private byte[] data;

    public TCP(Selector selector) {

        this.selector = selector;
        this.data = new byte[0];
    }

    public void setWriteExceptionCallback(TCPWriteExceptionCallback cb) {
        this.writeExceptionCallback = cb;
    }

    public boolean connect(String host, int port, TCPConnectCallback cb) {
        this.connectCallback = cb;

        try {
            socketChannel = getChannel();
            selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
            selectionKey.attach(this);
            InetSocketAddress address = new InetSocketAddress(host, port);
            socketChannel.connect(address);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (UnresolvedAddressException e) {
            e.printStackTrace();
            return false;
        }
    }

    public SocketChannel getBlockSocketChannel() {
        try {
            interestOps = selectionKey.interestOps();
            selectionKey.cancel();
            selector.selectNow();
            socketChannel.configureBlocking(true);
            selectionKey = null;
            return socketChannel;
        } catch (IOException e) {
            PushLog.d(TAG, "configure socket exception:" + e);
            return null;
        }
    }

    public void releaseBlockSocketChannel(SocketChannel channel) {
        try {
            assert (channel == socketChannel);
            socketChannel.configureBlocking(false);
            selectionKey = socketChannel.register(selector, interestOps);
            selectionKey.attach(this);
        } catch (IOException e) {
            PushLog.d(TAG, "configure socket exception:" + e);
        }
    }

    private SocketChannel getChannel() throws IOException {
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        Socket socket = socketChannel.socket();
        socket.setTcpNoDelay(true);
        return socketChannel;
    }

    public void handleEvent(SelectionKey key) {
        PushLog.d(TAG, "socket readyops:" + key.readyOps());
        if (key.isConnectable() && socketChannel.isConnectionPending()) {
            handleConnect();
            return;
        }

        if (key.isWritable()) {
            handleWrite();
        }
        if (key.isReadable()) {
            handleRead();
        }
    }

    private void handleConnect() {
        PushLog.d(TAG, "socket connected");
        try {
            this.socketChannel.finishConnect();
            selectionKey.interestOps(0);
        } catch (IOException e) {
            this.connectCallback.onConnect(this, -1);
            return;
        }
        this.connectCallback.onConnect(this, 0);
    }

    private void handleWrite() {
        if (data.length == 0) {
            setInterestedInWrite(false);
            return;
        }

        ByteBuffer toWrite = ByteBuffer.wrap(data);
        try {
            int nwrite = socketChannel.write(toWrite);
            if (nwrite == 0) {
                PushLog.d(TAG, "write 0....");
            }
            int nleft = data.length - nwrite;
            PushLog.d(TAG, "write data left:" + nleft);
            if (nleft > 0) {
                byte[] tmp = new byte[nleft];
                System.arraycopy(data, nwrite, tmp, 0, nleft);
                data = tmp;
            } else {
                data = new byte[0];
                setInterestedInWrite(false);
            }
        } catch (IOException e) {
            PushLog.d(TAG, "write exception:" + e);
            setInterestedInWrite(false);

            if (this.writeExceptionCallback != null) {
                this.writeExceptionCallback.onWriteException(this);
            }
        }
    }

    private void handleRead() {
        ByteBuffer readBuffer = ByteBuffer.allocate(64 * 1024);
        try {
            PushLog.d(TAG, "read......");

            int nread = socketChannel.read(readBuffer);
            if (nread < 0) {
                this.readCallback.onRead(this, null);
                return;
            }
            if (nread == 0) {
                PushLog.d(TAG, "read 0...");
                return;
            }

            readBuffer.flip();
            byte[] tmp = new byte[nread];
            readBuffer.get(tmp);
            this.readCallback.onRead(this, tmp);
        } catch (IOException e) {
            PushLog.d(TAG, "read exception:" + e);
            this.readCallback.onRead(this, null);
        }
    }

    public void startRead(TCPReadCallback cb) {
        this.readCallback = cb;
        setInterestedInRead(true);
    }

    public void close() {
        try {
            PushLog.d(TAG, "close tcp");
            if (selectionKey != null) {
                selectionKey.cancel();
            }
            if (socketChannel != null) {
                socketChannel.close();
            }
        } catch (IOException e) {
            PushLog.d(TAG, "close exception:" + e);
        }
    }

    public void writeData(byte[] bytes) {
        byte[] tmp = new byte[bytes.length + data.length];

        System.arraycopy(data, 0, tmp, 0, data.length);
        System.arraycopy(bytes, 0, tmp, data.length, bytes.length);
        this.data = tmp;

        setInterestedInWrite(true);
        PushLog.d(TAG, "write data left:" + data.length);
    }

    private void setInterestedInRead(boolean isInterested) {
        int oldInterestOps = selectionKey.interestOps();
        int newInterestOps = oldInterestOps;

        if (isInterested) {
            newInterestOps |= SelectionKey.OP_READ;
        } else {
            newInterestOps &= ~SelectionKey.OP_READ;
        }

        if (oldInterestOps != newInterestOps) {
            selectionKey.interestOps(newInterestOps);
            printOps(newInterestOps);
        }
    }

    private void setInterestedInWrite(boolean isInterested) {
        int oldInterestOps = selectionKey.interestOps();
        int newInterestOps = oldInterestOps;
        if (isInterested) {
            newInterestOps |= SelectionKey.OP_WRITE;
        } else {
            newInterestOps &= ~SelectionKey.OP_WRITE;
        }

        if (newInterestOps != oldInterestOps) {
            printOps(newInterestOps);
            selectionKey.interestOps(newInterestOps);
        }
    }

    static private void printOps(int newInterestOps) {

        String str = "";
        if ((newInterestOps & SelectionKey.OP_WRITE) != 0)
            str = "SelectionKey.OP_WRITE|";
        if ((newInterestOps & SelectionKey.OP_READ) != 0)
            str = str + "|SelectionKey.OP_READ|";
        if ((newInterestOps & SelectionKey.OP_CONNECT) != 0)
            str = str + "|SelectionKey.OP_CONNECT";
        if ((newInterestOps & SelectionKey.OP_ACCEPT) != 0)
            str = str + "|SelectionKey.OP_ACCEPT";
        PushLog.d(TAG, "interest ops:" + str);
    }
}
