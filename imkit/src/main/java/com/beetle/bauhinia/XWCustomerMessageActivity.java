package com.beetle.bauhinia;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.beetle.im.IMService;

import java.util.List;

/**
 * Created by houxh on 16/10/23.
 */

public class XWCustomerMessageActivity extends CustomerMessageActivity implements Application.ActivityLifecycleCallbacks {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        IMService.getInstance().start();
        getApplication().registerActivityLifecycleCallbacks(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMService.getInstance().stop();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

    }
    @Override
    public void onActivityStarted(Activity activity) {

    }
    public void onActivityResumed(Activity activity) {
        IMService.getInstance().enterForeground();
    }

    public void onActivityPaused(Activity activity) {

    }
    public void onActivityStopped(Activity activity) {
        if (!isAppOnForeground()) {
            IMService.getInstance().enterBackground();
        }
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }
    public void onActivityDestroyed(Activity activity) {

    }

    /**
     * 程序是否在前台运行
     *
     * @return
     */
    public boolean isAppOnForeground() {
        // Returns a list of application processes that are running on the
        // device

        ActivityManager activityManager =
                (ActivityManager) getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        String packageName = getApplicationContext().getPackageName();

        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        if (appProcesses == null)
            return false;

        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            // The name of the process that this object is associated with.
            if (appProcess.processName.equals(packageName)
                    && appProcess.importance
                    == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }

        return false;
    }
}
