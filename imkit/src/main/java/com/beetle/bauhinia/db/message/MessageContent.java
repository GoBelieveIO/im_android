package com.beetle.bauhinia.db.message;

public abstract class MessageContent {
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

    public static enum MessageType {
        MESSAGE_UNKNOWN,
        MESSAGE_TEXT,
        MESSAGE_AUDIO,
        MESSAGE_IMAGE,
        MESSAGE_LOCATION,
        MESSAGE_VIDEO,
        MESSAGE_GROUP_NOTIFICATION,
        MESSAGE_LINK,
        MESSAGE_ATTACHMENT,
        MESSAGE_HEADLINE,
        MESSAGE_VOIP,
        MESSAGE_GROUP_VOIP,
        MESSAGE_FILE,

        MESSAGE_P2P_SESSION,//建立p2p加密会话的控制消息
        MESSAGE_SECRET,

        MESSAGE_REVOKE,

        MESSAGE_TIME_BASE, //虚拟的消息，不会存入磁盘
    }




    public String raw;
    protected String uuid;

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
}

