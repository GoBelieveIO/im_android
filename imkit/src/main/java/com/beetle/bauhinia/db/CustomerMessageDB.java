/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;

import android.util.Log;

import com.beetle.bauhinia.db.message.Location;
import com.beetle.im.BytePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 16/1/18.
 * FileCustomerMessageDB vs SQLCustomerMessageDB
 */
public class CustomerMessageDB extends SQLCustomerMessageDB implements IMessageDB {
    public static final boolean SQL_ENGINE_DB = true;
    
    private static CustomerMessageDB instance = new CustomerMessageDB();

    public static CustomerMessageDB getInstance() {
        return instance;
    }



    public void saveMessageAttachment(IMessage msg, String address) {
        Location loc = (Location)msg.content;
        loc = Location.newLocation(loc.latitude, loc.longitude, address);
        this.updateContent(msg.msgLocalID, loc.getRaw());
    }

    public void saveMessage(IMessage imsg) {
        ICustomerMessage cm = (ICustomerMessage)imsg;
        this.insertMessage(imsg);
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
