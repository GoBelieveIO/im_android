package com.beetle.bauhinia.db;
import com.beetle.bauhinia.db.message.Attachment;
import com.beetle.bauhinia.db.message.Location;
import com.beetle.bauhinia.db.message.MessageContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by houxh on 2017/11/13.
 */

public class IGroupMessageDB implements IMessageDB {

    public static final int PAGE_SIZE = 10;

    public long currentUID;
    public long groupID;

    protected HashMap<Integer, Attachment> attachments = new HashMap<Integer, Attachment>();

    public List<IMessage> loadConversationData() {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();

        int pageSize;
        int count = 0;
        MessageIterator iter;
        iter = GroupMessageDB.getInstance().newMessageIterator(groupID);
        pageSize = PAGE_SIZE;
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= pageSize) {
                    break;
                }
            }
        }
        return messages;
    }

    public List<IMessage> loadConversationData(int messageID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();

        int pageSize;
        int count = 0;
        MessageIterator iter;
        iter = GroupMessageDB.getInstance().newMiddleMessageIterator(groupID, messageID);
        pageSize = 2*PAGE_SIZE;
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= pageSize) {
                    break;
                }
            }
        }
        return messages;

    }


    public List<IMessage> loadEarlierData(int messageID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = GroupMessageDB.getInstance().newMessageIterator(groupID, messageID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }



            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment) msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {

                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        return messages;
    }

    public List<IMessage> loadLateData(int messageID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = GroupMessageDB.getInstance().newBackwardMessageIterator(groupID, messageID);
        while (true) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }

        return messages;
    }


    public void saveMessageAttachment(IMessage msg, String address) {
        if (GroupMessageDB.SQL_ENGINE_DB) {
            Location loc = (Location)msg.content;
            loc = Location.newLocation(loc.latitude, loc.longitude, address);
            GroupMessageDB.getInstance().updateContent(msg.msgLocalID, loc.getRaw());
        } else {
            IMessage attachment = new IMessage();
            attachment.content = Attachment.newAttachment(msg.msgLocalID, address);
            attachment.sender = msg.sender;
            attachment.receiver = msg.receiver;
            saveMessage(attachment);
        }
    }

    public void saveMessage(IMessage imsg) {
        GroupMessageDB.getInstance().insertMessage(imsg, imsg.receiver);
    }

    public void removeMessage(IMessage imsg) {
        GroupMessageDB.getInstance().removeMessage(imsg.msgLocalID, imsg.receiver);
    }

    public void markMessageListened(IMessage imsg) {
        GroupMessageDB.getInstance().markMessageListened(imsg.msgLocalID, groupID);
    }

    public void markMessageFailure(IMessage imsg) {
        GroupMessageDB.getInstance().markMessageFailure(imsg.msgLocalID, groupID);
    }

    public void eraseMessageFailure(IMessage imsg) {
        GroupMessageDB.getInstance().eraseMessageFailure(imsg.msgLocalID, groupID);
    }

    public void clearConversation() {
        GroupMessageDB db = GroupMessageDB.getInstance();
        db.clearCoversation(this.groupID);
    }

    public IMessage newOutMessage() {
        IMessage msg = new IMessage();
        msg.sender = currentUID;
        msg.receiver = groupID;
        return msg;
    }
}
