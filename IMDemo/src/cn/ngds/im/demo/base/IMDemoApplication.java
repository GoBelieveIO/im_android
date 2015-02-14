package cn.ngds.im.demo.base;

import android.app.Application;
import android.util.Log;
import cn.ngds.im.demo.domain.UserHelper;
import com.gameservice.sdk.im.IMApi;
import com.gameservice.sdk.im.IMService;
import com.gameservice.sdk.push.v2.api.IMsgReceiver;
import com.gameservice.sdk.push.v2.api.SmartPush;
import com.gameservice.sdk.push.v2.api.SmartPushOpenUtils;

/**
 * IMDemoApplication
 * Description:
 */
public class IMDemoApplication extends Application {
    private static Application sApplication;

    private byte[] mDeviceToken;

    public byte[] getDeviceToken() {
        return mDeviceToken;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        startPushService();
    }

    public static Application getApplication() {
        return sApplication;
    }

    public static void logout() {
        UserHelper.INSTANCE.logout();
        IMService.getInstance().stop();
    }

    private void startPushService() {
        // 注册消息接受者
        SmartPush.registerReceiver(new IMsgReceiver() {
            @Override
            public void onMessage(String message) {
                // message为透传消息，需开发者在此处理
                Log.i("PUSH", "透传消息:" + message);
            }

            @Override
            public void onDeviceToken(byte[] tokenArray) {
                mDeviceToken = tokenArray;
            }
        });
        // 注册服务，并启动服务
        SmartPush.registerService(this);
    }


}
