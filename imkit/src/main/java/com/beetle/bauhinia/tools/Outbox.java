/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;

import android.util.Log;
import android.webkit.MimeTypeMap;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.types.Audio;
import com.beetle.bauhinia.api.types.Image;
import com.beetle.bauhinia.db.IMessage;

import java.io.File;
import java.util.ArrayList;

import com.beetle.im.IMMessage;
import com.google.gson.JsonObject;
import retrofit.mime.TypedFile;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 16/1/18.
 */
public abstract class Outbox {
    public static interface OutboxObserver {
        public void onAudioUploadSuccess(IMessage msg, String url);
        public void onAudioUploadFail(IMessage msg);
        public void onImageUploadSuccess(IMessage msg, String url);
        public void onImageUploadFail(IMessage msg);

        public void onVideoUploadSuccess(IMessage msg, String url, String thumbURL);
        public void onVideoUploadFail(IMessage msg);
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


    public boolean uploadVideo(final IMessage msg, final String path, String thumbPath) {
        File file;
        try {
            file = new File(thumbPath);
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
                        final String thumbURL = image.srcUrl;
                        String type = "video/mp4";
                        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
                        if (extension != null) {
                            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                        }
                        TypedFile typedFile = new TypedFile(type, new File(path));
                        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
                        imHttp.postFile(typedFile)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<com.beetle.bauhinia.api.types.File>() {
                                    @Override
                                    public void call(com.beetle.bauhinia.api.types.File f) {
                                        Outbox.this.sendVideoMessage(msg, f.srcUrl, thumbURL);
                                        onUploadVideoSuccess(msg, f.srcUrl, thumbURL);
                                        messages.remove(msg);
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        Outbox.this.markMessageFailure(msg);
                                        onUploadVideoFail(msg);
                                        messages.remove(msg);
                                    }
                                });
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

    public boolean uploadSecretVideo(final IMessage msg, final String path, String thumbPath) {
        try {
            new File(thumbPath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        messages.add(msg);
        assert (msg.secret);
        String secretFile = encryptFile(thumbPath, msg.receiver);
        TypedFile typedFile = new TypedFile("", new File(secretFile));
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postFile(typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<com.beetle.bauhinia.api.types.File>() {
                    @Override
                    public void call(com.beetle.bauhinia.api.types.File thumbnail) {
                        final String thumbURL = thumbnail.srcUrl;

                        String secretVideoFile = encryptFile(path, msg.receiver);
                        TypedFile typedFile = new TypedFile("", new File(secretVideoFile));
                        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
                        imHttp.postFile(typedFile)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<com.beetle.bauhinia.api.types.File>() {
                                    @Override
                                    public void call(com.beetle.bauhinia.api.types.File f) {

                                        Outbox.this.sendVideoMessage(msg, f.srcUrl, thumbURL);
                                        onUploadVideoSuccess(msg, f.srcUrl, thumbURL);
                                        messages.remove(msg);


                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        Outbox.this.markMessageFailure(msg);
                                        onUploadVideoFail(msg);
                                        messages.remove(msg);
                                    }
                                });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Outbox.this.markMessageFailure(msg);
                        onUploadVideoFail(msg);
                        messages.remove(msg);
                    }
                });
        return true;
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


    public boolean uploadSecretImage(final IMessage msg, String filePath) {
        try {
            new File(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        messages.add(msg);
        assert (msg.secret);
        String secretFile = encryptFile(filePath, msg.receiver);
        TypedFile typedFile = new TypedFile("", new File(secretFile));
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postFile(typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<com.beetle.bauhinia.api.types.File>() {
                    @Override
                    public void call(com.beetle.bauhinia.api.types.File f) {
                        Outbox.this.sendImageMessage(msg, f.srcUrl);
                        onUploadImageSuccess(msg, f.srcUrl);
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

    public boolean uploadSecretAudio(final IMessage msg, String file) {
        messages.add(msg);
        assert (msg.secret);
        String secretFile = encryptFile(file, msg.receiver);
        TypedFile typedFile = new TypedFile("", new File(secretFile));
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postFile(typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<com.beetle.bauhinia.api.types.File>() {
                    @Override
                    public void call(com.beetle.bauhinia.api.types.File f) {
                        Outbox.this.sendAudioMessage(msg, f.srcUrl);
                        onUploadAudioSuccess(msg, f.srcUrl);
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

    private void onUploadVideoSuccess(IMessage msg, String url, String thumbURL) {
        for (OutboxObserver ob : observers) {
            ob.onVideoUploadSuccess(msg, url, thumbURL);
        }
    }

    private void onUploadVideoFail(IMessage msg) {
        for (OutboxObserver ob : observers) {
            ob.onVideoUploadFail(msg);
        }
    }


    protected boolean encrypt(IMMessage msg) {
        assert (false);
        return false;
    }

    protected String encryptFile(String path, long peerUID) {
        assert (false);
        return "";
    }

    abstract protected void markMessageFailure(IMessage msg);
    abstract protected void sendImageMessage(IMessage imsg, String url);
    abstract protected void sendAudioMessage(IMessage imsg, String url);
    abstract protected void sendVideoMessage(IMessage imsg, String url, String thumbURL);
    abstract protected void saveMessageAttachment(IMessage msg, String address);

}
