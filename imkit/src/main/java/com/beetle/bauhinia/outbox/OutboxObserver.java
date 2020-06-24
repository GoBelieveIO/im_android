package com.beetle.bauhinia.outbox;

import com.beetle.bauhinia.db.IMessage;

public interface OutboxObserver {
    public void onAudioUploadSuccess(IMessage msg, String url);
    public void onAudioUploadFail(IMessage msg);
    public void onImageUploadSuccess(IMessage msg, String url);
    public void onImageUploadFail(IMessage msg);

    public void onVideoUploadSuccess(IMessage msg, String url, String thumbURL);
    public void onVideoUploadFail(IMessage msg);

    public void onFileUploadSuccess(IMessage msg, String url);
    public void onFileUploadFail(IMessage msg);
}
