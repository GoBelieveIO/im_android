/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.IMessageDB;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.bauhinia.db.message.Attachment;
import com.beetle.bauhinia.db.message.Audio;
import com.beetle.bauhinia.db.message.GroupNotification;
import com.beetle.bauhinia.db.message.GroupVOIP;
import com.beetle.bauhinia.db.message.Image;
import com.beetle.bauhinia.db.message.Location;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.bauhinia.db.message.Text;
import com.beetle.bauhinia.db.message.TimeBase;
import com.beetle.bauhinia.db.message.Video;
import com.beetle.bauhinia.tools.AudioUtil;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.FileDownloader;
import com.beetle.bauhinia.tools.TimeUtil;
import com.beetle.bauhinia.tools.VideoUtil;
import com.beetle.im.IMService;

import net.ypresto.androidtranscoder.MediaTranscoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/*
 at对象的用户名问题
 本地显示的优先级 用户备注 > 群内昵称 > 用户名

 派生类实现
 protected void checkAtName(IMessage messages)
 替换掉消息内容中at对象的用户名

 派生类重载
 protected void sendTextMessage(String text, List<Long> at, List<String> atNames)
 将有可能是本地备注的用户名修改为群内昵称或用户名

 */
public class MessageBaseActivity extends BaseActivity {
    protected static final String TAG = "imservice";

    //消息撤回的时限
    public static final int REVOKE_EXPIRE = 120;

    public static final int PAGE_SIZE = 10;

    //app 启动时间戳，app启动时初始化
    public static int uptime;
    static {
        uptime = now();
        Log.i(TAG, "uptime:" + uptime);
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }


    protected long conversationID;//uid or groupid or storeid
    protected long currentUID;
    protected int messageID;
    protected boolean hasLateMore;
    protected boolean hasEarlierMore;
    protected IMessageDB messageDB;

    protected ArrayList<IMessage> messages = new ArrayList<IMessage>();
    protected HashMap<Integer, Attachment> attachments = new HashMap<Integer, Attachment>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void loadConversationData() {
        messages = new ArrayList<IMessage>();
        List<IMessage> newMessages;
        if (messageID > 0) {
            newMessages = this.loadConversationData(conversationID, messageID);
        } else {
            newMessages = this.loadConversationData(conversationID);
        }

        //删除重复的消息,过滤掉不显示的消息
        HashSet<String> uuidSet = new HashSet<String>();
        for (int i = 0; i < newMessages.size(); i++) {
            IMessage msg = newMessages.get(i);
            if (!TextUtils.isEmpty(msg.getUUID()) && uuidSet.contains(msg.getUUID())) {
                continue;
            }
            if (msg.getType() == MessageContent.MessageType.MESSAGE_P2P_SESSION) {
                continue;
            }
            messages.add(msg);
            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
            }
        }
        int count = messages.size();

        downloadMessageContent(messages, count);
        updateNotificationDesc(messages, count);
        loadUserName(messages, count);
        checkMessageFailureFlag(messages, count);
        checkAtName(messages, count);
        resetMessageTimeBase();
    }


    protected int loadEarlierData() {
        int newCount = 0;
        if (!hasEarlierMore) {
            return newCount;
        }
        if (messages.size() == 0) {
            return newCount;
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
            return newCount;
        }

        HashSet<String> uuidSet = new HashSet<String>();
        for (int i  = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
            }
        }

        List<IMessage> newMessages = this.loadEarlierData(conversationID, firstMsg.msgLocalID);
        int count = newMessages.size();

        if (count == 0) {
            hasEarlierMore = false;
            return newCount;
        }
        for (int i = count-1; i >= 0; i--) {
            IMessage msg = newMessages.get(i);
            //重复的消息
            if (!TextUtils.isEmpty(msg.getUUID()) && uuidSet.contains(msg.getUUID())) {
                continue;
            }
            //不显示的消息
            if (msg.getType() == MessageContent.MessageType.MESSAGE_P2P_SESSION) {
                continue;
            }

            messages.add(0, msg);
            newCount++;
            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
            }
        }

        updateNotificationDesc(messages, count);
        downloadMessageContent(messages, count);
        loadUserName(messages, count);
        checkMessageFailureFlag(messages, count);
        checkAtName(messages, count);
        resetMessageTimeBase();
        return newCount;
    }

    protected int loadLateData() {
        int newCount = 0;
        if (!this.hasLateMore || messageID == 0) {
            return newCount;
        }

        if (messages.size() == 0) {
            return newCount;
        }

        HashSet<String> uuidSet = new HashSet<String>();
        for (int i  = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
            }
        }

        IMessage msg = messages.get(messages.size() - 1);
        List<IMessage> newMessages = this.loadLateData(conversationID, msg.msgLocalID);
        int count = newMessages.size();

        if (count == 0) {
            this.hasLateMore = false;
            return newCount;
        }

        for (int i = 0; i < count; i++) {
            IMessage m = newMessages.get(i);
            //重复消息
            if (!TextUtils.isEmpty(m.getUUID()) && uuidSet.contains(m.getUUID())) {
                continue;
            }
            //不需要显示的消息
            if (msg.getType() == MessageContent.MessageType.MESSAGE_P2P_SESSION) {
                continue;
            }

            downloadMessageContent(m);
            updateNotificationDesc(m);
            loadUserName(m);
            checkMessageFailureFlag(m);
            checkAtName(messages, count);
            messages.add(m);
            newCount++;
            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
            }
        }
        resetMessageTimeBase();
        return newCount;
    }


    protected ArrayList<IMessage> loadConversationData(long conversationID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = messageDB.newMessageIterator(conversationID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }
            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }

        return messages;
    }

    protected List<IMessage> loadConversationData(long conversationID, int messageID) {
        HashSet<String> uuidSet = new HashSet<String>();
        ArrayList<IMessage> messages = new ArrayList<IMessage>();

        int pageSize;
        int count = 0;
        MessageIterator iter;

        iter = messageDB.newMiddleMessageIterator(conversationID, messageID);
        pageSize = 2*PAGE_SIZE;

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

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else {
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= pageSize) {
                    break;
                }
            }
        }

        return messages;
    }

    protected List<IMessage> loadEarlierData(long conversationID, int messageID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = messageDB.newForwardMessageIterator(conversationID, messageID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(0, msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        return messages;
    }


    protected List<IMessage> loadLateData(long conversationID, int messageID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = messageDB.newBackwardMessageIterator(conversationID, messageID);
        while (true) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_ATTACHMENT) {
                Attachment attachment = (Attachment)msg.content;
                attachments.put(attachment.msg_id, attachment);
            } else{
                msg.isOutgoing = (msg.sender == currentUID);
                messages.add(msg);
                if (++count >= PAGE_SIZE) {
                    break;
                }
            }
        }
        return messages;
    }




    //加载消息发送者的名称和头像信息
    protected void loadUserName(IMessage msg) {
        MessageActivity.User u = getUser(msg.sender);

        msg.setSenderAvatar(u.avatarURL);
        if (TextUtils.isEmpty(u.name)) {
            msg.setSenderName(u.identifier);
            final IMessage fmsg = msg;
            asyncGetUser(msg.sender, new MessageActivity.GetUserCallback() {
                @Override
                public void onUser(MessageActivity.User u) {
                    fmsg.setSenderName(u.name);
                    fmsg.setSenderAvatar(u.avatarURL);
                }
            });
        } else {
            msg.setSenderName(u.name);
        }
    }

    protected void loadUserName(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            loadUserName(msg);
        }
    }

    void checkMessageFailureFlag(IMessage msg) {
        if (msg.isOutgoing) {
            if (msg.timestamp < uptime && !msg.isAck()) {
                msg.setFailure(true);
                markMessageFailure(msg);
            }
        }
    }

    void checkMessageFailureFlag(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage m = messages.get(i);
            checkMessageFailureFlag(m);
        }
    }


    protected void updateNotificationDesc(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage m = messages.get(i);
            updateNotificationDesc(m);
        }
    }

    protected void updateNotificationDesc(IMessage imsg) {
        if (imsg.getType() == MessageContent.MessageType.MESSAGE_GROUP_NOTIFICATION) {
            GroupNotification notification = (GroupNotification) imsg.content;
            if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_CREATED) {
                if (notification.master == currentUID) {
                    notification.description = String.format("您创建了\"%s\"群组", notification.groupName);
                } else {
                    notification.description = String.format("您加入了\"%s\"群组", notification.groupName);
                }
            } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_DISBAND) {
                notification.description = "群组已解散";
            } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED) {
                MessageActivity.User u = getUser(notification.member);
                if (TextUtils.isEmpty(u.name)) {
                    notification.description = String.format("\"%s\"加入群", u.identifier);
                    imsg.setDownloading(true);
                    final IMessage fmsg = imsg;
                    asyncGetUser(notification.member, new MessageActivity.GetUserCallback() {
                        @Override
                        public void onUser(MessageActivity.User u) {
                            GroupNotification notification = (GroupNotification) fmsg.content;
                            notification.description = String.format("\"%s\"加入群", u.name);
                            fmsg.setDownloading(false);
                        }
                    });
                } else {
                    notification.description = String.format("\"%s\"加入群", u.name);
                }
            } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED) {
                MessageActivity.User u = getUser(notification.member);
                if (TextUtils.isEmpty(u.name)) {
                    notification.description = String.format("\"%s\"离开群", u.identifier);
                    imsg.setDownloading(true);
                    final IMessage fmsg = imsg;
                    asyncGetUser(notification.member, new MessageActivity.GetUserCallback() {
                        @Override
                        public void onUser(MessageActivity.User u) {
                            GroupNotification notification = (GroupNotification) fmsg.content;
                            notification.description = String.format("\"%s\"离开群", u.name);
                            fmsg.setDownloading(false);
                        }
                    });
                } else {
                    notification.description = String.format("\"%s\"离开群", u.name);
                }
            } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED) {
                notification.description = String.format("群组改名为\"%s\"", notification.groupName);
            } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_NOTICE_UPDATED) {
                notification.description = String.format("群公告:%s", notification.notice);
            }
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_GROUP_VOIP) {
            GroupVOIP groupVOIP = (GroupVOIP)imsg.content;

            if (!groupVOIP.finished) {
                MessageActivity.User u = this.getUser(groupVOIP.initiator);
                String name = !TextUtils.isEmpty(u.name) ? u.name : u.identifier;
                groupVOIP.description = String.format("%s发起了语音聊天", name);
            } else {
                groupVOIP.description = "语音聊天已结束";
            }
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke)imsg.content;
            if (imsg.isOutgoing) {
                revoke.description = "你撤回了一条消息";
            } else {
                MessageActivity.User u = this.getUser(imsg.sender);
                String name = !TextUtils.isEmpty(u.name) ? u.name : u.identifier;
                revoke.description = String.format("\"%s\"撤回了一条消息", name);
            }
        }
    }


    protected void queryLocation(final IMessage msg) {
        final Location loc = (Location)msg.content;

        msg.setGeocoding(true);
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(loc.latitude, loc.longitude), 200, GeocodeSearch.AMAP);

        GeocodeSearch mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                if (i == 0 && regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null
                        && regeocodeResult.getRegeocodeAddress().getFormatAddress() != null) {
                    String address = regeocodeResult.getRegeocodeAddress().getFormatAddress();
                    Log.i(TAG, "address:" + address);
                    loc.address = address;

                    saveMessageAttachment(msg, address);
                } else {
                    // 定位失败;
                }
                msg.setGeocoding(false);
            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

            }
        });

        mGeocodeSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求
    }

    protected void downloadMessageContent(IMessage msg) {
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_AUDIO) {
            Audio audio = (Audio) msg.content;
            FileDownloader downloader = FileDownloader.getInstance();
            if (!FileCache.getInstance().isCached(audio.url) && !downloader.isDownloading(msg)) {
                downloader.download(msg);
            }
            msg.setDownloading(downloader.isDownloading(msg));
        } else if (msg.content.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
            Image image = (Image)msg.content;
            FileDownloader downloader = FileDownloader.getInstance();
            //加密的图片消息需要手动下载后解密
            if (msg.secret && !image.url.startsWith("file:") &&
                    !FileCache.getInstance().isCached(image.url) &&
                    !downloader.isDownloading(msg)) {
                downloader.download(msg);
            }
            msg.setDownloading(downloader.isDownloading(msg));
        } else if (msg.content.getType() == MessageContent.MessageType.MESSAGE_LOCATION) {
            Location loc = (Location)msg.content;
            Attachment attachment = attachments.get(msg.msgLocalID);
            if (attachment != null) {
                loc.address = attachment.address;
            }

            if (TextUtils.isEmpty(loc.address)) {
                queryLocation(msg);
            }
        } else if (msg.content.getType() == MessageContent.MessageType.MESSAGE_VIDEO) {
            Video video = (Video)msg.content;
            FileDownloader downloader = FileDownloader.getInstance();
            //加密的图片消息需要手动下载后解密
            if (msg.secret && !video.thumbnail.startsWith("file:") &&
                    !FileCache.getInstance().isCached(video.thumbnail) &&
                    !downloader.isDownloading(msg)) {
                downloader.download(msg);
            }
            msg.setDownloading(downloader.isDownloading(msg));
        }
    }

    protected void downloadMessageContent(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage msg = messages.get(i);
            downloadMessageContent(msg);
        }
    }

    protected void checkAtName(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage msg = messages.get(i);
            checkAtName(msg);
        }
    }

    protected void checkAtName(IMessage messages) {

    }

    protected void saveMessageAttachment(IMessage msg, String address) {
        this.messageDB.saveMessageAttachment(msg, address);
    }

    protected void saveMessage(IMessage imsg) {
        this.messageDB.saveMessage(imsg);
    }

    protected void removeMessage(IMessage imsg) {
        this.messageDB.removeMessage(imsg);
    }

    protected void markMessageListened(IMessage imsg) {
        this.messageDB.markMessageListened(imsg);
    }

    protected void markMessageFailure(IMessage imsg) {
        this.messageDB.markMessageFailure(imsg);
    }


    protected void eraseMessageFailure(IMessage imsg) {
        this.messageDB.eraseMessageFailure(imsg);
    }


    protected void resetMessageTimeBase() {
        ArrayList<IMessage> newMessages = new ArrayList<IMessage>();
        IMessage lastMsg = null;
        for (int i = 0; i < messages.size(); i++) {
            IMessage msg = messages.get(i);
            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_TIME_BASE) {
                continue;
            }
            //间隔10分钟，添加时间分割线
            if (lastMsg == null || msg.timestamp - lastMsg.timestamp > 10*60) {
                TimeBase timeBase = TimeBase.newTimeBase(msg.timestamp);
                String s = TimeUtil.formatTimeBase(timeBase.timestamp);
                timeBase.description = s;
                IMessage t = new IMessage();
                t.content = timeBase;
                t.timestamp = msg.timestamp;
                newMessages.add(t);
            }
            newMessages.add(msg);

            lastMsg = msg;
        }
        messages = newMessages;
    }


    protected void deleteMessage(IMessage imsg) {
        int index = -1;
        for (int i = 0; i < messages.size(); i++) {
            IMessage m = messages.get(i);
            if (m.msgLocalID == imsg.msgLocalID) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            messages.remove(index);
        }
    }

    protected void replaceMessage(IMessage imsg, IMessage other) {
        int index = -1;
        for (int i = 0; i < messages.size(); i++) {
            IMessage m = messages.get(i);
            if (m.msgLocalID == imsg.msgLocalID) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            messages.set(index, other);
        }
    }

    protected void insertMessage(IMessage imsg) {
        IMessage lastMsg = null;
        if (messages.size() > 0) {
            lastMsg = messages.get(messages.size() - 1);
        }
        //间隔10分钟，添加时间分割线
        if (lastMsg == null || imsg.timestamp - lastMsg.timestamp > 10*60) {
            TimeBase timeBase = TimeBase.newTimeBase(imsg.timestamp);
            String s = TimeUtil.formatTimeBase(timeBase.timestamp);
            timeBase.description = s;
            IMessage t = new IMessage();
            t.content = timeBase;
            t.timestamp = imsg.timestamp;
            messages.add(t);
        }

        checkAtName(imsg);
        messages.add(imsg);
    }

    protected void sendTextMessage(String text, List<Long> at, List<String> atNames) {
        if (text.length() == 0) {
            return;
        }

        sendMessageContent(Text.newText(text, at, atNames));
    }

    protected void sendVideoMessage(String path) {
        File f = new File(path);
        if (!f.exists()) {
            return;
        }

        final VideoUtil.Metadata meta = VideoUtil.getVideoMetadata(path);
        if (meta.duration < 1000) {
            Toast.makeText(this, "拍摄时间太短了", Toast.LENGTH_SHORT).show();
            return;
        }
        final int duration = meta.duration/1000;//单位秒

        Bitmap thumbnail = ThumbnailUtils.createVideoThumbnail(path,  MediaStore.Images.Thumbnails.MINI_KIND);
        if (thumbnail == null) {
            Log.w(TAG, "create video thumbnail fail");
        }
        Log.i(TAG, "thumb size:" + thumbnail.getWidth() + " " + thumbnail.getHeight());
        Log.i(TAG, "video path:" + path + " file size:" + f.length() + "video size:" + meta.width + " " + meta.height + " duration:" + meta.duration);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, os);
        try {
            String thumbURL = localImageURL();
            FileCache.getInstance().storeByteArray(thumbURL, os);

            final String thumbPath = FileCache.getInstance().getCachedFilePath(thumbURL);
            final String videoURL = localVideoURL();
            String p = FileCache.getInstance().getCachedFilePath(videoURL);
            final long startTime = SystemClock.uptimeMillis();
            MediaTranscoder.Listener listener = new MediaTranscoder.Listener() {
                @Override
                public void onTranscodeProgress(double progress) {
                    Log.i(TAG, "transcoder progress...");
                }

                @Override
                public void onTranscodeCompleted() {
                    Log.d(TAG, "transcoding took " + (SystemClock.uptimeMillis() - startTime) + "ms");

                    sendMessageContent(Video.newVideo(videoURL, "file:" + thumbPath, meta.width, meta.height, duration));
                }

                @Override
                public void onTranscodeCanceled() {
                    Log.i(TAG, "transcoder canceled");
                }

                @Override
                public void onTranscodeFailed(Exception exception) {
                    exception.printStackTrace();
                }
            };
            try {
                MediaTranscoder.getInstance().transcodeVideo(path, p, new VideoUtil.AACMediaFormat(), listener);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    protected void sendImageMessage(Bitmap bmp) {
        double w = bmp.getWidth();
        double h = bmp.getHeight();
        double newHeight = 640.0;
        double newWidth = newHeight*w/h;


        Bitmap bigBMP = Bitmap.createScaledBitmap(bmp, (int)newWidth, (int)newHeight, true);

        double sw = 256.0;
        double sh = 256.0*h/w;

        Bitmap thumbnail = Bitmap.createScaledBitmap(bmp, (int)sw, (int)sh, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bigBMP.compress(Bitmap.CompressFormat.JPEG, 100, os);
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, os2);

        String originURL = localImageURL();
        String thumbURL = localImageURL();
        try {
            FileCache.getInstance().storeByteArray(originURL, os);
            FileCache.getInstance().storeByteArray(thumbURL, os2);

            String path = FileCache.getInstance().getCachedFilePath(originURL);
            String thumbPath = FileCache.getInstance().getCachedFilePath(thumbURL);

            String tpath = path + "@256w_256h_0c";
            File f = new File(thumbPath);
            File t = new File(tpath);
            f.renameTo(t);

            sendMessageContent(Image.newImage("file:" + path, (int)newWidth, (int)newHeight));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    protected void sendAudioMessage(String tfile) {
        try {
            long mduration = AudioUtil.getAudioDuration(tfile);

            if (mduration < 1000) {
                Toast.makeText(this, "录音时间太短了", Toast.LENGTH_SHORT).show();
                return;
            }
            long duration = mduration/1000;

            String url = localAudioURL();

            Audio audio = Audio.newAudio(url, duration);
            FileInputStream is = new FileInputStream(new File(tfile));
            Log.i(TAG, "store audio url:" + audio.url);
            FileCache.getInstance().storeFile(audio.url, is);

            sendMessageContent(audio);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    protected void sendLocationMessage(float longitude, float latitude, String address) {
        Location loc = Location.newLocation(latitude, longitude);
        loc.address = address;
        sendMessageContent(loc);
    }

    protected void sendMessageContent(MessageContent content) {
        IMessage imsg = this.newOutMessage();

        imsg.setContent(content);
        imsg.timestamp = now();
        imsg.isOutgoing = true;
        saveMessage(imsg);
        loadUserName(imsg);

        if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_LOCATION) {
            Location loc = (Location)imsg.content;

            if (TextUtils.isEmpty(loc.address)) {
                queryLocation(imsg);
            } else {
                saveMessageAttachment(imsg, loc.address);
            }
        }

        sendMessage(imsg);
        insertMessage(imsg);
    }

    protected void revoke(IMessage msg) {
        if (TextUtils.isEmpty(msg.getUUID())) {
            return;
        }

        int now = now();
        if (now - msg.timestamp > REVOKE_EXPIRE) {
            Toast.makeText(this, "已经超过消息撤回的时间", Toast.LENGTH_SHORT).show();
            return;
        }

        if (IMService.getInstance().getConnectState() != IMService.ConnectState.STATE_CONNECTED) {
            Toast.makeText(this, "网络连接断开，撤回失败", Toast.LENGTH_SHORT).show();
            return;
        }

        IMessage imsg = this.newOutMessage();
        Revoke revoke = Revoke.newRevoke(msg.getUUID());
        imsg.setContent(revoke);
        imsg.timestamp = now();
        imsg.isOutgoing = true;
        sendMessage(imsg);
    }

    protected void resend(IMessage msg) {
        eraseMessageFailure(msg);
        msg.setFailure(false);
        this.sendMessage(msg);
    }

    protected IMessage newOutMessage() {
        assert(false);
        return null;
    }

    protected void sendMessage(IMessage imsg) {
        assert(false);
    }

    protected IMessage findMessage(int msgLocalID) {
        for (IMessage imsg : messages) {
            if (imsg.msgLocalID == msgLocalID) {
                return imsg;
            }
        }
        return null;
    }

    protected IMessage findMessage(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }
        for (IMessage imsg : messages) {
            if (imsg.getUUID().equals(uuid)) {
                return imsg;
            }
        }
        return null;
    }



    protected ArrayList<IMessage> getImageMessages() {
        ArrayList<IMessage> images = new ArrayList<IMessage>();

        MessageIterator iter = getMessageIterator();
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
                Image image = ((Image)msg.content);
                if (msg.secret && !image.url.startsWith("file:")) {
                    String path = FileCache.getInstance().getCachedFilePath(image.url);
                    if (path == null) {
                        //图片未下载完成或者解密失败
                        continue;
                    }
                    String url = "file:" + path;
                    msg.content = Image.newImage(url, image.width, image.height, image.getUUID());
                    images.add(msg);
                } else {
                    images.add(msg);
                }
            }
        }
        Collections.reverse(images);
        return images;
    }

    protected String localVideoURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/videos/"+ uuid.toString() + ".mp4";
    }

    protected String localImageURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/images/"+ uuid.toString() + ".png";
    }

    protected String localAudioURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/audios/" + uuid.toString() + ".amr";
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


    protected MessageIterator getMessageIterator() {
        return null;
    }
}
