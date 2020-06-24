/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package io.gobelieve.im.demo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.widget.Toolbar;

import com.beetle.bauhinia.CustomerMessageActivity;
import com.beetle.bauhinia.PeerMessageActivity;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.EPeerMessageDB;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.message.Audio;
import com.beetle.bauhinia.db.message.File;
import com.beetle.bauhinia.db.message.GroupNotification;
import com.beetle.bauhinia.db.message.GroupVOIP;
import com.beetle.bauhinia.db.message.Image;
import com.beetle.bauhinia.db.message.Location;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.P2PSession;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.bauhinia.db.message.Secret;
import com.beetle.bauhinia.db.message.Text;
import com.beetle.bauhinia.db.message.VOIP;
import com.beetle.bauhinia.db.message.Video;
import com.beetle.im.GroupMessageObserver;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.bauhinia.activity.BaseActivity;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.beetle.im.PeerMessageObserver;
import com.beetle.im.SystemMessageObserver;

import io.gobelieve.im.demo.model.Conversation;
import io.gobelieve.im.demo.model.ConversationDB;


public class MessageListActivity extends BaseActivity implements IMServiceObserver,
        PeerMessageObserver,
        GroupMessageObserver,
        SystemMessageObserver,
        AdapterView.OnItemClickListener {
    private static final String TAG = "beetle";

    private List<Conversation> conversations;
    private ListView lv;
    protected long currentUID = 0;


    private static final long APPID = 7;
    private static final long KEFU_ID = 55;


    private BaseAdapter adapter;
    class ConversationAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return conversations.size();
        }
        @Override
        public Object getItem(int position) {
            return conversations.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ConversationView view = null;
            if (convertView == null) {
                view = new ConversationView(MessageListActivity.this);
            } else {
                view = (ConversationView)convertView;
            }
            Conversation c = conversations.get(position);
            view.setConversation(c);;
            return view;
        }
    }

    // 初始化组件
    private void initWidget() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.support_toolbar);
        setSupportActionBar(toolbar);

        lv = (ListView) findViewById(R.id.list);
        adapter = new ConversationAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "main activity create...");

        setContentView(R.layout.activity_conversation);

        Intent intent = getIntent();

        currentUID = intent.getLongExtra("current_uid", 0);
        if (currentUID == 0) {
            Log.e(TAG, "current uid is 0");
            return;
        }

        IMService im =  IMService.getInstance();
        im.addObserver(this);
        im.addPeerObserver(this);
        im.addGroupObserver(this);
        im.addSystemObserver(this);

        loadConversations();
        initWidget();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        IMService im =  IMService.getInstance();
        im.removeObserver(this);
        im.removePeerObserver(this);
        im.removeGroupObserver(this);
        im.removeSystemObserver(this);
        Log.i(TAG, "message list activity destroyed");
    }


    public  String messageContentToString(MessageContent content) {
        if (content instanceof Text) {
            return ((Text) content).text;
        } else if (content instanceof Image) {
            return "一张图片";
        } else if (content instanceof Audio) {
            return "一段语音";
        } else if (content instanceof File) {
            return "一个文件";
        } else if (content instanceof Video) {
            return "一个视频";
        } else if (content instanceof com.beetle.bauhinia.db.message.Notification) {
            return ((com.beetle.bauhinia.db.message.Notification) content).description;
        } else if (content instanceof Location) {
            return "一个地理位置";
        } else if (content instanceof GroupVOIP) {
            return ((GroupVOIP) content).description;
        } else if (content instanceof VOIP) {
            VOIP voip = (VOIP) content;
            if (voip.videoEnabled) {
                return "视频聊天";
            } else {
                return "语音聊天";
            }
        } else if (content instanceof Secret) {
            return "消息未能解密";
        } else if (content instanceof P2PSession) {
            return "";
        } else {
            return "未知的消息类型";
        }
    }

    void updateConversationDetail(Conversation conv) {
        String detail = messageContentToString(conv.message.content);
        conv.setDetail(detail);
    }

    void updatePeerConversationName(Conversation conv) {
        User u = getUser(conv.cid);
        if (TextUtils.isEmpty(u.name)) {
            conv.setName(u.identifier);
            final Conversation fconv = conv;
            asyncGetUser(conv.cid, new GetUserCallback() {
                @Override
                public void onUser(User u) {
                    fconv.setName(u.name);
                    fconv.setAvatar(u.avatarURL);
                }
            });
        } else {
            conv.setName(u.name);
        }
        conv.setAvatar(u.avatarURL);
    }

    void updateGroupConversationName(Conversation conv) {
        Group g = getGroup(conv.cid);
        if (TextUtils.isEmpty(g.name)) {
            conv.setName(g.identifier);
            final Conversation fconv = conv;
            asyncGetGroup(conv.cid, new GetGroupCallback() {
                @Override
                public void onGroup(Group g) {
                    fconv.setName(g.name);
                    fconv.setAvatar(g.avatarURL);
                }
            });
        } else {
            conv.setName(g.name);
        }
        conv.setAvatar(g.avatarURL);
    }

    void loadConversations() {
        conversations = ConversationDB.getInstance().getConversations();
        boolean customerExists = false;
        for (Conversation conv : conversations) {
            if (conv.type == Conversation.CONVERSATION_PEER) {
                IMessage msg = PeerMessageDB.getInstance().getLastMessage(conv.cid);
                conv.message = msg;

                updatePeerConversationName(conv);
                updateNotificationDesc(conv);
                updateConversationDetail(conv);
            } else if (conv.type == Conversation.CONVERSATION_PEER_SECRET) {
                IMessage msg = EPeerMessageDB.getInstance().getLastMessage(conv.cid);
                conv.message = msg;

                updatePeerConversationName(conv);
                updateNotificationDesc(conv);
                updateConversationDetail(conv);
            } else if (conv.type == Conversation.CONVERSATION_GROUP) {
                IMessage msg = GroupMessageDB.getInstance().getLastMessage(conv.cid);
                conv.message = msg;

                updateGroupConversationName(conv);
                updateNotificationDesc(conv);
                updateConversationDetail(conv);
            } else if (conv.type == Conversation.CONVERSATION_CUSTOMER_SERVICE) {
                if (conv.cid != KEFU_ID) {
                    continue;
                }
                IMessage msg = CustomerMessageDB.getInstance().getLastMessage(conv.cid);
                conv.message = msg;

                conv.setName("客服");
                updateNotificationDesc(conv);
                updateConversationDetail(conv);
                customerExists = true;
            }
        }

        if (!customerExists) {
            ICustomerMessage   msg = new ICustomerMessage();
            msg.isSupport = true;
            msg.isOutgoing = false;
            msg.customerAppID = APPID;
            msg.customerID = currentUID;
            msg.storeID = KEFU_ID;
            msg.sellerID = 0;

            msg.content = Text.newText("如果你在使用过程中有任何问题和建议，记得给我们发信反馈哦");
            msg.sender = 0;
            msg.receiver = this.currentUID;
            msg.timestamp = now();

            Conversation conv = new Conversation();
            conv.message = msg;
            conv.cid = 0;
            conv.type = Conversation.CONVERSATION_CUSTOMER_SERVICE;
            conv.setName("客服");
            updateConversationDetail(conv);
            conversations.add(conv);
        }

        Comparator<Conversation> cmp = new Comparator<Conversation>() {
            public int compare(Conversation c1, Conversation c2) {

                int t1 = 0;
                int t2 = 0;
                if (c1.message != null) {
                    t1 = c1.message.timestamp;
                }
                if (c2.message != null) {
                    t2 = c2.message.timestamp;
                }

                if (t1 > t2) {
                    return -1;
                } else if (t1 == t2) {
                    return 0;
                } else {
                    return 1;
                }

            }
        };
        Collections.sort(conversations, cmp);
    }

    public static class User {
        public long uid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }

    public static class Group {
        public long gid;
        public String name;
        public String avatarURL;

        //name为nil时，界面显示identifier字段
        public String identifier;
    }



    public interface GetUserCallback {
        void onUser(User u);
    }

    public interface GetGroupCallback {
        void onGroup(Group g);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Conversation conv = conversations.get(position);
        Log.i(TAG, "conv:" + conv.getName());

        if (conv.type == Conversation.CONVERSATION_PEER) {
            onPeerClick(conv.cid);
        } else if (conv.type == Conversation.CONVERSATION_GROUP){
            onGroupClick(conv.cid);
        } else if (conv.type == Conversation.CONVERSATION_CUSTOMER_SERVICE) {
            onCustomerServiceClick(conv);
        }
    }

    @Override
    public void onConnectState(IMService.ConnectState state) {

    }



    public Conversation findConversation(long cid, int type) {
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
            if (conv.cid == cid && conv.type == type) {
                return conv;
            }
        }
        return null;
    }

    public int findConversationPosition(long cid, int type) {
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
            if (conv.cid == cid && conv.type == type) {
                return i;
            }
        }
        return -1;
    }

    public Conversation newPeerConversation(long cid) {
        Conversation conversation = new Conversation();
        conversation.type = Conversation.CONVERSATION_PEER;
        conversation.cid = cid;

        updatePeerConversationName(conversation);
        ConversationDB.getInstance().addConversation(conversation);
        return conversation;
    }

    public Conversation newGroupConversation(long cid) {
        Conversation conversation = new Conversation();
        conversation.type = Conversation.CONVERSATION_GROUP;
        conversation.cid = cid;
        updateGroupConversationName(conversation);
        ConversationDB.getInstance().addConversation(conversation);
        return conversation;
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }


    @Override
    public void onPeerSecretMessage(IMMessage msg) {

    }

    @Override
    public void onPeerMessage(IMMessage msg) {
        Log.i(TAG, "on peer message");
        IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        long cid = 0;
        if (msg.sender == this.currentUID) {
            cid = msg.receiver;
        } else {
            cid = msg.sender;
        }

        int pos = findConversationPosition(cid, Conversation.CONVERSATION_PEER);
        Conversation conversation = null;
        if (pos == -1) {
            conversation = newPeerConversation(cid);
        } else {
            conversation = conversations.get(pos);
        }

        conversation.message = imsg;
        updateConversationDetail(conversation);

        if (pos == -1) {
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else if (pos > 0) {
            conversations.remove(pos);
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else {
            //pos == 0
        }
    }


    @Override
    public void onPeerMessageACK(IMMessage im, int error) {
        Log.i(TAG, "message ack on main");

        long msgLocalID = im.msgLocalID;
        long uid = im.receiver;
        if (msgLocalID == 0) {
            MessageContent c = IMessage.fromRaw(im.plainContent);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke r = (Revoke)c;
                int pos = -1;
                if (!im.secret) {
                    pos = findConversationPosition(uid, Conversation.CONVERSATION_PEER);
                } else {
                    pos = findConversationPosition(uid, Conversation.CONVERSATION_PEER_SECRET);
                }
                Conversation conversation = conversations.get(pos);
                if (r.msgid.equals(conversation.message.getUUID())) {
                    conversation.message.setContent(r);
                    updateNotificationDesc(conversation);
                    updateConversationDetail(conversation);
                }
            }
        }
    }

    @Override
    public void onPeerMessageFailure(IMMessage im) {

    }


    @Override
    public void onGroupMessages(List<IMMessage> msgs) {
        HashMap<Long, Integer> unreadDict = new HashMap<>();
        HashMap<Long, IMMessage> msgDict = new HashMap<>();

        for (IMMessage msg : msgs) {
            int count = 0;
            if (unreadDict.containsKey(msg.receiver)) {
                count = (Integer)unreadDict.get(msg.receiver);
            }
            if (!msg.isGroupNotification && msg.sender != this.currentUID) {
                count += 1;
            }

            unreadDict.put(msg.receiver, count);
            msgDict.put(msg.receiver, msg);
        }

        Iterator iter = msgDict.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, IMMessage> entry = (Map.Entry<Long, IMMessage>)iter.next();

            IMMessage im = entry.getValue();
            Long groupId = entry.getKey();
            int unread = unreadDict.get(groupId);
            onGroupMessage(im, unread);
        }
    }


    public void onGroupMessage(IMMessage msg, int unread) {
        Log.i(TAG, "on group message");
        IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        if (msg.isGroupNotification) {
            GroupNotification groupNotification = GroupNotification.newGroupNotification(msg.content);
            imsg.receiver = groupNotification.groupID;
            imsg.timestamp = groupNotification.timestamp;
            imsg.setContent(groupNotification);
        } else {
            imsg.setContent(msg.content);
        }

        int pos = findConversationPosition(msg.receiver, Conversation.CONVERSATION_GROUP);
        Conversation conversation = null;
        if (pos == -1) {
            conversation = newGroupConversation(msg.receiver);
        } else {
            conversation = conversations.get(pos);
        }

        conversation.message = imsg;
        updateConversationDetail(conversation);

        if (pos == -1) {
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else if (pos > 0) {
            conversations.remove(pos);
            conversations.add(0, conversation);
            adapter.notifyDataSetChanged();
        } else {
            //pos == 0
        }
    }

    @Override
    public void onGroupMessageACK(IMMessage im, int error) {
        long msgLocalID = im.msgLocalID;
        long gid = im.receiver;
        if (msgLocalID == 0) {
            MessageContent c = IMessage.fromRaw(im.content);
            if (c.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
                Revoke r = (Revoke)c;
                int pos = -1;
                pos = findConversationPosition(gid, Conversation.CONVERSATION_GROUP);
                Conversation conversation = conversations.get(pos);
                if (r.msgid.equals(conversation.message.getUUID())) {
                    conversation.message.setContent(r);
                    updateNotificationDesc(conversation);
                    updateConversationDetail(conversation);
                }
            }
        }
    }

    @Override
    public void onGroupMessageFailure(IMMessage im) {

    }


    private void updateNotificationDesc(Conversation conv) {
        final IMessage imsg = conv.message;
        if (imsg == null || imsg.content.getType() != MessageContent.MessageType.MESSAGE_GROUP_NOTIFICATION) {
            return;
        }
        long currentUID = this.currentUID;
        GroupNotification notification = (GroupNotification)imsg.content;
        if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_CREATED) {
            if (notification.master == currentUID) {
                notification.description = String.format("您创建了\"%s\"群组", notification.groupName);
            } else {
                notification.description = String.format("您加入了\"%s\"群组", notification.groupName);
            }
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_DISBAND) {
            notification.description = "群组已解散";
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED) {
            User u = getUser(notification.member);
            if (TextUtils.isEmpty(u.name)) {
                notification.description = String.format("\"%s\"加入群", u.identifier);
                final GroupNotification fnotification = notification;
                final Conversation fconv = conv;
                asyncGetUser(notification.member, new GetUserCallback() {
                    @Override
                    public void onUser(User u) {
                        fnotification.description = String.format("\"%s\"加入群", u.name);
                        if (fconv.message == imsg) {
                            fconv.setDetail(fnotification.description);
                        }
                    }
                });
            } else {
                notification.description = String.format("\"%s\"加入群", u.name);
            }
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED) {
            User u = getUser(notification.member);
            if (TextUtils.isEmpty(u.name)) {
                notification.description = String.format("\"%s\"离开群", u.identifier);
                final GroupNotification fnotification = notification;
                final Conversation fconv = conv;
                asyncGetUser(notification.member, new GetUserCallback() {
                    @Override
                    public void onUser(User u) {
                        fnotification.description = String.format("\"%s\"离开群", u.name);
                        if (fconv.message == imsg) {
                            fconv.setDetail(fnotification.description);
                        }
                    }
                });
            } else {
                notification.description = String.format("\"%s\"离开群", u.name);
            }
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED) {
            notification.description = String.format("群组改名为\"%s\"", notification.groupName);
        }
    }



    @Override
    public void onSystemMessage(String sm) {
        Log.i(TAG, "system message:" + sm);
    }

    public boolean canBack() {
        return false;
    }





    protected User getUser(long uid) {
        User u = new User();
        u.uid = uid;
        u.name = null;
        u.avatarURL = "";
        u.identifier = String.format("%d", uid);
        return u;
    }


    protected void asyncGetUser(long uid, GetUserCallback cb) {
        final long fuid = uid;
        final GetUserCallback fcb = cb;
        new AsyncTask<Void, Integer, User>() {
            @Override
            protected User doInBackground(Void... urls) {
                User u = new User();
                u.uid = fuid;
                u.name = String.format("%d", fuid);
                u.avatarURL = "";
                u.identifier = String.format("%d", fuid);
                return u;
            }
            @Override
            protected void onPostExecute(User result) {
                fcb.onUser(result);
            }
        }.execute();
    }


    protected Group getGroup(long gid) {
        Group g = new Group();
        g.gid = gid;
        g.name = null;
        g.avatarURL = "";
        g.identifier = String.format("%d", gid);
        return g;
    }


    protected void asyncGetGroup(long gid, GetGroupCallback cb) {
        final long fgid = gid;
        final GetGroupCallback fcb = cb;
        new AsyncTask<Void, Integer, Group>() {
            @Override
            protected Group doInBackground(Void... urls) {
                Group g = new Group();
                g.gid = fgid;
                g.name = String.format("%d", fgid);
                g.avatarURL = "";
                g.identifier = String.format("%d", fgid);
                return g;
            }
            @Override
            protected void onPostExecute(Group result) {
                fcb.onGroup(result);
            }
        }.execute();
    }


    protected void onPeerClick(long uid) {
        User u = getUser(uid);

        Intent intent = new Intent(this, PeerMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", uid);
        if (TextUtils.isEmpty(u.name)) {
            intent.putExtra("peer_name", u.identifier);
        } else {
            intent.putExtra("peer_name", u.name);
        }
        intent.putExtra("current_uid", this.currentUID);
        startActivity(intent);
    }


    protected void onGroupClick(long gid) {
        Log.i(TAG, "group conversation");
    }


    protected void onCustomerServiceClick(Conversation conv) {
        ICustomerMessage msg = (ICustomerMessage)conv.message;

        Intent intent = new Intent(this, CustomerMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("store_id", msg.storeID);
        intent.putExtra("seller_id", msg.sellerID);
        intent.putExtra("app_id", APPID);
        intent.putExtra("current_uid", this.currentUID);
        intent.putExtra("peer_name", "客服");
        startActivity(intent);
    }

}
