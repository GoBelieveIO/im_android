/*
  Copyright (c) 2014-2019, GoBelieve
    All rights reserved.

  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;
import com.beetle.bauhinia.db.CustomerMessageDB;
import com.beetle.bauhinia.db.ICustomerMessage;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.*;
import com.beetle.im.CustomerMessage;
import com.beetle.im.IMService;


/**
 * Created by houxh on 16/1/18.
 */
public class CustomerOutbox extends Outbox {
    private static CustomerOutbox instance = new CustomerOutbox();
    public static CustomerOutbox getInstance() {
        return instance;
    }

    @Override
    protected void markMessageFailure(IMessage msg) {
        CustomerMessageDB.getInstance().markMessageFailure(msg.msgLocalID);
    }

    @Override
    protected void saveImageURL(IMessage msg, String url) {
        String content = "";
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
            Image image = (Image) msg.content;
            content = Image.newImage(url, image.width, image.height, image.getUUID()).getRaw();
        } else {
            return;
        }

        CustomerMessageDB.getInstance().updateContent(msg.msgLocalID, content);
    }

    @Override
    protected void saveAudioURL(IMessage msg, String url) {
        String content = "";
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_AUDIO) {
            Audio audio = (Audio)msg.content;
            content = Audio.newAudio(url, audio.duration, audio.getUUID()).getRaw();
        } else {
            return;
        }

        CustomerMessageDB.getInstance().updateContent(msg.msgLocalID, content);
    }

    @Override
    protected void saveVideoURL(IMessage msg, String url, String thumbURL) {
        String content = "";
        if (msg.content.getType() == MessageContent.MessageType.MESSAGE_VIDEO) {
            Video video = (Video)msg.content;
            content = Video.newVideo(url, thumbURL, video.width, video.height, video.duration, video.getUUID()).getRaw();
        } else {
            return;
        }

        CustomerMessageDB.getInstance().updateContent(msg.msgLocalID, content);
    }


    @Override
    protected void sendImageMessage(IMessage imsg, String url) {

        ICustomerMessage cm = (ICustomerMessage)imsg;

        CustomerMessage msg = new CustomerMessage();
        msg.msgLocalID = imsg.msgLocalID;
        msg.customerAppID = cm.customerAppID;
        msg.customerID = cm.customerID;
        msg.storeID = cm.storeID;
        msg.sellerID = cm.sellerID;

        Image image = (Image)imsg.content;
        msg.content = Image.newImage(url, image.width, image.height, image.getUUID()).getRaw();

        IMService im = IMService.getInstance();
        im.sendCustomerMessageAsync(msg);
    }

    @Override
    protected void sendAudioMessage(IMessage imsg, String url) {
        ICustomerMessage cm = (ICustomerMessage)imsg;
        Audio audio = (Audio)imsg.content;

        CustomerMessage msg = new CustomerMessage();
        msg.msgLocalID = imsg.msgLocalID;
        msg.customerAppID = cm.customerAppID;
        msg.customerID = cm.customerID;
        msg.storeID = cm.storeID;
        msg.sellerID = cm.sellerID;

        msg.content = Audio.newAudio(url, audio.duration, audio.getUUID()).getRaw();

        IMService im = IMService.getInstance();
        im.sendCustomerMessageAsync(msg);
    }


    @Override
    protected void sendVideoMessage(IMessage imsg, String url, String thumbURL) {

        ICustomerMessage cm = (ICustomerMessage)imsg;
        Video video = (Video)imsg.content;
        CustomerMessage msg = new CustomerMessage();
        msg.msgLocalID = imsg.msgLocalID;
        msg.customerAppID = cm.customerAppID;
        msg.customerID = cm.customerID;
        msg.storeID = cm.storeID;
        msg.sellerID = cm.sellerID;
        msg.content = Video.newVideo(url, thumbURL, video.width, video.height, video.duration, video.getUUID()).getRaw();

        IMService im = IMService.getInstance();
        im.sendCustomerMessageAsync(msg);
    }

}
