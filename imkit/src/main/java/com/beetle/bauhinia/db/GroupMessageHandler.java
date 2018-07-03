package com.beetle.bauhinia.db;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.message.GroupNotification;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
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

        if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke) imsg.content;
            int msgLocalID = db.getMessageId(revoke.msgid);
            if (msgLocalID > 0) {
                db.updateContent(msgLocalID, msg.content);
                db.removeMessageIndex(msgLocalID, imsg.receiver);
            }
            return true;
        } else {

            boolean r = db.insertMessage(imsg, imsg.receiver);
            msg.msgLocalID = imsg.msgLocalID;
            return r;
        }


    }
    public boolean handleMessageACK(IMMessage im) {
        int msgLocalID = im.msgLocalID;
        long gid = im.receiver;
        GroupMessageDB db = GroupMessageDB.getInstance();
        if (msgLocalID == 0) {
            MessageContent c = IMessage.fromRaw(im.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke r = (Revoke)c;
                int revokedMsgId = db.getMessageId(r.msgid);
                if (revokedMsgId > 0) {
                    db.updateContent(revokedMsgId, im.content);
                    db.removeMessageIndex(revokedMsgId, gid);
                }
            }
            return true;
        } else {
            return db.acknowledgeMessage(msgLocalID, gid);
        }
    }

    public boolean handleMessageFailure(IMMessage im) {
        int msgLocalID = im.msgLocalID;
        long gid = im.receiver;
        if (msgLocalID > 0) {
            GroupMessageDB db = GroupMessageDB.getInstance();
            return db.markMessageFailure(msgLocalID, gid);
        }
        return true;
    }

    public boolean handleGroupNotification(String notification) {
        GroupMessageDB db = GroupMessageDB.getInstance();
        GroupNotification groupNotification = GroupNotification.newGroupNotification(notification);
        IMessage imsg = new IMessage();
        imsg.sender = 0;
        imsg.receiver = groupNotification.groupID;
        imsg.timestamp = groupNotification.timestamp;
        imsg.setContent(groupNotification);
        return db.insertMessage(imsg, groupNotification.groupID);
    }

}
