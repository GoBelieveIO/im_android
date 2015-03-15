package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface IMPeerMessageHandler {
    public boolean handleMessage(IMMessage msg);
    public boolean handleMessageACK(int msgLocalID, long uid);
    public boolean handleMessageRemoteACK(int msgLocalID, long uid);
    public boolean handleMessageFailure(int msgLocalID, long uid);
}