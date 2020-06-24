package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

import java.util.UUID;

public class Video  extends MessageContent{
    public String url;
    public String thumbnail;
    public int width;
    public int height;
    public int duration;

    public MessageType getType() { return MessageType.MESSAGE_VIDEO; }

    private static Video newVideo(String url, String thumbnail, int width, int height, int duration, String uuid) {
        Video video = new Video();

        JsonObject content = new JsonObject();

        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("thumbnail", thumbnail);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        obj.addProperty("duration", duration);
        content.add(VIDEO, obj);
        content.addProperty("uuid", uuid);
        video.raw = content.toString();
        video.url = url;
        video.thumbnail = thumbnail;
        video.width = width;
        video.height = height;
        video.duration = duration;
        video.uuid = uuid;

        return video;
    }

    public static Video newVideo(String url, String thumbnail, int width, int height, int duration) {
        String uuid = UUID.randomUUID().toString();
        return newVideo(url, thumbnail, width, height, duration, uuid);
    }
}
