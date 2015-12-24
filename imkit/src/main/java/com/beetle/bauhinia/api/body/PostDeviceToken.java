package com.beetle.bauhinia.api.body;

import com.google.gson.annotations.SerializedName;

/**
 * Created by houxh on 15/2/2.
 */
public class PostDeviceToken {
    @SerializedName("ng_device_token")
    public String deviceToken;

    @SerializedName("xg_device_token")
    public String xgDeviceToken;

    @SerializedName("xm_device_token")
    public String xmDeviceToken;

    @SerializedName("hw_device_token")
    public String hwDeviceToken;

    @SerializedName("gcm_device_token")
    public String gcmDeviceToken;

}
