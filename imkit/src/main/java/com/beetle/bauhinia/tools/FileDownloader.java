/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;
import android.os.AsyncTask;

import android.util.Base64;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.Audio;
import com.beetle.bauhinia.db.message.Image;
import com.beetle.bauhinia.db.message.MessageContent;
import com.beetle.bauhinia.db.message.Video;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by houxh on 14-12-3.
 */
public class FileDownloader {
    public interface FileDownloaderObserver {
        public void onFileDownloadSuccess(IMessage msg);
        public void onFileDownloadFail(IMessage msg);
    }

    private static FileDownloader instance = new FileDownloader();
    public static FileDownloader getInstance() {
        return instance;
    }





    ArrayList<FileDownloaderObserver> observers = new ArrayList<FileDownloaderObserver>();

    ArrayList<IMessage> messages = new ArrayList<IMessage>();

    public void addObserver(FileDownloaderObserver ob) {
        if (observers.contains(ob)) {
            return;
        }
        observers.add(ob);
    }

    public void removeObserver(FileDownloaderObserver ob) {
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


    public void download(final IMessage imsg) {
        if (isDownloading(imsg)) {
            return;
        }

        String msgURL = null;
        if (imsg.getType() == MessageContent.MessageType.MESSAGE_AUDIO) {
            Audio audio = (Audio) imsg.content;
            msgURL = audio.url;
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
            Image image = (Image) imsg.content;
            msgURL = image.url;
        } else if (imsg.getType() == MessageContent.MessageType.MESSAGE_VIDEO) {
            Video video = (Video) imsg.content;
            msgURL = video.thumbnail;
        } else {
            return;
        }

        messages.add(imsg);
        final String url = msgURL;

        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... urls) {
                try {
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
                    FileDownloader.this.onDownloadSuccess(imsg);
                } else {
                    FileDownloader.this.onDownloadFail(imsg);
                }
            }
        }.execute();
    }

    private void onDownloadSuccess(IMessage msg) {
        for (FileDownloaderObserver ob : observers) {
            ob.onFileDownloadSuccess(msg);
        }
    }

    private void onDownloadFail(IMessage msg) {
        for (FileDownloaderObserver ob : observers) {
            ob.onFileDownloadFail(msg);
        }
    }
}
