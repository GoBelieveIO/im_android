package com.beetle.bauhinia.db;
import com.beetle.im.CustomerMessage;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerMessageHandler implements com.beetle.im.CustomerMessageHandler {

    private static CustomerMessageHandler instance = new CustomerMessageHandler();

    public static CustomerMessageHandler getInstance() {
        return instance;
    }

    @Override
    public boolean handleMessage(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        boolean r = db.insertMessage(imsg, msg.customer);
        msg.msgLocalID = imsg.msgLocalID;
        return r;
    }

    @Override
    public boolean handleMessageACK(int msgLocalID, long uid) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        return db.acknowledgeMessage(msgLocalID, uid);
    }

    @Override
    public boolean handleMessageFailure(int msgLocalID, long uid) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        return db.markMessageFailure(msgLocalID, uid);
    }
}
