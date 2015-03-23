package com.beetle.bauhinia.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by houxh on 14-7-22.
 */


public class IMessage {

    public static final String TEXT = "text";
    public static final String IMAGE = "image";
    public static final String LOCATION = "location";
    public static final String AUDIO = "audio";
    public static final String NOTIFICATION = "notification";


    public static enum MessageType {
        MESSAGE_UNKNOWN,
        MESSAGE_TEXT,
        MESSAGE_AUDIO,
        MESSAGE_IMAGE,
        MESSAGE_GROUP_NOTIFICATION,
    }

    static Gson gson = new GsonBuilder().create();

    public static Text newText(String text) {
        Text t = new Text();
        JsonObject textContent = new JsonObject();
        textContent.addProperty(TEXT, text);
        t.raw = textContent.toString();
        t.text = text;
        return t;
    }

    public static Audio newAudio(String url, long duration) {
        Audio audio = new Audio();

        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("duration", duration);
        audioJson.addProperty("url", url);
        content.add(AUDIO, audioJson);
        audio.raw = content.toString();

        audio.duration = duration;
        audio.url = url;
        return audio;
    }

    public static Image newImage(String url) {
        Image image = new Image();
        JsonObject content = new JsonObject();
        content.addProperty(IMAGE, url);
        image.raw = content.toString();
        image.image = url;
        return image;
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
            notification.master = obj.get("master").getAsLong();
            notification.groupName = obj.get("name").getAsString();

            notification.members = new ArrayList<>();
            JsonArray ary = obj.getAsJsonArray("members");
            Iterator<JsonElement> iter = ary.iterator();
            while (iter.hasNext()) {
                notification.members.add(iter.next().getAsLong());
            }
            notification.type = GroupNotification.NOTIFICATION_GROUP_CREATED;
        } else if (element.has("disband")) {
            JsonObject obj = element.getAsJsonObject("disband");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.type = GroupNotification.NOTIFICATION_GROUP_DISBAND;
        } else if (element.has("quit_group")) {
            JsonObject obj = element.getAsJsonObject("quit_group");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.member = obj.get("member_id").getAsLong();
            notification.type = GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED;
        } else if (element.has("add_member")) {
            JsonObject obj = element.getAsJsonObject("add_member");
            notification.groupID = obj.get("group_id").getAsLong();
            notification.member = obj.get("member_id").getAsLong();
            notification.type = GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED;
        }

        return notification;
    }

    public abstract static class MessageContent {
        public String raw;

        public MessageType getType() {
            return MessageType.MESSAGE_UNKNOWN;
        }

        public String getRaw() {
            return raw;
        }
    }

    public static class Text extends MessageContent {
        public String text;
        public MessageType getType() {
            return MessageType.MESSAGE_TEXT;
        }
    }

    public static class Image extends MessageContent {
        public String image;
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
    }

    public static class GroupNotification extends MessageContent {
        public static final int NOTIFICATION_GROUP_CREATED = 1;//群创建
        public static final int NOTIFICATION_GROUP_DISBAND = 2;//群解散
        public static final int NOTIFICATION_GROUP_MEMBER_ADDED = 3;//群成员加入
        public static final int NOTIFICATION_GROUP_MEMBER_LEAVED = 4;//群成员离开


        public MessageType getType() {
            return MessageType.MESSAGE_GROUP_NOTIFICATION;
        }

        public int type;

        public String description;
        public long groupID;

        //NOTIFICATION_GROUP_CREATED
        public String groupName;
        public long master;
        public ArrayList<Long> members;

        //NOTIFICATION_GROUP_MEMBER_LEAVED, NOTIFICATION_GROUP_MEMBER_ADDED
        public long member;
    }


    public static class Unknown extends MessageContent {}

    public void setContent(String raw) {
        try {
            JsonObject element = gson.fromJson(raw, JsonObject.class);
            if (element.has(TEXT)) {
                content = gson.fromJson(raw, Text.class);
            } else if (element.has(IMAGE)){
                content = gson.fromJson(raw, Image.class);
            } else if (element.has(AUDIO)) {
                content = gson.fromJson(element.get(AUDIO), Audio.class);
            } else if (element.has(NOTIFICATION)) {
                content = newGroupNotification(element.get(NOTIFICATION).getAsString());
            } else {
                content = new Unknown();
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
    public int timestamp;

}
