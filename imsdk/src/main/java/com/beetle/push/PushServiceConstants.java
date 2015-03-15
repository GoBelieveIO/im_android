package com.beetle.push;

/**
 * PushServiceConstants
 * Description:
 * Author:walker lx
 */
public interface PushServiceConstants {

    class IntentKey {
        protected final static String KEY_ACTION = "key_action";
        protected final static String KEY_PLAYERID = "key_playerid";
        protected final static String KEY_DATA = "key_data";
    }


    class PushAction {
        protected final static int START = 0x00;    //默认0 为起始状态
        protected final static int HEART_BEAT = 0x02;
    }


    class HandlerMsg {
        //app->service
        protected final static int REGISTER_CLIENT = 0x01;
        protected final static int UNREGISTER_CLIENT = 0x02;
        //service->app
        protected final static int PUSH_MESSAGE = 0x03;// 透传消息
        protected final static int DEBUG_INFO = 0x04;  // 调试信息
        protected final static int DEVICE_TOKEN = 0x05;// 设备Token
    }

}
