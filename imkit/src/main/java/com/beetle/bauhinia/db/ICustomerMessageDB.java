package com.beetle.bauhinia.db;
import android.text.TextUtils;
import com.beetle.bauhinia.db.message.Attachment;
import com.beetle.bauhinia.db.message.Location;
import com.beetle.bauhinia.db.message.MessageContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by houxh on 2017/11/13.
 */

public class ICustomerMessageDB implements IMessageDB {
    public static final int PAGE_SIZE = 10;
    public long currentUID;
    public long storeID;
    public long appID;
    public long sellerID;

    protected HashMap<Integer, Attachment> attachments = new HashMap<Integer, Attachment>();

    public List<IMessage> loadConversationData() {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = CustomerMessageDB.getInstance().newMessageIterator(storeID);
        while (iter != null) {
            ICustomerMessage msg = (ICustomerMessage)iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment) msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                msg.isOutgoing = !msg.isSupport;
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        return messages;
    }

    public List<IMessage> loadConversationData(int messageID) {
        return null;
    }

    public List<IMessage> loadLateData(int messageID) {
        return null;
    }

    public List<IMessage> loadEarlierData(int messageID) {

        ArrayList<IMessage> messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = CustomerMessageDB.getInstance().newMessageIterator(storeID, messageID);
        while (iter != null) {
            ICustomerMessage msg = (ICustomerMessage)iter.next();
            if (msg == null) {
                break;
            }


            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
                msg.isOutgoing = !msg.isSupport;
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        return messages;
    }


    public void saveMessageAttachment(IMessage msg, String address) {
        if (CustomerMessageDB.SQL_ENGINE_DB) {
            Location loc = (Location)msg.content;
            loc = Location.newLocation(loc.latitude, loc.longitude, address);
            CustomerMessageDB.getInstance().updateContent(msg.msgLocalID, loc.getRaw());
        } else {
            ICustomerMessage attachment = new ICustomerMessage();
            attachment.content = Attachment.newAttachment(msg.msgLocalID, address);
            attachment.sender = msg.sender;
            attachment.receiver = msg.receiver;
            saveMessage(attachment);
        }
    }

    public void saveMessage(IMessage imsg) {
        CustomerMessageDB.getInstance().insertMessage(imsg, storeID);
    }

    public void removeMessage(IMessage imsg) {
        CustomerMessageDB.getInstance().removeMessage(imsg.msgLocalID, storeID);
    }

    public void markMessageListened(IMessage imsg) {
        PeerMessageDB.getInstance().markMessageListened(imsg.msgLocalID, storeID);
    }

    public void markMessageFailure(IMessage imsg) {
        CustomerMessageDB.getInstance().markMessageFailure(imsg.msgLocalID, storeID);
    }


    public void eraseMessageFailure(IMessage imsg) {
        CustomerMessageDB.getInstance().eraseMessageFailure(imsg.msgLocalID, storeID);
    }

    public void clearConversation() {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        db.clearCoversation(this.storeID);
    }

    public IMessage newOutMessage() {
        ICustomerMessage msg = new ICustomerMessage();
        msg.customerAppID = appID;
        msg.customerID = currentUID;
        msg.storeID = storeID;
        msg.sellerID = sellerID;
        return msg;
    }
}
