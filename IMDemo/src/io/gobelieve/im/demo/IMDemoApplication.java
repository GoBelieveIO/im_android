package io.gobelieve.im.demo;

import android.app.Application;
import android.provider.Settings;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.GroupMessageHandler;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.PeerMessageHandler;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.beetle.push.IMsgReceiver;
import com.beetle.push.Push;
import com.beetle.push.instance.SmartPushServiceProvider;


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

        IMService mIMService = IMService.getInstance();
        //app可以单独部署服务器，给予第三方应用更多的灵活性
        //sandbox地址:"sandbox.imnode.gobelieve.io", "sandbox.pushnode.gobelieve.io"
        //"http://sandbox.api.gobelieve.io",
        mIMService.setHost("imnode.gobelieve.io");
        IMHttpAPI.setAPIURL("http://api.gobelieve.io");
        SmartPushServiceProvider.setHost("pushnode.gobelieve.io");

        //设置推送服务的回调
        Push.registerReceiver(new IMsgReceiver() {
            @Override
            public void onDeviceToken(byte[] tokenArrary) {
                IMDemoApplication.this.mDeviceToken = tokenArrary;
            }
        });
        //启动后台推送服务
        Push.registerService(getApplicationContext());

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

    public static Application getApplication() {
        return sApplication;
    }
}
