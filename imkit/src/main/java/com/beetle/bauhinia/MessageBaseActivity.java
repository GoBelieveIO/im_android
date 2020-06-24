/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
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
import com.beetle.bauhinia.db.message.ACK;
import com.beetle.bauhinia.db.message.Audio;
import com.beetle.bauhinia.db.message.Image;
import com.beetle.bauhinia.db.message.Location;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Revoke;
import com.beetle.bauhinia.db.message.Text;
import com.beetle.bauhinia.db.message.TimeBase;
import com.beetle.bauhinia.db.message.Video;
import com.beetle.bauhinia.outbox.OutboxObserver;
import com.beetle.bauhinia.tools.AudioUtil;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.FileDownloader;
import com.beetle.bauhinia.tools.TimeUtil;
import com.beetle.bauhinia.tools.VideoUtil;
import com.beetle.im.IMService;
import com.beetle.im.MessageACK;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import com.beetle.imkit.R;

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
public class MessageBaseActivity extends BaseActivity implements
        FileDownloader.FileDownloaderObserver,
        OutboxObserver {
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


    protected int pageSize = PAGE_SIZE;
    protected long conversationID;//uid or groupid or storeid
    protected long currentUID;
    protected long messageID;
    protected boolean hasLateMore;
    protected boolean hasEarlierMore;
    protected IMessageDB messageDB;

    protected ArrayList<IMessage> messages = new ArrayList<IMessage>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void loadData() {
        messages = new ArrayList<IMessage>();
        List<IMessage> newMessages;
        if (messageID > 0) {
            newMessages = this.loadConversationData(messageID);
        } else {
            newMessages = this.loadConversationData();
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

        prepareMessage(messages, count);
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

        List<IMessage> newMessages = this.loadEarlierData(firstMsg.msgLocalID);
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

        prepareMessage(messages, count);
        resetMessageTimeBase();
        return newCount;
    }

    protected int loadLaterData() {
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
        List<IMessage> newMessages = this.loadLaterData(msg.msgLocalID);
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

            prepareMessage(m);
            messages.add(m);
            newCount++;
            if (!TextUtils.isEmpty(msg.getUUID())) {
                uuidSet.add(msg.getUUID());
            }
        }
        resetMessageTimeBase();
        return newCount;
    }


    private List<IMessage> loadConversationData() {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = createMessageIterator();
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            msg.isOutgoing = (msg.sender == currentUID);
            messages.add(0, msg);
            if (++count >= pageSize) {
                break;
            }
        }
        return messages;
    }

    private List<IMessage> loadConversationData(long messageID) {
        HashSet<String> uuidSet = new HashSet<String>();
        ArrayList<IMessage> messages = new ArrayList<IMessage>();

        int count = 0;
        MessageIterator iter;

        iter = createMiddleMessageIterator(messageID);

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
            msg.isOutgoing = (msg.sender == currentUID);
            messages.add(0, msg);
            if (++count >= pageSize*2) {
                break;
            }
        }

        return messages;
    }

    private List<IMessage> loadEarlierData(long messageID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = createForwardMessageIterator(messageID);
        while (iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }
            msg.isOutgoing = (msg.sender == currentUID);
            messages.add(0, msg);
            if (++count >= pageSize) {
                break;
            }
        }
        return messages;
    }

    private List<IMessage> loadLaterData(long messageID) {
        ArrayList<IMessage> messages = new ArrayList<IMessage>();
        int count = 0;
        MessageIterator iter = createBackwardMessageIterator(messageID);
        while (true) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }

            msg.isOutgoing = (msg.sender == currentUID);
            messages.add(msg);
            if (++count >= pageSize) {
                break;
            }
        }
        return messages;
    }

    //加载消息发送者的名称和头像信息
    protected void loadUserName(IMessage msg) {
        if (msg.sender == 0) {
            return;
        }

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


    void checkMessageFailureFlag(IMessage msg) {
        if (msg.isOutgoing) {
            if (msg.timestamp < uptime && !msg.isAck()) {
                msg.setFailure(true);
                markMessageFailure(msg);
            }
        }
    }

    protected void updateNotificationDesc(IMessage imsg) {
        if (imsg.getType() == MessageContent.MessageType.MESSAGE_REVOKE) {
            Revoke revoke = (Revoke)imsg.content;
            if (imsg.isOutgoing) {
                revoke.description = getString(R.string.message_revoked, getString(R.string.you));
            } else {
                MessageActivity.User u = this.getUser(imsg.sender);
                String name = !TextUtils.isEmpty(u.name) ? u.name : u.identifier;
                revoke.description = getString(R.string.message_revoked, name);
            }
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_ACK) {
            ACK ack = (ACK)imsg.content;
            if (ack.error == MessageACK.MESSAGE_ACK_NOT_YOUR_FRIEND) {
                ack.description = getString(R.string.message_not_friend);
            } else if (ack.error == MessageACK.MESSAGE_ACK_IN_YOUR_BLACKLIST) {
                ack.description = getString(R.string.message_refuesed);
            } else if (ack.error == MessageACK.MESSAGE_ACK_NOT_MY_FRIEND) {
                ack.description = getString(R.string.message_not_my_friend);
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

    protected void checkAtName(IMessage message) {

    }

    protected void sendReaded(IMessage message) {

    }

    protected void prepareMessage(ArrayList<IMessage> messages, int count) {
        for (int i = 0; i < count; i++) {
            IMessage msg = messages.get(i);
            prepareMessage(msg);
        }
    }

    protected void prepareMessage(IMessage message) {
        loadUserName(message);
        downloadMessageContent(message);
        updateNotificationDesc(message);
        checkMessageFailureFlag(message);
        checkAtName(message);
        sendReaded(message);
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


    protected void sendVideoMessage(String path, String thumbPath) {
        File f = new File(path);
        File thumbFile = new File(thumbPath);
        if (!f.exists() || !thumbFile.exists()) {
            return;
        }

        final VideoUtil.Metadata meta = VideoUtil.getVideoMetadata(path);
        Log.i(TAG, "video mime:" + meta.videoMime + " audio mime:" + meta.audioMime);

        if (meta.duration < 1000) {
            Toast.makeText(this, getString(R.string.video_record_duration_warning), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(meta.videoMime) && !VideoUtil.isH264(meta.videoMime)) {
            Toast.makeText(this, getString(R.string.unsupported_video_encoding_warning), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(meta.audioMime) && !VideoUtil.isAcc(meta.audioMime)) {
            Toast.makeText(this, getString(R.string.unsupported_video_encoding_warning), Toast.LENGTH_SHORT).show();
            return;
        }

        final int duration = meta.duration/1000;//单位秒
        Log.i(TAG, "video path:" + path + " file size:" + f.length() + "video size:" + meta.width + " " + meta.height + " duration:" + meta.duration);

        try {
            String thumbURL = localImageURL();
            FileCache.getInstance().moveFile(thumbURL, thumbPath);
            String p1 = FileCache.getInstance().getCachedFilePath(thumbURL);

            final String videoURL = localVideoURL();
            FileCache.getInstance().moveFile(videoURL, path);

            sendMessageContent(Video.newVideo(videoURL, "file:" + p1, meta.width, meta.height, duration));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void sendImageMessage(Bitmap bmp) {
        //https://www.jianshu.com/p/5b77da571a5c
        double w = bmp.getWidth();
        double h = bmp.getHeight();
        double rate = w > h ? w/h : h/w;
        int scalePolicy = -1;// 0 origin, 1 max 1280, 2  min 800
        if (w <= 1280 && h <= 1280) {
            scalePolicy = 0;
        } else if (w > 1280) {
            if (rate <= 2) {
                //max 1280
                scalePolicy = 1;
            } else {
                if (h <= 1280){
                    scalePolicy = 0;
                } else if (h > 1280) {
                    //min 800
                    scalePolicy = 2;
                }
            }
        } else if (h > 1280) {
            if (rate <= 2) {
                //max 1280
                scalePolicy = 1;
            } else {
                //w <= 1280
                scalePolicy = 0;
            }
        }

        double newHeight = 0;
        double newWidth = 0;
        Bitmap bigBMP;
        if (scalePolicy == 0) {
            bigBMP = bmp;
            newWidth = bmp.getWidth();
            newHeight = bmp.getHeight();
        } else if (scalePolicy == 1) {
            if (w > h) {
                newWidth = 1280;
                newHeight = 1280/rate;
            } else {
                newHeight = 1280;
                newWidth = 1280/rate;
            }
            bigBMP = Bitmap.createScaledBitmap(bmp, (int)newWidth, (int)newHeight, true);
        } else if (scalePolicy == 2) {
            if (w > h) {
                newWidth = 800*rate;
                newHeight = 800;
            } else {
                newHeight = 800*rate;
                newWidth = 800;
            }
            bigBMP = Bitmap.createScaledBitmap(bmp, (int)newWidth, (int)newHeight, true);
        } else {
            Log.w(TAG, "invalid scale policy, width:" + w + " height:" + h);
            bigBMP = bmp;
            newWidth = bmp.getWidth();
            newHeight = bmp.getHeight();
        }

        double sw = 256.0;
        double sh = 256.0*h/w;

        Bitmap thumbnail = Bitmap.createScaledBitmap(bmp, (int)sw, (int)sh, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bigBMP.compress(Bitmap.CompressFormat.JPEG, 50, os);
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 50, os2);

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
                Toast.makeText(this, getString(R.string.voice_record_duration_warning), Toast.LENGTH_SHORT).show();
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

    protected void sendFileMessage(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            Pair<String, Long> fileInfo = getFileInfo(uri);

            String filename = fileInfo.first;
            long fileSize = fileInfo.second;
            if (TextUtils.isEmpty(filename)) {
                Log.i(TAG, "can't get filename");
                return;
            }
            String ext = "";
            int index = filename.lastIndexOf(".");
            if (index != -1) {
                ext = filename.substring(index);
            }

            final String fileURL = localFileURL(ext);

            FileCache.getInstance().storeFile(fileURL, in);

            File f = new File(FileCache.getInstance().getCachedFilePath(fileURL));

            int size = (int)(f.length());

            Log.i(TAG, "file size:" + size + " filename:" + filename);

            sendMessageContent(com.beetle.bauhinia.db.message.File.newFile(fileURL, filename, size));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Pair<String, Long> getFileInfo(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor.getCount() <= 0) {
            cursor.close();
            return null;
        }


        cursor.moveToFirst();

        String fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));

        long size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE));

        cursor.close();

        Pair<String, Long> pair = Pair.create(fileName, size);
        return pair;
    }

    protected void sendMessageContent(MessageContent content) {
        IMessage imsg = this.newOutMessage(content);

        imsg.setContent(content);
        imsg.timestamp = now();
        imsg.isOutgoing = true;
        saveMessage(imsg);
        loadUserName(imsg);

        if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_LOCATION) {
            Location loc = (Location)imsg.content;

            if (TextUtils.isEmpty(loc.address)) {
                queryLocation(imsg);
            }
        }

        sendMessage(imsg);
        insertMessage(imsg);
    }


    @Override
    public void onAudioUploadSuccess(IMessage imsg, String url) {
        Log.i(TAG, "audio upload success:" + url);
        IMessage m = findMessage(imsg.content.getUUID());
        if (m != null) {
            Audio audio = (Audio)m.content;
            Audio newAudio = Audio.newAudio(url, audio.duration);
            newAudio.generateRaw(audio.getUUID(), audio.getReference(), audio.getGroupId());
            m.content = newAudio;
            m.setUploading(false);
        }
    }

    @Override
    public void onAudioUploadFail(IMessage msg) {
        Log.i(TAG, "audio upload fail");
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }
    }

    @Override
    public void onImageUploadSuccess(IMessage msg, String url) {
        Log.i(TAG, "image upload success:" + url);
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            Image image = (Image)m.content;
            Image newImage = Image.newImage(url, image.width, image.height);
            newImage.generateRaw(image.getUUID(), image.getReference(), image.getGroupId());
            m.content = newImage;
            m.setUploading(false);
        }
    }

    @Override
    public void onImageUploadFail(IMessage msg) {
        Log.i(TAG, "image upload fail");
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }
    }

    @Override
    public void onVideoUploadSuccess(IMessage msg, String url, String thumbURL) {
        Log.i(TAG, "video upload success:" + url + " thumb url:" + thumbURL);
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            Video video = (Video)m.content;
            Video newVideo = Video.newVideo(url, thumbURL, video.width, video.height, video.duration);
            newVideo.generateRaw(video.getUUID(), video.getReference(), video.getGroupId());
            m.content = newVideo;
            m.setUploading(false);
        }
    }

    @Override
    public void onVideoUploadFail(IMessage msg) {
        Log.i(TAG, "video upload fail");
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }
    }

    @Override
    public void onFileUploadSuccess(IMessage msg, String url) {
        Log.i(TAG, "file upload success:" + url);
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            com.beetle.bauhinia.db.message.File file = (com.beetle.bauhinia.db.message.File)m.content;
            com.beetle.bauhinia.db.message.File newFile = com.beetle.bauhinia.db.message.File.newFile(url, file.filename, file.size);
            newFile.generateRaw(file.getUUID(), file.getReference(), file.getGroupId());
            m.content = newFile;
            m.setUploading(false);
        }
    }

    @Override
    public void onFileUploadFail(IMessage msg) {
        Log.i(TAG, "file upload fail");
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            m.setFailure(true);
            m.setUploading(false);
        }
    }

    @Override
    public void onFileDownloadSuccess(IMessage msg) {
        Log.i(TAG, "audio download success");
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            m.setDownloading(false);
        }
    }

    @Override
    public void onFileDownloadFail(IMessage msg) {
        Log.i(TAG, "audio download fail");
        IMessage m = findMessage(msg.content.getUUID());
        if (m != null) {
            m.setDownloading(false);
        }
    }

    protected void revoke(IMessage msg) {
        if (TextUtils.isEmpty(msg.getUUID())) {
            return;
        }

        int now = now();
        if (now - msg.timestamp > REVOKE_EXPIRE) {
            Toast.makeText(this, getString(R.string.revoke_timed_out), Toast.LENGTH_SHORT).show();
            return;
        }

        if (IMService.getInstance().getConnectState() != IMService.ConnectState.STATE_CONNECTED) {
            Toast.makeText(this, getString(R.string.revoke_connection_disconnect), Toast.LENGTH_SHORT).show();
            return;
        }

        Revoke revoke = Revoke.newRevoke(msg.getUUID());
        IMessage imsg = this.newOutMessage(revoke);
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


    protected IMessage newOutMessage(MessageContent content) {
        assert(false);
        return null;
    }

    protected void sendMessage(IMessage imsg) {
        assert(false);
    }

    protected IMessage findMessage(long msgLocalID) {
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
                    Image newImage = Image.newImage(url, image.width, image.height);
                    newImage.generateRaw(image.getUUID(), image.getReference(), image.getGroupId());
                    msg.content = newImage;

                    images.add(msg);
                } else {
                    images.add(msg);
                }
            }
        }
        Collections.reverse(images);
        return images;
    }

    protected String localFileURL(String ext) {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/videos/"+ uuid.toString() + ext;
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

    protected MessageIterator createMessageIterator() {
        MessageIterator iter = messageDB.newMessageIterator(conversationID);
        return iter;
    }

    protected MessageIterator createForwardMessageIterator(long messageID) {
        MessageIterator iter = messageDB.newForwardMessageIterator(conversationID, messageID);
        return iter;
    }

    protected MessageIterator createBackwardMessageIterator(long messageID) {
        MessageIterator iter = messageDB.newBackwardMessageIterator(conversationID, messageID);
        return iter;
    }

    protected MessageIterator createMiddleMessageIterator(long messageID) {
        MessageIterator iter = messageDB.newMiddleMessageIterator(conversationID, messageID);
        return iter;
    }
}
