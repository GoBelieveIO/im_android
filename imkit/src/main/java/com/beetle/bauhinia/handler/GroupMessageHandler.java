package com.beetle.bauhinia.handler;

import android.text.TextUtils;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.message.GroupNotification;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.im.IMMessage;
import com.beetle.im.MessageACK;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private void repairFailureMessage(String uuid) {
        //消息由本设备发出，则不需要重新入库，用于纠正消息标志位
        GroupMessageDB db = GroupMessageDB.getInstance();
        if (!TextUtils.isEmpty(uuid)) {
            IMessage m = db.getMessage(uuid);
            if (m == null) {
                return;
            }

            if ((m.flags & MessageFlag.MESSAGE_FLAG_FAILURE) != 0 || (m.flags & MessageFlag.MESSAGE_FLAG_ACK) == 0) {
                m.flags = m.flags & (~MessageFlag.MESSAGE_FLAG_FAILURE);
                m.flags = m.flags | MessageFlag.MESSAGE_FLAG_ACK;
                db.updateFlag(m.msgLocalID, m.flags);
            }
        }
    }
    @Override
    public boolean handleMessages(List<IMMessage> msgs) {
        GroupMessageDB db = GroupMessageDB.getInstance();

        ArrayList<IMessage> imsgs = new ArrayList<>();
        ArrayList<IMMessage> insertedMsgs = new ArrayList<>();
        ArrayList<IMessage> revokeMsgs = new ArrayList<>();
        for (IMMessage msg : msgs) {
            IMessage imsg = new IMessage();
            imsg.sender = msg.sender;
            imsg.receiver = msg.receiver;
            imsg.timestamp = msg.timestamp;

            if (msg.isGroupNotification) {
                assert(msg.sender == 0);
                GroupNotification groupNotification = GroupNotification.newGroupNotification(msg.content);
                imsg.receiver = groupNotification.groupID;
                imsg.timestamp = groupNotification.timestamp;
                msg.receiver = groupNotification.groupID;
                msg.timestamp = groupNotification.timestamp;
                imsg.setContent(groupNotification);
            } else {
                imsg.setContent(msg.content);
            }

            if (msg.sender == this.uid) {
                imsg.flags = MessageFlag.MESSAGE_FLAG_ACK;
            }

            if (msg.isSelf) {
                assert(msg.sender == uid);
                repairFailureMessage(imsg.getUUID());
            } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                revokeMsgs.add(imsg);
            } else {
                imsgs.add(imsg);
                insertedMsgs.add(msg);
            }
        }

        if (imsgs.size() > 0) {
            db.insertMessages(imsgs);
        }

        for (int i = 0; i < insertedMsgs.size(); i++) {
            IMMessage msg = insertedMsgs.get(i);
            IMessage m = imsgs.get(i);
            msg.msgLocalID = m.msgLocalID;
        }

        for (int i = 0; i < revokeMsgs.size(); i++) {
            IMessage imsg = revokeMsgs.get(i);
            Revoke revoke = (Revoke) imsg.content;
            long msgLocalID = db.getMessageId(revoke.msgid);
            if (msgLocalID > 0) {
                db.updateContent(msgLocalID, imsg.content.getRaw());
                db.removeMessageIndex(msgLocalID, imsg.receiver);
            }
        }

        return true;
    }

    public boolean handleMessageACK(IMMessage im, int error) {
        long msgLocalID = im.msgLocalID;
        long gid = im.receiver;
        GroupMessageDB db = GroupMessageDB.getInstance();
        if (error == MessageACK.MESSAGE_ACK_SUCCESS) {
            if (msgLocalID == 0) {
                MessageContent c = IMessage.fromRaw(im.content);
                if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                    Revoke r = (Revoke) c;
                    long revokedMsgId = db.getMessageId(r.msgid);
                    if (revokedMsgId > 0) {
                        db.updateContent(revokedMsgId, im.content);
                        db.removeMessageIndex(revokedMsgId, gid);
                    }
                }
                return true;
            } else {
                return db.acknowledgeMessage(msgLocalID);
            }
        } else {
            if (msgLocalID > 0) {
                return db.markMessageFailure(msgLocalID);
            }
            return true;
        }
    }

    public boolean handleMessageFailure(IMMessage im) {
        long msgLocalID = im.msgLocalID;
         if (msgLocalID > 0) {
            GroupMessageDB db = GroupMessageDB.getInstance();
            return db.markMessageFailure(msgLocalID);
        }
        return true;
    }
}
