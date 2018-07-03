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
public class IPeerMessageDB implements IMessageDB {

    public static final int PAGE_SIZE = 10;

    public long currentUID;
    public long peerUID;

    private boolean secret;

    protected HashMap<Integer, Attachment> attachments = new HashMap<Integer, Attachment>();

    private SQLPeerMessageDB db;

    public IPeerMessageDB(boolean secret) {
        this.secret = secret;
        if (secret) {
            db = EPeerMessageDB.getInstance();
        } else {
            db = PeerMessageDB.getInstance();
        }
    }

    public ArrayList<IMessage> loadConversationData() {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = db.newMessageIterator(peerUID);
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
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }

        return messages;
    }

    public List<IMessage> loadConversationData(int messageID) {
        HashSet<String> uuidSet = new HashSet<String>();
        ArrayList<IMessage> messages = new ArrayList<IMessage>();

        int pageSize;
        int count = 0;
        MessageIterator iter;

        iter = db.newMiddleMessageIterator(peerUID, messageID);
        pageSize = 2*PAGE_SIZE;

        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            //不加载重复的消息
            if (!TextUtils.isEmpty(msg.getUUID()) && uuidSet.contains(msg.getUUID())) {
                continue;
            }

            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
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
        MessageIterator iter = db.newMessageIterator(peerUID, messageID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
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
        MessageIterator iter = db.newBackwardMessageIterator(peerUID, messageID);
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
        if (PeerMessageDB.SQL_ENGINE_DB) {
            Location loc = (Location)msg.content;
            loc = Location.newLocation(loc.latitude, loc.longitude, address);
            PeerMessageDB.getInstance().updateContent(msg.msgLocalID, loc.getRaw());
        } else {
            IMessage attachment = new IMessage();
            attachment.content = Attachment.newAttachment(msg.msgLocalID, address);
            attachment.sender = msg.sender;
            attachment.receiver = msg.receiver;
            saveMessage(attachment);
        }
    }

    public void saveMessage(IMessage imsg) {
        db.insertMessage(imsg, peerUID);
    }

    public void removeMessage(IMessage imsg) {
        db.removeMessage(imsg.msgLocalID, peerUID);
    }

    public void markMessageListened(IMessage imsg) {
        db.markMessageListened(imsg.msgLocalID, peerUID);
    }

    public void markMessageFailure(IMessage imsg) {
        db.markMessageFailure(imsg.msgLocalID, peerUID);
    }

    public void eraseMessageFailure(IMessage imsg) {
        db.eraseMessageFailure(imsg.msgLocalID, peerUID);
    }

    public void clearConversation() {
        db.clearCoversation(this.peerUID);
    }

    public IMessage newOutMessage() {
        IMessage msg = new IMessage();
        msg.sender = this.currentUID;
        msg.receiver = this.peerUID;
        msg.secret = secret;
        return msg;
    }
}
