package com.beetle.im;

/**
 * Created by houxh on 14-7-23.
 */
public interface PeerMessageObserver {
    public void onPeerMessage(IMMessage msg);
    public void onPeerSecretMessage(IMMessage msg);
    public void onPeerMessageACK(IMMessage msg);
    public void onPeerMessageFailure(IMMessage msg);
}