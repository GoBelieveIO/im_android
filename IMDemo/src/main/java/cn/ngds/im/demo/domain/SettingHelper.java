package cn.ngds.im.demo.domain;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import cn.ngds.im.demo.base.IMDemoApplication;

/**
 * SettingHelper
 * Description: 设置页面信息本地化工具
 */
public enum SettingHelper {
    INSTANCE;

    private static final String CONFIG_NOTIFICATION = "config_notification";
    private static final String CONFIG_VIBRATE = "config_vibrate";
    private static final String CONFIG_SOUND = "config_sound";

    private boolean canNotify;
    private boolean canVibrate;
    private boolean hasSound;

    public void init() {
        canNotify = loadConfig(CONFIG_NOTIFICATION);
        canVibrate = loadConfig(CONFIG_VIBRATE);
        hasSound = loadConfig(CONFIG_SOUND);
    }

    public boolean isCanNotify() {
        return canNotify;
    }

    public void setCanNotify(boolean canNotify) {
        this.canNotify = canNotify;
        saveNotificationConfig(canNotify);
    }

    public boolean isCanVibrate() {
        return canVibrate;
    }

    public void setCanVibrate(boolean canVibrate) {
        this.canVibrate = canVibrate;
        saveVibrateConfig(canVibrate);
    }

    public boolean hasSound() {
        return hasSound;
    }

    public void setHasSound(boolean hasSound) {
        this.hasSound = hasSound;
        saveSoundConfig(hasSound);
    }

    private void saveNotificationConfig(boolean whetherNotify) {
        saveConfig(CONFIG_NOTIFICATION, whetherNotify);
    }

    private void saveVibrateConfig(boolean canVibrate) {
        saveConfig(CONFIG_VIBRATE, canVibrate);
    }

    private void saveSoundConfig(boolean hasSound) {
        saveConfig(CONFIG_SOUND, hasSound);
    }

    private boolean loadConfig(String key) {
        SharedPreferences
            preferences =
            PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
        return preferences.getBoolean(key, false);
    }

    private void saveConfig(String key, boolean value) {
        SharedPreferences
            preferences =
            PreferenceManager.getDefaultSharedPreferences(IMDemoApplication.getApplication());
        preferences.edit().putBoolean(key, value).commit();
    }



}
