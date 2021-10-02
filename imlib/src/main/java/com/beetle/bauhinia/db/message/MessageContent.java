package com.beetle.bauhinia.db.message;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class MessageContent implements Cloneable {
    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String IMAGE2 = "image2";
    public static final String LOCATION = "location";
    public static final String AUDIO = "audio";
    public static final String VIDEO = "video";
    public static final String NOTIFICATION = "notification";
    public static final String LINK = "link";
    public static final String ATTACHMENT = "attachment";
    public static final String HEADLINE = "headline";
    public static final String TIMEBASE = "timebase";
    public static final String VOIP = "voip";
    public static final String GROUP_VOIP = "group_voip";
    public static final String P2P_SESSION = "p2p_session";
    public static final String SECRET = "secret";
    public static final String FILE = "file";
    public static final String REVOKE = "revoke";
    public static final String ACK = "ack";
    public static final String CLASSROOM = "classroom";
    public static final String READED = "readed";
    public static final String TAG = "tag";
    public static final String CONFERENCE = "conference";

    public enum MessageType {
        MESSAGE_UNKNOWN,
        MESSAGE_TEXT,
        MESSAGE_AUDIO,
        MESSAGE_IMAGE,
        MESSAGE_LOCATION,
        MESSAGE_VIDEO,
        MESSAGE_GROUP_NOTIFICATION,
        MESSAGE_LINK,
        MESSAGE_ATTACHMENT,
        MESSAGE_HEADLINE, //成为好友提示
        MESSAGE_VOIP,
        MESSAGE_GROUP_VOIP,
        MESSAGE_FILE,
        MESSAGE_P2P_SESSION,//建立p2p加密会话的控制消息
        MESSAGE_SECRET,
        MESSAGE_REVOKE,//撤回消息
        MESSAGE_ACK,
        MESSAGE_CLASSROOM,//群课堂
        MESSAGE_READED, //消息已读
        MESSAGE_TAG,//给消息打标签
        MESSAGE_CONFERENCE,//视频会议

        MESSAGE_TIME_BASE, //虚拟的消息，不会存入磁盘
    }

    @Override
    public Object clone() {
        MessageContent stu = null;
        try{
            stu = (MessageContent)super.clone();
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return stu;
    }

    protected String raw;
    protected String uuid;
    protected long groupId;//群组会话内的私聊和群组会话的已读
    protected String reference;//引用的消息uuid

    //客服会话
    protected long storeId;
    protected String sessionId;

    protected String name;
    protected String appName;
    protected String storeName;


    public MessageType getType() {
        return MessageType.MESSAGE_UNKNOWN;
    }

    public String getRaw() {
        return raw;
    }
    public void setRaw(String raw) {
        this.raw = raw;
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }



    public long getStoreId() {
        return storeId;
    }

    public void setStoreId(long storeId) {
        this.storeId = storeId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }


    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String ref) {
        this.reference = ref;
    }

    public void generateRaw(String uuid) {
        if (uuid == null || raw == null) {
            return;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            obj.put("uuid", uuid);
            this.raw = obj.toString();
            this.uuid = uuid;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void generateRaw(long groupId) {
        if (raw == null) {
            return;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            obj.put("group_id", groupId);
            this.raw = obj.toString();
            this.groupId = groupId;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void generateRaw(String uuid, String reference, long groupId) {
        if (raw == null) {
            return;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            if (!TextUtils.isEmpty(uuid)) {
                obj.put("uuid", uuid);
            }
            if (!TextUtils.isEmpty(reference)) {
                obj.put("reference", reference);
            }
            if (groupId > 0) {
                obj.put("group_id", groupId);
            }
            this.raw = obj.toString();
            this.groupId = groupId;
            this.uuid = uuid;
            this.reference = reference;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void generateRaw(long storeId, String sessionId, String storeName, String name, String appName) {
        if (raw == null) {
            return;
        }
        try {
            JSONObject obj = new JSONObject(raw);
            if (!TextUtils.isEmpty(sessionId)) {
                obj.put("session_id", reference);
            }
            if (storeId > 0) {
                obj.put("store_id", storeId);
            }
            if (!TextUtils.isEmpty(storeName)) {
                obj.put("store_name", storeName);
            }
            if (!TextUtils.isEmpty(name)) {
                obj.put("name", name);
            }
            if (!TextUtils.isEmpty(appName)) {
                obj.put("app_name", appName);
            }

            this.raw = obj.toString();
            this.sessionId = sessionId;
            this.storeId = storeId;
            this.storeName = storeName;
            this.name = name;
            this.appName = appName;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}

