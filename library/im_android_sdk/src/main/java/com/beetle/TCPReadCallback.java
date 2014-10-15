package com.beetle;

 
public interface TCPReadCallback {
 
    public void onRead(Object tcp, byte[] data);
 
}