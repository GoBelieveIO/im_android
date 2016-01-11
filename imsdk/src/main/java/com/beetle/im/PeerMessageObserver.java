package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface PeerMessageObserver {
    public void onPeerInputting(long uid);

    public void onPeerMessage(IMMessage msg);
    public void onPeerMessageACK(int msgLocalID, long uid);
    public void onPeerMessageFailure(int msgLocalID, long uid);
}