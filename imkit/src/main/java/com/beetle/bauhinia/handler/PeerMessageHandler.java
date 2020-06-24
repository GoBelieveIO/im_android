/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.handler;

import android.text.TextUtils;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.message.ACK;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.im.IMMessage;
import com.beetle.im.MessageACK;

import java.util.Date;


/**
 * Created by houxh on 14-7-22.
 */
public class PeerMessageHandler implements com.beetle.im.PeerMessageHandler {
    private static PeerMessageHandler instance = new PeerMessageHandler();

    public static PeerMessageHandler getInstance() {
        return instance;
    }

    //当前用户id
    private long uid;
    public void setUID(long uid) {
        this.uid = uid;
    }

    private void repaireFailureMessage(String uuid) {
        PeerMessageDB db = PeerMessageDB.getInstance();
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

    public boolean handleMessage(IMMessage msg) {
        IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        imsg.secret = false;


        if (this.uid == msg.sender) {
            imsg.flags = MessageFlag.MESSAGE_FLAG_ACK;
        }

        long uid = this.uid == msg.sender ? msg.receiver : msg.sender;

        PeerMessageDB db = PeerMessageDB.getInstance();
        if (msg.isSelf) {
            assert (msg.sender == uid);
            //消息由本设备发出，则不需要重新入库，用于纠正消息标志位
            repaireFailureMessage(imsg.getUUID());
            return true;
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke) imsg.content;

            long msgLocalID = db.getMessageId(revoke.msgid);
            if (msgLocalID > 0) {
                db.updateContent(msgLocalID, msg.content);
                db.removeMessageIndex(msgLocalID);
            }
            return true;
        } else {
            boolean r = db.insertMessage(imsg, uid);
            msg.msgLocalID = imsg.msgLocalID;
            return r;
        }

    }

    public boolean handleMessageACK(IMMessage im, int error) {
        long msgLocalID = im.msgLocalID;
        PeerMessageDB db = PeerMessageDB.getInstance();

        if (error == MessageACK.MESSAGE_ACK_SUCCESS) {
            if (msgLocalID == 0) {
                MessageContent c = IMessage.fromRaw(im.plainContent);
                if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                    Revoke r = (Revoke) c;
                    long revokedMsgId = db.getMessageId(r.msgid);
                    if (revokedMsgId > 0) {
                        db.updateContent(revokedMsgId, im.plainContent);
                        db.removeMessageIndex(revokedMsgId);
                    }
                }
                return true;
            } else {
                return db.acknowledgeMessage(msgLocalID);
            }
        } else {
            IMessage ack = new IMessage();
            ack.sender = 0;
            ack.receiver = im.sender;
            ack.timestamp = now();
            ack.setContent(ACK.newACK(error));
            db.insertMessage(ack, im.receiver);
            if (msgLocalID > 0) {
                return db.markMessageFailure(msgLocalID);
            }
            return true;
        }
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    public boolean handleMessageFailure(IMMessage im) {
        long msgLocalID = im.msgLocalID;
        if (msgLocalID > 0) {
            PeerMessageDB db = PeerMessageDB.getInstance();
            return db.markMessageFailure(msgLocalID);
        }
        return true;
    }
}
