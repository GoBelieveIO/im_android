/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.outbox;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.message.*;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;


/**
 * Created by houxh on 14-12-3.
 */
public class PeerOutbox extends Outbox {
    private static final String TAG = "goubuli";

    private static PeerOutbox instance = new PeerOutbox();
    public static PeerOutbox getInstance() {
        return instance;
    }



    @Override
    protected void markMessageFailure(IMessage msg) {
        PeerMessageDB.getInstance().markMessageFailure(msg.msgLocalID);
    }

    @Override
    protected void updateMessageContent(long id, String content) {
        PeerMessageDB.getInstance().updateContent(id, content);
    }

    @Override
    protected void sendRawMessage(IMessage imsg, String raw) {
        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = raw;
        msg.plainContent = msg.content;

        boolean r = true;
        if (imsg.secret) {
            r = encrypt(msg, imsg.getUUID());
        }

        if (r) {
            IMService im = IMService.getInstance();
            im.sendPeerMessageAsync(msg);
        }
    }


}
