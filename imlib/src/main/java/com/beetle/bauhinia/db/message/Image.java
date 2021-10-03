package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class Image extends MessageContent {
    public String url;
    public int width;
    public int height;

    public Image(String url, int width, int height, String uuid) {
        JsonObject content = new JsonObject();
        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        content.add(IMAGE2, obj);
        content.addProperty("uuid", uuid);
        this.raw = content.toString();
        this.url = url;
        this.width = width;
        this.height = height;
        this.uuid = uuid;
    }

    public Image(Image other, String url) {
        super(other);
        this.width = other.width;
        this.height = other.height;
        this.url = url;

        try {
            JSONObject obj = new JSONObject(raw);
            obj.getJSONObject(IMAGE2).put("url", url);
            raw = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public MessageType getType() {
        return MessageType.MESSAGE_IMAGE;
    }
    
    private static Image newImage(String url, int width, int height, String uuid) {
        return new Image(url, width, height, uuid);
    }

    public static Image newImage(String url, int width, int height) {
        String uuid = UUID.randomUUID().toString();
        return newImage(url, width, height, uuid);
    }

}