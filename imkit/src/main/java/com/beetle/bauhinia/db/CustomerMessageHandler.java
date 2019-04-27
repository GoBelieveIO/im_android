/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db;
import android.text.TextUtils;

import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.im.CustomerMessage;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerMessageHandler implements com.beetle.im.CustomerMessageHandler {

    private static CustomerMessageHandler instance = new CustomerMessageHandler();

    public static CustomerMessageHandler getInstance() {
        return instance;
    }

    private void repairFailureMessage(String uuid) {
        //纠正消息标志位
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        if (!TextUtils.isEmpty(uuid)) {
            IMessage m = db.getMessage(uuid);
            if (m == null) {
                return;
            }

            if ((m.flags & MessageFlag.MESSAGE_FLAG_FAILURE) != 0 || (m.flags & MessageFlag.MESSAGE_FLAG_ACK) == 0) {
                m.flags = m.flags & (~MessageFlag.MESSAGE_FLAG_FAILURE);
                m.flags = m.flags | MessageFlag.MESSAGE_FLAG_ACK;
                db.updateFlag(m.msgLocalID, m.flags);
            }
        }
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

        if (msg.isSelf) {
            //纠正消息标志位
            repairFailureMessage(imsg.getUUID());
            return true;
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke) imsg.content;
            int msgLocalID = db.getMessageId(revoke.msgid);
            if (msgLocalID > 0) {
                db.updateContent(msgLocalID, msg.content);
                db.removeMessageIndex(msgLocalID);
            }
            return true;
        } else {
            boolean r = db.insertMessage(imsg);
            msg.msgLocalID = imsg.msgLocalID;
            return r;
        }
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


        if (msg.isSelf) {
            repairFailureMessage(imsg.getUUID());
            return true;
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke) imsg.content;
            int msgLocalID = db.getMessageId(revoke.msgid);
            if (msgLocalID > 0) {
                db.removeMessage(msgLocalID);
            }
            return true;
        } else {

            boolean r = db.insertMessage(imsg);
            msg.msgLocalID = imsg.msgLocalID;
            return r;
        }
    }

    @Override
    public boolean handleMessageACK(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        int msgLocalID = msg.msgLocalID;
        if (msgLocalID == 0) {
            MessageContent c = IMessage.fromRaw(msg.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke r = (Revoke)c;
                int revokedMsgId = db.getMessageId(r.msgid);
                if (revokedMsgId > 0) {
                    db.updateContent(revokedMsgId, msg.content);
                    db.removeMessageIndex(revokedMsgId);
                }
            }
            return true;
        } else {
            return db.acknowledgeMessage(msg.msgLocalID);
        }
    }

    @Override
    public boolean handleMessageFailure(CustomerMessage msg) {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        return db.markMessageFailure(msg.msgLocalID);
    }
}
