package com.beetle.bauhinia.db;
import com.beetle.im.IMMessage;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerServiceMessageHandler implements com.beetle.im.CustomerServiceHandler {

    private static CustomerServiceMessageHandler instance = new CustomerServiceMessageHandler();

    public static CustomerServiceMessageHandler getInstance() {
        return instance;
    }

    @Override
    public boolean handleMessage(IMMessage msg, long uid) {
        CustomerServiceMessageDB db = CustomerServiceMessageDB.getInstance();
        IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        boolean r = db.insertMessage(imsg, 0);
        msg.msgLocalID = imsg.msgLocalID;
        return r;
    }

    @Override
    public boolean handleMessageACK(int msgLocalID, long uid) {
        CustomerServiceMessageDB db = CustomerServiceMessageDB.getInstance();
        return db.acknowledgeMessage(msgLocalID, uid);
    }

    @Override
    public boolean handleMessageFailure(int msgLocalID, long uid) {
        CustomerServiceMessageDB db = CustomerServiceMessageDB.getInstance();
        return db.markMessageFailure(msgLocalID, uid);
    }
}
