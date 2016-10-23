package io.gobelieve.im.demo;
import android.app.Application;

/**
 */
public class IMDemoApplication extends Application {
    private static Application sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
    }


    public static Application getApplication() {
        return sApplication;
    }
}
