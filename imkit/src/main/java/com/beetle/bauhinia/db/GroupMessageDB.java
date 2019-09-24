package com.beetle.bauhinia.db;

import com.beetle.bauhinia.db.message.Location;

/**
 * Created by houxh on 15/3/21.
 * FileGroupMessageDB vs SQLGroupMessageDB
 */
public class GroupMessageDB extends SQLGroupMessageDB implements IMessageDB {
    public static final boolean SQL_ENGINE_DB = true;

    private static GroupMessageDB instance = new GroupMessageDB();

    public static GroupMessageDB getInstance() {
        return instance;
    }



    public void saveMessageAttachment(IMessage msg, String address) {
        Location loc = (Location)msg.content;
        loc = Location.newLocation(loc.latitude, loc.longitude, address);
        this.updateContent(msg.msgLocalID, loc.getRaw());
    }

    public void saveMessage(IMessage imsg) {
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
