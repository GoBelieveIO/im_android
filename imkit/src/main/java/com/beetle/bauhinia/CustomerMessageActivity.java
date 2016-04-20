package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.im.CustomerMessage;
import com.beetle.im.CustomerMessageObserver;
import com.beetle.bauhinia.tools.CustomerOutbox;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;

import java.util.ArrayList;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerMessageActivity extends MessageActivity
        implements CustomerMessageObserver, IMServiceObserver,
        CustomerOutbox.OutboxObserver {
    public static final String SEND_MESSAGE_NAME = "send_cs_message";
    public static final String CLEAR_MESSAGES = "clear_cs_messages";

    private final int PAGE_SIZE = 10;

    protected long currentUID;
    protected long peerUID;
    protected String peerName;

    //当前用户是否是客服人员
    private boolean isStaff() {
        return peerUID != 0;
    }

    public static class User {
        public long uid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }

    protected User getUser(long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("%d", uid);
        return u;
    }

    public interface GetUserCallback {
        void onUser(User u);
    }

    protected void asyncGetUser(long uid, GetUserCallback cb) {

    }

    private void loadUserName(IMessage msg) {
        User u = getUser(msg.sender);

        msg.setSenderAvatar(u.avatarURL);
        if (TextUtils.isEmpty(u.name)) {
            msg.setSenderName(u.identifier);
            final IMessage fmsg = msg;
            asyncGetUser(msg.sender, new GetUserCallback() {
                @Override
                public void onUser(User u) {
                    fmsg.setSenderName(u.name);
                    fmsg.setSenderAvatar(u.avatarURL);
                }
            });
        } else {
            msg.setSenderName(u.name);
        }
    }


    public CustomerMessageActivity() {
        super();
        sendNotificationName = SEND_MESSAGE_NAME;
        clearNotificationName = CLEAR_MESSAGES;
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
        peerUID = intent.getLongExtra("peer_uid", 0);
        if (peerUID == 0) {
            Log.w(TAG, "peer uid is 0");
        }
        peerName = intent.getStringExtra("peer_name");
        if (peerName == null) {
            Log.w(TAG, "peer name is null");
        }

        this.isShowUserName = intent.getBooleanExtra("show_name", false);
        this.sender = currentUID;
        this.receiver = peerUID;

        this.loadConversationData();
        if (peerUID == 0 && TextUtils.isEmpty(peerName)) {
            peerName = "客服";
        }
        if (!TextUtils.isEmpty(peerName)) {
            titleView.setText(peerName);
        }

        CustomerOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addCustomerServiceObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");
        CustomerOutbox.getInstance().removeObserver(this);

        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removeCustomerServiceObserver(this);
    }

    protected void loadConversationData() {
        messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = CustomerMessageDB.getInstance().newMessageIterator(peerUID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment) msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                loadUserName(msg);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }

        if (!isStaff()) {
            //找出最近联系过的客服人员ID
            for (int i = count - 1; i >= 0; i--) {
                IMessage msg = messages.get(i);
                if (msg.sender == this.currentUID && msg.receiver > 0) {
                    this.receiver = msg.receiver;
                    break;
                }
            }
        }
        downloadMessageContent(messages, count);
        checkMessageFailureFlag(messages, count);
        resetMessageTimeBase();
    }

    void checkMessageFailureFlag(IMessage msg) {
        if (msg.sender == this.currentUID) {
            if (msg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
                msg.setUploading(CustomerOutbox.getInstance().isUploading(msg));
            } else if (msg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
                msg.setUploading(CustomerOutbox.getInstance().isUploading(msg));
            }
            if (!msg.isAck() &&
                    !msg.isFailure() &&
                    !msg.getUploading() &&
                    !IMService.getInstance().isCustomerServiceMessageSending(msg.receiver, msg.msgLocalID)) {
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

        int count = 0;
        MessageIterator iter = CustomerMessageDB.getInstance().newMessageIterator(peerUID, firstMsg.msgLocalID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
                loadUserName(msg);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        if (count > 0) {
            downloadMessageContent(messages, count);
            checkMessageFailureFlag(messages, count);
            resetMessageTimeBase();
            adapter.notifyDataSetChanged();
            listview.setSelection(count);
        }
    }

    @Override
    protected MessageIterator getMessageIterator() {
        return CustomerMessageDB.getInstance().newMessageIterator(peerUID);
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
    public void onCustomerMessage(CustomerMessage msg) {
        if (isStaff() && msg.customer != peerUID) {
            return;
        }
        if (!isStaff() && msg.sender != currentUID) {
            //普通用户收到客服的回复消息,记录下客服的id
            this.receiver = msg.sender;
        }

        Log.i(TAG, "recv msg:" + msg.content);
        final IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        loadUserName(imsg);
        downloadMessageContent(imsg);

        insertMessage(imsg);
    }

    @Override
    public void onCustomerMessageACK(int msgLocalID, long uid) {
        if (isStaff()) {
            if (this.peerUID != uid) {
                return;
            }
        }
        Log.i(TAG, "customer service message ack");

        IMessage imsg = findMessage(msgLocalID);
        if (imsg == null) {
            Log.i(TAG, "can't find msg:" + msgLocalID);
            return;
        }
        imsg.setAck(true);
    }

    @Override
    public void onCustomerMessageFailure(int msgLocalID, long uid) {
        if (isStaff()) {
            if (this.peerUID != uid) {
                return;
            }
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
    void sendMessage(IMessage imsg) {
        if (imsg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
            CustomerOutbox ob = CustomerOutbox.getInstance();
            IMessage.Audio audio = (IMessage.Audio)imsg.content;
            imsg.setUploading(true);
            ob.uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
        } else if (imsg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
            IMessage.Image image = (IMessage.Image)imsg.content;
            //prefix:"file:"
            String path = image.image.substring(5);
            imsg.setUploading(true);
            CustomerOutbox.getInstance().uploadImage(imsg, path);
        } else {
            CustomerMessage msg = new CustomerMessage();
            msg.sender = imsg.sender;
            msg.receiver = imsg.receiver;
            msg.content = imsg.content.getRaw();
            msg.msgLocalID = imsg.msgLocalID;
            if (isStaff()) {
                msg.customer = imsg.receiver;
            } else {
                msg.customer = imsg.sender;
            }

            IMService im = IMService.getInstance();
            im.sendCustomerMessage(msg);
        }
    }

    @Override
    void saveMessage(IMessage imsg) {
        CustomerMessageDB.getInstance().insertMessage(imsg, peerUID);
    }

    @Override
    void markMessageFailure(IMessage imsg) {
        CustomerMessageDB.getInstance().markMessageFailure(imsg.msgLocalID, peerUID);
    }

    @Override
    void eraseMessageFailure(IMessage imsg) {
        CustomerMessageDB.getInstance().eraseMessageFailure(imsg.msgLocalID, peerUID);
    }

    @Override
    void clearConversation() {
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        db.clearCoversation(this.peerUID);
    }

    @Override
    public void onAudioUploadSuccess(IMessage imsg, String url) {
        Log.i(TAG, "audio upload success:" + url);
        if (isStaff() && imsg.receiver != this.peerUID) {
            return;
        }

        IMessage m = findMessage(imsg.msgLocalID);
        if (m != null) {
            m.setUploading(false);
        }
    }

    @Override
    public void onAudioUploadFail(IMessage msg) {
        Log.i(TAG, "audio upload fail");
        if (isStaff() && msg.receiver != this.peerUID) {
            return;
        }

        IMessage m = findMessage(msg.msgLocalID);
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }

    }

    @Override
    public void onImageUploadSuccess(IMessage msg, String url) {
        Log.i(TAG, "image upload success:" + url);
        if (isStaff() && msg.receiver != this.peerUID) {
            return;
        }
        IMessage m = findMessage(msg.msgLocalID);
        if (m != null) {
            m.setUploading(false);
        }
    }

    @Override
    public void onImageUploadFail(IMessage msg) {
        Log.i(TAG, "image upload fail");
        if (isStaff() && msg.receiver != this.peerUID) {
            return;
        }
        IMessage m = findMessage(msg.msgLocalID);
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }
    }
}
