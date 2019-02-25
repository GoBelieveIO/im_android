package io.gobelieve.im.demo;

import android.app.ActivityManager;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.GroupMessageHandler;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.PeerMessageHandler;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.client.PendingResult;
import com.huawei.hms.support.api.client.ResultCallback;
import com.huawei.hms.support.api.push.HuaweiPush;
import com.huawei.hms.support.api.push.TokenResult;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * PushDemoApplication
 * Description:
 */
public class PushDemoApplication extends Application implements HuaweiApiClient.ConnectionCallbacks, HuaweiApiClient.OnConnectionFailedListener {
    private static PushDemoApplication sApplication;

    private static final String TAG = "pushdemo";

    private String mXiaomiPushToken = null;
    private String mHuaweiPushToken = null;
    private boolean mIsLogin = false;
    private boolean mIsBind = false;

    //华为移动服务Client
    private HuaweiApiClient client;
    private UpdateUIBroadcastReceiver broadcastReceiver;


    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;

        if (!isAppProcess()) {
            Log.i(TAG, "service application create");
            return;
        }

        IMService mIMService = IMService.getInstance();
        //app可以单独部署服务器，给予第三方应用更多的灵活性
        //sandbox地址:"sandbox.imnode.gobelieve.io", "sandbox.pushnode.gobelieve.io"
        //"http://sandbox.api.gobelieve.io",
        mIMService.setHost("imnode.gobelieve.io");
        IMHttpAPI.setAPIURL("http://api.gobelieve.io");
        initPush();

        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        //设置设备唯一标识,用于多点登录时设备校验
        mIMService.setDeviceID(androidID);

        //监听网路状态变更
        mIMService.registerConnectivityChangeReceiver(getApplicationContext());

        //可以在登录成功后，设置每个用户不同的消息存储目录
        FileCache fc = FileCache.getInstance();
        fc.setDir(this.getDir("cache", MODE_PRIVATE));
        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));
        GroupMessageDB groupDB = GroupMessageDB.getInstance();
        groupDB.setDir(this.getDir("group", MODE_PRIVATE));

        mIMService.setPeerMessageHandler(PeerMessageHandler.getInstance());
        mIMService.setGroupMessageHandler(GroupMessageHandler.getInstance());
    }

    private void initPush() {
        // 华为设备启动华为push，其他设备启动小米push
        if (RomUtils.isHuaweiRom()) {
            initHuaweiPush();
        } else {
            initXiaomiPush();
        }
    }

    public static PushDemoApplication getApplication() {
        return sApplication;
    }



    private void initXiaomiPush() {
        // 注册push服务，注册成功后会向XiaomiPushReceiver发送广播
        // 可以从onCommandResult方法中MiPushCommandMessage对象参数中获取注册信息
        String appId = "2882303761517422920";
        String appKey = "5111742288920";
        MiPushClient.registerPush(this, appId, appKey);
    }

    public void setXiaomiPushToken(String token) {
        this.mXiaomiPushToken = token;
        if (!TextUtils.isEmpty(mXiaomiPushToken) && mIsLogin && !mIsBind) {
            // 已登录尚未绑定时
            bindWithXiaomi();
        }
    }


    private void initHuaweiPush() {

        //创建华为移动服务client实例用以使用华为push服务
        //需要指定api为HuaweiId.PUSH_API
        //连接回调以及连接失败监听
        client = new HuaweiApiClient.Builder(this)
                .addApi(HuaweiPush.PUSH_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        //建议在oncreate的时候连接华为移动服务
        //业务可以根据自己业务的形态来确定client的连接和断开的时机，但是确保connect和disconnect必须成对出现
        client.connect();

        registerBroadcast();
    }

    public void setHuaweiPushToken(String token) {
        this.mHuaweiPushToken = token;
        if (!TextUtils.isEmpty(mHuaweiPushToken) && mIsLogin && !mIsBind) {
            // 已登录尚未绑定时
            bindWithHuawei();
        }
    }

    public void bindDeviceTokenToIM() {
        mIsLogin = true;
        if (RomUtils.isHuaweiRom()) {
            // 由于华为推送的token是通过回调返回，此时可能还未获取，需等HuaweiPushReceiver执行setHuaweiPushToken
            if (!TextUtils.isEmpty(mHuaweiPushToken)) {
                bindWithHuawei();
            }
        } else {
            // 小米情况同华为
            if (!TextUtils.isEmpty(mXiaomiPushToken)) {
                bindWithXiaomi();
            }
        }
    }

    /**
     * 使用异步接口来获取pushtoken
     * 结果通过广播的方式发送给应用，不通过标准接口的pendingResul返回
     * CP可以自行处理获取到token
     * 同步获取token和异步获取token的方法CP只要根据自身需要选取一种方式即可
     */
    private void getTokenAsyn() {
        if(!client.isConnected()) {
            Log.i(TAG, "获取token失败，原因：HuaweiApiClient未连接");
            client.connect();
            return;
        }

        Log.i(TAG, "异步接口获取push token");
        PendingResult<TokenResult> tokenResult = HuaweiPush.HuaweiPushApi.getToken(client);
        tokenResult.setResultCallback(new ResultCallback<TokenResult>() {

            @Override
            public void onResult(TokenResult result) {
                //这边的结果只表明接口调用成功，是否能收到响应结果只在广播中接收
            }
        });
    }

    private void bindWithHuawei() {
        PostDeviceToken postDeviceToken = new PostDeviceToken();
        postDeviceToken.hwDeviceToken = mHuaweiPushToken;
        bindDeviceTokenToIM(postDeviceToken);
    }

    private void bindWithXiaomi() {
        PostDeviceToken postDeviceToken = new PostDeviceToken();
        postDeviceToken.xmDeviceToken = mXiaomiPushToken;
        bindDeviceTokenToIM(postDeviceToken);
    }

    private void bindDeviceTokenToIM(PostDeviceToken postDeviceToken) {
        IMHttpAPI.Singleton().bindDeviceToken(postDeviceToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object obj) {
                        Log.i("im", "bind success");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i("im", "bind fail");
                    }
                });
    }



    private boolean isAppProcess() {
        Context context = getApplicationContext();
        int pid = android.os.Process.myPid();
        Log.i(TAG, "pid:" + pid + "package name:" + context.getPackageName());
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.i(TAG, "package name:" + appProcess.processName + " importance:" + appProcess.importance + " pid:" + appProcess.pid);
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


    @Override
    public void onConnected() {
        //华为移动服务client连接成功，在这边处理业务自己的事件
        Log.i(TAG, "HuaweiApiClient 连接成功");

        getTokenAsyn();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        //HuaweiApiClient断开连接的时候，业务可以处理自己的事件
        Log.i(TAG, "HuaweiApiClient 连接断开");
        client.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult arg0) {
        Log.i(TAG, "HuaweiApiClient连接失败，错误码：" + arg0.getErrorCode());
        if(HuaweiApiAvailability.getInstance().isUserResolvableError(arg0.getErrorCode())) {
            final int errorCode = arg0.getErrorCode();
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    // 此方法必须在主线程调用
                    //HuaweiApiAvailability.getInstance().resolveError(HuaweiPushActivity.this, errorCode, REQUEST_HMS_RESOLVE_ERROR);
                }
            });
        } else {
            //其他错误码请参见开发指南或者API文档
        }
    }



    private void registerBroadcast() {
        String ACTION_UPDATEUI = "action.updateUI";

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATEUI);
        broadcastReceiver = new UpdateUIBroadcastReceiver();
        registerReceiver(broadcastReceiver, filter);
    }

    /**
     * 定义广播接收器（内部类）
     */
    private class UpdateUIBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getExtras().getInt("type");
            if(type == 1) {
                String token = intent.getExtras().getString("token");
                PushDemoApplication.this.setHuaweiPushToken(token);
            } else if (type == 2) {
                boolean status = intent.getExtras().getBoolean("pushState");
                Log.i(TAG, "连接状态:" + status);
            }
        }

    }

}
