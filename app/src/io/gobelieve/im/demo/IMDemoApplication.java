/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package io.gobelieve.im.demo;

import android.app.Application;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.HandlerThread;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.handler.CustomerMessageHandler;
import com.beetle.bauhinia.handler.GroupMessageHandler;
import com.beetle.bauhinia.handler.PeerMessageHandler;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.beetle.bauhinia.toolbar.emoticon.EmoticonManager;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * IMDemoApplication
 * Description:
 */
public class IMDemoApplication extends Application {
    private static final String TAG = "gobelieve";

    private static Application sApplication;

    private HandlerThread imThread;//处理im消息的线程

    private String mDeviceToken;
    public String getDeviceToken() {
        return mDeviceToken;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;

        imThread = new HandlerThread("im_service");
        imThread.start();

        IMService mIMService = IMService.getInstance();
        //app可以单独部署服务器，给予第三方应用更多的灵活性
        mIMService.setHost("imnode2.gobelieve.io");
        IMHttpAPI.setAPIURL("https://api.gobelieve.io/v2");

        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        //设置设备唯一标识,用于多点登录时设备校验
        mIMService.setDeviceID(androidID);

        mIMService.setLooper(imThread.getLooper());
        //监听网路状态变更
        IMService.getInstance().registerConnectivityChangeReceiver(getApplicationContext());

        //可以在登录成功后，设置每个用户不同的消息存储目录
        FileCache fc = FileCache.getInstance();
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //can write external storage
            String path = getExternalFilesDir(null).getAbsolutePath();
            File dir1 = new File(path, "download");
            if (!dir1.exists()) {
                dir1.mkdirs();
            }
            fc.setDir(dir1);
            Log.i(TAG, "file cache:" + dir1.getAbsolutePath());
        } else {
            File f = new File(getFilesDir(), "cache");
            if (!f.exists()) {
                f.mkdir();
            }
            fc.setDir(f);
            Log.i(TAG, "file cache:" + this.getDir("cache", MODE_PRIVATE).getAbsolutePath());
        }

        mIMService.setPeerMessageHandler(PeerMessageHandler.getInstance());
        mIMService.setGroupMessageHandler(GroupMessageHandler.getInstance());
        mIMService.setCustomerMessageHandler(CustomerMessageHandler.getInstance());

        //表情资源初始化
        EmoticonManager.getInstance().init(this);
    }

    public static Application getApplication() {
        return sApplication;
    }
}
