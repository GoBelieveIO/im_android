package com.beetle.bauhinia.tools;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.types.Audio;
import com.beetle.bauhinia.api.types.Image;
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
public class Outbox {

    public static interface OutboxObserver {
        public void onAudioUploadSuccess(IMessage msg, String url);
        public void onAudioUploadFail(IMessage msg);
        public void onImageUploadSuccess(IMessage msg, String url);
        public void onImageUploadFail(IMessage msg);
    }

    private static Outbox instance = new Outbox();
    public static Outbox getInstance() {
        return instance;
    }

    ArrayList<OutboxObserver> observers = new ArrayList<OutboxObserver>();
    ArrayList<IMessage> messages = new ArrayList<IMessage>();

    public void addObserver(OutboxObserver ob) {
        if (observers.contains(ob)) {
            return;
        }
        observers.add(ob);
    }

    public void removeObserver(OutboxObserver ob) {
        observers.remove(ob);
    }


    public boolean isUploading(IMessage msg) {
        for(IMessage m : messages) {
            if (m.sender == msg.sender &&
                m.receiver == msg.receiver &&
                m.msgLocalID == msg.msgLocalID) {
                return true;
            }
        }
        return false;
    }

    public boolean uploadImage(final IMessage msg, String filePath) {
        File file;
        try {
            file = new File(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        messages.add(msg);
        String type = ImageMIME.getMimeType(file);
        TypedFile typedFile = new TypedFile(type, file);
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postImages(type// + "; charset=binary"
                , typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Image>() {
                    @Override
                    public void call(Image image) {
                        Outbox.this.sendImageMessage(msg, image.srcUrl, false);
                        onUploadImageSuccess(msg, image.srcUrl);
                        messages.remove(msg);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onUploadImageFail(msg);
                        messages.remove(msg);
                    }
                });
        return true;
    }

    public boolean uploadAudio(final IMessage msg, String file) {
        messages.add(msg);
        String type = "audio/amr";
        TypedFile typedFile = new TypedFile(type, new File(file));
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postAudios(type, typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Audio>() {
                    @Override
                    public void call(Audio audio) {
                        Outbox.this.sendAudioMessage(msg, audio.srcUrl, false);
                        onUploadAudioSuccess(msg, audio.srcUrl);
                        messages.remove(msg);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onUploadAudioFail(msg);
                        messages.remove(msg);
                    }
                });
        return true;
    }

    public boolean uploadGroupImage(final IMessage msg, String filePath) {
        File file;
        try {
            file = new File(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        messages.add(msg);
        String type = ImageMIME.getMimeType(file);
        TypedFile typedFile = new TypedFile(type, file);
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postImages(type// + "; charset=binary"
                , typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Image>() {
                    @Override
                    public void call(Image image) {
                        Outbox.this.sendImageMessage(msg, image.srcUrl, true);
                        onUploadImageSuccess(msg, image.srcUrl);
                        messages.remove(msg);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onUploadImageFail(msg);
                        messages.remove(msg);
                    }
                });
        return true;
    }

    public boolean uploadGroupAudio(final IMessage msg, String file) {
        messages.add(msg);
        String type = "audio/amr";
        TypedFile typedFile = new TypedFile(type, new File(file));
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postAudios(type, typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Audio>() {
                    @Override
                    public void call(Audio audio) {
                        Outbox.this.sendAudioMessage(msg, audio.srcUrl, true);
                        onUploadAudioSuccess(msg, audio.srcUrl);
                        messages.remove(msg);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onUploadAudioFail(msg);
                        messages.remove(msg);
                    }
                });
        return true;
    }



    private void sendImageMessage(IMessage imsg, String url, boolean isGroup) {
        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.content = IMessage.newImage(url).getRaw();
        msg.msgLocalID = imsg.msgLocalID;

        IMService im = IMService.getInstance();
        if (isGroup) {
            im.sendGroupMessage(msg);
        } else {
            im.sendPeerMessage(msg);
        }
    }

    private void sendAudioMessage(IMessage imsg, String url, boolean isGroup) {
        IMessage.Audio audio = (IMessage.Audio)imsg.content;

        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = IMessage.newAudio(url, audio.duration).getRaw();

        IMService im = IMService.getInstance();
        if (isGroup) {
            im.sendGroupMessage(msg);
        } else {
            im.sendPeerMessage(msg);
        }
    }

    private void onUploadAudioSuccess(IMessage msg, String url) {
        for (OutboxObserver ob : observers) {
            ob.onAudioUploadSuccess(msg, url);
        }
    }

    private void onUploadAudioFail(IMessage msg) {
        for (OutboxObserver ob : observers) {
            ob.onAudioUploadFail(msg);
        }
    }

    private void onUploadImageSuccess(IMessage msg, String url) {
        for (OutboxObserver ob : observers) {
            ob.onImageUploadSuccess(msg, url);
        }
    }

    private void onUploadImageFail(IMessage msg) {
        for (OutboxObserver ob : observers) {
            ob.onImageUploadFail(msg);
        }
    }
}
