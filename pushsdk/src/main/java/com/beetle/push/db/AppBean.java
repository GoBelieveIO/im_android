package com.beetle.push.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.beetle.push.core.log.PushLog;
import com.beetle.push.core.util.SharePreferenceHelper;
import com.beetle.push.core.util.io.IoUtil;

import java.util.Arrays;


/**
 * NgdsAppBean
 * Description: 搭载sdk应用使用的相关信息
 * Author:walker lx
 */
public class AppBean {

    //如果有存储格式或者tag的变动，则增加version版本号并维护版本数据
    public final int version = 4;

    final String NGDS_SMART_PUSH_FILE_NAME = "push_db_" + version;


    public class PushSharePreferenceKey {
        public final static String DEVICE_TOKEN = "device_token";
    }

    private static AppBean sNgdsAppBean;
    private SharedPreferences mSharedPreferences;
    private byte[] mDeviceToken;
    private final static String TAG = "AppBean";


    private AppBean(Context context) {
        mSharedPreferences = getSharePreference(context);
        mDeviceToken = loadDeivceTokenStr();
    }


    public static AppBean getInstance(Context context) {
        if (null == sNgdsAppBean) {
            synchronized (AppBean.class) {
                if (null == sNgdsAppBean) {
                    sNgdsAppBean = new AppBean(context);
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


    private byte[] loadDeivceTokenStr() {
        String deviceTokenStr =
            mSharedPreferences.getString(PushSharePreferenceKey.DEVICE_TOKEN, null);
        if (TextUtils.isEmpty(deviceTokenStr)) {
            PushLog.d(TAG, "deviceTokenStr is null");
            return null;
        }
        return IoUtil.hex2bin(deviceTokenStr);
    }

    private SharedPreferences getSharePreference(Context context) {
        return context.getSharedPreferences(NGDS_SMART_PUSH_FILE_NAME, Context.MODE_PRIVATE);
    }
}
