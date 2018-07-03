package com.beetle.bauhinia.db;

import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.im.IMMessage;



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
        if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke) imsg.content;

            int msgLocalID = db.getMessageId(revoke.msgid);
            if (msgLocalID > 0) {
                db.updateContent(msgLocalID, msg.content);
                db.removeMessageIndex(msgLocalID, uid);
            }
            return true;
        } else {
            boolean r = db.insertMessage(imsg, uid);
            msg.msgLocalID = imsg.msgLocalID;
            return r;
        }

    }

    public boolean handleMessageACK(IMMessage im) {
        long uid = im.receiver;
        int msgLocalID = im.msgLocalID;
        PeerMessageDB db = PeerMessageDB.getInstance();
        if (msgLocalID == 0) {
            MessageContent c = IMessage.fromRaw(im.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke r = (Revoke)c;
                int revokedMsgId = db.getMessageId(r.msgid);
                if (revokedMsgId > 0) {
                    db.updateContent(revokedMsgId, im.content);
                    db.removeMessageIndex(revokedMsgId, uid);
                }
            }
            return true;
        } else {
            return db.acknowledgeMessage(msgLocalID, uid);
        }
    }

    public boolean handleMessageFailure(IMMessage im) {
        long uid = im.receiver;
        int msgLocalID = im.msgLocalID;
        if (msgLocalID > 0) {
            PeerMessageDB db = PeerMessageDB.getInstance();
            return db.markMessageFailure(msgLocalID, uid);
        }
        return true;
    }
}
