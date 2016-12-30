package io.gobelieve.im.demo;

import android.app.Application;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.GroupMessageHandler;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.PeerMessageHandler;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.huawei.android.pushagent.api.PushManager;
import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushManager;
import com.xiaomi.mipush.sdk.MiPushClient;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;


/**
 * PushDemoApplication
 * Description:
 */
public class PushDemoApplication extends Application {
    private static PushDemoApplication sApplication;

    private String mXGPushToken = null;
    private String mXiaomiPushToken = null;
    private String mHuaweiPushToken = null;
    private boolean mIsLogin = false;
    private boolean mIsBind = false;

    private final int USE_XG = 1;
    private final int USE_XM = 0;
    private final int USE_HW = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;

        IMService mIMService = IMService.getInstance();
        //app可以单独部署服务器，给予第三方应用更多的灵活性
        //sandbox地址:"sandbox.imnode.gobelieve.io", "sandbox.pushnode.gobelieve.io"
        //"http://sandbox.api.gobelieve.io",
        mIMService.setHost("imnode.gobelieve.io");
        IMHttpAPI.setAPIURL("http://api.gobelieve.io");
        initPush();
    }

    private void initPush() {
        if (USE_HW != 0) {
            initHuaweiPush();
        } else if (USE_XM != 0) {
            initXiaomiPush();
        } else if (USE_XG != 0) {
            initXGPush();
        }
    }

    public static PushDemoApplication getApplication() {
        return sApplication;
    }

    private boolean isXiaomiDevice() {
        String os = Build.HOST;
        return !TextUtils.isEmpty(os) && os.toLowerCase().contains("miui");
    }

    private void initXGPush() {
        XGIOperateCallback callback = new XGIOperateCallback() {
            @Override
            public void onSuccess(Object data, int i) {
                Log.d("TPush", "注册成功，设备token为：" + data);
                PushDemoApplication.this.setXGPushToken((String)data);
            }

            @Override
            public void onFail(Object data, int errCode, String msg) {
                Log.d("TPush", "注册失败，错误码：" + errCode + ",错误信息：" + msg);
            }

        };
        //接入信鸽推送
        XGPushManager.registerPush(getApplicationContext(), callback);
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

    private boolean isHuaweiDevice() {
        String os = Build.HOST;
        return !TextUtils.isEmpty(os) && os.toLowerCase().contains("huawei");
    }

    private void initHuaweiPush() {
        PushManager.requestToken(this);
    }

    public void setHuaweiPushToken(String token) {
        this.mHuaweiPushToken = token;
        if (!TextUtils.isEmpty(mHuaweiPushToken) && mIsLogin && !mIsBind) {
            // 已登录尚未绑定时
            bindWithHuawei();
        }
    }

    public void setXGPushToken(String token) {
        this.mXGPushToken = token;
        if (!TextUtils.isEmpty(mXGPushToken) && mIsLogin && !mIsBind) {
            // 已登录尚未绑定时
            bindWithXG();
        }
    }

    public void bindDeviceTokenToIM() {
        mIsLogin = true;

        if (!TextUtils.isEmpty(mHuaweiPushToken)) {
            bindWithHuawei();
        }
        // 小米情况同华为
        if (!TextUtils.isEmpty(mXiaomiPushToken)) {
            bindWithXiaomi();
        }
        if (!TextUtils.isEmpty(mXGPushToken)) {
            bindWithXG();
        }
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

    private void bindWithXG() {
        PostDeviceToken postDeviceToken = new PostDeviceToken();
        postDeviceToken.xgDeviceToken = mXGPushToken;
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
}
