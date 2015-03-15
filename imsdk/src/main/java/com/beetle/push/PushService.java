package com.beetle.push;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import com.beetle.push.singleton.PushInterfaceProvider;

import java.util.Calendar;

public class PushService extends Service
    implements PushReceiver.NetworkStateObserver, PushServiceConstants {
    public final static String HEART_BEAT_ACTION = "com.gameservice.android.intent.alarm";
    private static PendingIntent alarm;
    private final static String TAG = "SmartPushService";


    class ServiceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HandlerMsg.REGISTER_CLIENT: {
                    mClientMessenger = msg.replyTo;
                    onP2pConnected();
                }
                break;
                case HandlerMsg.UNREGISTER_CLIENT:
                    mClientMessenger = null;
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }

    }

    /**
     * 当进程间的handle建立连接时
     */
    private void onP2pConnected() {
        byte[] deviceToken = null;
        deviceToken = PushInterfaceProvider.getPushServiceInstance(this).onP2PConnected(this);
        if (deviceToken != null && deviceToken.length > 0) {
            sendDeviceToken(deviceToken);
        }
    }


    class MainThreadHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }


    Messenger mClientMessenger;
    final Messenger mMessenger = new Messenger(new ServiceHandler());

    final MainThreadHandler mainThreadHandler = new MainThreadHandler();

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        PushInterfaceProvider.getPushServiceInstance(this).onServiceCreate(this);

        PushReceiver.addObserver(this);
        alarm = startAlarm(this);
    }

    private PendingIntent startAlarm(Context appContext) {
        AlarmManager alarmMgr;
        PendingIntent alarmIntent;
        alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(appContext, PushReceiver.class);
        intent.setAction(HEART_BEAT_ACTION);
        alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);
        Calendar calendar = Calendar.getInstance();

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
            PushInterfaceProvider.getPushServiceInstance(appContext).getHeatBeatPeriod(), alarmIntent);

        return alarmIntent;
    }

    private static void stopAlarm(Context appContext, PendingIntent alarm) {
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(alarm);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on registerService command");
        if (intent == null) {
            //restart after killed
            return START_STICKY;
        }
        int action = intent.getIntExtra(IntentKey.KEY_ACTION, PushAction.START);
        switch (action) {
            case PushAction.HEART_BEAT:
                PushInterfaceProvider.getPushServiceInstance(this).onHeartBeat(this);
                break;
            case PushAction.START:
            default:
                return START_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onNetworkChange() {
        PushInterfaceProvider.getPushServiceInstance(this).onNetworkChange(this);
    }

    public void onDeviceToken(final byte[] deviceToken) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                sendDeviceToken(deviceToken);
            }
        });
    }

    private void sendDebugInfo(String debugInfo) {
        if (null != mClientMessenger) {
            Message m = Message.obtain(null, HandlerMsg.DEBUG_INFO);
            m.getData().putString(IntentKey.KEY_DATA, debugInfo);
            try {
                mClientMessenger.send(m);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendDeviceToken(byte[] tokenArrary) {
        if (null != mClientMessenger) {
            Message m = Message.obtain(null, HandlerMsg.DEVICE_TOKEN);
            m.getData().putByteArray(IntentKey.KEY_DATA, tokenArrary);
            try {
                mClientMessenger.send(m);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendDeliverMsg(String jsonMsg) {
        if (null != mClientMessenger) {
            Message m = Message.obtain(null, HandlerMsg.PUSH_MESSAGE);
            m.getData().putString(IntentKey.KEY_DATA, jsonMsg);
            try {
                mClientMessenger.send(m);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void showNotification(Object notification) {
        PushInterfaceProvider.getPushServiceInstance(this).onShowNotification(this, notification);
    }

    public void onPushMessage(final Object notification) {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                showNotification(notification);
            }
        });
    }

    @Override
    public void onDestroy() {
        PushInterfaceProvider.getPushServiceInstance(this).onServiceDestroy(this);
        PushReceiver.removeObserver(this);
        stopAlarm(this, alarm);
        super.onDestroy();
    }
}
