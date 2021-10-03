package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class File extends MessageContent {
    public String url;
    public String filename;
    public int size;

    public File(String url, String filename, int size) {
        String uuid = UUID.randomUUID().toString();
        JsonObject content = new JsonObject();
        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("size", size);
        obj.addProperty("filename", filename);
        content.add(FILE, obj);
        content.addProperty("uuid", uuid);

        this.raw = content.toString();
        this.url = url;
        this.filename = filename;
        this.size = size;
        this.uuid = uuid;
    }

    public File(File other, String url) {
        super(other);
        this.filename = other.filename;
        this.size = other.size;
        this.url = url;
        try {
            JSONObject obj = new JSONObject(raw);
            obj.getJSONObject(FILE).put("url", url);
            raw = obj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public MessageType getType() {
        return MessageType.MESSAGE_FILE;
    }

    public static File newFile(String url, String filename, int size) {
        return new File(url, filename, size);
    }
}
