package com.beetle.push.face;

import android.content.Context;

/**
 * SmartPushServiceInterface
 * Description: SmartPushService中调用业务逻辑的代码壳
 * Author:walker lee
 */
public interface SmartPushServiceInterface {
    /**
     * 做一些在SmartPushService onCreate方法中初始化的事情
     *
     * @param context context
     */
    public void onServiceCreate(Context context);

    /**
     * SmartPushService中的心跳包回调函数
     *
     * @param context context
     */
    public void onHeartBeat(Context context);

    /**
     * p2p连接建立成功时的回调函数
     *
     * @param context context
     */
    public byte[] onP2PConnected(Context context);


    /**
     * SmartPushService中网络状态变化回调函数
     *
     * @param context context
     */
    public void onNetworkChange(Context context);

    /**
     *
     * @param object
     */
    public void onShowNotification(Context context, Object object);

    /**
     * service 释放的时候的回调
     *
     * @param context context
     */
    public void onServiceDestroy(Context context);

    /**
     * 获取心跳包间隔
     * @return PushService中心跳包间隔时长
     */
    public long getHeatBeatPeriod();

    /**
     * Receiver中接受网络状态变化的广播
     */
    public void onReceiverNetworkChange(Context context);
}
