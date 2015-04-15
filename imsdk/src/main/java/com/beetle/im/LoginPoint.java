package com.beetle.im;

/**
 * Created by houxh on 15/2/3.
 */
public class LoginPoint {
    public static final int PLATFORM_IOS = 1;
    public static final int PLATFORM_ANDROID = 2;
    public static final int PLATFORM_WEB = 3;

    public int upTimestamp;//上线时间戳
    public int platformID;//平台id
    public String deviceID;//设备id
}
