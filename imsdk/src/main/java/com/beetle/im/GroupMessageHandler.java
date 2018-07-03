package com.beetle.im;

/**
 * Created by houxh on 15/3/21.
 */
public interface GroupMessageHandler {
    public boolean handleMessage(IMMessage msg);
    public boolean handleMessageACK(IMMessage msg);
    public boolean handleMessageFailure(IMMessage msg);
    public boolean handleGroupNotification(String notification);
}
