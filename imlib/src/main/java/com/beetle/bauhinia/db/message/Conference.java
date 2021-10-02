package com.beetle.bauhinia.db.message;

import com.google.gson.annotations.SerializedName;

public class Conference extends MessageContent {
    @SerializedName("master_id")
    public long masterID;

    @SerializedName("channel_id")
    public String channelID;

    @SerializedName("server_id")
    public long serverID;

    @SerializedName("mic_mode")
    public String micMode;

    public MessageContent.MessageType getType() {
        return MessageContent.MessageType.MESSAGE_CONFERENCE;
    }
}
