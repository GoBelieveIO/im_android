package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.db.message.GroupNotification;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.bauhinia.tools.FileDownloader;
import com.beetle.bauhinia.outbox.GroupOutbox;
import com.beetle.im.GroupMessageObserver;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.MessageACK;

import java.util.List;


/**
 * Created by houxh on 15/3/21.
 */
public class GroupMessageActivity extends MessageActivity implements
        IMServiceObserver, GroupMessageObserver {
    protected long groupID;
    protected String groupName;

    public GroupMessageActivity() {
        super();
        isShowUserName = true;
        isShowReaded = false;
        isShowReply = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        items[ITEM_VIDEO_CALL_ID] = false;
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        currentUID = intent.getLongExtra("current_uid", 0);
        if (currentUID == 0) {
            Log.e(TAG, "current uid is 0");
            return;
        }
        groupID = intent.getLongExtra("group_id", 0);
        if (groupID == 0) {
            Log.e(TAG, "peer uid is 0");
            return;
        }
        groupName = intent.getStringExtra("group_name");
        if (groupName == null) {
            Log.e(TAG, "peer name is null");
            return;
        }

        messageID = intent.getIntExtra("message_id", 0);
        this.conversationID = groupID;

        getSupportActionBar().setTitle(groupName);

        this.messageDB = GroupMessageDB.getInstance();

        this.hasLateMore = this.messageID > 0;
        this.hasEarlierMore = true;
        this.loadData();

        if (this.messages.size() > 0) {
            if (messageID > 0) {
                int index = -1;
                for (int i = 0; i < this.messages.size(); i++) {
                    if (messageID == this.messages.get(i).msgLocalID) {
                        index = i;
                        break;
                    }
                }

                if (index != -1) {
                    listview.setSelection(index);
                }
            } else {
                //显示最后一条消息
                listview.setSelection(this.messages.size() - 1);
            }
        }

        GroupOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addGroupObserver(this);
        FileDownloader.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");

        GroupOutbox.getInstance().removeObserver(this);
        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removeGroupObserver(this);
        FileDownloader.getInstance().removeObserver(this);
    }

    @Override
    protected MessageIterator getMessageIterator() {
        return GroupMessageDB.getInstance().newMessageIterator(groupID);
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {
        if (state == IMService.ConnectState.STATE_CONNECTED) {
            enableSend();
        } else {
            disableSend();
        }
    }

    @Override
    public void onGroupMessages(List<IMMessage> msgs) {
        for (IMMessage msg : msgs) {
            if (msg.isGroupNotification) {
                assert(msg.sender == 0);
                this.onGroupNotification(msg.content);
            } else {
                this.onGroupMessage(msg);
            }
        }

    }

    public void onGroupMessage(IMMessage msg) {
        if (msg.receiver != groupID) {
            return;
        }
        Log.i(TAG, "recv msg:" + msg.content);
        final IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        imsg.isOutgoing = (msg.sender == this.currentUID);
        if (imsg.isOutgoing) {
            imsg.flags |= MessageFlag.MESSAGE_FLAG_ACK;
        }

        IMessage mm = findMessage(imsg.getUUID());
        if (mm != null) {
            Log.i(TAG, "receive repeat message:" + imsg.getUUID());
            if (imsg.isOutgoing) {
                int flags = imsg.flags;
                flags = flags & ~MessageFlag.MESSAGE_FLAG_FAILURE;
                flags = flags | MessageFlag.MESSAGE_FLAG_ACK;
                mm.setFlags(flags);
            }
            return;
        }

        if (msg.isSelf) {
            return;
        }

        loadUserName(imsg);

        downloadMessageContent(imsg);
        updateNotificationDesc(imsg);
        if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke)imsg.content;
            IMessage m = findMessage(revoke.msgid);
            if (m != null) {
                replaceMessage(m, imsg);
            }
        } else {
            insertMessage(imsg);
        }
    }

    @Override
    public void onGroupMessageACK(IMMessage im, int error) {
        long msgLocalID = im.msgLocalID;
        long gid = im.receiver;
        if (gid != groupID) {
            return;
        }
        Log.i(TAG, "message ack");

        if (error == MessageACK.MESSAGE_ACK_SUCCESS) {
            if (msgLocalID > 0) {
                IMessage imsg = findMessage(msgLocalID);
                if (imsg == null) {
                    Log.i(TAG, "can't find msg:" + msgLocalID);
                    return;
                }
                imsg.setAck(true);
            } else {
                MessageContent c = IMessage.fromRaw(im.content);
                if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                    Revoke r = (Revoke) c;
                    IMessage imsg = findMessage(r.msgid);
                    if (imsg == null) {
                        Log.i(TAG, "can't find msg:" + msgLocalID);
                        return;
                    }
                    imsg.setContent(r);
                    updateNotificationDesc(imsg);
                    adapter.notifyDataSetChanged();
                }
            }
        } else {
            if (msgLocalID > 0) {
                IMessage imsg = findMessage(msgLocalID);
                if (imsg == null) {
                    Log.i(TAG, "can't find msg:" + msgLocalID);
                    return;
                }
                imsg.setFailure(true);
            } else {
                MessageContent c = IMessage.fromRaw(im.content);
                if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                    Toast.makeText(this, "撤回失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onGroupMessageFailure(IMMessage im) {
        long msgLocalID = im.msgLocalID;
        long gid = im.receiver;
        if (gid != groupID) {
            return;
        }
        Log.i(TAG, "message failure");

        if (msgLocalID > 0) {
            IMessage imsg = findMessage(msgLocalID);
            if (imsg == null) {
                Log.i(TAG, "can't find msg:" + msgLocalID);
                return;
            }
            imsg.setFailure(true);
        } else {
            MessageContent c = IMessage.fromRaw(im.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Toast.makeText(this, "撤回失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onGroupNotification(String text) {
        GroupNotification notification = GroupNotification.newGroupNotification(text);

        if (notification.groupID != groupID) {
            return;
        }

        IMessage imsg = new IMessage();
        imsg.sender = 0;
        imsg.receiver = groupID;
        imsg.timestamp = notification.timestamp;
        imsg.setContent(notification);

        updateNotificationDesc(imsg);

        if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED) {
            this.groupName = notification.groupName;
            getSupportActionBar().setTitle(groupName);
        }
        insertMessage(imsg);
    }


    @Override
    protected void sendMessage(IMessage imsg) {
        GroupOutbox.getInstance().sendMessage(imsg);
    }


    @Override
    protected void clearConversation() {
        super.clearConversation();
        GroupMessageDB db = GroupMessageDB.getInstance();
        db.clearConversation(this.groupID);
    }

    @Override
    protected IMessage newOutMessage(MessageContent content) {
        IMessage msg = new IMessage();
        msg.sender = currentUID;
        msg.receiver = groupID;
        msg.receiverCount = 0;
        msg.content = content;
        return msg;
    }
}
