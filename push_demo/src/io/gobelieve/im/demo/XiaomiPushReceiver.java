package io.gobelieve.im.demo;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;


/**
 * 1、PushMessageReceiver是个抽象类，该类继承了BroadcastReceiver。
 * 2、需要将自定义的DemoMessageReceiver注册在AndroidManifest.xml文件中 <receiver
 * android:exported="true"
 * android:name="com.xiaomi.mipushdemo.DemoMessageReceiver"> <intent-filter>
 * <action android:name="com.xiaomi.mipush.RECEIVE_MESSAGE" /> </intent-filter>
 * <intent-filter> <action android:name="com.xiaomi.mipush.ERROR" />
 * </intent-filter> <intent-filter>
 * <action android:name="com.xiaomi.mipush.MESSAGE_ARRIVED" /></intent-filter>
 * </receiver>
 * 3、DemoMessageReceiver的onReceivePassThroughMessage方法用来接收服务器向客户端发送的透传消息
 * 4、DemoMessageReceiver的onNotificationMessageClicked方法用来接收服务器向客户端发送的通知消息，
 * 这个回调方法会在用户手动点击通知后触发
 * 5、DemoMessageReceiver的onNotificationMessageArrived方法用来接收服务器向客户端发送的通知消息，
 * 这个回调方法是在通知消息到达客户端时触发。另外应用在前台时不弹出通知的通知消息到达客户端也会触发这个回调函数
 * 6、DemoMessageReceiver的onCommandResult方法用来接收客户端向服务器发送命令后的响应结果
 * 7、DemoMessageReceiver的onReceiveRegisterResult方法用来接收客户端向服务器发送注册命令后的响应结果
 * 8、以上这些方法运行在非UI线程中
 * <p>
 * 9、在miui中，如果程序未启动，且不在自启动白名单中，onReceivePassThroughMessage和onNotificationMessageArrived
 * 将不会被触发，其余方法则可以，并启动application
 * <p>
 * Created by ZhangWF(zhangwf0929@gmail.com) on 15/7/8.
 */
public class XiaomiPushReceiver extends PushMessageReceiver  {

    private static final String TAG = "XiaomiPush";

    /**
     * 接收服务器推送的透传消息
     *
     * @param context
     * @param message
     */
    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
        Log.d(TAG, "onReceivePassThroughMessage " + message);
    }

    /**
     * 接收服务器推送的通知消息，用户点击后触发
     * <p>
     * 如果用户点击了预定义通知消息，消息不会通过onNotificationMessageClicked方法传到客户端。
     * 如果服务端调用Message.Builder类的extra(String key, String value)方法设置了Constants.EXTRA_PARAM_NOTIFY_EFFECT的值，则为预定义通知消息；否则为自定义通知消息
     *
     * @param context
     * @param message
     */
    @Override
    public void onNotificationMessageClicked(Context context, MiPushMessage message) {
        Log.d(TAG, "onNotificationMessageClicked " + message);
    }

    /**
     * 接收服务器推送的通知消息，消息到达客户端时触发
     * <p>
     * 在MIUI上，只有应用处于启动状态，或者自启动白名单中，才可以通过此方法接受到该消息。
     *
     * @param context
     * @param message
     */
    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage message) {
        Log.d(TAG, "onNotificationMessageArrived " + message);
    }

    /**
     * 获取给服务器发送命令的结果
     *
     * @param context
     * @param message
     */
    @Override
    public void onCommandResult(Context context, MiPushCommandMessage message) {
        Log.d(TAG, "onCommandResult " + message);
        if (message == null || TextUtils.isEmpty(message.getCommand()) || message.getResultCode() != ErrorCode.SUCCESS) {
            return;
        }
        String command = message.getCommand();
//        List<String> arguments = message.getCommandArguments();
//        String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
//        String cmdArg2 = ((arguments != null && arguments.size() > 1) ? arguments.get(1) : null);
//        String reason = message.getReason();;

        switch (command) {
            // 注册
            case MiPushClient.COMMAND_REGISTER:
                break;
            // 别名
            case MiPushClient.COMMAND_SET_ALIAS:
                break;
            // 取消别名
            case MiPushClient.COMMAND_UNSET_ALIAS:
                break;
            // 设置账号
            case MiPushClient.COMMAND_SET_ACCOUNT:
                break;
            // 取消账号
            case MiPushClient.COMMAND_UNSET_ACCOUNT:
                break;
            // 订阅标签
            case MiPushClient.COMMAND_SUBSCRIBE_TOPIC:
                break;
            // 取消订阅标签
            case MiPushClient.COMMAND_UNSUBSCRIBE_TOPIC:
                break;
            // 设置消息注册时间
            case MiPushClient.COMMAND_SET_ACCEPT_TIME:
                break;
            default:
                break;
        }
    }

    /**
     * 获取给服务器发送注册命令的结果
     *
     * @param context
     * @param message
     */
    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) {
        Log.d(TAG, "onReceiveRegisterResult " + message);
        if (message!=null && message.getCommandArguments()!=null && message.getCommandArguments().size()>0) {
            PushDemoApplication.getApplication().setXiaomiPushToken(message.getCommandArguments().get(0));
        }
    }
}
