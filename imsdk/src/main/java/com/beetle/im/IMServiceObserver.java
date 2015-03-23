package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface IMServiceObserver {
    public void onConnectState(IMService.ConnectState state);
    public void onPeerInputting(long uid);

    public void onPeerMessage(IMMessage msg);
    public void onPeerMessageACK(int msgLocalID, long uid);
    public void onPeerMessageRemoteACK(int msgLocalID, long uid);
    public void onPeerMessageFailure(int msgLocalID, long uid);

    public void onGroupMessage(IMMessage msg);
    public void onGroupMessageACK(int msgLocalID, long uid);
    public void onGroupMessageFailure(int msgLocalID, long uid);
    public void onGroupNotification(String notification);
}
