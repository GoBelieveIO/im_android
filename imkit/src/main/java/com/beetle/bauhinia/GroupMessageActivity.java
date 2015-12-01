package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.tools.AudioDownloader;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.Outbox;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.LoginPoint;

import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by houxh on 15/3/21.
 */
public class GroupMessageActivity extends MessageActivity implements IMServiceObserver {

    public static final String SEND_MESSAGE_NAME = "send_group_message";
    public static final String CLEAR_MESSAGES = "clear_group_messages";

    private final int PAGE_SIZE = 10;

    protected long currentUID;
    protected long groupID;
    protected String groupName;

    public GroupMessageActivity() {
        super();
        sendNotificationName = SEND_MESSAGE_NAME;
        clearNotificationName = CLEAR_MESSAGES;
        isShowUserName = true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        this.sender = currentUID;
        this.receiver = groupID;

        this.loadConversationData();
        titleView.setText(groupName);

        IMService.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");
        IMService.getInstance().removeObserver(this);
    }

    protected String getUserName(long uid) {
        return "...";
    }

    private void loadUserName(long uid) {
        if (names.containsKey(uid)) {
            return;
        }

        String name = getUserName(uid);
        if (!TextUtils.isEmpty(name)) {
            names.put(uid, name);
        }
    }

    protected void loadConversationData() {
        messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = GroupMessageDB.getInstance().newMessageIterator(groupID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                loadUserName(msg.sender);
                updateNotificationDesc(msg);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        downloadMessageContent(messages, count);
    }

    protected void loadEarlierData() {
        if (messages.size() == 0) {
            return;
        }

        IMessage firsMsg = messages.get(0);
        int count = 0;
        MessageIterator iter = GroupMessageDB.getInstance().newMessageIterator(groupID, firsMsg.msgLocalID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment) msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                loadUserName(msg.sender);
                updateNotificationDesc(msg);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        if (count > 0) {
            downloadMessageContent(messages, count);
            adapter.notifyDataSetChanged();
            listview.setSelection(count);
        }
    }


    public void onConnectState(IMService.ConnectState state) {
        if (state == IMService.ConnectState.STATE_CONNECTED) {
            enableSend();
        } else {
            disableSend();
        }
        setSubtitle();
    }

    public void onLoginPoint(LoginPoint lp) {

    }

    public void onPeerInputting(long uid) {

    }

    public void onPeerMessage(IMMessage msg) {

    }
    private IMessage findMessage(int msgLocalID) {
        for (IMessage imsg : messages) {
            if (imsg.msgLocalID == msgLocalID) {
                return imsg;
            }
        }
        return null;
    }

    public void onPeerMessageACK(int msgLocalID, long uid) {

    }

    public void onPeerMessageFailure(int msgLocalID, long uid) {

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

        loadUserName(imsg.sender);

        downloadMessageContent(imsg);
        insertMessage(imsg);
    }

    public void onGroupMessageACK(int msgLocalID, long gid) {
        if (gid != groupID) {
            return;
        }
        Log.i(TAG, "message ack");

        IMessage imsg = findMessage(msgLocalID);
        if (imsg == null) {
            Log.i(TAG, "can't find msg:" + msgLocalID);
            return;
        }
        imsg.setAck(true);
    }

    public void onGroupMessageFailure(int msgLocalID, long gid) {
        if (gid != groupID) {
            return;
        }
        Log.i(TAG, "message failure");

        IMessage imsg = findMessage(msgLocalID);
        if (imsg == null) {
            Log.i(TAG, "can't find msg:" + msgLocalID);
            return;
        }
        imsg.setFailure(true);
    }

    public void onGroupNotification(String text) {
        IMessage.GroupNotification notification = IMessage.newGroupNotification(text);

        if (notification.groupID != groupID) {
            return;
        }

        IMessage imsg = new IMessage();
        imsg.sender = 0;
        imsg.receiver = groupID;
        imsg.timestamp = now();
        imsg.setContent(notification);

        updateNotificationDesc(imsg);

        insertMessage(imsg);
    }

    private void updateNotificationDesc(IMessage imsg) {
        if (imsg.content.getType() != IMessage.MessageType.MESSAGE_GROUP_NOTIFICATION) {
            return;
        }

        IMessage.GroupNotification notification = (IMessage.GroupNotification)imsg.content;
        if (notification.type == IMessage.GroupNotification.NOTIFICATION_GROUP_CREATED) {
            if (notification.master == currentUID) {
                notification.description = String.format("您创建了\"%s\"群组", notification.groupName);
            } else {
                notification.description = String.format("您加入了\"%s\"群组", notification.groupName);
            }
        } else if (notification.type == IMessage.GroupNotification.NOTIFICATION_GROUP_DISBAND) {
            notification.description = "群组已解散";
        } else if (notification.type == IMessage.GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED) {
            notification.description = String.format("\"%s\"加入群", getUserName(notification.member));
        } else if (notification.type == IMessage.GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED) {
            notification.description = String.format("\"%s\"离开群", getUserName(notification.member));
        }
    }



    void sendMessage(IMessage imsg) {
        if (imsg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
            Outbox ob = Outbox.getInstance();
            IMessage.Audio audio = (IMessage.Audio)imsg.content;
            ob.uploadGroupAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
        } else if (imsg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
            IMessage.Image image = (IMessage.Image)imsg.content;
            //prefix:"file:"
            String path = image.image.substring(5);
            Outbox.getInstance().uploadGroupImage(imsg, path);
        } else {
            IMMessage msg = new IMMessage();
            msg.sender = imsg.sender;
            msg.receiver = imsg.receiver;
            msg.content = imsg.content.getRaw();
            msg.msgLocalID = imsg.msgLocalID;
            IMService im = IMService.getInstance();
            im.sendGroupMessage(msg);
        }
    }

    void saveMessage(IMessage imsg) {
        GroupMessageDB.getInstance().insertMessage(imsg, imsg.receiver);
    }

    void markMessageFailure(IMessage imsg) {
        long cid = 0;
        if (imsg.sender == this.currentUID) {
            cid = imsg.receiver;
        } else {
            cid = imsg.sender;
        }
        GroupMessageDB.getInstance().markMessageFailure(imsg.msgLocalID, cid);
    }

    void eraseMessageFailure(IMessage imsg) {
        long cid = 0;
        if (imsg.sender == this.currentUID) {
            cid = imsg.receiver;
        } else {
            cid = imsg.sender;
        }
        GroupMessageDB.getInstance().eraseMessageFailure(imsg.msgLocalID, cid);
    }

    void clearConversation() {
        GroupMessageDB db = GroupMessageDB.getInstance();
        db.clearCoversation(this.groupID);
    }

}
