/**
 * @author 慕容秋 (muroqiu@qq.com)
 *         Create on 14-9-12
 *
 *         消息通知栏处理
 */
package com.beetle.push.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;

import com.beetle.push.core.log.PushLog;
import com.beetle.push.core.util.Utils;
import com.beetle.push.type.PushInfo;

public class SmartPushNotification {
    private final static String TAG = "SmartPushNotification";
    private Context mContext;
    private int id;

    public SmartPushNotification(Context ctx) {
        mContext = ctx;
        id = ("pushsdk" + System.currentTimeMillis()).hashCode();
    }

    /**
     * 显示消息通知栏
     *
     * @param pushInfo
     */
    public void showNotification(PushInfo pushInfo) {
        PushLog.d(TAG, "in show notification");
        if (pushInfo == null) {
            return;
        }

        Intent intent = null;
        String packageName;
        switch (pushInfo.getType()) {
            case PushInfo.PUSH_TYPE_APP_WITHOUT_PACKAGENAME:
                packageName = pushInfo.getPackageName();
                if (TextUtils.isEmpty(packageName)) {
                    packageName = mContext.getPackageName();
                }
                // 获取启动App的Intent
                intent = getIntentOfStartApp(pushInfo, packageName);
                break;
            case PushInfo.PUSH_TYPE_ACTIVITY_WITHOUT_PACKAGENAME:
                packageName = pushInfo.getPackageName();
                if (TextUtils.isEmpty(packageName)) {
                    packageName = mContext.getPackageName();
                }
                // 获取启动App指定页面的Intent
                intent = getIntentOfStartActivity(pushInfo, packageName);
                break;
            case PushInfo.PUSH_TYPE_WEB:
                break;
            case PushInfo.PUSH_TYPE_DOWNLOAD:
                // 跳转调下载分支
                // 先不做下载了  20140915
                //                appDownload(pushInfo);
                return;
            case PushInfo.PUSH_TYPE_DELIVER:
                // 跳转调下载分支
                return;

            case PushInfo.PUSH_TYPE_APP:
                packageName = pushInfo.getPackageName();
                if (TextUtils.isEmpty(packageName)) {
                    packageName = mContext.getPackageName();
                }
                //后门可以打开其他应用
                intent = getIntentOfStartApp(pushInfo, packageName);
                break;

            case PushInfo.PUSH_TYPE_ACTIVITY:
                packageName = pushInfo.getPackageName();
                if (TextUtils.isEmpty(packageName)) {
                    packageName = mContext.getPackageName();
                }
                //后门可以打开其他应用指定Activity
                intent = getIntentOfStartActivity(pushInfo, packageName);
                break;

            default:
                return;
        }

        PendingIntent pendingIntent = null;
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(mContext, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            PushLog.d(TAG, "intent is null ");
        }
        Notification notification = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Notification.Builder ntBuilder = new Notification.Builder(mContext)
                .setContentTitle(pushInfo.getTitle())
                .setContentText(pushInfo.getContent())
                .setAutoCancel(true)
                .setOngoing(!pushInfo.isClearable())
                .setTicker(pushInfo.getTitle() + ":" + pushInfo.getContent())
                .setContentIntent(pendingIntent)
                .setLargeIcon(((BitmapDrawable) Utils.getAppIconDrable(mContext)).getBitmap());

            notification = ntBuilder.getNotification();
        } else {
            notification = new Notification();
            notification.flags |= Notification.FLAG_AUTO_CANCEL;
            if (!pushInfo.isClearable()) {
                notification.flags |= notification.flags
                    | Notification.FLAG_ONGOING_EVENT;
            }
            if (null == pendingIntent) {
                pendingIntent = PendingIntent.getActivity(mContext, 0,
                    new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            }
            notification.setLatestEventInfo(mContext, pushInfo.getTitle(), pushInfo.getContent(),
                pendingIntent);
        }

        if (pushInfo.isRing()) {
            notification.defaults |= Notification.DEFAULT_SOUND;
        }
        if (pushInfo.isVibrate()) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        // small icon
        int statDrawable = Utils.getAppIcon(mContext);
        notification.icon = statDrawable;

        NotificationManager manager = (NotificationManager) mContext
            .getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(id, notification);
    }


    /**
     * 获取启动App的Intent
     *
     * @param pushInfo
     */
    private Intent getIntentOfStartApp(PushInfo pushInfo, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            PushLog.e(TAG, "packageName is null");
            return null;
        }
        PackageManager localPackageManager = mContext.getPackageManager();
        Intent localIntent =
            localPackageManager.getLaunchIntentForPackage(packageName);
        if (localIntent != null) {
            localIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            if (pushInfo.getOptionParams() != null) {
                try {
                    Bundle extras = Utils.parseJSON2Bundle(pushInfo.getOptionParams());
                    localIntent.putExtras(extras);
                } catch (Exception e) {
                    // 服务端返回"{}"，不用处理
                }
            }
        } else {
            PushLog.e(TAG, "incorrect notification packageName is:" + packageName);
        }

        return localIntent;

    }

    /**
     * 获取启动App特定Activity的Intent
     *
     * @param pushInfo
     * @return
     */
    private Intent getIntentOfStartActivity(PushInfo pushInfo, String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return null;
        }
        ComponentName componetName = new ComponentName(
            // 这个是另外一个应用程序的包名
            packageName,
            // 这个参数是要启动的Activity
            pushInfo.getActivity());
        try {
            Intent intent = new Intent();
            //            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.setComponent(componetName);

            if (pushInfo.getOptionParams() != null) {
                try {
                    Bundle extras = Utils.parseJSON2Bundle(pushInfo.getOptionParams());
                    intent.putExtras(extras);
                } catch (Exception e) {
                    // 服务端返回"{}"，不用处理
                }
            }
            return intent;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
