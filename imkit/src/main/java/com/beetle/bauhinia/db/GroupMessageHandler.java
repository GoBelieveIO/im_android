package com.beetle.bauhinia.db;

import com.beetle.im.IMMessage;

import java.util.Date;

/**
 * Created by houxh on 15/3/21.
 */
public class GroupMessageHandler implements com.beetle.im.GroupMessageHandler {

    private static GroupMessageHandler instance = new GroupMessageHandler();

    public static GroupMessageHandler getInstance() {
        return instance;
    }


    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    //当前用户id
    private long uid;

    public void setUID(long uid) {
        this.uid = uid;
    }

    public boolean handleMessage(IMMessage msg) {
        GroupMessageDB db = GroupMessageDB.getInstance();
        IMessage imsg = new IMessage();
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.timestamp = msg.timestamp;
        imsg.setContent(msg.content);
        if (msg.sender == this.uid) {
            imsg.flags = MessageFlag.MESSAGE_FLAG_ACK;
        }
        boolean r = db.insertMessage(imsg, imsg.receiver);
        msg.msgLocalID = imsg.msgLocalID;
        return r;

    }
    public boolean handleMessageACK(int msgLocalID, long gid) {
        GroupMessageDB db = GroupMessageDB.getInstance();
        return db.acknowledgeMessage(msgLocalID, gid);
    }

    public boolean handleMessageFailure(int msgLocalID, long gid) {
        GroupMessageDB db = GroupMessageDB.getInstance();
        return db.markMessageFailure(msgLocalID, gid);
    }

    public boolean handleGroupNotification(String notification) {
        GroupMessageDB db = GroupMessageDB.getInstance();
        IMessage.GroupNotification groupNotification = IMessage.newGroupNotification(notification);
        IMessage imsg = new IMessage();
        imsg.sender = 0;
        imsg.receiver = groupNotification.groupID;
        imsg.timestamp = groupNotification.timestamp;
        imsg.setContent(groupNotification);
        return db.insertMessage(imsg, groupNotification.groupID);
    }

}
