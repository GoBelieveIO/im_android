package com.beetle.push.connect;



/**
 * Created by houxh on 14-8-4.
 * run in IoLooper
 */
public interface PushClientObserver {
    public void onDeviceToken(byte[] deviceToken);

    public void onPushMessage(Protocol.Notification notification);
}
