package com.beetle.bauhinia.db;

import com.beetle.bauhinia.constant.MessageKeys;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Created by houxh on 14-7-22.
 */


public class IMessage implements MessageKeys {
    public static enum MessageType {
        MESSAGE_UNKNOWN,
        MESSAGE_TEXT,
        MESSAGE_AUDIO,
        MESSAGE_IMAGE,
    }

    static Gson gson = new GsonBuilder().create();

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

    public static class Unknown extends MessageContent {}

    public void setContent(String raw) {
        try {
            JsonObject element = gson.fromJson(raw, JsonObject.class);
            if (element.has(TEXT)) {
                content = gson.fromJson(raw, Text.class);
            } else if (element.has(IMAGE)){
                content = gson.fromJson(raw, Image.class);
            } else if (element.has(AUDIO)){
                content = gson.fromJson(element.get(AUDIO), Audio.class);
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
