package com.beetle.push.face;

import android.app.Activity;

/**
 * ToolUtilInterface
 * Description: 开放工具接口,如判断用户的rom与手机是否是需要提示用户进行设置通知的机型
 * 调用场景:一般是在主进程中被调用
 * Author:walker lee
 */
public interface ToolUtilInterface {
    /**
     * 展示引导用户去设置显示通知(只会弹出一次)
     *
     * @param activity activity
     */
    public void showGuideOnce(Activity activity);

    /**
     * @return 是否是需要提示用户显示通知或者自启动的机型.
     */
    public boolean matchTargetModel();

    /**
     * 显示引导用户设置显示通知的对话框
     *
     * @param activity activity
     */
    public void showGuide(Activity activity);
}
