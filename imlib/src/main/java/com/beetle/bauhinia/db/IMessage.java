package com.beetle.bauhinia.db;

import com.beetle.bauhinia.db.message.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;


/*
 raw format
 {
    "text":"文本",
    "image":"image url",
    "image2": {
        "url":"image url",
        "width":"宽度(整数)",
        "height":"高度(整数)"
    }
    "audio": {
        "url":"audio url",
        "duration":"时长(整数)"
    }
    "location":{
        "latitude":"纬度(浮点数)",
        "longitude":"经度(浮点数)"
    }
    "notification":"群组通知内容"
    "link":{
        "image":"图片url",
        "url":"跳转url",
        "title":"标题"
    }
    "video":{
         "url":"视频url",
         "thumbnail":"视频缩略图url",
         "width":"宽度(整数)",
         "height":"高度(整数)",
         "duration":"时长(整数)"
     }
     "file":{
         "url":"文件url",
         "filename":"文件名称",
         "size":"文件大小"
     }
     "revoke": {
        "msgid": "被撤回消息的uuid"
     }
     "classroom": {
         "master_id": "群课堂发起人(整数)",
         "channel_id": "频道id",
         "server_id": "频道对应的服务器id(整数)"
     }
     "readed":{
         "msgid":"已读消息uuid"
     }
}*/

/**
 * Created by houxh on 14-7-22.
 */

public class IMessage implements Cloneable {
    static Gson gson = new GsonBuilder().create();

    public long msgLocalID;
    public int flags;
    public long sender;
    public long receiver;
    public MessageContent content;
    public ArrayList<String> tags;
    public int timestamp;//单位秒
    public boolean secret;//点对点加密

    public int readerCount;//群组消息已读数量
    public int receiverCount;//未存储到数据库
    public int referenceCount;//被引用的次数

    //以下字段未保存在文件中
    public boolean isOutgoing; //当前用户发出的消息
    private String senderName;
    private String senderAvatar;
    private boolean uploading;
    private boolean playing;
    private boolean downloading;
    private boolean geocoding;

    public IMessage() {
        tags = new ArrayList<>();
    }

    @Override
    public Object clone() {
        IMessage stu = null;
        try{
            stu = (IMessage)super.clone();
            stu.content = (MessageContent)content.clone();
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return stu;
    }

    public static MessageContent fromRaw(String raw) {
        MessageContent  content;
        try {
            JsonObject element = gson.fromJson(raw, JsonObject.class);
            if (element.has(MessageContent.TEXT)) {
                content = gson.fromJson(raw, Text.class);
            } else if (element.has(MessageContent.IMAGE2)) {
                content = gson.fromJson(element.get(MessageContent.IMAGE2), Image.class);
            } else if (element.has(MessageContent.IMAGE)) {
                Image image = new Image();
                image.url = element.get(MessageContent.IMAGE).getAsString();
                content = image;
            } else if (element.has(MessageContent.AUDIO)) {
                content = gson.fromJson(element.get(MessageContent.AUDIO), Audio.class);
            } else if (element.has(MessageContent.NOTIFICATION)) {
                content = GroupNotification.newGroupNotification(element.get(MessageContent.NOTIFICATION).getAsString());
            } else if (element.has(MessageContent.LOCATION)) {
                content = gson.fromJson(element.get(MessageContent.LOCATION), Location.class);
            } else if (element.has(MessageContent.ATTACHMENT)) {
                content = gson.fromJson(element.get(MessageContent.ATTACHMENT), Attachment.class);
            } else if (element.has(MessageContent.LINK)) {
                content = gson.fromJson(element.get(MessageContent.LINK), Link.class);
            } else if (element.has(MessageContent.HEADLINE)) {
                content = gson.fromJson(element.get(MessageContent.HEADLINE), Headline.class);
            } else if (element.has(MessageContent.VOIP)) {
                content = gson.fromJson(element.get(MessageContent.VOIP), VOIP.class);
            } else if (element.has(MessageContent.GROUP_VOIP)) {
                content = gson.fromJson(element.get(MessageContent.GROUP_VOIP), GroupVOIP.class);
            } else if (element.has(MessageContent.P2P_SESSION)) {
                content = gson.fromJson(element.get(MessageContent.P2P_SESSION), P2PSession.class);
            } else if (element.has(MessageContent.SECRET)) {
                content = gson.fromJson(element.get(MessageContent.SECRET), Secret.class);
            } else if (element.has(MessageContent.FILE)) {
                content = gson.fromJson(element.get(MessageContent.FILE), File.class);
            } else if (element.has(MessageContent.VIDEO)) {
                content = gson.fromJson(element.get(MessageContent.VIDEO), Video.class);
            } else if (element.has(MessageContent.REVOKE)) {
                content = gson.fromJson(element.get(MessageContent.REVOKE), Revoke.class);
            } else if (element.has(MessageContent.READED)) {
                content = gson.fromJson(element.get(MessageContent.READED), Readed.class);
            } else if (element.has(MessageContent.ACK)) {
                content = gson.fromJson(element.get(MessageContent.ACK), ACK.class);
            } else if (element.has(MessageContent.CLASSROOM)) {
                content = gson.fromJson(element.get(MessageContent.CLASSROOM), Classroom.class);
            } else if (element.has(MessageContent.TAG)) {
                content = gson.fromJson(element.get(MessageContent.TAG), Tag.class);
            } else {
                content = new Unknown();
            }
            if (element.has("uuid")) {
                content.setUUID(element.get("uuid").getAsString());
            }
            if (element.has("group_id")) {
                content.setGroupId(element.get("group_id").getAsLong());
            }
            if (element.has("reference")) {
                content.setReference(element.get("reference").getAsString());
            }
        } catch (Exception e) {
            content = new Unknown();
        }

        content.setRaw(raw);
        return content;
    }

    public void setContent(String raw) {
        content = fromRaw(raw);
    }

    public void setContent(MessageContent content) {
        this.content = content;
    }

    public String getUUID() {
        if (this.content != null) {
            return this.content.getUUID() != null ? this.content.getUUID() : "";
        } else {
            return "";
        }
    }

    public String getReference() {
        if (this.content != null) {
            return this.content.getReference() != null ? this.content.getReference() : "";
        } else {
            return "";
        }
    }

    public MessageContent.MessageType getType() {
        if (content != null) {
            return content.getType();
        } else {
            return MessageContent.MessageType.MESSAGE_UNKNOWN;
        }
    }

    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(
            this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName,
                                          PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void setUploading(boolean uploading) {
        boolean old = this.uploading;
        this.uploading = uploading;
        changeSupport.firePropertyChange("uploading", old, this.uploading);
    }

    public boolean getUploading() {
        return this.uploading;
    }

    public void setPlaying(boolean playing) {
        boolean old = this.playing;
        this.playing = playing;
        changeSupport.firePropertyChange("playing", old, this.playing);
    }

    public boolean getPlaying() {
        return this.playing;
    }

    public void setDownloading(boolean downloading) {
        boolean old = this.downloading;
        this.downloading = downloading;
        changeSupport.firePropertyChange("downloading", old, this.downloading);
    }

    public boolean getDownloading() {
        return this.downloading;
    }

    public void setFlags(int f) {
        int old = flags;
        flags = f;
        changeSupport.firePropertyChange("flags", old, flags);
    }

    public boolean isFailure() {
        return (flags & MessageFlag.MESSAGE_FLAG_FAILURE) != 0;
    }

    public void setFailure(boolean f) {
        boolean old = isFailure();
        if (f) {
            flags = flags | MessageFlag.MESSAGE_FLAG_FAILURE;
        } else {
            flags = flags & (~MessageFlag.MESSAGE_FLAG_FAILURE);
        }
        changeSupport.firePropertyChange("failure", old, f);
    }

    public boolean isAck() {
        return (flags & MessageFlag.MESSAGE_FLAG_ACK) != 0;
    }

    public void setAck(boolean ack) {
        boolean old = isAck();
        if (ack) {
            flags = flags | MessageFlag.MESSAGE_FLAG_ACK;
        } else {
            flags = flags & (~MessageFlag.MESSAGE_FLAG_ACK);
        }
        changeSupport.firePropertyChange("ack", old, ack);
    }

    public boolean isListened() {
        return (flags & MessageFlag.MESSAGE_FLAG_LISTENED) != 0;
    }


    public void setListened(boolean listened) {
        boolean old = isListened();
        if (listened) {
            flags = flags | MessageFlag.MESSAGE_FLAG_LISTENED;
        } else {
            flags = flags & (~MessageFlag.MESSAGE_FLAG_LISTENED);
        }
        changeSupport.firePropertyChange("listened", old, listened);
    }


    public void setReaded(boolean readed) {
        boolean old = isReaded();
        if (readed) {
            flags = flags | MessageFlag.MESSAGE_FLAG_READED;
        } else {
            flags = flags & (~MessageFlag.MESSAGE_FLAG_READED);
        }
        changeSupport.firePropertyChange("readed", old, readed);
    }

    public boolean isReaded() {
        return (flags & MessageFlag.MESSAGE_FLAG_READED) != 0;
    }


    public boolean getGeocoding() {
        return this.geocoding;
    }

    public void setGeocoding(boolean geocoding) {
        boolean old = this.geocoding;
        this.geocoding = geocoding;
        changeSupport.firePropertyChange("geocoding", old, geocoding);
    }

    public void setSenderName(String senderName) {
        String old = this.senderName;
        this.senderName = senderName;
        changeSupport.firePropertyChange("senderName", old, this.senderName);
    }

    public String getSenderName() {
        return this.senderName;
    }

    public void setSenderAvatar(String senderAvatar) {
        String old = this.senderAvatar;
        this.senderAvatar = senderAvatar;
        changeSupport.firePropertyChange("senderAvatar", old, this.senderAvatar);
    }

    public String getSenderAvatar() {
        return this.senderAvatar;
    }

    public int getReaderCount() {
        return this.readerCount;
    }

    public void setReaderCount(int count) {
        int old = this.readerCount;
        this.readerCount = count;
        changeSupport.firePropertyChange("readerCount", old, this.readerCount);
    }

    public int getReferenceCount() {
        return this.referenceCount;
    }

    public void setReferenceCount(int count) {
        int old = this.referenceCount;
        this.referenceCount = count;
        changeSupport.firePropertyChange("referenceCount", old, this.referenceCount);
    }

    public void addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
            changeSupport.firePropertyChange("tags", null, this.tags);
        }
    }

    public void deleteTag(String tag) {
        if (tags.contains(tag)) {
            tags.remove(tag);
            changeSupport.firePropertyChange("tags", null, this.tags);
        }
    }


    public List<String> getTags() {
        return this.tags;
    }

}
