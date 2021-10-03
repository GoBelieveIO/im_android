package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class Video  extends MessageContent{
    public String url;
    public String thumbnail;
    public int width;
    public int height;
    public int duration;

    public Video(String url, String thumbnail, int width, int height, int duration, String uuid) {
        JsonObject content = new JsonObject();

        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("thumbnail", thumbnail);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        obj.addProperty("duration", duration);
        content.add(VIDEO, obj);
        content.addProperty("uuid", uuid);
        this.raw = content.toString();
        this.url = url;
        this.thumbnail = thumbnail;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.uuid = uuid;
    }

    public Video(Video other, String url, String thumbnail) {
        super(other);
        this.width = other.width;
        this.height = other.height;
        this.duration = other.duration;
        this.url = url;
        this.thumbnail = thumbnail;

        try {
            JSONObject obj = new JSONObject(raw);
            obj.getJSONObject(VIDEO).put("url", url);
            obj.getJSONObject(VIDEO).put("thumbnail", url);
            raw = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public MessageType getType() { return MessageType.MESSAGE_VIDEO; }

    private static Video newVideo(String url, String thumbnail, int width, int height, int duration, String uuid) {
        return new Video(url, thumbnail, width, height, duration, uuid);
    }

    public static Video newVideo(String url, String thumbnail, int width, int height, int duration) {
        String uuid = UUID.randomUUID().toString();
        return newVideo(url, thumbnail, width, height, duration, uuid);
    }
}
