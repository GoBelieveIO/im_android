package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;



public class Tag extends MessageContent {
    @SerializedName("add_tag")
    public String addTag;
    @SerializedName("delete_tag")
    public String deleteTag;

    public String msgid;

    public static Tag newAddTag(String msgid, String t) {
        Tag tag = new Tag();
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("add_tag", t);
        json.addProperty("msgid", msgid);
        content.add(TAG, json);

        tag.setRaw(content.toString());
        tag.msgid = msgid;
        tag.addTag = t;
        return tag;
    }

    public static Tag newDeleteTag(String msgid, String t) {
        Tag tag = new Tag();
        JsonObject content = new JsonObject();
        JsonObject json = new JsonObject();
        json.addProperty("delete_tag", t);
        json.addProperty("msgid", msgid);
        content.add(TAG, json);
        tag.setRaw(content.toString());
        tag.msgid = msgid;
        tag.deleteTag = t;
        return tag;
    }


    public MessageType getType() {
        return MessageType.MESSAGE_TAG;
    }
}
