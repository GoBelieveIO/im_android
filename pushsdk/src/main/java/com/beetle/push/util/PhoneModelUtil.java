package com.beetle.push.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.beetle.push.core.util.SharePreferenceHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * PhoneModelUtil
 * Description: 判断手机型号,提供相关帮助信息窗口
 */
public class PhoneModelUtil {

    private static final String TAG = "PhoneModelUtil";



    private static final String MI_TAG = "Xiaomi";
    private static final String MI_RED_TAG = "NUBIA";
    private static final String VIVO_TAG = "vivo";
    private static final String MEIZU_TAG = "Meizu";
    private static List<String> TIP_MODEL_LIST = new ArrayList<String>();

    //存储版本,如果需要reset已发布出去客户端sdk的配置时可提高该版本号
    private static final int VERSION = 2;


    private static final String FILE_NAME = "ngds_phone_model_" + VERSION;
    private static final String TIP_IS_SHOWN = "tip_is_shown";

    static {
        TIP_MODEL_LIST.add(VIVO_TAG);
        TIP_MODEL_LIST.add(MI_TAG);
        TIP_MODEL_LIST.add(MI_RED_TAG);
        TIP_MODEL_LIST.add(MEIZU_TAG);
    }

    public static String getPhoneModel() {
        return Build.MANUFACTURER;
    }

    private static final String TIP =
        "当前操作环境可能会产生游戏内容通知无法显示或者无法及时收到的现象,如果想获取更好信息体验,请在rom相关配置中允许弹出通知以及自启动";



    /**
     * 判断是否是满足需要弹出条件的机型
     *
     * @return
     */
    public static boolean matchTargetModel() {
        String phoneModel = getPhoneModel();
        for (String model : TIP_MODEL_LIST) {
            if (phoneModel.equalsIgnoreCase(model)) {
                return true;
            }
        }
        return false;
    }

    public static void showGuideOnce(final Activity activity) {
        if (isNeedShowSettingStartTip(activity)) {
            final Context context = activity.getApplicationContext();
            changeTipIsShown(context);
            new AlertDialog.Builder(activity).setTitle("提示").setIcon(
                android.R.drawable.ic_dialog_info)
                .setMessage(TIP)
                .setPositiveButton(
                    "确定",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                activity.startActivityForResult(
                                    new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + context.getPackageName())), 0);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).setNegativeButton("不再显示", null).show();
        }
    }

    public static void showGuide(final Activity activity) {
        final Context context = activity.getApplicationContext();
        new AlertDialog.Builder(activity).setTitle("提示").setIcon(
            android.R.drawable.ic_dialog_info)
            .setMessage(TIP)
            .setPositiveButton(
                "确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            activity.startActivityForResult(
                                new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:" + context.getPackageName())), 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).setNegativeButton("返回", null).show();
    }

    private static void changeTipIsShown(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, 0);
        SharePreferenceHelper.save(sharedPreferences, TIP_IS_SHOWN, true);
    }

    private static boolean isNeedShowSettingStartTip(Context context) {
        boolean isTargetModel = matchTargetModel();
        SharedPreferences sharedPreferences = context.getSharedPreferences(FILE_NAME, 0);
        boolean tipIsShown = sharedPreferences.getBoolean(TIP_IS_SHOWN, false);
        return isTargetModel && !tipIsShown;
    }
}
