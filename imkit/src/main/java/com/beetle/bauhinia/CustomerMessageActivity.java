/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.MessageFlag;
import com.beetle.bauhinia.db.message.Audio;
import com.beetle.bauhinia.db.message.Image;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.bauhinia.db.message.Video;
import com.beetle.bauhinia.tools.FileDownloader;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;
import com.beetle.im.CustomerMessage;
import com.beetle.im.CustomerMessageObserver;
import com.beetle.bauhinia.tools.CustomerOutbox;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;

/**
 * Created by houxh on 16/1/18.
 */
public class CustomerMessageActivity extends MessageActivity
        implements CustomerMessageObserver, IMServiceObserver
        {
    public static final String SEND_MESSAGE_NAME = "send_cs_message";
    public static final String CLEAR_MESSAGES = "clear_cs_messages";
    public static final String CLEAR_NEW_MESSAGES = "clear_cs_new_messages";

    protected String title;

    protected long appID;
    protected long storeID;
    protected long sellerID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        items[ITEM_VIDEO_CALL_ID] = false;

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        currentUID = intent.getLongExtra("current_uid", 0);
        appID = intent.getLongExtra("app_id", 0);
        storeID = intent.getLongExtra("store_id", 0);
        sellerID = intent.getLongExtra("seller_id", 0);
        title = intent.getStringExtra("title");

        Log.i(TAG, "uid:" + currentUID + " app id:" + appID + " store id:" + storeID + " seller id:" + sellerID + " title:" + title);
        if (appID == 0 || currentUID == 0 || storeID == 0) {
            return;
        }

        this.isShowUserName = intent.getBooleanExtra("show_name", false);


        this.conversationID = storeID;
        this.messageDB = CustomerMessageDB.getInstance();

        this.hasLateMore = this.messageID > 0;
        this.hasEarlierMore = true;
        this.loadConversationData();

        if (!TextUtils.isEmpty(title)) {
            getSupportActionBar().setTitle(title);
        }
        //显示最后一条消息
        if (this.messages.size() > 0) {
            listview.setSelection(this.messages.size() - 1);
        }

        CustomerOutbox.getInstance().addObserver(this);
        IMService.getInstance().addObserver(this);
        IMService.getInstance().addCustomerServiceObserver(this);
        FileDownloader.getInstance().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "peer message activity destory");

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(this.storeID, CLEAR_NEW_MESSAGES);
        nc.postNotification(notification);

        CustomerOutbox.getInstance().removeObserver(this);
        IMService.getInstance().removeObserver(this);
        IMService.getInstance().removeCustomerServiceObserver(this);
        FileDownloader.getInstance().removeObserver(this);
    }


    @Override
    protected MessageIterator getMessageIterator() {
        return CustomerMessageDB.getInstance().newMessageIterator(storeID);
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
    public void onCustomerSupportMessage(CustomerMessage msg) {
        Log.i(TAG, "recv msg:" + msg.content);
        final ICustomerMessage imsg = new ICustomerMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;

        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.isSupport = true;
        imsg.isOutgoing = false;
        imsg.sender = msg.storeID;
        imsg.receiver = msg.customerID;
        imsg.setContent(msg.content);

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
    public void onCustomerMessage(CustomerMessage msg) {
        Log.i(TAG, "recv msg:" + msg.content);
        final ICustomerMessage imsg = new ICustomerMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;

        imsg.customerAppID = msg.customerAppID;
        imsg.customerID = msg.customerID;
        imsg.storeID = msg.storeID;
        imsg.sellerID = msg.sellerID;
        imsg.isSupport = false;
        imsg.isOutgoing = true;
        imsg.sender = msg.customerID;
        imsg.receiver = msg.storeID;
        imsg.setContent(msg.content);

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
    protected boolean sendMessage(IMessage imsg) {
        boolean r = true;
        if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_AUDIO) {
            CustomerOutbox ob = CustomerOutbox.getInstance();
            Audio audio = (Audio)imsg.content;
            imsg.setUploading(true);
            ob.uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
        } else if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
            Image image = (Image) imsg.content;
            //prefix:"file:"
            String path = image.url.substring(5);
            imsg.setUploading(true);
            CustomerOutbox.getInstance().uploadImage(imsg, path);
        } else if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_VIDEO) {
            Video video = (Video)imsg.content;
            imsg.setUploading(true);
            //prefix: "file:"
            String path = video.thumbnail.substring(5);
            String videoPath = FileCache.getInstance().getCachedFilePath(video.url);
            CustomerOutbox.getInstance().uploadVideo(imsg, videoPath, path);
        } else {
            ICustomerMessage cm = (ICustomerMessage)imsg;
            CustomerMessage msg = new CustomerMessage();

            msg.msgLocalID = cm.msgLocalID;
            msg.customerAppID = cm.customerAppID;
            msg.customerID = cm.customerID;
            msg.storeID = cm.storeID;
            msg.sellerID = cm.sellerID;
            msg.content = cm.content.getRaw();

            IMService im = IMService.getInstance();
            r = im.sendCustomerMessage(msg);
        }
        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(imsg, SEND_MESSAGE_NAME);
        nc.postNotification(notification);
        return r;
    }


    @Override
    void clearConversation() {
        super.clearConversation();
        CustomerMessageDB db = CustomerMessageDB.getInstance();
        db.clearConversation(this.storeID);

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(this.storeID, CLEAR_MESSAGES);
        nc.postNotification(notification);
    }

    @Override
    protected IMessage newOutMessage() {
        ICustomerMessage msg = new ICustomerMessage();
        msg.customerAppID = appID;
        msg.customerID = currentUID;
        msg.storeID = storeID;
        msg.sellerID = sellerID;
        return msg;
    }

}
