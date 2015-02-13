package com.gameservice.sdk.im;

/**
 * IMApi
 * Description: IM SDK的开放APi
 * Author:walker lee
 */
public class IMApi {
    /**
     * 将deviceToken绑定到服务器以接收离线消息
     * @param deviceToken 用户设备token
     */
    public static void bindDeviceToken(String deviceToken) {
        IMService.getInstance().bindDeviceToken(deviceToken);
    }
}
