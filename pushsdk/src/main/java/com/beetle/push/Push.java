package com.beetle.push;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;

import com.beetle.push.singleton.PushInterfaceProvider;

/**
 * 对外接口类
 *
 * @author 慕容秋 (muroqiu@qq.com)
 *         Create on 14-9-16
 */
public class Push implements PushServiceConstants {
    private static final String TAG = "SmartPush";
    private static IMsgReceiver receiver;
    private static boolean stopped = true;
    private static MasterServiceConnection mServiceConnection = new MasterServiceConnection();

    /**
     * 注册push回调接口
     */
    public static void registerReceiver(IMsgReceiver receiver) {
        Push.receiver = receiver;
    }

    /**
     * 注销push回调
     */
    public static void unRegisterReceiver() {
        Push.receiver = null;
    }

    /**
     * 启动push服务
     */
    public static void registerService(Context context) {
        if (!stopped) {
            return;
        }

        stopped = false;
        Context appContext = context.getApplicationContext();
        //在app进程退出后，保证service进程不会立刻跟着退出
        Intent startIntent = new Intent(appContext, PushService.class);
        appContext.startService(startIntent);

        Intent intent = new Intent(appContext, PushService.class);
        appContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 展示引导用户去设置显示通知(只会弹出一次)
     *
     * @param activity
     */
    public static void showGuideOnce(Activity activity) {
        PushInterfaceProvider.getToolUtilInstance(activity).showGuideOnce(activity);
    }

    /**
     * @return 是否是需要提示用户显示通知或者自启动的机型.
     */
    public static boolean matchTargetModel(Context context) {
        return PushInterfaceProvider.getToolUtilInstance(context).matchTargetModel();
    }

    /**
     * 显示引导用户设置显示通知的对话框
     *
     * @param activity activity
     */
    public static void showGuide(Activity activity) {
        PushInterfaceProvider.getToolUtilInstance(activity).showGuide(activity);
    }

    private static class MasterServiceConnection implements ServiceConnection {
        private static Messenger mMessenger;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            android.util.Log.i("PUSH", "master service connected");
            mMessenger = new Messenger(service);
            try {
                Message msg = Message.obtain(null, HandlerMsg.REGISTER_CLIENT);
                msg.replyTo = new Messenger(new CallbackHandler());
                mMessenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            android.util.Log.i("PUSH", "master service disconnected");
            mMessenger = null;
        }
    }


    private static class CallbackHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HandlerMsg.PUSH_MESSAGE:
                    handlePushMessage(msg);
                    break;
                case HandlerMsg.DEVICE_TOKEN:
                    handleDeviceToken(msg);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

        private void handlePushMessage(Message msg) {
            String jsonStr = msg.getData().getString(IntentKey.KEY_DATA);
            Log.i(TAG, "push message:" + jsonStr);
        }

        private void handleDeviceToken(Message msg) {
            byte[] tokenArrary = msg.getData().getByteArray(IntentKey.KEY_DATA);
            if (receiver != null) {
                receiver.onDeviceToken(tokenArrary);
            }
        }
    }

}
