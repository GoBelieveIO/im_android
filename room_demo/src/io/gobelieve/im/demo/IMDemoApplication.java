/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


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
        IMService.getInstance().registerConnectivityChangeReceiver(getApplicationContext());
    }
}
