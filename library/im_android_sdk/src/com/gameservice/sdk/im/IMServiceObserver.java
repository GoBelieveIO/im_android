package com.gameservice.sdk.im;


public interface IMServiceObserver {
    //当前连接状态
    public void onConnectState(IMService.ConnectState state);
    //收到IM消息
    public void onPeerMessage(IMMessage msg);
    //服务器已收到
    public void onPeerMessageACK(int msgLocalID, long uid);
    //接受方已收到
    public void onPeerMessageRemoteACK(int msgLocalID, long uid);
    //消息发送失败
    public void onPeerMessageFailure(int msgLocalID, long uid);

    //当前用户ID在其它地方登录
    public void onLoginPoint(LoginPoint lp);
}
