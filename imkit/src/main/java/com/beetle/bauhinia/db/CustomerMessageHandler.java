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
    public boolean handleCustomerSupportMessage(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        ICustomerMessage imsg = new ICustomerMessage();

        imsg.timestamp = msg.timestamp;
        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.sender = msg.customerID;
        imsg.receiver = msg.storeID;
        imsg.isSupport = true;

        imsg.setContent(msg.content);
        boolean r = db.insertMessage(imsg, msg.storeID);
        msg.msgLocalID = imsg.msgLocalID;
        return r;
    }

    @Override
    public boolean handleMessage(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        ICustomerMessage imsg = new ICustomerMessage();

        imsg.timestamp = msg.timestamp;
        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.sender = msg.customerID;
        imsg.receiver = msg.storeID;
        imsg.isSupport = false;

        imsg.setContent(msg.content);
        boolean r = db.insertMessage(imsg, msg.storeID);
        msg.msgLocalID = imsg.msgLocalID;
        return r;
    }

    @Override
    public boolean handleMessageACK(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        return db.acknowledgeMessage(msg.msgLocalID, msg.storeID);
    }

    @Override
    public boolean handleMessageFailure(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        return db.markMessageFailure(msg.msgLocalID, msg.storeID);
    }
}
