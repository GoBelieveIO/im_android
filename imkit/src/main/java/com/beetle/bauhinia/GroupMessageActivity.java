package com.beetle.bauhinia;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.tools.AudioDownloader;
import com.beetle.bauhinia.tools.AudioUtil;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.GroupOutbox;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;
import com.beetle.im.GroupMessageObserver;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;


/**
 * Created by houxh on 15/3/21.
 */
public class GroupMessageActivity extends MessageActivity implements
        IMServiceObserver, GroupMessageObserver,
        AudioDownloader.AudioDownloaderObserver,
        GroupOutbox.OutboxObserver {

    public static final String SEND_MESSAGE_NAME = "send_group_message";
    public static final String CLEAR_MESSAGES = "clear_group_messages";
    public static final String CLEAR_NEW_MESSAGES = "clear_group_new_messages";

    private final int PAGE_SIZE = 10;

    protected long sender;
    protected long receiver;

    protected long currentUID;
    protected long groupID;
    protected String groupName;

    public GroupMessageActivity() {
        super();
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
        getSupportActionBar().setTitle(groupName);
        //显示最后一条消息
        if (this.messages.size() > 0) {
            listview.setSelection(this.messages.size() - 1);
        }

        GroupOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addGroupObserver(this);
        AudioDownloader.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(this.groupID, CLEAR_NEW_MESSAGES);
        nc.postNotification(notification);

        GroupOutbox.getInstance().removeObserver(this);
        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removeGroupObserver(this);
        AudioDownloader.getInstance().removeObserver(this);
    }



    protected void loadConversationData() {
        HashSet<String> uuidSet = new HashSet<String>();
        messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = GroupMessageDB.getInstance().newMessageIterator(groupID);
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


            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                updateNotificationDesc(msg);
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        downloadMessageContent(messages, count);
        loadUserName(messages, count);
        checkMessageFailureFlag(messages, count);
        resetMessageTimeBase();
    }

    protected void loadEarlierData() {
        if (messages.size() == 0) {
            return;
        }

        IMessage firstMsg = null;
        for (int i  = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            if (msg.msgLocalID > 0) {
                firstMsg = msg;
                break;
            }
        }
        if (firstMsg == null) {
            return;
        }

        HashSet<String> uuidSet = new HashSet<String>();
        for (int i  = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
            }
        }

        int count = 0;
        MessageIterator iter = GroupMessageDB.getInstance().newMessageIterator(groupID, firstMsg.msgLocalID);
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

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment) msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                updateNotificationDesc(msg);
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        if (count > 0) {
            downloadMessageContent(messages, count);
            loadUserName(messages, count);
            checkMessageFailureFlag(messages, count);
            resetMessageTimeBase();
            adapter.notifyDataSetChanged();
            listview.setSelection(count);
        }
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
        setSubtitle();
    }

    @Override
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

        if (!TextUtils.isEmpty(imsg.getUUID()) && findMessage(imsg.getUUID()) != null) {
            Log.i(TAG, "receive repeat message:" + imsg.getUUID());
            return;
        }

        loadUserName(imsg);

        downloadMessageContent(imsg);
        insertMessage(imsg);
    }

    @Override
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
    @Override
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

    @Override
    public void onGroupNotification(String text) {
        IMessage.GroupNotification notification = IMessage.newGroupNotification(text);

        if (notification.groupID != groupID) {
            return;
        }

        IMessage imsg = new IMessage();
        imsg.sender = 0;
        imsg.receiver = groupID;
        imsg.timestamp = notification.timestamp;
        imsg.setContent(notification);

        updateNotificationDesc(imsg);

        insertMessage(imsg);
    }

    private void updateNotificationDesc(IMessage imsg) {
        if (imsg.content.getType() != IMessage.MessageType.MESSAGE_GROUP_NOTIFICATION) {
            return;
        }

        IMessage.GroupNotification notification = (IMessage.GroupNotification)imsg.content;
        if (notification.notificationType == IMessage.GroupNotification.NOTIFICATION_GROUP_CREATED) {
            if (notification.master == currentUID) {
                notification.description = String.format("您创建了\"%s\"群组", notification.groupName);
            } else {
                notification.description = String.format("您加入了\"%s\"群组", notification.groupName);
            }
        } else if (notification.notificationType == IMessage.GroupNotification.NOTIFICATION_GROUP_DISBAND) {
            notification.description = "群组已解散";
        } else if (notification.notificationType == IMessage.GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED) {
            User u = getUser(notification.member);
            if (TextUtils.isEmpty(u.name)) {
                notification.description = String.format("\"%s\"加入群", u.identifier);
                imsg.setDownloading(true);
                final IMessage fmsg = imsg;
                asyncGetUser(notification.member, new GetUserCallback() {
                    @Override
                    public void onUser(User u) {
                        IMessage.GroupNotification notification = (IMessage.GroupNotification)fmsg.content;
                        notification.description = String.format("\"%s\"加入群", u.name);
                        fmsg.setDownloading(false);
                    }
                });
            } else {
                notification.description = String.format("\"%s\"加入群", u.name);
            }
        } else if (notification.notificationType == IMessage.GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED) {
            User u = getUser(notification.member);
            if (TextUtils.isEmpty(u.name)) {
                notification.description = String.format("\"%s\"离开群", u.identifier);
                imsg.setDownloading(true);
                final IMessage fmsg = imsg;
                asyncGetUser(notification.member, new GetUserCallback() {
                    @Override
                    public void onUser(User u) {
                        IMessage.GroupNotification notification = (IMessage.GroupNotification)fmsg.content;
                        notification.description = String.format("\"%s\"离开群", u.name);
                        fmsg.setDownloading(false);
                    }
                });
            } else {
                notification.description = String.format("\"%s\"离开群", u.name);
            }
        } else if (notification.notificationType == IMessage.GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED) {
            notification.description = String.format("群组改名为\"%s\"", notification.groupName);
        }
    }


    void checkMessageFailureFlag(IMessage msg) {
        if (msg.sender == this.currentUID) {
            if (msg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
                msg.setUploading(GroupOutbox.getInstance().isUploading(msg));
            } else if (msg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
                msg.setUploading(GroupOutbox.getInstance().isUploading(msg));
            }
            if (!msg.isAck() &&
                    !msg.isFailure() &&
                    !msg.getUploading() &&
                    !IMService.getInstance().isGroupMessageSending(groupID, msg.msgLocalID)) {
                markMessageFailure(msg);
                msg.setFailure(true);
            }
        }
    }

    void checkMessageFailureFlag(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage m = messages.get(i);
            checkMessageFailureFlag(m);
        }
    }

    @Override
    protected void sendMessageContent(IMessage.MessageContent content) {
        IMessage imsg = new IMessage();
        imsg.sender = this.sender;
        imsg.receiver = this.receiver;
        imsg.setContent(content);
        imsg.timestamp = now();
        imsg.isOutgoing = true;
        saveMessage(imsg);

        loadUserName(imsg);
        if (imsg.content.getType() == IMessage.MessageType.MESSAGE_LOCATION) {
            IMessage.Location loc = (IMessage.Location)imsg.content;

            if (TextUtils.isEmpty(loc.address)) {
                queryLocation(imsg);
            } else {
                saveMessageAttachment(imsg, loc.address);
            }
        }
        sendMessage(imsg);

        insertMessage(imsg);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(imsg, SEND_MESSAGE_NAME);
        nc.postNotification(notification);
    }

    @Override
    protected void resend(IMessage msg) {
        eraseMessageFailure(msg);
        msg.setFailure(false);
        this.sendMessage(msg);
    }

    protected void sendMessage(IMessage imsg) {
        if (imsg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
            GroupOutbox ob = GroupOutbox.getInstance();
            IMessage.Audio audio = (IMessage.Audio)imsg.content;
            imsg.setUploading(true);
            ob.uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
        } else if (imsg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
            IMessage.Image image = (IMessage.Image)imsg.content;
            //prefix:"file:"
            String path = image.url.substring(5);
            imsg.setUploading(true);
            GroupOutbox.getInstance().uploadImage(imsg, path);
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

    @Override
    void saveMessageAttachment(IMessage msg, String address) {
        IMessage attachment = new IMessage();
        attachment.content = IMessage.newAttachment(msg.msgLocalID, address);
        attachment.sender = msg.sender;
        attachment.receiver = msg.receiver;
        saveMessage(attachment);
    }

    void saveMessage(IMessage imsg) {
        GroupMessageDB.getInstance().insertMessage(imsg, imsg.receiver);
    }

    @Override
    protected void markMessageListened(IMessage imsg) {
        GroupMessageDB.getInstance().markMessageListened(imsg.msgLocalID, imsg.receiver);
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
        super.clearConversation();
        GroupMessageDB db = GroupMessageDB.getInstance();
        db.clearCoversation(this.groupID);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(this.receiver, CLEAR_MESSAGES);
        nc.postNotification(notification);
    }

    @Override
    public void onAudioUploadSuccess(IMessage msg, String url) {
        Log.i(TAG, "audio upload success:" + url);
        if (msg.receiver == this.groupID) {
            IMessage m = findMessage(msg.msgLocalID);
            if (m != null) {
                m.setUploading(false);
            }
        }
    }

    @Override
    public void onAudioUploadFail(IMessage msg) {
        Log.i(TAG, "audio upload fail");
        if (msg.receiver == this.groupID) {
            IMessage m = findMessage(msg.msgLocalID);
            if (m != null) {
                m.setFailure(true);
                m.setUploading(false);
            }
        }
    }

    @Override
    public void onImageUploadSuccess(IMessage msg, String url) {
        Log.i(TAG, "image upload success:" + url);
        if (msg.receiver == this.groupID) {
            IMessage m = findMessage(msg.msgLocalID);
            if (m != null) {
                m.setUploading(false);
            }
        }
    }

    @Override
    public void onImageUploadFail(IMessage msg) {
        Log.i(TAG, "image upload fail");
        if (msg.receiver == this.groupID) {
            IMessage m = findMessage(msg.msgLocalID);
            if (m != null) {
                m.setFailure(true);
                m.setUploading(false);
            }
        }
    }

    @Override
    public void onAudioDownloadSuccess(IMessage msg) {
        Log.i(TAG, "audio download success");
    }
    @Override
    public void onAudioDownloadFail(IMessage msg) {
        Log.i(TAG, "audio download fail");
    }



}
