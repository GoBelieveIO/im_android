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
import com.beetle.im.IMMessage;
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
    protected void saveMessageAttachment(IMessage msg, String url) {
        if (CustomerMessageDB.SQL_ENGINE_DB) {
            String content = "";
            if (msg.content.getType() == MessageContent.MessageType.MESSAGE_AUDIO) {
                Audio audio = (Audio)msg.content;
                content = Audio.newAudio(url, audio.duration, audio.getUUID()).getRaw();
            } else if (msg.content.getType() == MessageContent.MessageType.MESSAGE_IMAGE) {
                Image image = (Image) msg.content;
                content = Image.newImage(url, image.width, image.height, image.getUUID()).getRaw();
            } else {
                return;
            }

            CustomerMessageDB.getInstance().updateContent(msg.msgLocalID, content);
        } else {
            ICustomerMessage attachment = new ICustomerMessage();
            attachment.content = Attachment.newURLAttachment(msg.msgLocalID, url);
            attachment.sender = msg.sender;
            attachment.receiver = msg.receiver;
            saveMessage(attachment);
        }
    }

    void saveMessage(IMessage imsg) {
        ICustomerMessage m = (ICustomerMessage)imsg;
        CustomerMessageDB.getInstance().insertMessage(imsg);
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
        im.sendCustomerMessage(msg);
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
        im.sendCustomerMessage(msg);
    }


    @Override
    protected void sendVideoMessage(IMessage imsg, String url, String thumbURL) {

        ICustomerMessage cm = (ICustomerMessage)imsg;
        Video video = (Video)imsg.content;



        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = Video.newVideo(url, thumbURL, video.width, video.height, video.duration, video.getUUID()).getRaw();

        IMService im = IMService.getInstance();
        im.sendGroupMessage(msg);
    }

}
