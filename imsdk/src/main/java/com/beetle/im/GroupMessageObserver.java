package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface GroupMessageObserver {
    public void onGroupMessage(IMMessage msg);
    public void onGroupMessageACK(IMMessage msg);
    public void onGroupMessageFailure(IMMessage msg);
    public void onGroupNotification(String notification);
}