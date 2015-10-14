package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface PeerMessageHandler {
    public boolean handleMessage(IMMessage msg, long uid);
    public boolean handleMessageACK(int msgLocalID, long uid);
    public boolean handleMessageFailure(int msgLocalID, long uid);
}