package com.beetle.bauhinia;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.im.IMService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by houxh on 16/10/23.
 */

public class XWCustomerMessageActivity extends CustomerMessageActivity implements Application.ActivityLifecycleCallbacks {

    private String token;
    private Handler mainHandler;

    private long lastSellerID;//上一次会话的客服号
    private String name;//客服昵称
    private String avatar;//客服头像
    private String status;//客服状态 online/offline
    private int gotTimestamp = 0;//一个小时后重新获取新的客服

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getApplication().registerActivityLifecycleCallbacks(this);

        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        if (TextUtils.isEmpty(token) || storeID == 0) {
            Log.i(TAG, "invalid token/storeID");
            return;
        }

        IMService.getInstance().start();
        mainHandler = new Handler(Looper.getMainLooper());

        this.loadSupporter();
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {
        if (state == IMService.ConnectState.STATE_CONNECTED && this.sellerID > 0) {
            enableSend();
        } else {
            disableSend();
        }
        setSubtitle();
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    private void getSupporter() {

        Log.i(TAG, "get supporter...");
        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... urls) {
                //重试5次
                for (int i = 0; i < 5; i++) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = newGetSupporterRequest(storeID);
                    try {
                        Response resp = client.newCall(request).execute();
                        String r = resp.body().string();
                        return r;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                return "";
            }

            @Override
            protected void onPostExecute(String result) {
                if (TextUtils.isEmpty(result)) {
                    Toast.makeText(XWCustomerMessageActivity.this, "无法请求客服服务，请检查你的网络", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JSONObject obj = new JSONObject(result);
                    JSONObject data = obj.getJSONObject("data");
                    final long sellerID = data.getLong("seller_id");
                    final String name = data.getString("name");
                    final String avatar = data.optString("avatar");
                    final String status = data.getString("status");

                    boolean newSeller = (sellerID != XWCustomerMessageActivity.this.lastSellerID);

                    XWCustomerMessageActivity.this.name = name;
                    XWCustomerMessageActivity.this.avatar = avatar;
                    XWCustomerMessageActivity.this.status = status;
                    XWCustomerMessageActivity.this.gotTimestamp = XWCustomerMessageActivity.this.now();
                    XWCustomerMessageActivity.this.sellerID = sellerID;
                    XWCustomerMessageActivity.this.saveSupporter();


                    Log.i(TAG, "get supporter:" + sellerID + " name:" + name);

                    if (newSeller) {
                        if (IMService.getInstance().getConnectState() ==  IMService.ConnectState.STATE_CONNECTED) {
                            XWCustomerMessageActivity.this.enableSend();
                        }

                        ICustomerMessage msg = new ICustomerMessage();
                        msg.customerID = currentUID;
                        msg.customerAppID = appID;
                        msg.storeID = storeID;
                        msg.sellerID = sellerID;

                        msg.timestamp = now();
                        msg.sender = storeID;
                        msg.receiver = currentUID;

                        msg.isSupport = true;
                        msg.isOutgoing = false;

                        String t = String.format("%s为您服务", name);
                        msg.setContent(IMessage.newHeadline(t));
                        saveMessage(msg);
                        insertMessage(msg);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();

                    Toast.makeText(XWCustomerMessageActivity.this, "请求客服服务失败", Toast.LENGTH_SHORT).show();
                }



            }
        }.execute();

    }

    private static String URL = "http://api.gobelieve.io";

    private Request newGetSupporterRequest(long storeID) {
        String url = URL + "/supporters?store_id=" + storeID;
        String auth = String.format("Bearer %s", this.token);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", auth)
                .get()
                .build();

        return request;
    }

    private void loadSupporter() {
        SharedPreferences supporter = getSharedPreferences("supporter", Context.MODE_PRIVATE);
        long storeID = supporter.getLong("store_id", 0);
        long sellerID = supporter.getLong("seller_id", 0);
        String name = supporter.getString("name", "");
        String avatar = supporter.getString("avatar", "");
        String status = supporter.getString("status", "");
        int ts = supporter.getInt("timestamp", 0);

        if (storeID != this.storeID) {
            return;
        }

        this.sellerID = sellerID;
        this.name = name;
        this.avatar = avatar;
        this.status = status;
        this.gotTimestamp = ts;
    }

    private void saveSupporter() {
        SharedPreferences pref = getSharedPreferences("supporter", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putLong("store_id", this.storeID);
        editor.putLong("seller_id", this.sellerID);
        editor.putString("name", (this.name != null ? this.name : ""));
        editor.putString("avatar", this.avatar != null ? this.avatar : "");
        editor.putString("status", this.status != null ? this.status : "");
        editor.putInt("timestamp", this.gotTimestamp);
        editor.commit();
    }

    @Override
    protected void onStart() {
        super.onStart();

        int now = now();
        //当前没有客服或者距离上次获取客服的时间超过1小时
        if (this.sellerID == 0 || (now - this.gotTimestamp) > 3600) {
            this.lastSellerID = this.sellerID;
            this.sellerID = 0;
            getSupporter();
            disableSend();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMService.getInstance().stop();
        getApplication().unregisterActivityLifecycleCallbacks(this);
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
