package io.gobelieve.im.demo;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.im.IMService;


/**
 * IMDemoApplication
 * Description:
 */
public class IMDemoApplication extends Application {
    private static final String TAG = "gobelieve";

    @Override
    public void onCreate() {
        super.onCreate();

        IMService mIMService = IMService.getInstance();
        //app可以单独部署服务器，给予第三方应用更多的灵活性
        mIMService.setHost("imnode2.gobelieve.io");
        IMHttpAPI.setAPIURL("http://api.gobelieve.io");

        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        //设置设备唯一标识,用于多点登录时设备校验
        mIMService.setDeviceID(androidID);

        //监听网路状态变更
        registerConnectivityChangeReceiver(getApplicationContext());
        IMService.getInstance().setReachable(isOnNet(getApplicationContext()));
    }


    private boolean isOnNet(Context context) {
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
            Log.i(TAG, "active net info:" + activeNetInfo);
        }
        return isOnNet;
    }

    class NetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive (Context context, Intent intent) {
            boolean reachable = isOnNet(context);
            IMService.getInstance().onNetworkConnectivityChange(reachable);
        }
    };

    public void registerConnectivityChangeReceiver(Context context) {
        NetworkReceiver  receiver = new NetworkReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        context.registerReceiver(receiver, filter);
    }


}
