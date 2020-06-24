package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

import java.util.UUID;

public class File extends MessageContent {
    public String url;
    public String filename;
    public int size;

    public MessageType getType() {
        return MessageType.MESSAGE_FILE;
    }

    public static File newFile(String url, String filename, int size) {
        String uuid = UUID.randomUUID().toString();
        File f = new File();
        JsonObject content = new JsonObject();

        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("size", size);
        obj.addProperty("filename", filename);

        content.add(FILE, obj);
        content.addProperty("uuid", uuid);

        f.raw = content.toString();
        f.url = url;
        f.filename = filename;
        f.size = size;
        f.uuid = uuid;
        return  f;
    }
}
