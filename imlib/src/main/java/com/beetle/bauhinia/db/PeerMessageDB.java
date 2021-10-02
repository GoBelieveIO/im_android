package com.beetle.bauhinia.db;
import com.beetle.bauhinia.db.message.Location;

class BasePeerMessageDB extends SQLPeerMessageDB implements IMessageDB {

    public boolean clearConversation(String conversationID) {
        long peer = Long.parseLong(conversationID);
        return clearConversation(peer);
    }

    public void saveMessageAttachment(IMessage msg, String address) {
        Location loc = (Location)msg.content;
        loc = Location.newLocation(loc.latitude, loc.longitude, address);
        this.updateContent(msg.msgLocalID, loc.getRaw());
    }

    public void saveMessage(IMessage imsg) {
        assert(imsg.isOutgoing);
        this.insertMessage(imsg, imsg.receiver);
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


public class PeerMessageDB extends BasePeerMessageDB {
    private static PeerMessageDB instance = new PeerMessageDB();

    public static PeerMessageDB getInstance() {
        return instance;
    }


    PeerMessageDB() {
        secret = 0;
    }
}