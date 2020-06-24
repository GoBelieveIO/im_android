package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.beetle.bauhinia.db.*;
import com.beetle.bauhinia.db.message.ACK;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.bauhinia.tools.FileDownloader;
import com.beetle.bauhinia.outbox.PeerOutbox;

import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.MessageACK;
import com.beetle.im.PeerMessageObserver;


public class PeerMessageActivity extends MessageActivity implements
        IMServiceObserver, PeerMessageObserver {
    protected long peerUID;
    protected String peerName;
    protected String peerAvatar;
    protected boolean secret;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        items[ITEM_VIDEO_CALL_ID] = false;
        super.onCreate(savedInstanceState);
        isShowUserName = false;
        isShowReaded = false;
        isShowReply = false;


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
            peerName = "";
        }
        peerAvatar = intent.getStringExtra("peer_avatar");
        if (peerAvatar == null) {
            peerAvatar = "";
        }

        secret = intent.getBooleanExtra("secret", false);


        messageID = intent.getIntExtra("message_id", 0);

        Log.i(TAG, "local id:" + currentUID +  "peer id:" + peerUID);

        this.conversationID = peerUID;

        if (secret) {
            getSupportActionBar().setTitle(peerName + "(密)");
        } else {
            getSupportActionBar().setTitle(peerName);
        }

        if (secret) {
            messageDB = EPeerMessageDB.getInstance();
        } else {
            messageDB = PeerMessageDB.getInstance();
        }

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

        PeerOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addPeerObserver(this);
        FileDownloader.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");

        PeerOutbox.getInstance().removeObserver(this);
        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removePeerObserver(this);
        FileDownloader.getInstance().removeObserver(this);
    }

    @Override
    protected MessageIterator getMessageIterator() {
        if (secret) {
            return EPeerMessageDB.getInstance().newMessageIterator(peerUID);
        } else {
            return PeerMessageDB.getInstance().newMessageIterator(peerUID);
        }
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
    public void onPeerMessage(IMMessage msg) {
        if (msg.sender != peerUID && msg.receiver != peerUID) {
            return;
        }
        if (this.secret) {
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
            //清空消息失败标志位
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
    public void onPeerSecretMessage(IMMessage msg) {
        if (msg.sender != peerUID && msg.receiver != peerUID) {
            return;
        }
        if (!this.secret) {
            return;
        }

        Log.i(TAG, "recv msg:" + msg.content);
        final IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.secret = true;
        imsg.setContent(msg.content);
        imsg.isOutgoing = (msg.sender == this.currentUID);
        if (imsg.isOutgoing) {
            imsg.flags |= MessageFlag.MESSAGE_FLAG_ACK;
        }

        if (imsg.getType() == MessageContent.MessageType.MESSAGE_P2P_SESSION) {
            handleP2PSession(imsg);
            return;
        }

        IMessage mm = findMessage(imsg.getUUID());
        if (mm != null) {
            Log.i(TAG, "receive repeat message:" + imsg.getUUID());
            //清空消息失败标志位
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
    public void onPeerMessageACK(IMMessage im, int error) {
        long msgLocalID = im.msgLocalID;
        long uid = im.receiver;
        if (peerUID != uid) {
            return;
        }
        Log.i(TAG, "message ack:" + error);

        if (error == MessageACK.MESSAGE_ACK_SUCCESS) {
            if (msgLocalID > 0) {
                IMessage imsg = findMessage(msgLocalID);
                if (imsg == null) {
                    Log.i(TAG, "can't find msg:" + msgLocalID);
                    return;
                }
                imsg.setAck(true);
            } else {
                MessageContent c = IMessage.fromRaw(im.plainContent);
                if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                    Revoke r = (Revoke) c;
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

            IMessage ack = new IMessage();
            ack.sender = 0;
            ack.receiver = im.sender;
            ack.timestamp = now();
            ack.setContent(ACK.newACK(error));
            updateNotificationDesc(ack);
            insertMessage(ack);
        }
    }

    @Override
    public void onPeerMessageFailure(IMMessage im) {
        long msgLocalID = im.msgLocalID;
        long uid = im.receiver;
        if (peerUID != uid) {
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

    protected void handleP2PSession(IMessage imsg) {

    }

    @Override
    protected void sendMessage(IMessage imsg) {
        PeerOutbox.getInstance().sendMessage(imsg);
    }


    @Override
    protected IMessage newOutMessage(MessageContent content) {
        IMessage msg = new IMessage();
        msg.sender = this.currentUID;
        msg.receiver = this.peerUID;
        msg.secret = secret;
        msg.content = content;
        return msg;
    }
}
