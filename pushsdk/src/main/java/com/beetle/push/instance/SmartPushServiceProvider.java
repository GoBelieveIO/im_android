package com.beetle.push.instance;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import com.beetle.push.connect.Protocol;
import com.beetle.push.connect.PushClient;
import com.beetle.push.connect.PushClientObserver;
import com.beetle.push.core.log.PushLog;
import com.beetle.push.db.AppBean;
import com.beetle.push.type.PushInfo;
import com.beetle.push.ui.SmartPushNotification;
import com.beetle.push.util.NetWorkUtil;
import com.beetle.push.face.SmartPushServiceInterface;
import com.beetle.push.connect.IoLoop;
import com.beetle.push.core.util.Utils;
import com.beetle.push.core.util.io.IoUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

/**
 * SmartPushServiceProvider
 * Description: SmartPushService中调用业务逻辑的代码实体
 * Author:walker lee
 */
public class SmartPushServiceProvider implements SmartPushServiceInterface, PushClientObserver {
    private final static String TAG = "SmartPushService";
    /**
     * alrammanager 心跳包间隔时间，微信为300s ，qq为180s
     */
    private final static int ALARM_INTERVAL = 6 * 60 * 1000;

    public final static String HOST = "pushnode.gobelieve.io";

    public final static int PORT = 6225;


    private PushClient mClient;
    private Context mPushServiceContext;

    private static String pushHost = HOST;

    public static void setHost(String host) {
        pushHost = host;
    }

    @Override
    public void onServiceCreate(Context context) {
        initServiceContext(context);
        System.out.println("local onServiceCreate");
        PushLog.initFileLoger(context, "pushv2");
        try {
            IoLoop loop = IoLoop.getDefaultLoop();
            if (!loop.isAlive()) {
                loop.prepare();
                loop.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            PushLog.d(TAG, "registerService io loop fail");
            return;
        }

        NetWorkUtil.checkNetwork(context);
        startPushClient(context);
    }

    private void initServiceContext(Context context) {
        mPushServiceContext = context;
    }

    @Override
    public void onHeartBeat(Context context) {
        initServiceContext(context);
        //唤醒ioloop线程
        IoLoop.getDefaultLoop().asyncSend(new IoLoop.IoRunnable() {
            @Override
            public void run() {
                PushLog.d(TAG, "ioloop thread wakeup");
                mClient.sendPing();
            }
        });
    }

    @Override
    public byte[] onP2PConnected(Context context) {
        byte[] deviceToken =
            AppBean.getInstance(context).getDeivceToken();
        return deviceToken;
    }

    @Override
    public void onServiceDestroy(Context context) {
        PushLog.d(TAG, "master service destroy");
        mClient.stop();
        mPushServiceContext = null;
    }

    @Override
    public long getHeatBeatPeriod() {
        return ALARM_INTERVAL;
    }

    @Override
    public void onReceiverNetworkChange(Context context) {

    }

    @Override
    public void onNetworkChange(Context context) {
        if (Utils.isOnNet(context)) {
            mClient.stop();
            mClient.start();
        } else {
            mClient.stop();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onShowNotification(Context context, Object notification) {
        if (null == notification) {
            PushLog.d(TAG, "object is null");
            return;
        }
        Protocol.Notification notificationIns = (Protocol.Notification) notification;
        try {
            String json = new String(notificationIns.content, Charset.forName("UTF-8"));
            JSONObject jsonObject = new JSONObject(json);
            PushInfo pushInfo = new PushInfo(jsonObject);
            PushLog.d(TAG, "receive notification:" + json);
            // 透传消息直接处理
            if (pushInfo.getType() == PushInfo.PUSH_TYPE_DELIVER) {
                try {
                    Method method = context.getClass().getMethod("sendDeliverMsg", String.class);
                    method.invoke(context, pushInfo.getContent());
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                new SmartPushNotification(mPushServiceContext).showNotification(pushInfo);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onDeviceToken(byte[] deviceToken) {
        if (null == mPushServiceContext) {
            PushLog.d(TAG, "SmartPushService is empty");
            return;
        }
        AppBean.getInstance(mPushServiceContext).setDeviceToken(deviceToken);
        try {
            Method methodOnDeviceToken =
                mPushServiceContext.getClass().getMethod("onDeviceToken", byte[].class);
            methodOnDeviceToken.invoke(mPushServiceContext, deviceToken);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPushMessage(Protocol.Notification notification) {
        if (null == mPushServiceContext) {
            android.util.Log.d(TAG, "SmartPushService is empty");
            return;
        }
        try {
            Method methodToInvoke =
                mPushServiceContext.getClass().getMethod("onPushMessage", Object.class);
            methodToInvoke.invoke(mPushServiceContext, notification);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private void startPushClient(Context context) {
        PowerManager powerManager =
            ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
        PowerManager.WakeLock wakelock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "master service");
        wakelock.setReferenceCounted(false);

        mClient = new PushClient(this, wakelock);

        byte[] deviceToken = AppBean.getInstance(context).getDeivceToken();
        if (null != deviceToken) {
            mClient.setDeviceToken(deviceToken);
            PushLog.d(TAG, "token:" + IoUtil.bin2HexForTest(deviceToken));
        }
        mClient.setHost(pushHost);
        mClient.setPort(PORT);
        String appid = Utils.loadAppId(context);
        String appkey = Utils.loadAppKey(context);
        mClient.setAppID(Long.parseLong(appid));
        mClient.setAppKey(appkey);
        if (Utils.isOnNet(context)) {
            mClient.start();
        }
    }

}
