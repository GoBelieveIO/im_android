package com.beetle.im;

import java.util.List;

/**
 * Created by houxh on 14-7-23.
 */
public interface GroupMessageObserver {
    public void onGroupMessages(List<IMMessage> msg);
    public void onGroupMessageACK(IMMessage msg);
    public void onGroupMessageFailure(IMMessage msg);
    public void onGroupNotification(String notification);
}