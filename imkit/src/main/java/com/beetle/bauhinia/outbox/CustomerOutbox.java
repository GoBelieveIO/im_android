/*
  Copyright (c) 2014-2019, GoBelieve
    All rights reserved.

  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.outbox;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.*;
import com.beetle.im.CustomerMessage;
import com.beetle.im.IMService;


/**
 * Created by houxh on 16/1/18.
 */
public class CustomerOutbox extends Outbox {
    private static CustomerOutbox instance = new CustomerOutbox();
    public static CustomerOutbox getInstance() {
        return instance;
    }

    @Override
    protected void updateMessageContent(long id, String content) {
        CustomerMessageDB.getInstance().updateContent(id, content);
    }

    @Override
    protected void markMessageFailure(IMessage msg) {
        CustomerMessageDB.getInstance().markMessageFailure(msg.msgLocalID);
    }

    @Override
    protected void sendRawMessage(IMessage imsg, String raw) {
        ICustomerMessage cm = (ICustomerMessage)imsg;

        CustomerMessage msg = new CustomerMessage();
        msg.msgLocalID = imsg.msgLocalID;
        msg.senderAppID = cm.senderAppID;
        msg.sender = cm.sender;
        msg.receiverAppID = cm.receiverAppID;
        msg.receiver = cm.receiver;
        msg.content = raw;

        IMService im = IMService.getInstance();
        im.sendCustomerMessageAsync(msg);
    }


}
