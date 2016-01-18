package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.db.CustomerServiceMessageDB;
import com.beetle.im.CustomerServiceMessageObserver;
import com.beetle.bauhinia.tools.CustomerServiceOutbox;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;

import java.util.ArrayList;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerServiceMessageActivity extends MessageActivity
        implements CustomerServiceMessageObserver, IMServiceObserver,
        CustomerServiceOutbox.OutboxObserver {
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

    public CustomerServiceMessageActivity() {
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

        this.sender = currentUID;
        this.receiver = peerUID;

        this.loadConversationData();
        if (peerUID == 0 && TextUtils.isEmpty(peerName)) {
            peerName = "客服";
        }
        if (!TextUtils.isEmpty(peerName)) {
            titleView.setText(peerName);
        }

        CustomerServiceOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addCustomerServiceObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");
        CustomerServiceOutbox.getInstance().removeObserver(this);

        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removeCustomerServiceObserver(this);
    }

    protected void loadConversationData() {
        messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = CustomerServiceMessageDB.getInstance().newMessageIterator(peerUID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment) msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
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
                msg.setUploading(CustomerServiceOutbox.getInstance().isUploading(msg));
            } else if (msg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
                msg.setUploading(CustomerServiceOutbox.getInstance().isUploading(msg));
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
    public void onCustomerServiceMessage(IMMessage msg) {
        if (isStaff()) {
            if (msg.sender != peerUID && msg.receiver != peerUID) {
                return;
            }
        } else {
            if (msg.sender != currentUID && msg.receiver != currentUID) {
                return;
            }

            if (msg.sender != currentUID) {
                //可能是新的客服人员
                this.receiver = msg.sender;
            }
        }

        Log.i(TAG, "recv msg:" + msg.content);
        final IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        downloadMessageContent(imsg);

        insertMessage(imsg);
    }

    @Override
    public void onCustomerServiceMessageACK(int msgLocalID, long uid) {
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
    public void onCustomerServiceMessageFailure(int msgLocalID, long uid) {
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
            CustomerServiceOutbox ob = CustomerServiceOutbox.getInstance();
            IMessage.Audio audio = (IMessage.Audio)imsg.content;
            imsg.setUploading(true);
            ob.uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
        } else if (imsg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
            IMessage.Image image = (IMessage.Image)imsg.content;
            //prefix:"file:"
            String path = image.image.substring(5);
            imsg.setUploading(true);
            CustomerServiceOutbox.getInstance().uploadImage(imsg, path);
        } else {
            IMMessage msg = new IMMessage();
            msg.sender = imsg.sender;
            msg.receiver = imsg.receiver;
            msg.content = imsg.content.getRaw();
            msg.msgLocalID = imsg.msgLocalID;
            IMService im = IMService.getInstance();
            im.sendCustomerServiceMessage(msg);
        }
    }

    @Override
    void saveMessage(IMessage imsg) {
        CustomerServiceMessageDB.getInstance().insertMessage(imsg, peerUID);
    }

    @Override
    void markMessageFailure(IMessage imsg) {
        CustomerServiceMessageDB.getInstance().markMessageFailure(imsg.msgLocalID, peerUID);
    }

    @Override
    void eraseMessageFailure(IMessage imsg) {
        CustomerServiceMessageDB.getInstance().eraseMessageFailure(imsg.msgLocalID, peerUID);
    }

    @Override
    void clearConversation() {
        CustomerServiceMessageDB db = CustomerServiceMessageDB.getInstance();
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
