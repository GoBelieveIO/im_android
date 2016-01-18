package io.gobelieve.im.demo;

import android.app.Application;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.GroupMessageHandler;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.PeerMessageHandler;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IMDemoApplication
 * Description:
 */
public class IMDemoApplication extends Application {
    private static Application sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;

        IMService mIMService = IMService.getInstance();
        //app可以单独部署服务器，给予第三方应用更多的灵活性
        mIMService.setHost("imnode.gobelieve.io");
        IMHttpAPI.setAPIURL("http://api.gobelieve.io");

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

        //预先做dns查询
        refreshHost();
    }

    private void refreshHost() {
        new AsyncTask<Void, Integer, Integer>() {
            @Override
            protected Integer doInBackground(Void... urls) {
                for (int i = 0; i < 10; i++) {
                    String imHost = lookupHost("imnode.gobelieve.io");
                    String apiHost = lookupHost("api.gobelieve.io");
                    if (TextUtils.isEmpty(imHost) || TextUtils.isEmpty(apiHost)) {
                        try {
                            Thread.sleep(1000 * 1);
                        } catch (InterruptedException e) {
                        }
                        continue;
                    } else {
                        break;
                    }
                }
                return 0;
            }

            private String lookupHost(String host) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(host);
                    Log.i("beetle", "host name:" + inetAddress.getHostName() + " " + inetAddress.getHostAddress());
                    return inetAddress.getHostAddress();
                } catch (UnknownHostException exception) {
                    exception.printStackTrace();
                    return "";
                }
            }
        }.execute();
    }

    public static Application getApplication() {
        return sApplication;
    }

}
