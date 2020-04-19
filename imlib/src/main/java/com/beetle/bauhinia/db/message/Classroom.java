package com.beetle.bauhinia.db.message;

import com.google.gson.annotations.SerializedName;

public class Classroom extends MessageContent {
    @SerializedName("master_id")
    public long masterID;

    @SerializedName("channel_id")
    public String channelID;

    @SerializedName("server_id")
    public long serverID;

    @SerializedName("mic_mode")
    public String micMode;

    public MessageType getType() {
        return MessageType.MESSAGE_CLASSROOM;
    }
}
