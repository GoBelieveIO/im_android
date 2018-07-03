package com.beetle.bauhinia.db.message;


import com.beetle.bauhinia.db.IMessage;
import com.google.gson.*;

import java.util.ArrayList;
import java.util.Iterator;

public  class GroupNotification extends Notification {
    public static final int NOTIFICATION_GROUP_CREATED = 1;//群创建
    public static final int NOTIFICATION_GROUP_DISBAND = 2;//群解散
    public static final int NOTIFICATION_GROUP_MEMBER_ADDED = 3;//群成员加入
    public static final int NOTIFICATION_GROUP_MEMBER_LEAVED = 4;//群成员离开
    public static final int NOTIFICATION_GROUP_NAME_UPDATED = 5;
    public static final int NOTIFICATION_GROUP_NOTICE_UPDATED = 6; //群公告


    public MessageType getType() {
        return MessageType.MESSAGE_GROUP_NOTIFICATION;
    }

    public int notificationType;

    public long groupID;

    public int timestamp;//单位:秒

    //NOTIFICATION_GROUP_CREATED
    public String groupName;
    public long master;
    public ArrayList<Long> members;

    public String notice;

    //NOTIFICATION_GROUP_MEMBER_LEAVED, NOTIFICATION_GROUP_MEMBER_ADDED
    public long member;
    public String memberName;





    public static GroupNotification newGroupNotification(String text) {
        GroupNotification notification = new GroupNotification();

        JsonObject content = new JsonObject();
        content.addProperty(NOTIFICATION, text);
        notification.raw = content.toString();

        try {
            Gson gson = new GsonBuilder().create();
            JsonObject element = gson.fromJson(text, JsonObject.class);

            if (element.has("create")) {
                JsonObject obj = element.getAsJsonObject("create");
                notification.groupID = obj.get("group_id").getAsLong();
                notification.timestamp = obj.get("timestamp").getAsInt();
                notification.master = obj.get("master").getAsLong();
                notification.groupName = obj.get("name").getAsString();

                notification.members = new ArrayList<Long>();
                JsonArray ary = obj.getAsJsonArray("members");
                Iterator<JsonElement> iter = ary.iterator();
                while (iter.hasNext()) {
                    JsonElement e = iter.next();
                    if (e.isJsonObject()) {
                        notification.members.add(e.getAsJsonObject().get("uid").getAsLong());
                    } else {
                        notification.members.add(e.getAsLong());
                    }
                }
                notification.notificationType = GroupNotification.NOTIFICATION_GROUP_CREATED;
            } else if (element.has("disband")) {
                JsonObject obj = element.getAsJsonObject("disband");
                notification.groupID = obj.get("group_id").getAsLong();
                notification.timestamp = obj.get("timestamp").getAsInt();
                notification.notificationType = GroupNotification.NOTIFICATION_GROUP_DISBAND;
            } else if (element.has("quit_group")) {
                JsonObject obj = element.getAsJsonObject("quit_group");
                notification.groupID = obj.get("group_id").getAsLong();
                notification.timestamp = obj.get("timestamp").getAsInt();
                notification.member = obj.get("member_id").getAsLong();
                if (obj.get("name") != null) {
                    notification.memberName = obj.get("name").getAsString();
                }
                notification.notificationType = GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED;
            } else if (element.has("add_member")) {
                JsonObject obj = element.getAsJsonObject("add_member");
                notification.groupID = obj.get("group_id").getAsLong();
                notification.timestamp = obj.get("timestamp").getAsInt();
                notification.member = obj.get("member_id").getAsLong();
                if (obj.get("name") != null) {
                    notification.memberName = obj.get("name").getAsString();
                }
                notification.notificationType = GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED;
            } else if (element.has("update_name")) {
                JsonObject obj = element.getAsJsonObject("update_name");
                notification.groupID = obj.get("group_id").getAsLong();
                notification.groupName = obj.get("name").getAsString();
                notification.timestamp = obj.get("timestamp").getAsInt();
                notification.notificationType = GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED;
            } else if (element.has("update_notice")) {
                JsonObject obj = element.getAsJsonObject("update_notice");
                notification.groupID = obj.get("group_id").getAsLong();
                notification.timestamp = obj.get("timestamp").getAsInt();
                notification.notice = obj.get("notice").getAsString();
                notification.notificationType = GroupNotification.NOTIFICATION_GROUP_NOTICE_UPDATED;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return notification;
    }




}
