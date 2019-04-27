/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.tools;

import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.message.*;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;


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

        Image image = (Image)imsg.content;
        msg.content = Image.newImage(url, image.width, image.height, image.getUUID()).getRaw();
        msg.msgLocalID = imsg.msgLocalID;

        IMService im = IMService.getInstance();
        im.sendGroupMessage(msg);
    }

    @Override
    protected void sendAudioMessage(IMessage imsg, String url) {
        Audio audio = (Audio)imsg.content;

        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = Audio.newAudio(url, audio.duration, audio.getUUID()).getRaw();

        IMService im = IMService.getInstance();
        im.sendGroupMessage(msg);
    }

    @Override
    protected void sendVideoMessage(IMessage imsg, String url, String thumbURL) {
        Video video = (Video)imsg.content;

        IMMessage msg = new IMMessage();
        msg.sender = imsg.sender;
        msg.receiver = imsg.receiver;
        msg.msgLocalID = imsg.msgLocalID;
        msg.content = Video.newVideo(url, thumbURL, video.width, video.height, video.duration, video.getUUID()).getRaw();

        IMService im = IMService.getInstance();
        im.sendGroupMessage(msg);
    }



    @Override
    protected void markMessageFailure(IMessage msg) {
        GroupMessageDB.getInstance().markMessageFailure(msg.msgLocalID);
    }

    @Override
    protected void saveMessageAttachment(IMessage msg, String url) {
        if (GroupMessageDB.SQL_ENGINE_DB) {
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
            GroupMessageDB.getInstance().updateContent(msg.msgLocalID, content);
        } else {
            IMessage attachment = new IMessage();
            attachment.content = Attachment.newURLAttachment(msg.msgLocalID, url);
            attachment.sender = msg.sender;
            attachment.receiver = msg.receiver;
            saveMessage(attachment);
        }
    }

    void saveMessage(IMessage imsg) {
        GroupMessageDB.getInstance().insertMessage(imsg, imsg.receiver);
    }


}
