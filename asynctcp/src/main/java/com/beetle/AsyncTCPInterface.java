package com.beetle;

public interface AsyncTCPInterface {

    public void setConnectCallback(TCPConnectCallback cb);
    public void setReadCallback(TCPReadCallback cb);
    public boolean connect(String host, int port);

    // close async tcp only stop read&write.
    public void close();

    // release the underline resource, like socket fd, java object weak reference.
    public void release();

    public void writeData(byte[] bytes);

    public void startRead();
}
