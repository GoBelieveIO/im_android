package com.beetle.push.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class NetWorkUtil {
    public static String TAG = "NetWorkUtil";
    private static volatile int sLastNetType = -1;
    private static volatile NetworkInfo.State sLastNetState = NetworkInfo.State.UNKNOWN;
    private static volatile String sLastWifiAP_SSID = null;
    private static volatile String sLastWifiAP_MAC = null;

    static volatile boolean sIsNetWorkChanged = false;
    private static boolean isFirstIn = true;

    public static void checkNetwork(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();

        String apMac = wifiInfo.getBSSID();
        String apSSID = wifiInfo.getSSID();

        NetworkInfo.State sCurrenNetState;
        int sCurrenNetType;
        if (null != networkInfo) {
            sCurrenNetState = networkInfo.getState();
            sCurrenNetType = networkInfo.getType();
        } else {
            sCurrenNetState = NetworkInfo.State.UNKNOWN;
            sCurrenNetType = -1;
        }

        if (isFirstIn) {
            sLastNetState = sCurrenNetState;
            sLastNetType = sCurrenNetType;
            sLastWifiAP_MAC = apMac;
            sLastWifiAP_SSID = apSSID;
            isFirstIn = false;
            return;
        }
        if (sCurrenNetState.equals(sLastNetState) && sCurrenNetType == sLastNetType) {
            switch (sCurrenNetType) {
                case ConnectivityManager.TYPE_WIFI:
                    if (apMac.equalsIgnoreCase(sLastWifiAP_MAC) && apSSID.equals(sLastWifiAP_SSID)) {
                        sIsNetWorkChanged = false;
                    }
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    sIsNetWorkChanged = false;
                    break;
                default:
                    sIsNetWorkChanged = true;
                    break;
            }
        } else {
            sIsNetWorkChanged = true;
        }
        sLastNetState = sCurrenNetState;
        sLastNetType = sCurrenNetType;
        sLastWifiAP_MAC = apMac;
        sLastWifiAP_SSID = apSSID;
    }

    public static boolean isNetWorkChanged() {
        return sIsNetWorkChanged;
    }
}
