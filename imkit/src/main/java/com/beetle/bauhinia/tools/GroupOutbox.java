package com.beetle.bauhinia.tools;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.types.Audio;
import com.beetle.bauhinia.api.types.Image;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;

import java.io.File;
import java.util.ArrayList;

import retrofit.mime.TypedFile;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 14-12-3.
 */
public class GroupOutbox extends Outbox{
    private static GroupOutbox instance = new GroupOutbox();
    public static GroupOutbox getInstance() {
        return instance;
    }

    @Override
    protected void sendImageMessage(IMessage imsg, String url) {
        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;

        IMessage.Image image = (IMessage.Image)imsg.content;
        msg.content = IMessage.newImage(url, image.width, image.height, image.getUUID()).getRaw();
        msg.msgLocalID = imsg.msgLocalID;

        IMService im = IMService.getInstance();
        im.sendGroupMessage(msg);
    }

    @Override
    protected void sendAudioMessage(IMessage imsg, String url) {
        IMessage.Audio audio = (IMessage.Audio)imsg.content;

        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = IMessage.newAudio(url, audio.duration, audio.getUUID()).getRaw();

        IMService im = IMService.getInstance();
        im.sendGroupMessage(msg);
    }

    @Override
    protected void markMessageFailure(IMessage msg) {
        GroupMessageDB.getInstance().markMessageFailure(msg.msgLocalID, msg.receiver);
    }

}
