package com.beetle.bauhinia.tools;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.types.Audio;
import com.beetle.bauhinia.api.types.Image;
import com.beetle.bauhinia.db.IMessage;

import java.io.File;
import java.util.ArrayList;

import retrofit.mime.TypedFile;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 16/1/18.
 */
public class Outbox {
    public static interface OutboxObserver {
        public void onAudioUploadSuccess(IMessage msg, String url);
        public void onAudioUploadFail(IMessage msg);
        public void onImageUploadSuccess(IMessage msg, String url);
        public void onImageUploadFail(IMessage msg);
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
        imHttp.postImages(type
                , typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Image>() {
                    @Override
                    public void call(Image image) {
                        Outbox.this.sendImageMessage(msg, image.srcUrl);
                        onUploadImageSuccess(msg, image.srcUrl);
                        messages.remove(msg);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Outbox.this.markMessageFailure(msg);
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
                        Outbox.this.sendAudioMessage(msg, audio.srcUrl);
                        onUploadAudioSuccess(msg, audio.srcUrl);
                        messages.remove(msg);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Outbox.this.markMessageFailure(msg);
                        onUploadAudioFail(msg);
                        messages.remove(msg);
                    }
                });
        return true;
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


    protected void markMessageFailure(IMessage msg) {

    }

    protected void sendImageMessage(IMessage imsg, String url) {

    }

    protected void sendAudioMessage(IMessage imsg, String url) {

    }

}
