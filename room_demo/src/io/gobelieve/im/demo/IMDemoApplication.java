package io.gobelieve.im.demo;

import android.app.Application;
import android.provider.Settings;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.im.IMService;


/**
 * IMDemoApplication
 * Description:
 */
public class IMDemoApplication extends Application {
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
        mIMService.registerConnectivityChangeReceiver(getApplicationContext());
    }

}
