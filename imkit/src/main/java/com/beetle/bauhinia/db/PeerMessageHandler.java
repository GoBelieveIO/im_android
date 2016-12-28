package com.beetle.bauhinia.db;

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
        PeerMessageDB db = PeerMessageDB.getInstance();
        IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        if (this.uid == msg.sender) {
            imsg.flags = MessageFlag.MESSAGE_FLAG_ACK;
        }

        long uid = this.uid == msg.sender ? msg.receiver : msg.sender;
        boolean r = db.insertMessage(imsg, uid);
        msg.msgLocalID = imsg.msgLocalID;
        return r;
    }

    public boolean handleMessageACK(int msgLocalID, long uid) {
        PeerMessageDB db = PeerMessageDB.getInstance();
        return db.acknowledgeMessage(msgLocalID, uid);
    }

    public boolean handleMessageFailure(int msgLocalID, long uid) {
        PeerMessageDB db = PeerMessageDB.getInstance();
        return db.markMessageFailure(msgLocalID, uid);
    }
}
