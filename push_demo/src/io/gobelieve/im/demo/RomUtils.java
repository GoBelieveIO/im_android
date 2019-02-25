package io.gobelieve.im.demo;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

public class RomUtils {
    static final String TAG = "goubuli";
    private static final String KEY_MIUI_VERSION_CODE = "ro.miui.ui.version.code";
    private static final String KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name";
    private static final String KEY_MIUI_INTERNAL_STORAGE = "ro.miui.internal.storage";



    private static String getSystemProperty(String propName) {
        String line;
        BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop " + propName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            Log.e(TAG, "Unable to read sysprop " + propName, ex);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    Log.e(TAG, "Exception while closing InputStream", e);
                }
            }
        }
        return line;
    }

    public static boolean isHuaweiRom() {
        return isHuawei() && !TextUtils.isEmpty(getEmuiVersion());
    }

    private static boolean isHuawei() {
        String manufacturer = Build.MANUFACTURER;
        return !TextUtils.isEmpty(manufacturer) && manufacturer.contains("HUAWEI");
    }

    public static boolean isMiuiRom() {
        return isMiui() && !TextUtils.isEmpty(getMiuiVersion());
    }

    private static boolean isMiui() {
        return (Build.MANUFACTURER.equalsIgnoreCase("Xiaomi"));
    }

    public static String getMiuiVersion() {
        return getSystemProperty("ro.miui.ui.version.name");
    }

    public static String getEmuiVersion() {
        String emuiVerion = "";
        Class<?>[] clsArray = new Class<?>[] { String.class };
        Object[] objArray = new Object[] { "ro.build.version.emui" };
        try {
            Class<?> SystemPropertiesClass = Class
                    .forName("android.os.SystemProperties");
            Method get = SystemPropertiesClass.getDeclaredMethod("get",
                    clsArray);
            String version = (String) get.invoke(SystemPropertiesClass,
                    objArray);
            Log.d(TAG, "get EMUI version is:" + version);
            if (!TextUtils.isEmpty(version)) {
                return version;
            }
        } catch (ClassNotFoundException e) {
            Log.e(TAG, " getEmuiVersion wrong, ClassNotFoundException");
        } catch (LinkageError e) {
            Log.e(TAG, " getEmuiVersion wrong, LinkageError");
        } catch (NoSuchMethodException e) {
            Log.e(TAG, " getEmuiVersion wrong, NoSuchMethodException");
        } catch (NullPointerException e) {
            Log.e(TAG, " getEmuiVersion wrong, NullPointerException");
        } catch (Exception e) {
            Log.e(TAG, " getEmuiVersion wrong");
        }
        return emuiVerion;
    }


    private static boolean isVivo() {
        return Build.MANUFACTURER.equals("vivo");
    }

    private static boolean isSamsung() {
        return Build.MANUFACTURER.equals("samsung");
    }

    private static boolean isMIUI() {
        return !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_CODE))
                || !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_VERSION_NAME))
                || !TextUtils.isEmpty(getSystemProperty(KEY_MIUI_INTERNAL_STORAGE));
    }

    private static boolean isH2OS() {
        String key = "ro.rom.version";
        String value = getSystemProperty(key);
        return (value != null && value.indexOf("H2OS") != -1);
    }

    private static boolean isFlyme() {
        try {
            // Invoke Build.hasSmartBar()
            final Method method = Build.class.getMethod("hasSmartBar");
            return method != null;
        } catch (final Exception e) {
            return false;
        }
    }

    private static boolean isOppo() {
        String key = "ro.build.version.opporom";
        String value = getSystemProperty(key);
        return !TextUtils.isEmpty(value);
    }


    public static void openH2OSAutoRunSetting(Context context) {
        try {
            Intent i = new Intent();
            ComponentName comp = new ComponentName("com.oneplus.security", "com.oneplus.security.autorun.AutorunMainActivity");
            i.setComponent(comp);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void openOppoAutoRunSetting(Context context) {
        try {
            Intent i = new Intent();
            ComponentName comp = new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity");
            i.setComponent(comp);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void openMIUIAutoRunSetting(Context context) {
        try {
            Intent i = new Intent();
            ComponentName comp = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
            i.setComponent(comp);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void openSamsungAutoRunSetting(Context context) {
        try {
            Intent i = new Intent();
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            ComponentName comp = new ComponentName("com.samsung.android.sm_cn",
                    "com.samsung.android.sm.ui.ram.AutoRunActivity");
            i.setComponent(comp);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void openHuaWeiAutoRunSetting(Context context) {
        try {
            Intent i = new Intent();
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            ComponentName comp = new ComponentName("com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity");
            i.setComponent(comp);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void openVIVOAutoRunSetting(Context context) {
        try {
            Intent i = new Intent();
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            ComponentName comp =  ComponentName.unflattenFromString("com.iqoo.secure" +
                    "/.safeguard.PurviewTabActivity");
            i.setComponent(comp);
            context.startActivity(i);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void openDefaultAutoRunSetting(Context context) {
        context.startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    public static void openAutoRunSetting(Context context) {
        if (isOppo()) {
            openOppoAutoRunSetting(context);
        } else if (isH2OS()) {
            openH2OSAutoRunSetting(context);
        } else if (isMIUI()) {
            openMIUIAutoRunSetting(context);
        } else if (isSamsung()) {
            openSamsungAutoRunSetting(context);
        } else if (isHuawei()) {
            openHuaWeiAutoRunSetting(context);
        } else if (isVivo()) {
            openVIVOAutoRunSetting(context);
        } else {
            openDefaultAutoRunSetting(context);
        }
    }

}
