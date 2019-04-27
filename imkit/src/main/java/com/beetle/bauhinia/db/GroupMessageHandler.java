/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;

import android.text.TextUtils;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.message.GroupNotification;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.im.IMMessage;

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
        for (IMMessage msg : msgs) {
            IMessage imsg = new IMessage();
            imsg.sender = msg.sender;
            imsg.receiver = msg.receiver;
            imsg.timestamp = msg.timestamp;
            imsg.setContent(msg.content);
            if (msg.sender == this.uid) {
                imsg.flags = MessageFlag.MESSAGE_FLAG_ACK;
            }

            if (msg.isSelf) {
                assert(msg.sender == uid);
                repairFailureMessage(imsg.getUUID());
                continue;
            } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke revoke = (Revoke) imsg.content;
                int msgLocalID = db.getMessageId(revoke.msgid);
                if (msgLocalID > 0) {
                    db.updateContent(msgLocalID, msg.content);
                    db.removeMessageIndex(msgLocalID, imsg.receiver);
                }
                continue;
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

        return true;
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
            return db.acknowledgeMessage(msgLocalID);
        }
    }

    public boolean handleMessageFailure(IMMessage im) {
        int msgLocalID = im.msgLocalID;
        long gid = im.receiver;
        if (msgLocalID > 0) {
            GroupMessageDB db = GroupMessageDB.getInstance();
            return db.markMessageFailure(msgLocalID);
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
