package com.beetle.push.db;

/**
 * NgdsPushDataConsts
 * Description: push sdk中存储数据相关常量配置
 * Author:walker lx
 */
public interface NgdsPushDataConsts {
    //如果有存储格式或者tag的变动，则增加version版本号并维护版本数据
    public final int version = 4;

    final String NGDS_SMART_PUSH_FILE_NAME = "ngds_smart_analystic_info" + version;


    public class PushSharePreferenceKey {
        public final static String DEVICE_TOKEN = "device_token";
    }
}
