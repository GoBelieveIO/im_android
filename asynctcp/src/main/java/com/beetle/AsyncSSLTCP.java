package com.beetle;

public class AsyncSSLTCP implements AsyncTCPInterface {
    private int sock;
    private int events;
    private int state;
    private long ssl;
    private long sslCTX;

    private byte[] data;
    
    private TCPConnectCallback connectCallback;
    private TCPReadCallback readCallback;
    private long self;
   

    public void setConnectCallback(TCPConnectCallback cb) {
	connectCallback = cb;
    }
    public void setReadCallback(TCPReadCallback cb) {
	readCallback = cb;
    }
    public native boolean connect(String host, int port);
    public native void close();

    public native void writeData(byte[] bytes);
    
    public native void startRead();
  

    static {
        System.loadLibrary("async_ssl_tcp");
    }
}
