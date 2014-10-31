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
    public final String SENDER_ID = "sender_id";
    public final String RECEIVER_ID = "receiver_id";

    private long senderId = LOGOUT_ID;
    private long receiverId = LOGOUT_ID;

    public long getSenderId() {
        if (LOGOUT_ID == senderId) {
            SharedPreferences
                preferences =
                PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
            senderId = preferences.getLong(SENDER_ID, LOGOUT_ID);
        }
        return senderId;
    }

    public long getReceiverId() {
        if (LOGOUT_ID == receiverId) {
            SharedPreferences
                preferences =
                PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
            receiverId = preferences.getLong(RECEIVER_ID, LOGOUT_ID);
        }
        return receiverId;
    }

    public void setSenderId(long senderId) {
        this.senderId = senderId;
        SharedPreferences
            preferences =
            PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
        preferences.edit().putLong(SENDER_ID, senderId).commit();
    }

    public void setReceiverId(long receiverId) {
        this.receiverId = receiverId;
        SharedPreferences
            preferences =
            PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
        preferences.edit().putLong(RECEIVER_ID, receiverId).commit();
    }

    /**
     * 用户帮助类
     *
     * @return 是否是重新登录
     */
    public boolean validUser() {
        long senderId = getSenderId();
        long receiverId = getReceiverId();
        return senderId != LOGOUT_ID && receiverId != LOGOUT_ID;
    }

    public void logout() {
        setSenderId(LOGOUT_ID);
        setReceiverId(LOGOUT_ID);
    }
}
