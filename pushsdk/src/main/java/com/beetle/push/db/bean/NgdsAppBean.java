package com.beetle.push.db.bean;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import com.beetle.push.db.NgdsPushDataConsts;
import com.beetle.push.core.log.NgdsLog;
import com.beetle.push.core.util.SharePreferenceHelper;
import com.beetle.push.core.util.io.IoUtil;

import java.util.Arrays;


/**
 * NgdsAppBean
 * Description: 搭载sdk应用使用的相关信息
 * Author:walker lx
 */
public class NgdsAppBean implements NgdsPushDataConsts {
    private static NgdsAppBean sNgdsAppBean;
    private SharedPreferences mSharedPreferences;
    private byte[] mDeviceToken;
    private final static String TAG = "NgdsAppBean";


    private NgdsAppBean(Context context) {
        mSharedPreferences = getSharePreference(context);
        mDeviceToken = loadDeivceTokenStr();
    }


    public static NgdsAppBean getInstance(Context context) {
        if (null == sNgdsAppBean) {
            synchronized (NgdsAppBean.class) {
                if (null == sNgdsAppBean) {
                    sNgdsAppBean = new NgdsAppBean(context);
                }
            }
        }
        return sNgdsAppBean;
    }

    public void setDeviceToken(byte[] deviceToken) {
        if (!Arrays.equals(mDeviceToken, deviceToken)) {
            mDeviceToken = deviceToken;
        }
        SharePreferenceHelper
            .save(mSharedPreferences, PushSharePreferenceKey.DEVICE_TOKEN,
                IoUtil.bin2Hex(deviceToken));
    }

    public byte[] getDeivceToken() {
        return mDeviceToken;
    }

    public String getDeivceTokenStr() {
        if (null == mDeviceToken) {
            return null;
        }
        return IoUtil.bin2Hex(mDeviceToken);
    }

    private byte[] loadDeivceTokenStr() {
        String deviceTokenStr =
            mSharedPreferences.getString(PushSharePreferenceKey.DEVICE_TOKEN, null);
        if (TextUtils.isEmpty(deviceTokenStr)) {
            NgdsLog.d(TAG, "deviceTokenStr is null");
            return null;
        }
        return IoUtil.hex2bin(deviceTokenStr);
    }

    private SharedPreferences getSharePreference(Context context) {
        return context.getSharedPreferences(NGDS_SMART_PUSH_FILE_NAME, Context.MODE_PRIVATE);
    }
}
