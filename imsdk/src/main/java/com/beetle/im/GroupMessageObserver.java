package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface GroupMessageObserver {
    public void onGroupMessage(IMMessage msg);
    public void onGroupMessageACK(int msgLocalID, long uid);
    public void onGroupMessageFailure(int msgLocalID, long uid);
    public void onGroupNotification(String notification);
}