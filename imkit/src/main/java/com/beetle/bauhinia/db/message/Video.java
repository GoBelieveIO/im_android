/*                                                                            
  Copyright (c) 2014-2019, GoBelieve     
    All rights reserved.		    				     			
 
  This source code is licensed under the BSD-style license found in the
  LICENSE file in the root directory of this source tree. An additional grant
  of patent rights can be found in the PATENTS file in the same directory.
*/


package com.beetle.bauhinia.db.message;

import com.google.gson.JsonObject;

import java.util.UUID;

public class Video  extends MessageContent{
    public String url;
    public String thumbnail;
    public int width;
    public int height;
    public int duration;

    public MessageType getType() { return MessageType.MESSAGE_VIDEO; }

    public static Video newVideo(String url, String thumbnail, int width, int height, int duration, String uuid) {
        Video video = new Video();

        JsonObject content = new JsonObject();

        JsonObject obj = new JsonObject();
        obj.addProperty("url", url);
        obj.addProperty("thumbnail", thumbnail);
        obj.addProperty("width", width);
        obj.addProperty("height", height);
        obj.addProperty("duration", duration);
        content.add(VIDEO, obj);
        content.addProperty("uuid", uuid);
        video.raw = content.toString();
        video.url = url;
        video.thumbnail = thumbnail;
        video.width = width;
        video.height = height;
        video.duration = duration;
        video.uuid = uuid;

        return video;
    }

    public static Video newVideo(String url, String thumbnail, int width, int height, int duration) {
        String uuid = UUID.randomUUID().toString();
        return newVideo(url, thumbnail, width, height, duration, uuid);
    }
}
