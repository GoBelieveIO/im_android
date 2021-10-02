package com.beetle.bauhinia.handler;
import android.text.TextUtils;

import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.im.CustomerMessage;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerMessageHandler implements com.beetle.im.CustomerMessageHandler {

    private static CustomerMessageHandler instance = new CustomerMessageHandler();

    public static CustomerMessageHandler getInstance() {
        return instance;
    }

    //当前用户id
    private long uid;
    private long appid;
    public void setUID(long uid) {
        this.uid = uid;
    }
    public void setAppId(long appid) {
        this.appid = appid;
    }

    private void repairFailureMessage(String uuid) {
        //纠正消息标志位
        CustomerMessageDB db = CustomerMessageDB.getInstance();
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
    public boolean handleMessage(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        ICustomerMessage imsg = new ICustomerMessage();

        imsg.timestamp = msg.timestamp;
        imsg.senderAppID = msg.senderAppID;
        imsg.sender = msg.sender;

        imsg.receiverAppID = msg.receiverAppID;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        long peerAppID;
        long peer;
        if (msg.senderAppID == this.appid && msg.sender == this.uid) {
            imsg.flags = MessageFlag.MESSAGE_FLAG_ACK;
            peerAppID = msg.receiverAppID;
            peer = msg.receiver;
        } else {
            peerAppID = msg.senderAppID;
            peer = msg.sender;
        }

        if (msg.isSelf) {
            repairFailureMessage(imsg.getUUID());
            return true;
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke) imsg.content;
            long msgLocalID = db.getMessageId(revoke.msgid);
            if (msgLocalID > 0) {
                db.removeMessage(msgLocalID);
            }
            return true;
        } else {
            boolean r = db.insertMessage(imsg, peerAppID, peer);
            msg.msgLocalID = imsg.msgLocalID;
            return r;
        }
    }

    @Override
    public boolean handleMessageACK(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        long msgLocalID = msg.msgLocalID;
        if (msgLocalID == 0) {
            MessageContent c = IMessage.fromRaw(msg.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke r = (Revoke)c;
                long revokedMsgId = db.getMessageId(r.msgid);
                if (revokedMsgId > 0) {
                    db.updateContent(revokedMsgId, msg.content);
                    db.removeMessageIndex(revokedMsgId);
                }
            }
            return true;
        } else {
            return db.acknowledgeMessage(msg.msgLocalID);
        }
    }

    @Override
    public boolean handleMessageFailure(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        return db.markMessageFailure(msg.msgLocalID);
    }
}
