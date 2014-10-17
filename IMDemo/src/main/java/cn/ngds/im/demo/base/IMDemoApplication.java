package cn.ngds.im.demo.base;

import android.app.Application;

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

    public static Application getApplication(){
        return sApplication;
    }
}
