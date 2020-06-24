/*
  Copyright (c) 2014-2019, GoBelieve
    All rights reserved.

  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.outbox;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.*;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;


/**
 * Created by houxh on 14-12-3.
 */
public class GroupOutbox extends Outbox{
    private static GroupOutbox instance = new GroupOutbox();
    public static GroupOutbox getInstance() {
        return instance;
    }

    @Override
    protected void sendRawMessage(IMessage imsg, String raw) {
        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = raw;
        IMService im = IMService.getInstance();
        im.sendGroupMessageAsync(msg);
    }

    @Override
    protected void updateMessageContent(long id, String content) {
        GroupMessageDB.getInstance().updateContent(id, content);
    }

    @Override
    protected void markMessageFailure(IMessage msg) {
        GroupMessageDB.getInstance().markMessageFailure(msg.msgLocalID);
    }
}
