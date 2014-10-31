package cn.ngds.im.demo.base;

import android.app.Application;
import cn.ngds.im.demo.domain.UserHelper;
import com.gameservice.sdk.im.IMService;

/**
 * IMDemoApplication
 * Description:
 * Author:walker lx
 */
public class IMDemoApplication extends Application {
    private static Application sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;

    }

    public static Application getApplication() {
        return sApplication;
    }


    public static void logout() {
        UserHelper.INSTANCE.logout();
        IMService.getInstance().stop();
    }
}
