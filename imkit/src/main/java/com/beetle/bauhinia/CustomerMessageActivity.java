package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.types.Supporter;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.bauhinia.tools.FileDownloader;
import com.beetle.im.CustomerMessage;
import com.beetle.im.CustomerMessageObserver;
import com.beetle.bauhinia.outbox.CustomerOutbox;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerMessageActivity extends MessageActivity
        implements CustomerMessageObserver, IMServiceObserver {
    protected String sessionID;
    protected long currentUID;
    protected long appid;//当前appid
    protected long peerAppID;
    protected long peerUID;
    protected long storeID;

    protected String name;//当前用户昵称
    protected String appName;
    protected String peerName;
    protected String peerAppName;
    protected String storeName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        items[ITEM_VIDEO_CALL_ID] = false;
        super.onCreate(savedInstanceState);
        isShowUserName = true;
        isShowReaded = false;
        isShowReply = false;

        Intent intent = getIntent();
        appid = intent.getLongExtra("app_id", 0);
        currentUID = intent.getLongExtra("current_uid", 0);
        storeID = intent.getLongExtra("store_id", 0);
        peerUID = intent.getLongExtra("peer_uid", 0);
        peerAppID = intent.getLongExtra("peer_app_id", 0);

        peerName = intent.getStringExtra("peer_name");
        if (peerName == null) {
            peerName = "";
        }

        peerAppName = intent.getStringExtra("peer_app_name");
        if (peerAppName == null) {
            peerAppName = "";
        }

        storeName = intent.getStringExtra("store_name");
        if (storeName == null) {
            storeName = "";
        }

        sessionID = intent.getStringExtra("session_id");
        if (sessionID == null) {
            sessionID = "";
        }
        appName = intent.getStringExtra("app_name");
        if (appName == null) {
            appName = "";
        }
        name = intent.getStringExtra("name");
        if (name == null) {
            name = "";
        }

        if (appid == 0 || currentUID == 0) {
            Log.e(TAG, "invalid param");
            finish();
            return;
        }

        Log.i(TAG, "appid:" + appid +" uid:" + currentUID + " peer app id:" + peerAppID + " peer uid:" + peerUID + " store id:" + storeID);

        this.conversationID = "" + peerAppID + "_" + peerUID;
        this.messageDB = CustomerMessageDB.getInstance();
        this.hasLateMore = this.messageID > 0;
        this.hasEarlierMore = true;
        this.loadData();

        if (!TextUtils.isEmpty(storeName)) {
            getSupportActionBar().setTitle(storeName);
        }
        //显示最后一条消息
        if (this.messages.size() > 0) {
            listview.setSelection(this.messages.size() - 1);
        }

        CustomerOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addCustomerServiceObserver(this);
        FileDownloader.getInstance().addObserver(this);

        if (peerAppID == 0 || peerUID == 0) {
            this.disableSend();

            IMHttpAPI.Singleton().getSupporter(this.storeID)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Supporter>() {
                        @Override
                        public void call(Supporter supporter) {
                            Log.i(TAG, "got supporter:" + supporter.id + " name:" + supporter.name);
                            peerAppID = supporter.appid;
                            peerUID = supporter.id;
                            IMService.ConnectState state = IMService.getInstance().getConnectState();
                            if (state == IMService.ConnectState.STATE_CONNECTED) {
                                enableSend();
                            }
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e(TAG, "get supporter err:" + throwable.getMessage());
                        }
                    });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");

        CustomerOutbox.getInstance().removeObserver(this);
        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removeCustomerServiceObserver(this);
        FileDownloader.getInstance().removeObserver(this);
    }



    @Override
    public void onConnectState(IMService.ConnectState state) {
        if (state == IMService.ConnectState.STATE_CONNECTED) {
            if (peerAppID != 0 && peerUID != 0) {
                enableSend();
            }
        } else {
            disableSend();
        }
    }



    @Override
    public void onCustomerMessage(CustomerMessage msg) {
        Log.i(TAG, "recv msg:" + msg.content);
        final ICustomerMessage imsg = new ICustomerMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;

        imsg.senderAppID = msg.senderAppID;
        imsg.sender = msg.sender;
        imsg.receiverAppID = msg.receiverAppID;
        imsg.receiver = msg.receiver;
        if (msg.senderAppID == appid && msg.sender == this.currentUID) {
            imsg.isOutgoing = true;
            imsg.flags |= MessageFlag.MESSAGE_FLAG_ACK;
        } else {
            imsg.isOutgoing = false;
        }
        imsg.setContent(msg.content);

        long msgPeerAppID;
        long msgPeer;

        if (imsg.isOutgoing) {
            msgPeerAppID = msg.receiverAppID;
            msgPeer = msg.receiver;
        } else {
            msgPeerAppID = msg.senderAppID;
            msgPeer = msg.sender;
        }

        if (peerAppID != 0 && peerUID != 0) {
            if (msgPeerAppID != peerAppID || msgPeer != peerUID) {
                return;
            }
        }

        if (storeID != 0 && imsg.getStoreId() != storeID) {
            return;
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
                deleteMessage(m);
            }
        }
        insertMessage(imsg);
    }

    @Override
    public void onCustomerMessageACK(CustomerMessage msg) {

        Log.i(TAG, "customer service message ack");

        if (msg.msgLocalID > 0) {
            IMessage imsg = findMessage(msg.msgLocalID);
            if (imsg == null) {
                Log.i(TAG, "can't find msg:" + msg.msgLocalID);
                return;
            }
            imsg.setAck(true);
        } else {
            MessageContent c = IMessage.fromRaw(msg.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke r = (Revoke)c;
                IMessage imsg = findMessage(r.msgid);
                if (imsg == null) {
                    Log.i(TAG, "can't find msg:" + r.msgid);
                    return;
                }
                imsg.setContent(r);
                updateNotificationDesc(imsg);
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onCustomerMessageFailure(CustomerMessage msg) {
        Log.i(TAG, "message failure");

        if (msg.msgLocalID > 0) {
            IMessage imsg = findMessage(msg.msgLocalID);
            if (imsg == null) {
                Log.i(TAG, "can't find msg:" + msg.msgLocalID);
                return;
            }
            imsg.setFailure(true);
        } else {
            MessageContent c = IMessage.fromRaw(msg.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Toast.makeText(this, "撤回失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected MessageIterator createMessageIterator() {
        CustomerMessageDB db = (CustomerMessageDB) messageDB;
        MessageIterator iter = db.newMessageIterator(storeID);
        return iter;
    }

    @Override
    protected MessageIterator createForwardMessageIterator(long messageID) {
        CustomerMessageDB db = (CustomerMessageDB) messageDB;
        MessageIterator iter = db.newForwardMessageIterator(storeID, messageID);
        return iter;
    }

    @Override
    protected MessageIterator createBackwardMessageIterator(long messageID) {
        CustomerMessageDB db = (CustomerMessageDB) messageDB;
        MessageIterator iter = db.newBackwardMessageIterator(storeID, messageID);
        return iter;
    }

    @Override
    protected MessageIterator createMiddleMessageIterator(long messageID) {
        CustomerMessageDB db = (CustomerMessageDB) messageDB;
        MessageIterator iter = db.newMiddleMessageIterator(storeID, messageID);
        return iter;
    }


    @Override
    protected boolean getMessageOutgoing(IMessage msg) {
        ICustomerMessage m = (ICustomerMessage)msg;
        return (msg.sender == currentUID && m.senderAppID == appid);
    }

    @Override
    protected void sendMessage(IMessage imsg) {
        CustomerOutbox.getInstance().sendMessage(imsg);
    }

    @Override
    protected IMessage newOutMessage(MessageContent content) {
        ICustomerMessage msg = new ICustomerMessage();
        msg.senderAppID = appid;
        msg.sender = currentUID;
        msg.receiverAppID = peerAppID;
        msg.receiver = peerUID;

        content.generateRaw(storeID, sessionID, storeName, name, appName);
        msg.content = content;
        return msg;
    }


    @Override
    protected void loadUserName(IMessage msg) {
        if (!(msg instanceof ICustomerMessage)) {
            return;
        }

        User u = new User();
        ICustomerMessage cm = (ICustomerMessage) msg;
        u.name = msg.content.getName();
        u.identifier = String.format("%d", msg.sender);
        u.avatarURL = "";

        msg.setSenderAvatar(u.avatarURL);
        if (TextUtils.isEmpty(u.name)) {
            msg.setSenderName(u.identifier);
        } else {
            msg.setSenderName(u.name);
        }
    }
}
