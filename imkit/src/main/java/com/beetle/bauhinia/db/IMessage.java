package com.beetle.bauhinia.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

/**
 * Created by houxh on 14-7-22.
 */


public class IMessage {

    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String IMAGE2 = "image2";
    public static final String LOCATION = "location";
    public static final String AUDIO = "audio";
    public static final String NOTIFICATION = "notification";
    public static final String LINK = "link";
    public static final String ATTACHMENT = "attachment";
    public static final String HEADLINE = "headline";
    public static final String TIMEBASE = "timebase";

    public static enum MessageType {
        MESSAGE_UNKNOWN,
        MESSAGE_TEXT,
        MESSAGE_AUDIO,
        MESSAGE_IMAGE,
        MESSAGE_LOCATION,
        MESSAGE_GROUP_NOTIFICATION,
        MESSAGE_LINK,
        MESSAGE_ATTACHMENT,
        MESSAGE_HEADLINE,
        MESSAGE_TIME_BASE //虚拟的消息，不会存入磁盘
    }

    static Gson gson = new GsonBuilder().create();

    public static Text newText(String text) {
        Text t = new Text();
        String uuid = UUID.randomUUID().toString();

        JsonObject textContent = new JsonObject();
        textContent.addProperty(TEXT, text);
        textContent.addProperty("uuid", uuid);
        t.raw = textContent.toString();
        t.text = text;
        t.uuid = uuid;
        return t;
    }

    public static Audio newAudio(String url, long duration, String uuid) {
        Audio audio = new Audio();


        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("duration", duration);
        audioJson.addProperty("url", url);
        content.add(AUDIO, audioJson);
        content.addProperty("uuid", uuid);
        audio.raw = content.toString();

        audio.duration = duration;
        audio.url = url;
        audio.uuid = uuid;
        return audio;
    }

    public static Audio newAudio(String url, long duration) {
        String uuid = UUID.randomUUID().toString();
        return newAudio(url, duration, uuid);
    }

    public static Image newImage(String url, int width, int height, String uuid) {
        Image image = new Image();

        JsonObject content = new JsonObject();
        //兼容性
        content.addProperty(IMAGE, url);
        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        content.add(IMAGE2, obj);
        content.addProperty("uuid", uuid);
        image.raw = content.toString();
        image.url = url;
        image.width = width;
        image.height = height;
        image.uuid = uuid;
        return image;
    }

    public static Image newImage(String url, int width, int height) {
        String uuid = UUID.randomUUID().toString();
        return newImage(url, width, height, uuid);
    }

    public static Location newLocation(float latitude, float longitude) {
        Location location = new Location();
        String uuid = UUID.randomUUID().toString();

        JsonObject content = new JsonObject();
        JsonObject locationJson = new JsonObject();
        locationJson.addProperty("latitude", latitude);
        locationJson.addProperty("longitude", longitude);
        content.add(LOCATION, locationJson);
        content.addProperty("uuid", uuid);
        location.raw = content.toString();
        location.longitude = longitude;
        location.latitude = latitude;
        location.uuid = uuid;
        return location;
    }

    public static Attachment newAttachment(int msgLocalID, String address) {
        Attachment attachment = new Attachment();
        JsonObject content = new JsonObject();
        JsonObject attachmentJson = new JsonObject();
        attachmentJson.addProperty("msg_id", msgLocalID);
        attachmentJson.addProperty("address", address);
        content.add(ATTACHMENT, attachmentJson);
        attachment.raw = content.toString();
        attachment.address = address;
        attachment.msg_id = msgLocalID;
        return attachment;
    }

    public static Headline newHeadline(String headline) {
        Headline head = new Headline();
        JsonObject content = new JsonObject();
        JsonObject headlineJson = new JsonObject();
        headlineJson.addProperty("headline", headline);
        content.add(HEADLINE, headlineJson);
        head.raw = content.toString();
        head.headline = headline;
        head.description = headline;
        return head;
    }

    public static TimeBase newTimeBase(int timestamp) {
        TimeBase tb = new TimeBase();
        JsonObject content = new JsonObject();
        content.addProperty("timestamp", timestamp);
        tb.raw = content.toString();
        tb.timestamp = timestamp;
        return tb;
    }

    public static GroupNotification newGroupNotification(String text) {
        GroupNotification notification = new GroupNotification();

        JsonObject content = new JsonObject();
        content.addProperty(NOTIFICATION, text);
        notification.raw = content.toString();

        Gson gson = new GsonBuilder().create();
        JsonObject element = gson.fromJson(text, JsonObject.class);

        if (element.has("create")) {
            JsonObject obj = element.getAsJsonObject("create");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.timestamp = obj.get("timestamp").getAsInt();
            notification.master = obj.get("master").getAsLong();
            notification.groupName = obj.get("name").getAsString();

            notification.members = new ArrayList<Long>();
            JsonArray ary = obj.getAsJsonArray("members");
            Iterator<JsonElement> iter = ary.iterator();
            while (iter.hasNext()) {
                notification.members.add(iter.next().getAsLong());
            }
            notification.notificationType = GroupNotification.NOTIFICATION_GROUP_CREATED;
        } else if (element.has("disband")) {
            JsonObject obj = element.getAsJsonObject("disband");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.timestamp = obj.get("timestamp").getAsInt();
            notification.notificationType = GroupNotification.NOTIFICATION_GROUP_DISBAND;
        } else if (element.has("quit_group")) {
            JsonObject obj = element.getAsJsonObject("quit_group");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.timestamp = obj.get("timestamp").getAsInt();
            notification.member = obj.get("member_id").getAsLong();
            notification.notificationType = GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED;
        } else if (element.has("add_member")) {
            JsonObject obj = element.getAsJsonObject("add_member");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.timestamp = obj.get("timestamp").getAsInt();
            notification.member = obj.get("member_id").getAsLong();
            notification.notificationType = GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED;
        } else if (element.has("update_name")) {
            JsonObject obj = element.getAsJsonObject("update_name");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.groupName = obj.get("name").getAsString();
            notification.timestamp = obj.get("timestamp").getAsInt();
            notification.notificationType = GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED;
        }

        return notification;
    }

    public abstract static class MessageContent {
        protected String raw;
        protected String uuid;

        public MessageType getType() {
            return MessageType.MESSAGE_UNKNOWN;
        }

        public String getRaw() {
            return raw;
        }

        public String getUUID() {
            return uuid;
        }

        public void setUUID(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class Text extends MessageContent {
        public String text;
        public MessageType getType() {
            return MessageType.MESSAGE_TEXT;
        }
    }

    public static class Image extends MessageContent {
        public String url;
        public int width;
        public int height;
        public MessageType getType() {
            return MessageType.MESSAGE_IMAGE;
        }
    }

    public static class Audio extends MessageContent {
        public String url;
        public long duration;
        public MessageType getType() {
            return MessageType.MESSAGE_AUDIO;
        }
    }

    public static class Location extends MessageContent {
        public float latitude;
        public float longitude;
        public String address;
        public MessageType getType() { return MessageType.MESSAGE_LOCATION; }
    }

    public static abstract class Notification extends MessageContent {
        public String description;

        public String getDescription() {
            return this.description;
        }
    }

    public static class TimeBase extends Notification {
        public int timestamp;
        public MessageType getType() {
            return MessageType.MESSAGE_TIME_BASE;
        }
    }

    public static class Headline extends Notification {
        public String headline;

        public String getDescription() {
            return this.headline;
        }
        public MessageType getType() {
            return MessageType.MESSAGE_HEADLINE;
        }
    }

    public static class GroupNotification extends Notification {
        public static final int NOTIFICATION_GROUP_CREATED = 1;//群创建
        public static final int NOTIFICATION_GROUP_DISBAND = 2;//群解散
        public static final int NOTIFICATION_GROUP_MEMBER_ADDED = 3;//群成员加入
        public static final int NOTIFICATION_GROUP_MEMBER_LEAVED = 4;//群成员离开
        public static final int NOTIFICATION_GROUP_NAME_UPDATED = 5;


        public MessageType getType() {
            return MessageType.MESSAGE_GROUP_NOTIFICATION;
        }

        public int notificationType;

        public long groupID;

        public int timestamp;//单位:秒

        //NOTIFICATION_GROUP_CREATED
        public String groupName;
        public long master;
        public ArrayList<Long> members;

        //NOTIFICATION_GROUP_MEMBER_LEAVED, NOTIFICATION_GROUP_MEMBER_ADDED
        public long member;
    }

    public static class Link extends MessageContent {
        public String title;
        public String content;
        public String url;
        public String image;
        public MessageType getType() { return MessageType.MESSAGE_LINK; }
    }

    public static class Attachment extends MessageContent {
        public int msg_id;
        public String address;

        public MessageType getType() {
            return MessageType.MESSAGE_ATTACHMENT;
        }
    }

    public static class Unknown extends MessageContent {}

    public void setContent(String raw) {
        try {
            JsonObject element = gson.fromJson(raw, JsonObject.class);
            if (element.has(TEXT)) {
                content = gson.fromJson(raw, Text.class);
            } else if (element.has(IMAGE2)) {
                content = gson.fromJson(element.get(IMAGE2), Image.class);
            } else if (element.has(IMAGE)) {
                Image image = new Image();
                image.url = element.get(IMAGE).getAsString();
                content = image;
            } else if (element.has(AUDIO)) {
                content = gson.fromJson(element.get(AUDIO), Audio.class);
            } else if (element.has(NOTIFICATION)) {
                content = newGroupNotification(element.get(NOTIFICATION).getAsString());
            } else if (element.has(LOCATION)) {
                content = gson.fromJson(element.get(LOCATION), Location.class);
            } else if (element.has(ATTACHMENT)) {
                content = gson.fromJson(element.get(ATTACHMENT), Attachment.class);
            } else if (element.has(LINK)) {
                content = gson.fromJson(element.get(LINK), Link.class);
            } else if (element.has(HEADLINE)) {
                content = gson.fromJson(element.get(HEADLINE), Headline.class);
            } else {
                content = new Unknown();
            }
            if (element.has("uuid")) {
                content.setUUID(element.get("uuid").getAsString());
            }
        } catch (Exception e) {
            content = new Unknown();
        }

        content.raw = raw;


    }

    public void setContent(MessageContent content) {
        this.content = content;
    }

    public int msgLocalID;
    public int flags;
    public long sender;
    public long receiver;
    public MessageContent content;
    public int timestamp;//单位秒

    //以下字段未保存在文件中
    public boolean isOutgoing; //当前用户发出的消息
    private String senderName;
    private String senderAvatar;
    private boolean uploading;
    private boolean playing;
    private boolean downloading;
    private boolean geocoding;

    public String getUUID() {
        if (this.content != null) {
            return this.content.getUUID() != null ? this.content.getUUID() : "";
        } else {
            return "";
        }
    }


    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(
            this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void setUploading(boolean uploading) {
        boolean old = this.uploading;
        this.uploading = uploading;
        changeSupport.firePropertyChange("uploading", old, this.uploading);
    }

    public boolean getUploading() {
        return this.uploading;
    }

    public void setPlaying(boolean playing) {
        boolean old = this.playing;
        this.playing = playing;
        changeSupport.firePropertyChange("playing", old, this.playing);
    }

    public boolean getPlaying() {
        return this.playing;
    }

    public void setDownloading(boolean downloading) {
        boolean old = this.downloading;
        this.downloading = downloading;
        changeSupport.firePropertyChange("downloading", old, this.downloading);
    }

    public boolean getDownloading() {
        return this.downloading;
    }

    public boolean isFailure() {
        return (flags & MessageFlag.MESSAGE_FLAG_FAILURE) != 0;
    }

    public void setFailure(boolean f) {
        boolean old = isFailure();
        if (f) {
            flags = flags | MessageFlag.MESSAGE_FLAG_FAILURE;
        } else {
            flags = flags & (~MessageFlag.MESSAGE_FLAG_FAILURE);
        }
        changeSupport.firePropertyChange("failure", old, f);
    }

    public boolean isAck() {
        return (flags & MessageFlag.MESSAGE_FLAG_ACK) != 0;
    }

    public void setAck(boolean ack) {
        boolean old = isAck();
        if (ack) {
            flags = flags | MessageFlag.MESSAGE_FLAG_ACK;
        } else {
            flags = flags & (~MessageFlag.MESSAGE_FLAG_ACK);
        }
        changeSupport.firePropertyChange("ack", old, ack);
    }

    public boolean isListened() {
        return (flags & MessageFlag.MESSAGE_FLAG_LISTENED) != 0;
    }

    public void setListened(boolean listened) {
        boolean old = isListened();
        if (listened) {
            flags = flags | MessageFlag.MESSAGE_FLAG_LISTENED;
        } else {
            flags = flags & (~MessageFlag.MESSAGE_FLAG_LISTENED);
        }
        changeSupport.firePropertyChange("listened", old, listened);
    }

    public boolean getGeocoding() {
        return this.geocoding;
    }

    public void setGeocoding(boolean geocoding) {
        boolean old = this.geocoding;
        this.geocoding = geocoding;
        changeSupport.firePropertyChange("geocoding", old, geocoding);
    }

    public void setSenderName(String senderName) {
        String old = this.senderName;
        this.senderName = senderName;
        changeSupport.firePropertyChange("senderName", old, this.senderName);
    }

    public String getSenderName() {
        return this.senderName;
    }

    public void setSenderAvatar(String senderAvatar) {
        String old = this.senderAvatar;
        this.senderAvatar = senderAvatar;
        changeSupport.firePropertyChange("senderAvatar", old, this.senderAvatar);
    }

    public String getSenderAvatar() {
        return this.senderAvatar;
    }

}
