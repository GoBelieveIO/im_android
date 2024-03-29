/*
  Copyright (c) 2014-2019, GoBelieve
    All rights reserved.

  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.outbox;

import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.types.Audio;
import com.beetle.bauhinia.api.types.Image;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Video;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.bauhinia.tools.ImageMIME;
import com.beetle.im.IMMessage;

import java.io.File;
import java.util.ArrayList;

import retrofit.mime.TypedFile;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 16/1/18.
 */
public abstract class Outbox {

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


    public void sendMessage(IMessage imsg) {
        boolean r = true;
        if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_AUDIO) {
            com.beetle.bauhinia.db.message.Audio audio = (com.beetle.bauhinia.db.message.Audio) imsg.content;
            imsg.setUploading(true);
            if (imsg.secret) {
                uploadSecretAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
            } else {
                uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.url));
            }
        } else if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
            com.beetle.bauhinia.db.message.Image image = (com.beetle.bauhinia.db.message.Image) imsg.content;
            //prefix:"file:"
            String path = image.url.substring(5);
            imsg.setUploading(true);
            if (imsg.secret) {
                uploadSecretImage(imsg, path);
            } else {
                uploadImage(imsg, path);
            }
        } else if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_VIDEO) {
            Video video = (Video) imsg.content;
            imsg.setUploading(true);
            //prefix: "file:"
            String path = video.thumbnail.substring(5);
            String videoPath = FileCache.getInstance().getCachedFilePath(video.url);
            if (imsg.secret) {
                uploadSecretVideo(imsg, videoPath, path);
            } else {
                uploadVideo(imsg, videoPath, path);
            }
        } else if (imsg.content.getType() == MessageContent.MessageType.MESSAGE_FILE) {
            com.beetle.bauhinia.db.message.File file = (com.beetle.bauhinia.db.message.File) imsg.content;
            imsg.setUploading(true);
            String filePath = FileCache.getInstance().getCachedFilePath(file.url);
            uploadFile(imsg, filePath);
        } else {
            sendRawMessage(imsg, imsg.content.getRaw());
        }
    }


    public boolean uploadFile(final IMessage msg, final String path) {
        File file;
        try {
            file = new File(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        messages.add(msg);
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        if (TextUtils.isEmpty(type)) {
            type = "application/octet-stream";
        }
        TypedFile typedFile = new TypedFile(type, file);
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postFile(typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<com.beetle.bauhinia.api.types.File>() {
                    @Override
                    public void call(com.beetle.bauhinia.api.types.File f) {
                        String newPath = FileCache.getInstance().getCachedFilePath(f.srcUrl);
                        //避免重现下载
                        new File(path).renameTo(new File(newPath));
                        Outbox.this.saveFileURL(msg, f.srcUrl);
                        Outbox.this.sendFileMessage(msg, f.srcUrl);
                        onUploadFileSuccess(msg, f.srcUrl);
                        messages.remove(msg);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Outbox.this.markMessageFailure(msg);
                        onUploadFileFail(msg);
                        messages.remove(msg);
                    }
                });

        return true;
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
                                        String newPath = FileCache.getInstance().getCachedFilePath(f.srcUrl);
                                        //避免重现下载
                                        new File(path).renameTo(new File(newPath));
                                        Outbox.this.saveVideoURL(msg, f.srcUrl, thumbURL);
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
                        Outbox.this.saveImageURL(msg, image.srcUrl);
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

    public boolean uploadAudio(final IMessage msg, final String filePath) {
        messages.add(msg);
        String type = "audio/amr";
        TypedFile typedFile = new TypedFile(type, new File(filePath));
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.postAudios(type, typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Audio>() {
                    @Override
                    public void call(Audio audio) {
                        String newPath = FileCache.getInstance().getCachedFilePath(audio.srcUrl);
                        //避免重现下载
                        new File(filePath).renameTo(new File(newPath));
                        Outbox.this.saveAudioURL(msg, audio.srcUrl);
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


    private void onUploadFileSuccess(IMessage msg, String url) {
        for (OutboxObserver ob : observers) {
            ob.onFileUploadSuccess(msg, url);
        }
    }

    private void onUploadFileFail(IMessage msg) {
        for (OutboxObserver ob : observers) {
            ob.onFileUploadFail(msg);
        }
    }

    protected void sendImageMessage(IMessage imsg, String url) {
        com.beetle.bauhinia.db.message.Image image = (com.beetle.bauhinia.db.message.Image) imsg.content;
        com.beetle.bauhinia.db.message.Image newImage = new com.beetle.bauhinia.db.message.Image(image, url);
        sendRawMessage(imsg, newImage.getRaw());
    }


    protected void sendAudioMessage(IMessage imsg, String url) {
        com.beetle.bauhinia.db.message.Audio audio = (com.beetle.bauhinia.db.message.Audio) imsg.content;
        com.beetle.bauhinia.db.message.Audio newAudio = new com.beetle.bauhinia.db.message.Audio(audio, url);
        sendRawMessage(imsg, newAudio.getRaw());
    }


    protected void sendVideoMessage(IMessage imsg, String url, String thumbURL) {
        Video video = (Video) imsg.content;
        Video newVideo = new Video(video, url, thumbURL);
        sendRawMessage(imsg, newVideo.getRaw());
    }


    protected void sendFileMessage(IMessage imsg, String url) {
        com.beetle.bauhinia.db.message.File file = (com.beetle.bauhinia.db.message.File) imsg.content;
        com.beetle.bauhinia.db.message.File newFile = com.beetle.bauhinia.db.message.File.newFile(url, file.filename, file.size);
        newFile.generateRaw(file.getUUID(), file.getReference(), file.getGroupId());
        sendRawMessage(imsg, newFile.getRaw());
    }

    protected void saveImageURL(IMessage msg, String url) {
        String content = "";
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
            com.beetle.bauhinia.db.message.Image image = (com.beetle.bauhinia.db.message.Image) msg.content;
            com.beetle.bauhinia.db.message.Image newImage = new com.beetle.bauhinia.db.message.Image(image, url);
            content = newImage.getRaw();
        } else {
            return;
        }

        updateMessageContent(msg.msgLocalID, content);
    }

    protected void saveAudioURL(IMessage msg, String url) {
        String content = "";
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_AUDIO) {
            com.beetle.bauhinia.db.message.Audio audio = (com.beetle.bauhinia.db.message.Audio) msg.content;
            com.beetle.bauhinia.db.message.Audio newAudio = new com.beetle.bauhinia.db.message.Audio(audio, url);
            content = newAudio.getRaw();
        } else {
            return;
        }

        updateMessageContent(msg.msgLocalID, content);
    }

    protected void saveVideoURL(IMessage msg, String url, String thumbURL) {
        String content = "";
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_VIDEO) {
            Video video = (Video) msg.content;
            Video newVideo = new Video(video, url, thumbURL);
            content = newVideo.getRaw();
        } else {
            return;
        }

        updateMessageContent(msg.msgLocalID, content);
    }


    protected void saveFileURL(IMessage msg, String url) {
        String content = "";
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_FILE) {
            com.beetle.bauhinia.db.message.File file = (com.beetle.bauhinia.db.message.File) msg.content;
            com.beetle.bauhinia.db.message.File newFile = new com.beetle.bauhinia.db.message.File(file, url);
            content = newFile.getRaw();
        } else {
            return;
        }

        updateMessageContent(msg.msgLocalID, content);
    }


    protected boolean encrypt(IMMessage msg, String uuid) {
        assert (false);
        return false;
    }

    protected String encryptFile(String path, long peerUID) {
        assert (false);
        return "";
    }

    abstract protected void markMessageFailure(IMessage msg);

    abstract protected void updateMessageContent(long id, String content);

    abstract protected void sendRawMessage(IMessage imsg, String raw);


}
