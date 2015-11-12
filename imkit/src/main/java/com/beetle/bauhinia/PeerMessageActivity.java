package com.beetle.bauhinia;

import android.content.Intent;
import android.os.*;
import android.util.Log;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.im.*;

import com.beetle.bauhinia.tools.FileCache;

import com.beetle.bauhinia.tools.Outbox;
import com.beetle.im.Timer;

import java.util.*;


import static android.os.SystemClock.uptimeMillis;


public class PeerMessageActivity extends MessageActivity implements IMServiceObserver {

    public static final String SEND_MESSAGE_NAME = "send_message";
    public static final String CLEAR_MESSAGES = "clear_messages";

    private final int PAGE_SIZE = 10;

    protected long currentUID;
    protected long peerUID;
    protected String peerName;

    public PeerMessageActivity() {
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
            Log.e(TAG, "peer uid is 0");
            return;
        }
        peerName = intent.getStringExtra("peer_name");
        if (peerName == null) {
            Log.e(TAG, "peer name is null");
            return;
        }

        this.sender = currentUID;
        this.receiver = peerUID;

        this.loadConversationData();
        titleView.setText(peerName);

        IMService.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");
        IMService.getInstance().removeObserver(this);
    }


    protected void loadConversationData() {
        messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter = PeerMessageDB.getInstance().newMessageIterator(peerUID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }

        downloadMessageContent(messages, count);
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

        int count = 0;
        MessageIterator iter = PeerMessageDB.getInstance().newMessageIterator(peerUID, firstMsg.msgLocalID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == IMessage.MessageType.MESSAGE_ATTACHMENT) {
                IMessage.Attachment attachment = (IMessage.Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        if (count > 0) {
            downloadMessageContent(messages, count);
            resetMessageTimeBase();
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
        Log.i(TAG, "login point:" + lp.deviceID + " platform id:" + lp.platformID);
    }

    public void onPeerInputting(long uid) {
        if (uid == peerUID) {
            setSubtitle("对方正在输入");
            Timer t = new Timer() {
                @Override
                protected void fire() {
                    setSubtitle();
                }
            };
            long start = uptimeMillis() + 10*1000;
            t.setTimer(start);
            t.resume();
        }
    }

    public void onPeerMessage(IMMessage msg) {
        if (msg.sender != peerUID && msg.receiver != peerUID) {
            return;
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
    private IMessage findMessage(int msgLocalID) {
        for (IMessage imsg : messages) {
            if (imsg.msgLocalID == msgLocalID) {
                return imsg;
            }
        }
        return null;
    }

    public void onPeerMessageACK(int msgLocalID, long uid) {
        if (peerUID != uid) {
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

    public void onPeerMessageFailure(int msgLocalID, long uid) {
        if (peerUID != uid) {
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

    public void onGroupMessage(IMMessage msg) {

    }
    public void onGroupMessageACK(int msgLocalID, long uid) {

    }
    public void onGroupMessageFailure(int msgLocalID, long uid) {

    }
    public void onGroupNotification(String notification) {

    }

    void sendMessage(IMessage imsg) {
        if (imsg.content.getType() == IMessage.MessageType.MESSAGE_AUDIO) {
            Outbox ob = Outbox.getInstance();
            IMessage.Audio audio = (IMessage.Audio)imsg.content;
            ob.uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
        } else if (imsg.content.getType() == IMessage.MessageType.MESSAGE_IMAGE) {
            IMessage.Image image = (IMessage.Image)imsg.content;
            //prefix:"file:"
            String path = image.image.substring(5);
            Outbox.getInstance().uploadImage(imsg, path);
        } else {
            IMMessage msg = new IMMessage();
            msg.sender = imsg.sender;
            msg.receiver = imsg.receiver;
            msg.content = imsg.content.getRaw();
            msg.msgLocalID = imsg.msgLocalID;
            IMService im = IMService.getInstance();
            im.sendPeerMessage(msg);
        }
    }

    void saveMessage(IMessage imsg) {
        if (imsg.sender == this.currentUID) {
            PeerMessageDB.getInstance().insertMessage(imsg, imsg.receiver);
        } else {
            PeerMessageDB.getInstance().insertMessage(imsg, imsg.sender);
        }
    }

    void markMessageFailure(IMessage imsg) {
        long cid = 0;
        if (imsg.sender == this.currentUID) {
            cid = imsg.receiver;
        } else {
            cid = imsg.sender;
        }
        PeerMessageDB.getInstance().markMessageFailure(imsg.msgLocalID, cid);
    }

    void eraseMessageFailure(IMessage imsg) {
        long cid = 0;
        if (imsg.sender == this.currentUID) {
            cid = imsg.receiver;
        } else {
            cid = imsg.sender;
        }
        PeerMessageDB.getInstance().eraseMessageFailure(imsg.msgLocalID, cid);
    }

    void clearConversation() {
        PeerMessageDB db = PeerMessageDB.getInstance();
        db.clearCoversation(this.peerUID);
    }
}
