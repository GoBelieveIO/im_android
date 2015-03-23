package com.beetle.bauhinia.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;

import com.beetle.im.IMService;
import com.beetle.imkit.R;

import java.util.List;

/**
 * Created by tsung on 12/10/14.
 */
public class BaseActivity extends ActionBarActivity {
    protected ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IMService.getInstance().enterForeground();

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (canBack()) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                actionBar.setDisplayHomeAsUpEnabled(false);
            }
            actionBar.show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!isAppOnForeground()) {
            //app 进入后台,停止IMService,采用push机制接收离线消息
            Log.i("im", "app enter background");
            IMService.getInstance().enterBackground();
        }
    }

    public boolean canBack() {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (canBack()) {
                    onBackPressed();
                    return true;
                }
        }
        return false;
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
