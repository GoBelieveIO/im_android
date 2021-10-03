package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public  class Audio extends MessageContent {
    public String url;
    public long duration;

    Audio(String url, long duration, String uuid) {
        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("duration", duration);
        audioJson.addProperty("url", url);
        content.add(AUDIO, audioJson);
        content.addProperty("uuid", uuid);
        this.raw = content.toString();
        this.duration = duration;
        this.url = url;
        this.uuid = uuid;
    }


    public Audio(Audio other, String url) {
        super(other);
        this.duration = other.duration;
        this.url = url;
        try {
            JSONObject obj = new JSONObject(raw);
            obj.getJSONObject(AUDIO).put("url", url);
            raw = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static Audio newAudio(String url, long duration, String uuid) {
        Audio audio = new Audio(url, duration, uuid);
        return audio;
    }

    public static Audio newAudio(String url, long duration) {
        String uuid = UUID.randomUUID().toString();
        return newAudio(url, duration, uuid);
    }

    public MessageType getType() {
        return MessageType.MESSAGE_AUDIO;
    }
}
