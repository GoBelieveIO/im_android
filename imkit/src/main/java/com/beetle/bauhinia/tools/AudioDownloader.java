package com.beetle.bauhinia.tools;
import android.os.AsyncTask;

import com.beetle.bauhinia.db.IMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by houxh on 14-12-3.
 */
public class AudioDownloader {
    public interface AudioDownloaderObserver {
        public void onAudioDownloadSuccess(IMessage msg);
        public void onAudioDownloadFail(IMessage msg);
    }

    private static AudioDownloader instance = new AudioDownloader();
    public static AudioDownloader getInstance() {
        return instance;
    }


    ArrayList<AudioDownloaderObserver> observers = new ArrayList<AudioDownloaderObserver>();

    ArrayList<IMessage> messages = new ArrayList<IMessage>();

    public void addObserver(AudioDownloaderObserver ob) {
        if (observers.contains(ob)) {
            return;
        }
        observers.add(ob);
    }

    public void removeObserver(AudioDownloaderObserver ob) {
        observers.remove(ob);
    }

    public boolean isDownloading(IMessage msg) {
        for(IMessage m : messages) {
            if (m.sender == msg.sender &&
                m.receiver == msg.receiver &&
                m.msgLocalID == msg.msgLocalID) {
                return true;
            }
        }
        return false;
    }

    public void downloadAudio(final IMessage imsg)  throws IOException {
        if (isDownloading(imsg)) {
            return;
        }
        messages.add(imsg);

        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... urls) {
                try {
                    IMessage.Audio audio = (IMessage.Audio) imsg.content;
                    String url = audio.url;
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url).build();
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        InputStream inputStream = response.body().byteStream();
                        FileCache.getInstance().storeFile(url, inputStream);
                        inputStream.close();
                        return true;
                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                messages.remove(imsg);
                if (result) {
                    AudioDownloader.this.onDownloadSuccess(imsg);
                } else {
                    AudioDownloader.this.onDownloadFail(imsg);
                }
            }
        }.execute();
    }

    private void onDownloadSuccess(IMessage msg) {
        for (AudioDownloaderObserver ob : observers) {
            ob.onAudioDownloadSuccess(msg);
        }
    }

    private void onDownloadFail(IMessage msg) {
        for (AudioDownloaderObserver ob : observers) {
            ob.onAudioDownloadFail(msg);
        }
    }
}
