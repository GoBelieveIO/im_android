package com.beetle.im;

/**
 * Created by houxh on 15/3/21.
 */
public interface GroupMessageHandler {
    public boolean handleMessage(IMMessage msg);
    public boolean handleMessageACK(int msgLocalID, long uid);
    public boolean handleMessageFailure(int msgLocalID, long uid);
    public boolean handleGroupNotification(String notification);
}
