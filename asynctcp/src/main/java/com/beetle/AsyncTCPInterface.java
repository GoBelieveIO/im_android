package com.beetle;

public interface AsyncTCPInterface {

    public void setConnectCallback(TCPConnectCallback cb);
    public void setReadCallback(TCPReadCallback cb);
    public  boolean connect(String host, int port);
    public  void close();

    public  void writeData(byte[] bytes);

    public  void startRead();
}
