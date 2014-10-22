package cn.ngds.im.demo.domain;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import cn.ngds.im.demo.base.IMDemoApplication;

/**
 * UserHelper
 * Description:
 */
public enum UserHelper {
    INSTANCE;
    public static final long LOGOUT_ID = -1;
    public final String USER_ID = "user_id";

    private long userId = LOGOUT_ID;

    public long getUserId() {
        if (LOGOUT_ID == userId) {
            SharedPreferences
                preferences =
                PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
            userId = preferences.getLong(USER_ID, LOGOUT_ID);
        }
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
        SharedPreferences
            preferences =
            PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
        preferences.edit().putLong(USER_ID, userId).commit();
    }

    /**
     * 用户帮助类
     *
     * @return 是否是重新登录
     */
    public boolean validUser() {
        long userId = getUserId();
        return userId != LOGOUT_ID;
    }

    public void logout() {
        setUserId(LOGOUT_ID);
    }
}
