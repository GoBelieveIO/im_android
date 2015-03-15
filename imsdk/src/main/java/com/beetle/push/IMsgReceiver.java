package com.beetle.push;

public interface IMsgReceiver {
    // 接收DeviceToken
    public void onDeviceToken(byte[] tokenArrary);
}
