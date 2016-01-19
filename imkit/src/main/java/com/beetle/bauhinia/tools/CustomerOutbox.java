package com.beetle.bauhinia.tools;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.im.CustomerMessage;
import com.beetle.im.IMService;



/**
 * Created by houxh on 16/1/18.
 */
public class CustomerOutbox extends Outbox {

    //当前登录用户是否为客服人员,默认为false
    private static boolean isStaff = false;

    public static void setIsStaff(boolean s) {
        isStaff = s;
    }


    private static CustomerOutbox instance = new CustomerOutbox();
    public static CustomerOutbox getInstance() {
        return instance;
    }

    @Override
    protected void markMessageFailure(IMessage msg) {
        CustomerMessageDB.getInstance().markMessageFailure(msg.msgLocalID, msg.receiver);
    }

    @Override
    protected void sendImageMessage(IMessage imsg, String url) {
        CustomerMessage msg = new CustomerMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.content = IMessage.newImage(url).getRaw();
        msg.msgLocalID = imsg.msgLocalID;

        if (isStaff) {
            msg.customer = imsg.receiver;
        } else {
            msg.customer = imsg.sender;
        }

        IMService im = IMService.getInstance();
        im.sendCustomerMessage(msg);
    }

    @Override
    protected void sendAudioMessage(IMessage imsg, String url) {
        IMessage.Audio audio = (IMessage.Audio)imsg.content;

        CustomerMessage msg = new CustomerMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = IMessage.newAudio(url, audio.duration).getRaw();

        if (isStaff) {
            msg.customer = imsg.receiver;
        } else {
            msg.customer = imsg.sender;
        }

        IMService im = IMService.getInstance();
        im.sendCustomerMessage(msg);
    }

}
