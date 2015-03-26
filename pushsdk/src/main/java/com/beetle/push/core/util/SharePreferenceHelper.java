package com.beetle.push.core.util;

import android.content.SharedPreferences;

import com.beetle.push.core.log.PushLog;

/**
 * SharePreferenceHelper
 * Description:
 * Author:walker lx
 * email:lixinforlove@gmail.com
 */
public class SharePreferenceHelper {
    private final static String TAG = "SharePreferenceHelper";

    public static void save(SharedPreferences sharedPreferences, String key, Object value) {
        if (value instanceof String) {
            sharedPreferences.edit().putString(key, (String) value).commit();
        } else if (value instanceof Integer) {
            sharedPreferences.edit().putInt(key, (Integer) value).commit();
        } else if (value instanceof Long) {
            sharedPreferences.edit().putLong(key, (Long) value).commit();
        } else if (value instanceof Boolean) {
            sharedPreferences.edit().putBoolean(key, (Boolean) value).commit();
        } else {
            PushLog.d(TAG, "NgdsSharePreferenceHelper: Miss type");
        }

    }

    public static void remove(SharedPreferences sharedPreferences, String key) {
        sharedPreferences.edit().remove(key).commit();
    }
}
