package com.beetle.bauhinia.db;

import com.beetle.bauhinia.db.message.Location;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerPeerMessageDB extends SQLCustomerMessageDB implements IMessageDB {
    private static CustomerPeerMessageDB instance = new CustomerPeerMessageDB();

    public static CustomerPeerMessageDB getInstance() {
        return instance;
    }


    private static class PeerId {
        public long appid;
        public long uid;
    }

    PeerId parsePeerId(String conversationID) {
        PeerId id = new PeerId();
        int p = conversationID.indexOf('_');
        if (p != -1) {
            String p1 = conversationID.substring(0, p);
            String p2= conversationID.substring(p + 1);
            id.appid = Long.parseLong(p1);
            id.uid = Long.parseLong(p2);
        }
        return id;

    }
    //获取最近的消息
    public MessageIterator newMessageIterator(String conversationID) {
        PeerId id = parsePeerId(conversationID);
        return newCustomerPeerMessageIterator(id.appid, id.uid);
    }
    //获取之前的消息
    public MessageIterator newForwardMessageIterator(String conversationID, long firstMsgID) {
        PeerId id = parsePeerId(conversationID);
        return newCustomerPeerForwardMessageIterator(id.appid, id.uid, firstMsgID);
    }
    //获取之后的消息
    public MessageIterator newBackwardMessageIterator(String conversationID, long msgID) {
        PeerId id = parsePeerId(conversationID);
        return newCustomerPeerBackwardMessageIterator(id.appid, id.uid, msgID);
    }
    //获取前后的消息
    public MessageIterator newMiddleMessageIterator(String conversationID, long msgID) {
        PeerId id = parsePeerId(conversationID);
        return newCustomerPeerMiddleMessageIterator(id.appid, id.uid, msgID);
    }

    public boolean clearConversation(String conversationID) {
        PeerId id = parsePeerId(conversationID);
        return clearConversation(id.appid, id.uid);
    }

    public void saveMessageAttachment(IMessage msg, String address) {
        Location loc = (Location)msg.content;
        loc = Location.newLocation(loc.latitude, loc.longitude, address);
        this.updateContent(msg.msgLocalID, loc.getRaw());
    }

    public void saveMessage(IMessage imsg) {
        assert(imsg.isOutgoing);
        ICustomerMessage m = (ICustomerMessage)imsg;
        this.insertMessage(imsg, m.receiverAppID, m.receiver);
    }

    public void removeMessage(IMessage imsg) {
        this.removeMessage(imsg.msgLocalID);
    }


    public void markMessageListened(IMessage imsg) {
        this.markMessageListened(imsg.msgLocalID);
    }


    public void markMessageFailure(IMessage imsg) {
        this.markMessageFailure(imsg.msgLocalID);
    }


    public void eraseMessageFailure(IMessage imsg) {
        this.eraseMessageFailure(imsg.msgLocalID);
    }


}
