package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface PeerMessageHandler {
    public boolean handleMessage(IMMessage msg);
    public boolean handleMessageACK(IMMessage msg);
    public boolean handleMessageFailure(IMMessage msg);
}