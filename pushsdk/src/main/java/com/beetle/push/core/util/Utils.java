package com.beetle.push.core.util;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Proxy;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * 工具类
 * 业务相关参数获取工具类
 *
 * @author 曾广贤 (muroqiu@qq.com)
 */
public class Utils {
    private static final String META_APPID = "GOBELIEVE_APPID";
    private static final String META_APPKEY = "GOBELIEVE_APPKEY";


    /**
     * 获取设备号
     *
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        // 直接从配置文件读取
        SharedPreferences settings = context
            .getSharedPreferences("deviceId", 0);
        String deviceId = settings.getString("deviceId", null);
        if (TextUtils.isEmpty(deviceId)) {
            try {
                TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
                deviceId = tm.getDeviceId();
            } catch (Exception e) {

            }
            if (TextUtils.isEmpty(deviceId)) {
                // 有些机器有可能取不到设备号用GUID表示
                deviceId = UUID.randomUUID().toString().replace("-", "");
            }
            // 保存到配置文件(这样可以保证每次都一样)
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("deviceId", deviceId);
            editor.commit();
        }

        return deviceId;
    }



    /**
     * 读取AppID
     *
     * @param ctx
     * @return
     */
    public static String loadAppId(Context ctx) {
        String appId = "";
        ApplicationInfo appInfo = null;
        try {
            appInfo = ctx.getPackageManager()
                .getApplicationInfo(ctx.getPackageName(),
                    PackageManager.GET_META_DATA);
            appId = appInfo.metaData.getInt(META_APPID) + "";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return appId;
    }

    /**
     * 读取AppKey
     *
     * @param ctx
     * @return
     */
    public static String loadAppKey(Context ctx) {
        String appSecret = "";
        ApplicationInfo appInfo = null;
        try {
            appInfo = ctx.getPackageManager()
                .getApplicationInfo(ctx.getPackageName(),
                    PackageManager.GET_META_DATA);
            appSecret = appInfo.metaData.getString(META_APPKEY);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return appSecret;
    }


    /**
     * 获取应用程序Icon资源ID
     *
     * @param ctx
     * @return
     */
    public static int getAppIcon(Context ctx) {
        int resId = 0;
        ApplicationInfo appInfo = null;
        try {
            appInfo = ctx.getPackageManager()
                .getApplicationInfo(ctx.getPackageName(),
                    PackageManager.GET_ACTIVITIES);
            resId = appInfo.icon;
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (resId == 0) {
            resId = android.R.drawable.sym_def_app_icon;
        }

        return resId;
    }

    /**
     * 获取应用程序的Icon资源
     *
     * @param ctx
     * @return
     */
    public static Drawable getAppIconDrable(Context ctx) {
        Drawable drawable = null;
        try {
            drawable = ctx.getPackageManager().getApplicationIcon(ctx.getPackageName());
        } catch (Exception e) {
            drawable = ctx.getResources().getDrawable(android.R.drawable.sym_def_app_icon);
            e.printStackTrace();
        }

        return drawable;
    }

    /**
     * 获取本地下载存储地址
     *
     * @return
     */
    public static String getDownloadPath(Context ctx) {
        String result = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
            + "Downloads" + File.separator + ctx.getPackageName() + File.separator;
        return result;
    }

    /**
     * 将json对象转换成Bundle
     *
     * @param json
     * @return
     * @throws org.json.JSONException
     */
    public static Bundle parseJSON2Bundle(JSONObject json) {
        Bundle map = new Bundle();

        JSONArray ja = json.names();
        try {
            for (int i = 0; i < ja.length(); i++) {
                String k = ja.getString(i);

                String v = json.getString(k);
                map.putString(k, v);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }

    public static int dp2pixls(int dp) {
        int unit = TypedValue.COMPLEX_UNIT_DIP;
        Resources r = Resources.getSystem();

        return (int) (TypedValue.applyDimension(unit, dp, r.getDisplayMetrics()) + 0.5f);
    }


    public static boolean isOnWifi(Context context) {
        if (null == context) {
            Log.e("", "context is null");
            return false;
        }
        boolean isOnWifi = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (null != activeNetInfo
            && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            isOnWifi = true;
        }
        return isOnWifi;
    }

    public static boolean isOnNet(Context context) {
        if (null == context) {
            Log.e("", "context is null");
            return false;
        }
        boolean isOnNet = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (null != activeNetInfo) {
            isOnNet = activeNetInfo.isConnected();
        }
        return isOnNet;
    }


    public static boolean isOnAppProcess(Context context) {
        Context appContext = context.getApplicationContext();
        int pid = android.os.Process.myPid();
        ActivityManager activityManager =
            (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
            activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (pid == appProcess.pid) {
                if (appProcess.processName.equals(context.getPackageName())) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * 获取当前应用名称
     *
     * @param context
     * @return
     */
    public static String getCurrentApplicationName(Context context) {
        if (!validContext(context)) {
            return null;
        }
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        packageManager = context.getPackageManager();
        if (null == packageManager) {
            return null;
        }
        try {
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;

        }
    }



    /**
     * 获取当前版本号
     *
     * @param context
     * @return
     */
    public static String getCurrentVersionName(Context context) {
        if (!validContext(context)) {
            return null;
        }
        PackageManager packageManager = context.getPackageManager();
        if (null == packageManager) {
            return null;
        }
        // getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 上下文有效性判断
     *
     * @param context
     * @return false if not valid
     */
    public static boolean validContext(Context context) {
        if (null == context) {
            Log.e("", "Context is null");
            return false;
        }
        return true;
    }


    public enum NetWorkType {
        INVALID("invalid"), WAP("wap"), SECOND_GENERATION("2g"), THIRD_GENERATION("3g"), WIFI(
            "wifi");
        private String description;

        NetWorkType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取网络状态
     *
     * @param context 上下文
     * @return int 网络状态 0 invalid, 1 wap,2 2g,3 3g,4 wifi.
     */

    public static int getNetWorkType(Context context) {
        NetWorkType netWorkType = NetWorkType.INVALID;
        ConnectivityManager manager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            String type = networkInfo.getTypeName();

            if (type.equalsIgnoreCase("WIFI")) {
                netWorkType = NetWorkType.WIFI;
            } else if (type.equalsIgnoreCase("MOBILE")) {
                String proxyHost = Proxy.getDefaultHost();

                netWorkType = TextUtils.isEmpty(proxyHost)
                    ?
                    (isFastMobileNetwork(context) ?
                        NetWorkType.THIRD_GENERATION :
                        NetWorkType.SECOND_GENERATION)
                    :
                    NetWorkType.WAP;
            }
        }

        return netWorkType.ordinal();
    }

    /**
     * 获取网络状态文字描述
     * @param context
     * @return
     */
    public static String getNetWorkTypeDes(Context context) {
        int netWorkType = getNetWorkType(context);
        String networkTypeDes = null;
        try {
            networkTypeDes = NetWorkType.values()[netWorkType].getDescription();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return networkTypeDes;
    }

    public static boolean isFastMobileNetwork(Context context) {
        TelephonyManager telephonyManager =
            (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        switch (telephonyManager.getNetworkType()) {
            case TelephonyManager.NETWORK_TYPE_1xRTT:
                return false; // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_CDMA:
                return false; // ~ 14-64 kbps
            case TelephonyManager.NETWORK_TYPE_EDGE:
                return false; // ~ 50-100 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
                return true; // ~ 400-1000 kbps
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
                return true; // ~ 600-1400 kbps
            case TelephonyManager.NETWORK_TYPE_GPRS:
                return false; // ~ 100 kbps
            case TelephonyManager.NETWORK_TYPE_HSDPA:
                return true; // ~ 2-14 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPA:
                return true; // ~ 700-1700 kbps
            case TelephonyManager.NETWORK_TYPE_HSUPA:
                return true; // ~ 1-23 Mbps
            case TelephonyManager.NETWORK_TYPE_UMTS:
                return true; // ~ 400-7000 kbps
            case TelephonyManager.NETWORK_TYPE_EHRPD:
                return true; // ~ 1-2 Mbps
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
                return true; // ~ 5 Mbps
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return true; // ~ 10-20 Mbps
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return false; // ~25 kbps
            case TelephonyManager.NETWORK_TYPE_LTE:
                return true; // ~ 10+ Mbps
            case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                return false;
            default:
                return false;
        }
    }

}
